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
            return await _context.Squads
                .Include(s => s.Members)
                    .ThenInclude(m => m.User)
                .ToListAsync();
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

            return squad;
        }

        // POST: api/squads
        [HttpPost]
        public async Task<ActionResult<Squad>> CreateSquad(Squad squad)
        {
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
            member.Squad = null;
            member.User = null;

            _context.SquadMembers.Add(member);
            await _context.SaveChangesAsync();

            return Ok(member);
        }

        // DELETE: api/squads/5/members/3
        [HttpDelete("{squadId}/members/{userId}")]
        public async Task<IActionResult> RemoveMember(int squadId, int userId)
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
    }
}