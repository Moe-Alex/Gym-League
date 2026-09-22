using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using GymLeagueAPI.Data;
using GymLeagueAPI.Models;

namespace GymLeagueAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class WorkoutsController : ControllerBase
    {
        private readonly GymLeagueDbContext _context;

        public WorkoutsController(GymLeagueDbContext context)
        {
            _context = context;
        }

        // GET: api/workouts/user/5
        [HttpGet("user/{userId}")]
        public async Task<ActionResult<IEnumerable<Workout>>> GetWorkoutsForUser(int userId)
        {
            return await _context.Workouts
                .Where(w => w.UserId == userId)
                .Include(w => w.WorkoutExercises)
                    .ThenInclude(we => we.Exercise)
                .OrderByDescending(w => w.WorkoutDate)
                .ToListAsync();
        }

        // GET: api/workouts/5
        [HttpGet("{id}")]
        public async Task<ActionResult<Workout>> GetWorkout(int id)
        {
            var workout = await _context.Workouts
                .Include(w => w.WorkoutExercises)
                    .ThenInclude(we => we.Exercise)
                .FirstOrDefaultAsync(w => w.WorkoutId == id);

            if (workout == null)
            {
                return NotFound();
            }

            return workout;
        }

        // POST: api/workouts
[HttpPost]
public async Task<ActionResult<Workout>> CreateWorkout(Workout workout)
{
    // Check that the user exists
    var userExists = await _context.Users
        .AnyAsync(u => u.UserId == workout.UserId);

    if (!userExists)
    {
        return BadRequest("User does not exist.");
    }

    // Check exercises before saving
    foreach (var workoutExercise in workout.WorkoutExercises)
    {
        var exerciseExists = await _context.Exercises
            .AnyAsync(e => e.ExerciseId == workoutExercise.ExerciseId);

        if (!exerciseExists)
        {
            return BadRequest(
                $"Exercise {workoutExercise.ExerciseId} does not exist."
            );
        }

        // EF will set WorkoutId after the workout is created
        workoutExercise.WorkoutId = 0;

        // Do not attach navigation objects from the request
        workoutExercise.Workout = null;
        workoutExercise.Exercise = null;
    }

    _context.Workouts.Add(workout);

    await _context.SaveChangesAsync();

    return CreatedAtAction(
        nameof(GetWorkout),
        new { id = workout.WorkoutId },
        workout
    );
}

        // GET: api/workouts/musclegroupvolume/5
        // Returns total volume (sets x reps x weight) per muscle group for a user
        [HttpGet("musclegroupvolume/{userId}")]
        public async Task<ActionResult<Dictionary<string, double>>> GetMuscleGroupVolume(int userId)
        {
            var workoutExercises = await _context.WorkoutExercises
                .Include(we => we.Exercise)
                .Include(we => we.Workout)
                .Where(we => we.Workout.UserId == userId && we.Completed)
                .ToListAsync();

            var volumeByMuscleGroup = workoutExercises
                .GroupBy(we => we.Exercise!.MuscleGroup)
                .ToDictionary(
                    g => g.Key,
                    g => g.Sum(we => we.Sets * we.Reps * we.Weight)
                );

            return volumeByMuscleGroup;
        }

        // DELETE: api/workouts/5
        [HttpDelete("{id}")]
        public async Task<IActionResult> DeleteWorkout(int id)
        {
            var workout = await _context.Workouts.FindAsync(id);
            if (workout == null)
            {
                return NotFound();
            }

            _context.Workouts.Remove(workout);
            await _context.SaveChangesAsync();

            return NoContent();
        }
    }
}