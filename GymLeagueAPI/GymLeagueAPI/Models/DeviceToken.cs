namespace GymLeagueAPI.Models
{
    /// <summary>
    /// A Firebase Cloud Messaging registration token for one installed copy of the app.
    /// A user can have several (phone and tablet); a token belongs to one user at a time.
    /// </summary>
    public class DeviceToken
    {
        public int DeviceTokenId { get; set; }
        public int UserId { get; set; }
        public string Token { get; set; } = string.Empty;
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    }
}
