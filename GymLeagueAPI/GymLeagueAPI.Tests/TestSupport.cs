using GymLeagueAPI.Data;
using GymLeagueAPI.Models;
using GymLeagueAPI.Services;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;

namespace GymLeagueAPI.Tests
{
    /// <summary>Helpers shared by the controller tests.</summary>
    internal static class TestSupport
    {
        // A fresh in-memory database for every test, so tests never affect each other
        public static GymLeagueDbContext CreateContext()
        {
            var options = new DbContextOptionsBuilder<GymLeagueDbContext>()
                .UseInMemoryDatabase(Guid.NewGuid().ToString())
                .Options;

            return new GymLeagueDbContext(options);
        }

        public static User AddUser(GymLeagueDbContext context, string username, int xp = 0)
        {
            var user = new User
            {
                FirebaseUid = Guid.NewGuid().ToString(),
                Username = username,
                Email = $"{username}@example.com",
                XP = xp,
                Level = LevelCalculator.LevelForXp(xp)
            };

            context.Users.Add(user);
            context.SaveChanges();
            return user;
        }

        public static Exercise AddExercise(GymLeagueDbContext context, string name, string muscleGroup)
        {
            var exercise = new Exercise
            {
                ExerciseName = name,
                MuscleGroup = muscleGroup,
                DefaultSets = 3,
                DefaultReps = 10
            };

            context.Exercises.Add(exercise);
            context.SaveChanges();
            return exercise;
        }

        public static NullLogger<T> Logger<T>() => NullLogger<T>.Instance;
    }

    /// <summary>Records the notifications a controller asks for instead of sending them.</summary>
    internal class FakePushNotificationService : IPushNotificationService
    {
        public List<SentPush> Sent { get; } = new();

        public Task SendToUserAsync(
            int userId,
            string title,
            string body,
            IDictionary<string, string>? data = null,
            CancellationToken cancellationToken = default)
        {
            Sent.Add(new SentPush(userId, title, body, data));
            return Task.CompletedTask;
        }
    }

    internal record SentPush(int UserId, string Title, string Body, IDictionary<string, string>? Data);
}
