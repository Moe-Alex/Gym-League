using FirebaseAdmin.Messaging;
using GymLeagueAPI.Data;
using Microsoft.EntityFrameworkCore;

namespace GymLeagueAPI.Services
{
    /// <summary>
    /// Sends real push notifications through Firebase Cloud Messaging (FCM).
    /// Respects the user's "notifications enabled" setting and removes tokens that Firebase
    /// reports as no longer valid (for example after the app was uninstalled).
    /// A failure to send never breaks the request that triggered it.
    /// </summary>
    public class FcmPushNotificationService : IPushNotificationService
    {
        // Must match the channel the Android app creates (NotificationHelper.CHANNEL_ID)
        private const string AndroidChannelId = "challenges";

        private readonly GymLeagueDbContext _context;
        private readonly ILogger<FcmPushNotificationService> _logger;

        public FcmPushNotificationService(
            GymLeagueDbContext context,
            ILogger<FcmPushNotificationService> logger)
        {
            _context = context;
            _logger = logger;
        }

        public async Task SendToUserAsync(
            int userId,
            string title,
            string body,
            IDictionary<string, string>? data = null,
            CancellationToken cancellationToken = default)
        {
            try
            {
                var notificationsEnabled = await _context.UserSettings
                    .Where(s => s.UserId == userId)
                    .Select(s => (bool?)s.NotificationsEnabled)
                    .FirstOrDefaultAsync(cancellationToken);

                if (notificationsEnabled == false)
                {
                    _logger.LogInformation("User {UserId} has turned notifications off.", userId);
                    return;
                }

                var tokens = await _context.DeviceTokens
                    .Where(t => t.UserId == userId)
                    .Select(t => t.Token)
                    .ToListAsync(cancellationToken);

                if (tokens.Count == 0)
                {
                    _logger.LogInformation("User {UserId} has no registered devices.", userId);
                    return;
                }

                var message = new MulticastMessage
                {
                    Tokens = tokens,
                    Notification = new Notification { Title = title, Body = body },
                    Data = data == null ? null : new Dictionary<string, string>(data),
                    Android = new AndroidConfig
                    {
                        Priority = Priority.High,
                        Notification = new AndroidNotification { ChannelId = AndroidChannelId }
                    }
                };

                var response = await FirebaseMessaging.DefaultInstance
                    .SendEachForMulticastAsync(message, cancellationToken);

                _logger.LogInformation(
                    "Push '{Title}' for user {UserId}: {Success} sent, {Failure} failed.",
                    title,
                    userId,
                    response.SuccessCount,
                    response.FailureCount);

                await RemoveDeadTokensAsync(tokens, response, cancellationToken);
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Could not send push notification to user {UserId}.", userId);
            }
        }

        private async Task RemoveDeadTokensAsync(
            List<string> tokens,
            BatchResponse response,
            CancellationToken cancellationToken)
        {
            var dead = new List<string>();

            for (int i = 0; i < response.Responses.Count; i++)
            {
                var result = response.Responses[i];

                if (!result.IsSuccess &&
                    result.Exception?.MessagingErrorCode == MessagingErrorCode.Unregistered)
                {
                    dead.Add(tokens[i]);
                }
            }

            if (dead.Count == 0)
            {
                return;
            }

            var rows = await _context.DeviceTokens
                .Where(t => dead.Contains(t.Token))
                .ToListAsync(cancellationToken);

            _context.DeviceTokens.RemoveRange(rows);
            await _context.SaveChangesAsync(cancellationToken);
        }
    }
}
