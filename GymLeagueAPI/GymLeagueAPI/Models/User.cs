namespace GymLeagueAPI.Models
{
    public class User
    {
        public int UserId { get; set; }
        public string FirebaseUid { get; set; } = string.Empty; // links to Firebase Auth account
        public string Username { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string? ProfileImageUrl { get; set; }
        public int XP { get; set; } = 0;
        public int Level { get; set; } = 1;
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        // Navigation properties
        public ICollection<Workout> Workouts { get; set; } = new List<Workout>();
        public UserSettings? Settings { get; set; }
    }
}