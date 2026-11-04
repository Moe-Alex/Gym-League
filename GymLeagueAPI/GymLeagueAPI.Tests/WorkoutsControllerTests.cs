using GymLeagueAPI.Controllers;
using GymLeagueAPI.Models;
using Microsoft.AspNetCore.Mvc;

namespace GymLeagueAPI.Tests
{
    public class WorkoutsControllerTests
    {
        private static Workout NewWorkout(int userId, int exerciseId, int xp, int sets = 3, int reps = 10, double weight = 50)
        {
            return new Workout
            {
                UserId = userId,
                WorkoutName = "Chest day",
                DurationMinutes = 30,
                Completed = true,
                XPEarned = xp,
                WorkoutExercises = new List<WorkoutExercise>
                {
                    new()
                    {
                        ExerciseId = exerciseId,
                        Sets = sets,
                        Reps = reps,
                        Weight = weight,
                        Completed = true
                    }
                }
            };
        }

        [Fact]
        public async Task CreateWorkout_AddsXpToTheUser_AndUpdatesTheLevel()
        {
            using var context = TestSupport.CreateContext();
            var user = TestSupport.AddUser(context, "moe", xp: 90);
            var bench = TestSupport.AddExercise(context, "Bench Press", "Chest");
            var controller = new WorkoutsController(context);

            var result = await controller.CreateWorkout(NewWorkout(user.UserId, bench.ExerciseId, xp: 30));

            Assert.IsType<CreatedAtActionResult>(result.Result);
            var saved = await context.Users.FindAsync(user.UserId);
            Assert.Equal(120, saved!.XP);
            Assert.Equal(2, saved.Level);
        }

        [Fact]
        public async Task CreateWorkout_IgnoresNegativeXp()
        {
            using var context = TestSupport.CreateContext();
            var user = TestSupport.AddUser(context, "moe", xp: 40);
            var bench = TestSupport.AddExercise(context, "Bench Press", "Chest");
            var controller = new WorkoutsController(context);

            await controller.CreateWorkout(NewWorkout(user.UserId, bench.ExerciseId, xp: -500));

            var saved = await context.Users.FindAsync(user.UserId);
            Assert.Equal(40, saved!.XP);
        }

        [Fact]
        public async Task CreateWorkout_ForUnknownUser_IsRejected()
        {
            using var context = TestSupport.CreateContext();
            var bench = TestSupport.AddExercise(context, "Bench Press", "Chest");

            var result = await new WorkoutsController(context)
                .CreateWorkout(NewWorkout(999, bench.ExerciseId, xp: 30));

            Assert.IsType<BadRequestObjectResult>(result.Result);
        }

        [Fact]
        public async Task CreateWorkout_WithUnknownExercise_IsRejected_AndGivesNoXp()
        {
            using var context = TestSupport.CreateContext();
            var user = TestSupport.AddUser(context, "moe");

            var result = await new WorkoutsController(context)
                .CreateWorkout(NewWorkout(user.UserId, 999, xp: 30));

            Assert.IsType<BadRequestObjectResult>(result.Result);
            Assert.Equal(0, (await context.Users.FindAsync(user.UserId))!.XP);
        }

        [Fact]
        public async Task MuscleGroupVolume_SumsSetsRepsAndWeight_PerMuscleGroup()
        {
            using var context = TestSupport.CreateContext();
            var user = TestSupport.AddUser(context, "moe");
            var bench = TestSupport.AddExercise(context, "Bench Press", "Chest");
            var squat = TestSupport.AddExercise(context, "Squat", "Legs");
            var controller = new WorkoutsController(context);

            await controller.CreateWorkout(NewWorkout(user.UserId, bench.ExerciseId, 30, sets: 3, reps: 10, weight: 50));   // 1500
            await controller.CreateWorkout(NewWorkout(user.UserId, bench.ExerciseId, 30, sets: 2, reps: 5, weight: 60));    // 600
            await controller.CreateWorkout(NewWorkout(user.UserId, squat.ExerciseId, 30, sets: 4, reps: 5, weight: 100));   // 2000

            var volume = (await controller.GetMuscleGroupVolume(user.UserId)).Value!;

            Assert.Equal(2, volume.Count);
            Assert.Equal(2100, volume["Chest"]);
            Assert.Equal(2000, volume["Legs"]);
        }

        [Fact]
        public async Task MuscleGroupVolume_OnlyCountsThatUsersCompletedSets()
        {
            using var context = TestSupport.CreateContext();
            var moe = TestSupport.AddUser(context, "moe");
            var caleb = TestSupport.AddUser(context, "caleb");
            var bench = TestSupport.AddExercise(context, "Bench Press", "Chest");
            var controller = new WorkoutsController(context);

            var unfinished = NewWorkout(moe.UserId, bench.ExerciseId, 30);
            unfinished.WorkoutExercises.First().Completed = false;
            await controller.CreateWorkout(unfinished);
            await controller.CreateWorkout(NewWorkout(caleb.UserId, bench.ExerciseId, 30));

            var volume = (await controller.GetMuscleGroupVolume(moe.UserId)).Value!;

            Assert.Empty(volume);
        }

        [Fact]
        public async Task MuscleGroupVolume_ForAUserWithNoWorkouts_IsEmpty()
        {
            using var context = TestSupport.CreateContext();
            var user = TestSupport.AddUser(context, "moe");

            var volume = (await new WorkoutsController(context).GetMuscleGroupVolume(user.UserId)).Value!;

            Assert.Empty(volume);
        }
    }
}
