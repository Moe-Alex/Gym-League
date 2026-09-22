namespace GymLeagueAPI.Models
{
    public class Challenge
    {
        public int ChallengeId { get; set; }
        public string Title { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public string Type { get; set; } = string.Empty; // MostWorkouts, LongestStreak, MostXP, MostReps
        public int Target { get; set; }
        public int XpReward { get; set; }
        public int CurrentProgress { get; set; } = 0;
        public bool Completed { get; set; } = false;
        public DateTime EndDate { get; set; }
        public int CreatedByUserId { get; set; }
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    }
}