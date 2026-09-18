namespace GymLeagueAPI.Models
{
    public class Exercise
    {
        public int ExerciseId { get; set; }
        public string ExerciseName { get; set; } = string.Empty;
        public string MuscleGroup { get; set; } = string.Empty; // e.g. "Chest", "Back", "Legs"
        public string? Description { get; set; }
        public string? VideoUrl { get; set; } // for the exercise library feature
        public int DefaultSets { get; set; }
        public int DefaultReps { get; set; }

        // Navigation property
        public ICollection<WorkoutExercise> WorkoutExercises { get; set; } = new List<WorkoutExercise>();
    }
}