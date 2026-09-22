namespace GymLeagueAPI.Models
{
    public class SquadMember
    {
        public int SquadMemberId { get; set; }

        public int SquadId { get; set; }

        public int UserId { get; set; }

        public int CurrentStreak { get; set; } = 0;

        public DateTime JoinedAt { get; set; } = DateTime.UtcNow;

        // Navigation properties
        public Squad? Squad { get; set; }

        public User? User { get; set; }
    }
}