using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using GymLeagueAPI.Data;
using GymLeagueAPI.Models;

namespace GymLeagueAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class ChallengesController : ControllerBase
    {
        private readonly GymLeagueDbContext _context;

        public ChallengesController(GymLeagueDbContext context)
        {
            _context = context;
        }

        // GET: api/challenges
        [HttpGet]
        public async Task<ActionResult<IEnumerable<Challenge>>> GetChallenges()
        {
            return await _context.Challenges
                .OrderBy(c => c.EndDate)
                .ToListAsync();
        }

        // GET: api/challenges/5
        [HttpGet("{id}")]
        public async Task<ActionResult<Challenge>> GetChallenge(int id)
        {
            var challenge = await _context.Challenges.FindAsync(id);

            if (challenge == null)
            {
                return NotFound();
            }

            return challenge;
        }

        // POST: api/challenges
        [HttpPost]
        public async Task<ActionResult<Challenge>> CreateChallenge(Challenge challenge)
        {
            var userExists = await _context.Users
                .AnyAsync(u => u.UserId == challenge.CreatedByUserId);

            if (!userExists)
            {
                return BadRequest("User does not exist.");
            }

            challenge.CurrentProgress = 0;
            challenge.Completed = false;
            challenge.CreatedAt = DateTime.UtcNow;

            _context.Challenges.Add(challenge);
            await _context.SaveChangesAsync();

            return CreatedAtAction(nameof(GetChallenge), new { id = challenge.ChallengeId }, challenge);
        }

        // POST: api/challenges/5/progress
        [HttpPost("{id}/progress")]
        public async Task<ActionResult<Challenge>> LogProgress(int id)
        {
            var challenge = await _context.Challenges.FindAsync(id);

            if (challenge == null)
            {
                return NotFound();
            }

            if (!challenge.Completed)
            {
                challenge.CurrentProgress += 1;

                if (challenge.CurrentProgress >= challenge.Target)
                {
                    challenge.CurrentProgress = challenge.Target;
                    challenge.Completed = true;
                }

                await _context.SaveChangesAsync();
            }

            return challenge;
        }

        // DELETE: api/challenges/5
        [HttpDelete("{id}")]
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
    }
}