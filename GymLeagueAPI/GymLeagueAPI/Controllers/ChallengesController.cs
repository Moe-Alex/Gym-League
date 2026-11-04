using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using GymLeagueAPI.Data;
using GymLeagueAPI.Models;
using GymLeagueAPI.Models.Dtos;
using GymLeagueAPI.Services;

namespace GymLeagueAPI.Controllers
{
    /// <summary>
    /// Battle Arena: head-to-head challenges between two users, with accept/decline,
    /// per-person progress, a winner, XP rewards and push notifications.
    /// </summary>
    [ApiController]
    [Route("api/[controller]")]
    public class ChallengesController : ControllerBase
    {
        private const int MaxXpReward = 500;

        private readonly GymLeagueDbContext _context;
        private readonly IPushNotificationService _push;
        private readonly ILogger<ChallengesController> _logger;

        public ChallengesController(
            GymLeagueDbContext context,
            IPushNotificationService push,
            ILogger<ChallengesController> logger)
        {
            _context = context;
            _push = push;
            _logger = logger;
        }

        // GET: api/challenges            (everything)
        // GET: api/challenges?userId=5   (my challenges plus open challenges I could accept)
        [HttpGet]
        public async Task<ActionResult<IEnumerable<ChallengeDto>>> GetChallenges([FromQuery] int? userId = null)
        {
            var query = _context.Challenges.AsQueryable();

            if (userId.HasValue)
            {
                var uid = userId.Value;

                query = query.Where(c =>
                    c.CreatedByUserId == uid ||
                    c.OpponentUserId == uid ||
                    c.Status == ChallengeStatus.Open);
            }

            var challenges = await query
                .OrderBy(c => c.EndDate)
                .ToListAsync();

            return await ToDtos(challenges);
        }

        // GET: api/challenges/5
        [HttpGet("{id:int}")]
        public async Task<ActionResult<ChallengeDto>> GetChallenge(int id)
        {
            var challenge = await _context.Challenges.FindAsync(id);

            if (challenge == null)
            {
                return NotFound();
            }

            return (await ToDtos(new List<Challenge> { challenge })).Single();
        }

        // POST: api/challenges
        [HttpPost]
        public async Task<ActionResult<ChallengeDto>> CreateChallenge(CreateChallengeRequest request)
        {
            if (string.IsNullOrWhiteSpace(request.Title))
            {
                return BadRequest("A title is required.");
            }

            if (request.Target <= 0)
            {
                return BadRequest("The target must be greater than zero.");
            }

            if (request.XpReward <= 0 || request.XpReward > MaxXpReward)
            {
                return BadRequest($"The XP reward must be between 1 and {MaxXpReward}.");
            }

            if (ChallengeRules.HasEnded(new Challenge { EndDate = request.EndDate }, DateTime.UtcNow))
            {
                return BadRequest("The end date must be today or later.");
            }

            var creator = await _context.Users.FindAsync(request.CreatedByUserId);

            if (creator == null)
            {
                return BadRequest("User does not exist.");
            }

            User? opponent = null;

            if (request.OpponentUserId.HasValue)
            {
                if (request.OpponentUserId.Value == request.CreatedByUserId)
                {
                    return BadRequest("You cannot challenge yourself.");
                }

                opponent = await _context.Users.FindAsync(request.OpponentUserId.Value);

                if (opponent == null)
                {
                    return BadRequest("The opponent does not exist.");
                }
            }

            var challenge = new Challenge
            {
                Title = request.Title.Trim(),
                Description = request.Description?.Trim() ?? string.Empty,
                Type = request.Type,
                Target = request.Target,
                XpReward = request.XpReward,
                EndDate = request.EndDate,
                CreatedByUserId = request.CreatedByUserId,
                OpponentUserId = opponent?.UserId,
                Status = opponent == null ? ChallengeStatus.Open : ChallengeStatus.Pending,
                CreatedAt = DateTime.UtcNow
            };

            _context.Challenges.Add(challenge);
            await _context.SaveChangesAsync();

            _logger.LogInformation(
                "Challenge {ChallengeId} '{Title}' created by user {UserId} ({Status}).",
                challenge.ChallengeId,
                challenge.Title,
                creator.UserId,
                challenge.Status);

            if (opponent != null)
            {
                await _push.SendToUserAsync(
                    opponent.UserId,
                    $"{creator.Username} challenged you!",
                    $"{challenge.Title}: first to {challenge.Target} wins +{challenge.XpReward} XP.",
                    PushData("challenge_invite", challenge.ChallengeId));
            }

            var dto = (await ToDtos(new List<Challenge> { challenge })).Single();

            return CreatedAtAction(nameof(GetChallenge), new { id = challenge.ChallengeId }, dto);
        }

        // POST: api/challenges/5/accept?userId=7
        [HttpPost("{id:int}/accept")]
        public async Task<ActionResult<ChallengeDto>> AcceptChallenge(int id, [FromQuery] int userId)
        {
            var challenge = await _context.Challenges.FindAsync(id);

            if (challenge == null)
            {
                return NotFound();
            }

            var user = await _context.Users.FindAsync(userId);

            if (user == null)
            {
                return BadRequest("User does not exist.");
            }

            var error = ChallengeRules.TryAccept(challenge, userId, DateTime.UtcNow);

            if (error != null)
            {
                return BadRequest(error);
            }

            await _context.SaveChangesAsync();

            _logger.LogInformation("User {UserId} accepted challenge {ChallengeId}.", userId, id);

            await _push.SendToUserAsync(
                challenge.CreatedByUserId,
                $"{user.Username} accepted your challenge!",
                $"{challenge.Title} is live. First to {challenge.Target} wins.",
                PushData("challenge_accepted", challenge.ChallengeId));

            return (await ToDtos(new List<Challenge> { challenge })).Single();
        }

        // POST: api/challenges/5/decline?userId=7
        [HttpPost("{id:int}/decline")]
        public async Task<ActionResult<ChallengeDto>> DeclineChallenge(int id, [FromQuery] int userId)
        {
            var challenge = await _context.Challenges.FindAsync(id);

            if (challenge == null)
            {
                return NotFound();
            }

            var user = await _context.Users.FindAsync(userId);

            if (user == null)
            {
                return BadRequest("User does not exist.");
            }

            var error = ChallengeRules.TryDecline(challenge, userId);

            if (error != null)
            {
                return BadRequest(error);
            }

            await _context.SaveChangesAsync();

            _logger.LogInformation("User {UserId} declined challenge {ChallengeId}.", userId, id);

            await _push.SendToUserAsync(
                challenge.CreatedByUserId,
                $"{user.Username} declined your challenge",
                $"{challenge.Title} was turned down.",
                PushData("challenge_declined", challenge.ChallengeId));

            return (await ToDtos(new List<Challenge> { challenge })).Single();
        }

        // POST: api/challenges/5/progress?userId=7
        // Logs one point of progress for that user. Whoever reaches the target first wins,
        // receives the XP reward and the other person is told they lost.
        [HttpPost("{id:int}/progress")]
        public async Task<ActionResult<ChallengeDto>> LogProgress(int id, [FromQuery] int userId)
        {
            var challenge = await _context.Challenges.FindAsync(id);

            if (challenge == null)
            {
                return NotFound();
            }

            var user = await _context.Users.FindAsync(userId);

            if (user == null)
            {
                return BadRequest("User does not exist.");
            }

            var error = ChallengeRules.TryLogProgress(challenge, userId, DateTime.UtcNow, out var completedNow);

            if (error != null)
            {
                return BadRequest(error);
            }

            if (completedNow)
            {
                user.XP += challenge.XpReward;
                user.Level = LevelCalculator.LevelForXp(user.XP);
            }

            await _context.SaveChangesAsync();

            if (completedNow)
            {
                _logger.LogInformation(
                    "User {UserId} won challenge {ChallengeId} and earned {Xp} XP.",
                    userId,
                    id,
                    challenge.XpReward);

                var loserId = challenge.CreatedByUserId == userId
                    ? challenge.OpponentUserId
                    : challenge.CreatedByUserId;

                if (loserId.HasValue)
                {
                    await _push.SendToUserAsync(
                        loserId.Value,
                        $"{user.Username} won the battle",
                        $"{challenge.Title} is over. Challenge them again!",
                        PushData("challenge_completed", challenge.ChallengeId));
                }
            }

            return (await ToDtos(new List<Challenge> { challenge })).Single();
        }

        // DELETE: api/challenges/5
        [HttpDelete("{id:int}")]
        public async Task<IActionResult> DeleteChallenge(int id)
        {
            var challenge = await _context.Challenges.FindAsync(id);
            if (challenge == null)
            {
                return NotFound();
            }

            _context.Challenges.Remove(challenge);
            await _context.SaveChangesAsync();

            return NoContent();
        }

        // Extra data sent with every push so the app knows what happened
        private static Dictionary<string, string> PushData(string type, int challengeId)
        {
            return new Dictionary<string, string>
            {
                ["type"] = type,
                ["challengeId"] = challengeId.ToString()
            };
        }

        // Adds the creator, opponent and winner usernames to each challenge
        private async Task<List<ChallengeDto>> ToDtos(List<Challenge> challenges)
        {
            var userIds = challenges
                .SelectMany(c => new int?[] { c.CreatedByUserId, c.OpponentUserId, c.WinnerUserId })
                .Where(id => id.HasValue)
                .Select(id => id!.Value)
                .Distinct()
                .ToList();

            var usernames = await _context.Users
                .Where(u => userIds.Contains(u.UserId))
                .ToDictionaryAsync(u => u.UserId, u => u.Username);

            return challenges
                .Select(c => ChallengeDto.From(c, usernames))
                .ToList();
        }
    }
}
