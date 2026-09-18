namespace GymLeagueAPI.Models
{
    public class Workout
    {
        public int WorkoutId { get; set; }
        public int UserId { get; set; }
        public string WorkoutName { get; set; } = string.Empty;
        public DateTime WorkoutDate { get; set; } = DateTime.UtcNow;
        public int DurationMinutes { get; set; }
        public bool Completed { get; set; } = false;
        public int XPEarned { get; set; }

        // Navigation properties
        public User? User { get; set; }
        public ICollection<WorkoutExercise> WorkoutExercises { get; set; } = new List<WorkoutExercise>();
    }
}