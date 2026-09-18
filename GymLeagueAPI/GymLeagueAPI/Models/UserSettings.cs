namespace GymLeagueAPI.Models
{
    public class UserSettings
    {
        public int UserSettingsId { get; set; }
        public int UserId { get; set; }
        public bool NotificationsEnabled { get; set; } = true;
        public string AppearanceMode { get; set; } = "Dark"; // matches your dark-mode-only design
        public string Language { get; set; } = "English";

        // Navigation property
        public User? User { get; set; }
    }
}