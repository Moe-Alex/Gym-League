using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using GymLeagueAPI.Data;
using GymLeagueAPI.Models;
using GymLeagueAPI.Models.Dtos;

namespace GymLeagueAPI.Controllers
{
    /// <summary>Stores the Firebase Cloud Messaging tokens the app registers so we can push to a user.</summary>
    [ApiController]
    [Route("api/[controller]")]
    public class DeviceTokensController : ControllerBase
    {
        private readonly GymLeagueDbContext _context;
        private readonly ILogger<DeviceTokensController> _logger;

        public DeviceTokensController(GymLeagueDbContext context, ILogger<DeviceTokensController> logger)
        {
            _context = context;
            _logger = logger;
        }

        // POST: api/devicetokens
        // Saves a token for a user. If the token is already known it is moved to this user
        // (for example when someone signs in with a different account on the same phone).
        [HttpPost]
        public async Task<IActionResult> Register(RegisterDeviceTokenRequest request)
        {
            if (string.IsNullOrWhiteSpace(request.Token))
            {
                return BadRequest("A token is required.");
            }

            var userExists = await _context.Users.AnyAsync(u => u.UserId == request.UserId);

            if (!userExists)
            {
                return BadRequest("User does not exist.");
            }

            var existing = await _context.DeviceTokens
                .FirstOrDefaultAsync(t => t.Token == request.Token);

            if (existing == null)
            {
                _context.DeviceTokens.Add(new DeviceToken
                {
                    UserId = request.UserId,
                    Token = request.Token,
                    UpdatedAt = DateTime.UtcNow
                });
            }
            else
            {
                existing.UserId = request.UserId;
                existing.UpdatedAt = DateTime.UtcNow;
            }

            await _context.SaveChangesAsync();

            _logger.LogInformation("Device token registered for user {UserId}.", request.UserId);

            return NoContent();
        }

        // DELETE: api/devicetokens?token=abc
        // Called when a user signs out so the phone stops receiving their notifications.
        [HttpDelete]
        public async Task<IActionResult> Unregister([FromQuery] string token)
        {
            var existing = await _context.DeviceTokens
                .FirstOrDefaultAsync(t => t.Token == token);

            if (existing != null)
            {
                _context.DeviceTokens.Remove(existing);
                await _context.SaveChangesAsync();
            }

            return NoContent();
        }
    }
}
