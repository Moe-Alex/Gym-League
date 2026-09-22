using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using GymLeagueAPI.Data;
using GymLeagueAPI.Models;

namespace GymLeagueAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class ExercisesController : ControllerBase
    {
        private readonly GymLeagueDbContext _context;

        public ExercisesController(GymLeagueDbContext context)
        {
            _context = context;
        }

        // GET: api/exercises
        [HttpGet]
        public async Task<ActionResult<IEnumerable<Exercise>>> GetExercises()
        {
            return await _context.Exercises.ToListAsync();
        }

        // GET: api/exercises/5
        [HttpGet("{id}")]
        public async Task<ActionResult<Exercise>> GetExercise(int id)
        {
            var exercise = await _context.Exercises.FindAsync(id);

            if (exercise == null)
            {
                return NotFound();
            }

            return exercise;
        }

        // GET: api/exercises/musclegroup/Chest
        [HttpGet("musclegroup/{muscleGroup}")]
        public async Task<ActionResult<IEnumerable<Exercise>>> GetExercisesByMuscleGroup(string muscleGroup)
        {
            var exercises = await _context.Exercises
                .Where(e => e.MuscleGroup.ToLower() == muscleGroup.ToLower())
                .ToListAsync();

            return exercises;
        }

        // POST: api/exercises
        [HttpPost]
        public async Task<ActionResult<Exercise>> CreateExercise(Exercise exercise)
        {
            _context.Exercises.Add(exercise);
            await _context.SaveChangesAsync();

            return CreatedAtAction(nameof(GetExercise), new { id = exercise.ExerciseId }, exercise);
        }

        // PUT: api/exercises/5
        [HttpPut("{id}")]
        public async Task<IActionResult> UpdateExercise(int id, Exercise exercise)
        {
            if (id != exercise.ExerciseId)
            {
                return BadRequest();
            }

            _context.Entry(exercise).State = EntityState.Modified;
            await _context.SaveChangesAsync();

            return NoContent();
        }

        // DELETE: api/exercises/5?userId=3
        // Base/seeded exercises (CreatedByUserId == null) can never be deleted.
        // Custom exercises can only be deleted by the user who created them.
        [HttpDelete("{id}")]
        public async Task<IActionResult> DeleteExercise(int id, [FromQuery] int userId)
        {
            var exercise = await _context.Exercises.FindAsync(id);

            if (exercise == null)
            {
                return NotFound();
            }

            if (exercise.CreatedByUserId == null)
            {
                return Forbid();
            }

            if (exercise.CreatedByUserId != userId)
            {
                return Forbid();
            }

            _context.Exercises.Remove(exercise);
            await _context.SaveChangesAsync();

            return NoContent();
        }
    }
}