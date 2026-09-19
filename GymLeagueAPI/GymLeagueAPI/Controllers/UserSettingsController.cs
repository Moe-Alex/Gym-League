using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using GymLeagueAPI.Data;
using GymLeagueAPI.Models;

namespace GymLeagueAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class UserSettingsController : ControllerBase
    {
        private readonly GymLeagueDbContext _context;

        public UserSettingsController(GymLeagueDbContext context)
        {
            _context = context;
        }

        // GET: api/usersettings/user/5
        [HttpGet("user/{userId}")]
        public async Task<ActionResult<UserSettings>> GetSettingsForUser(int userId)
        {
            var settings = await _context.UserSettings
                .FirstOrDefaultAsync(s => s.UserId == userId);

            if (settings == null)
            {
                return NotFound();
            }

            return settings;
        }

        // POST: api/usersettings
        // Creates a settings record for a user (typically called once, right after registration)
        [HttpPost]
        public async Task<ActionResult<UserSettings>> CreateSettings(UserSettings settings)
        {
            _context.UserSettings.Add(settings);
            await _context.SaveChangesAsync();

            return CreatedAtAction(nameof(GetSettingsForUser), new { userId = settings.UserId }, settings);
        }

        // PUT: api/usersettings/5
        // Updates an existing settings record (e.g. user toggles notifications off)
        [HttpPut("{id}")]
        public async Task<IActionResult> UpdateSettings(int id, UserSettings settings)
        {
            if (id != settings.UserSettingsId)
            {
                return BadRequest();
            }

            _context.Entry(settings).State = EntityState.Modified;
            await _context.SaveChangesAsync();

            return NoContent();
        }
    }
}