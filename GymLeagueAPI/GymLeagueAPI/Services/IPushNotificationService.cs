namespace GymLeagueAPI.Services
{
    /// <summary>Sends push notifications to every device a user has registered.</summary>
    public interface IPushNotificationService
    {
        Task SendToUserAsync(
            int userId,
            string title,
            string body,
            IDictionary<string, string>? data = null,
            CancellationToken cancellationToken = default);
    }
}
