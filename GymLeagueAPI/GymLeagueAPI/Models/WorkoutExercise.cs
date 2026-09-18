namespace GymLeagueAPI.Models
{
    public class WorkoutExercise
    {
        public int WorkoutExerciseId { get; set; }
        public int WorkoutId { get; set; }
        public int ExerciseId { get; set; }
        public int Sets { get; set; }
        public int Reps { get; set; }
        public double Weight { get; set; } // in kg
        public bool Completed { get; set; } = false;

        // Navigation properties
        public Workout? Workout { get; set; }
        public Exercise? Exercise { get; set; }
    }
}