using GymLeagueAPI.Models;

namespace GymLeagueAPI.Services
{
    /// <summary>
    /// The Battle Arena rules, kept free of database and HTTP code so they are easy to unit test.
    /// Each method returns null when the action is allowed (and applies it to the challenge),
    /// or a message that explains why it was refused.
    /// </summary>
    public static class ChallengeRules
    {
        /// <summary>A challenge is open for progress until the end of its end date (UTC).</summary>
        public static bool HasEnded(Challenge challenge, DateTime now)
        {
            return now >= challenge.EndDate.Date.AddDays(1);
        }

        public static string? TryAccept(Challenge challenge, int userId, DateTime now)
        {
            if (challenge.Status == ChallengeStatus.Completed || challenge.Status == ChallengeStatus.Declined)
            {
                return "This challenge is no longer available.";
            }

            if (challenge.Status == ChallengeStatus.Active)
            {
                return "This challenge has already been accepted.";
            }

            if (challenge.CreatedByUserId == userId)
            {
                return "You cannot accept your own challenge.";
            }

            if (challenge.Status == ChallengeStatus.Pending && challenge.OpponentUserId != userId)
            {
                return "This challenge was sent to someone else.";
            }

            if (HasEnded(challenge, now))
            {
                return "This challenge has already ended.";
            }

            challenge.OpponentUserId = userId;
            challenge.Status = ChallengeStatus.Active;
            return null;
        }

        public static string? TryDecline(Challenge challenge, int userId)
        {
            if (challenge.Status != ChallengeStatus.Pending)
            {
                return "Only a challenge sent directly to you can be declined.";
            }

            if (challenge.OpponentUserId != userId)
            {
                return "This challenge was sent to someone else.";
            }

            challenge.Status = ChallengeStatus.Declined;
            return null;
        }

        /// <summary>
        /// Adds one point of progress for a participant. When they reach the target the
        /// challenge is completed and they are recorded as the winner.
        /// </summary>
        public static string? TryLogProgress(Challenge challenge, int userId, DateTime now, out bool completedNow)
        {
            completedNow = false;

            if (challenge.Status == ChallengeStatus.Completed)
            {
                return "This challenge is already finished.";
            }

            if (challenge.Status != ChallengeStatus.Active)
            {
                return "This challenge has not been accepted yet.";
            }

            if (HasEnded(challenge, now))
            {
                return "This challenge has already ended.";
            }

            bool isCreator = challenge.CreatedByUserId == userId;
            bool isOpponent = challenge.OpponentUserId == userId;

            if (!isCreator && !isOpponent)
            {
                return "You are not part of this challenge.";
            }

            int progress;
            if (isCreator)
            {
                challenge.CurrentProgress += 1;
                progress = challenge.CurrentProgress;
            }
            else
            {
                challenge.OpponentProgress += 1;
                progress = challenge.OpponentProgress;
            }

            if (progress >= challenge.Target)
            {
                if (isCreator)
                {
                    challenge.CurrentProgress = challenge.Target;
                }
                else
                {
                    challenge.OpponentProgress = challenge.Target;
                }

                challenge.Completed = true;
                challenge.Status = ChallengeStatus.Completed;
                challenge.WinnerUserId = userId;
                completedNow = true;
            }

            return null;
        }
    }
}
