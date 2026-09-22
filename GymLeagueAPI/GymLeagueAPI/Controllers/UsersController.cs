using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using GymLeagueAPI.Data;
using GymLeagueAPI.Models;

namespace GymLeagueAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class UsersController : ControllerBase
    {
        private readonly GymLeagueDbContext _context;

        public UsersController(GymLeagueDbContext context)
        {
            _context = context;
        }

        // GET: api/users
        [HttpGet]
        public async Task<ActionResult<IEnumerable<User>>> GetUsers()
        {
            return await _context.Users.ToListAsync();
        }

        // GET: api/users/5
        [HttpGet("{id:int}")]
        public async Task<ActionResult<User>> GetUser(int id)
        {
            var user = await _context.Users.FindAsync(id);

            if (user == null)
            {
                return NotFound();
            }

            return user;
        }

        // GET: api/users/firebase/{firebaseUid}
        [HttpGet("firebase/{firebaseUid}")]
        public async Task<ActionResult<User>> GetUserByFirebaseUid(string firebaseUid)
        {
            var user = await _context.Users
                .FirstOrDefaultAsync(u => u.FirebaseUid == firebaseUid);

            if (user == null)
            {
                return NotFound();
            }

            return user;
        }

        // GET: api/users/username/john123
        [HttpGet("username/{username}")]
        public async Task<ActionResult<User>> GetUserByUsername(string username)
        {
            var user = await _context.Users
                .FirstOrDefaultAsync(u => u.Username.ToLower() == username.ToLower());

            if (user == null)
            {
                return NotFound();
            }

            return user;
        }

        // POST: api/users
        [HttpPost]
        public async Task<ActionResult<User>> CreateUser(User user)
        {
            var existingUser = await _context.Users
                .FirstOrDefaultAsync(u => u.FirebaseUid == user.FirebaseUid);

            if (existingUser != null)
            {
                return Conflict("A user with this Firebase UID already exists.");
            }

            _context.Users.Add(user);

            await _context.SaveChangesAsync();

            return CreatedAtAction(
                nameof(GetUser),
                new { id = user.UserId },
                user
            );
        }
    }
}