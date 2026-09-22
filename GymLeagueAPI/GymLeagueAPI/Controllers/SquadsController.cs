using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using GymLeagueAPI.Data;
using GymLeagueAPI.Models;

namespace GymLeagueAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class SquadsController : ControllerBase
    {
        private readonly GymLeagueDbContext _context;

        public SquadsController(GymLeagueDbContext context)
        {
            _context = context;
        }

        // GET: api/squads
        [HttpGet]
        public async Task<ActionResult<IEnumerable<Squad>>> GetSquads()
        {
            var squads = await _context.Squads
                .Include(s => s.Members)
                    .ThenInclude(m => m.User)
                .ToListAsync();

            foreach (var squad in squads)
            {
                await CalculateSquadStreaks(squad);
            }

            await _context.SaveChangesAsync();

            return squads;
        }

        // GET: api/squads/5
        [HttpGet("{id}")]
        public async Task<ActionResult<Squad>> GetSquad(int id)
        {
            var squad = await _context.Squads
                .Include(s => s.Members)
                    .ThenInclude(m => m.User)
                .FirstOrDefaultAsync(s => s.SquadId == id);

            if (squad == null)
            {
                return NotFound();
            }

            await CalculateSquadStreaks(squad);

            await _context.SaveChangesAsync();

            return squad;
        }

        // POST: api/squads
        [HttpPost]
        public async Task<ActionResult<Squad>> CreateSquad(Squad squad)
        {
            squad.SharedStreak = 0;
            squad.CreatedAt = DateTime.UtcNow;

            _context.Squads.Add(squad);
            await _context.SaveChangesAsync();

            return CreatedAtAction(
                nameof(GetSquad),
                new { id = squad.SquadId },
                squad
            );
        }

        // POST: api/squads/5/members
        [HttpPost("{squadId}/members")]
        public async Task<ActionResult<SquadMember>> AddMember(
            int squadId,
            SquadMember member)
        {
            var squadExists =
                await _context.Squads.AnyAsync(
                    s => s.SquadId == squadId
                );

            if (!squadExists)
            {
                return NotFound("Squad not found.");
            }

            var userExists =
                await _context.Users.AnyAsync(
                    u => u.UserId == member.UserId
                );

            if (!userExists)
            {
                return BadRequest("User not found.");
            }

            var alreadyMember =
                await _context.SquadMembers.AnyAsync(
                    sm =>
                        sm.SquadId == squadId &&
                        sm.UserId == member.UserId
                );

            if (alreadyMember)
            {
                return BadRequest(
                    "User is already a member of this squad."
                );
            }

            member.SquadId = squadId;
            member.CurrentStreak = 0;
            member.JoinedAt = DateTime.UtcNow;

            member.Squad = null;
            member.User = null;

            _context.SquadMembers.Add(member);

            await _context.SaveChangesAsync();

            return Ok(member);
        }

        // DELETE: api/squads/5/members/3
        [HttpDelete("{squadId}/members/{userId}")]
        public async Task<IActionResult> RemoveMember(
            int squadId,
            int userId)
        {
            var member = await _context.SquadMembers
                .FirstOrDefaultAsync(sm =>
                    sm.SquadId == squadId &&
                    sm.UserId == userId);

            if (member == null)
            {
                return NotFound("Membership not found.");
            }

            _context.SquadMembers.Remove(member);

            await _context.SaveChangesAsync();

            return NoContent();
        }

        // -------------------------------------------------
        // STREAK CALCULATION
        // -------------------------------------------------

        private async Task CalculateSquadStreaks(Squad squad)
        {
            if (squad.Members.Count == 0)
            {
                squad.SharedStreak = 0;
                return;
            }

            var allMemberWorkoutDates =
                new List<HashSet<DateTime>>();

            foreach (var member in squad.Members)
            {
                // Get all unique days on which this user
                // completed at least one workout.
                var completedWorkoutDates =
                    await _context.Workouts
                        .Where(w =>
                            w.UserId == member.UserId &&
                            w.Completed)
                        .Select(w => w.WorkoutDate.Date)
                        .Distinct()
                        .ToListAsync();

                var workoutDateSet =
                    completedWorkoutDates.ToHashSet();

                // Calculate this member's individual streak.
                member.CurrentStreak =
                    CalculateCurrentStreak(workoutDateSet);

                allMemberWorkoutDates.Add(workoutDateSet);
            }

            // Find dates where EVERY squad member trained.
            var sharedDates =
                new HashSet<DateTime>(
                    allMemberWorkoutDates.First()
                );

            foreach (var memberDates
                     in allMemberWorkoutDates.Skip(1))
            {
                sharedDates.IntersectWith(memberDates);
            }

            // Shared streak = consecutive days where
            // every current squad member trained.
            squad.SharedStreak =
                CalculateCurrentStreak(sharedDates);
        }

        private int CalculateCurrentStreak(
            HashSet<DateTime> workoutDates)
        {
            if (workoutDates.Count == 0)
            {
                return 0;
            }

            var today = DateTime.UtcNow.Date;
            var yesterday = today.AddDays(-1);

            DateTime currentDate;

            // If the user trained today, count backwards
            // starting from today.
            if (workoutDates.Contains(today))
            {
                currentDate = today;
            }
            // If they haven't trained today yet but trained
            // yesterday, preserve the streak.
            else if (workoutDates.Contains(yesterday))
            {
                currentDate = yesterday;
            }
            else
            {
                return 0;
            }

            var streak = 0;

            while (workoutDates.Contains(currentDate))
            {
                streak++;
                currentDate =
                    currentDate.AddDays(-1);
            }

            return streak;
        }
    }
}