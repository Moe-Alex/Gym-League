using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using GymLeagueAPI.Data;

namespace GymLeagueAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class PersonalRecordsController : ControllerBase
    {
        private readonly GymLeagueDbContext _context;

        public PersonalRecordsController(GymLeagueDbContext context)
        {
            _context = context;
        }

        // GET: api/personalrecords/user/5
        // Returns each exercise the user has done, their heaviest lift, and an estimated 1-rep max
        [HttpGet("user/{userId}")]
        public async Task<ActionResult<IEnumerable<object>>> GetPersonalRecords(int userId)
        {
            var workoutExercises = await _context.WorkoutExercises
                .Include(we => we.Exercise)
                .Include(we => we.Workout)
                .Where(we => we.Workout!.UserId == userId && we.Completed && we.Weight > 0)
                .ToListAsync();

            var personalRecords = workoutExercises
                .GroupBy(we => new { we.ExerciseId, we.Exercise!.ExerciseName })
                .Select(g =>
                {
                    // Find the set with the heaviest weight for this exercise
                    var bestSet = g.OrderByDescending(we => we.Weight).First();

                    // Epley formula: estimated 1-rep max = weight x (1 + reps / 30)
                    double estimatedOneRepMax = bestSet.Weight * (1 + (bestSet.Reps / 30.0));

                    return new
                    {
                        ExerciseId = g.Key.ExerciseId,
                        ExerciseName = g.Key.ExerciseName,
                        HeaviestWeight = bestSet.Weight,
                        RepsAtHeaviestWeight = bestSet.Reps,
                        EstimatedOneRepMax = Math.Round(estimatedOneRepMax, 1),
                        DateAchieved = bestSet.Workout!.WorkoutDate
                    };
                })
                .OrderByDescending(pr => pr.EstimatedOneRepMax)
                .ToList();

            return Ok(personalRecords);
        }

        // GET: api/personalrecords/user/5/exercise/3
        // Returns the single best record for one specific exercise
        [HttpGet("user/{userId}/exercise/{exerciseId}")]
        public async Task<ActionResult<object>> GetPersonalRecordForExercise(int userId, int exerciseId)
        {
            var bestSet = await _context.WorkoutExercises
                .Include(we => we.Exercise)
                .Include(we => we.Workout)
                .Where(we => we.Workout!.UserId == userId
                             && we.ExerciseId == exerciseId
                             && we.Completed
                             && we.Weight > 0)
                .OrderByDescending(we => we.Weight)
                .FirstOrDefaultAsync();

            if (bestSet == null)
            {
                return NotFound("No record found for this exercise yet.");
            }

            double estimatedOneRepMax = bestSet.Weight * (1 + (bestSet.Reps / 30.0));

            return Ok(new
            {
                ExerciseId = bestSet.ExerciseId,
                ExerciseName = bestSet.Exercise!.ExerciseName,
                HeaviestWeight = bestSet.Weight,
                RepsAtHeaviestWeight = bestSet.Reps,
                EstimatedOneRepMax = Math.Round(estimatedOneRepMax, 1),
                DateAchieved = bestSet.Workout!.WorkoutDate
            });
        }
    }
}