namespace GymLeagueAPI.Models
{
    public class Squad
    {
        public int SquadId { get; set; }

        public string SquadName { get; set; } = string.Empty;

        public int SharedStreak { get; set; } = 0;

        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        public ICollection<SquadMember> Members { get; set; }
            = new List<SquadMember>();
    }
}