namespace GymLeagueAPI.Services
{
    /// <summary>
    /// Used when no Firebase credentials are configured (local development, tests).
    /// It only logs, so the rest of the API behaves exactly the same.
    /// </summary>
    public class NoOpPushNotificationService : IPushNotificationService
    {
        private readonly ILogger<NoOpPushNotificationService> _logger;

        public NoOpPushNotificationService(ILogger<NoOpPushNotificationService> logger)
        {
            _logger = logger;
        }

        public Task SendToUserAsync(
            int userId,
            string title,
            string body,
            IDictionary<string, string>? data = null,
            CancellationToken cancellationToken = default)
        {
            _logger.LogInformation(
                "Push notifications are not configured. Skipped '{Title}' for user {UserId}.",
                title,
                userId);

            return Task.CompletedTask;
        }
    }
}
