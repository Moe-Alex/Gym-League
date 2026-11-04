namespace GymLeagueAPI.Models
{
    /// <summary>
    /// Battle Arena challenge. A challenge is a head-to-head race between two users:
    /// the creator and an opponent. The first participant to reach <see cref="Target"/>
    /// wins and is awarded <see cref="XpReward"/> XP.
    /// </summary>
    public class Challenge
    {
        public int ChallengeId { get; set; }
        public string Title { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public string Type { get; set; } = string.Empty; // MostWorkouts, LongestStreak, MostXP, MostReps
        public int Target { get; set; }
        public int XpReward { get; set; }

        // Progress of the creator (kept under its original name so older clients still work)
        public int CurrentProgress { get; set; } = 0;

        // Progress of the opponent
        public int OpponentProgress { get; set; } = 0;

        public bool Completed { get; set; } = false;
        public DateTime EndDate { get; set; }
        public int CreatedByUserId { get; set; }

        // Null while the challenge is open to anyone; set once an opponent is chosen or has accepted
        public int? OpponentUserId { get; set; }

        // Open, Pending, Active, Completed or Declined (see ChallengeStatus)
        public string Status { get; set; } = ChallengeStatus.Open;

        // Set when the challenge is won
        public int? WinnerUserId { get; set; }

        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    }

    /// <summary>
    /// The life cycle of a Battle Arena challenge.
    /// Open      - created by a user, anyone else may accept it.
    /// Pending   - sent directly to one opponent (for example from the Squad screen), waiting for a reply.
    /// Active    - accepted; both participants may log progress.
    /// Completed - someone reached the target and won.
    /// Declined  - the invited opponent said no.
    /// </summary>
    public static class ChallengeStatus
    {
        public const string Open = "Open";
        public const string Pending = "Pending";
        public const string Active = "Active";
        public const string Completed = "Completed";
        public const string Declined = "Declined";
    }
}
