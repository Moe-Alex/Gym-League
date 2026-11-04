namespace GymLeagueAPI.Models.Dtos
{
    /// <summary>What the API returns for a challenge, including the usernames of the people involved.</summary>
    public class ChallengeDto
    {
        public int ChallengeId { get; set; }
        public string Title { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public string Type { get; set; } = string.Empty;
        public int Target { get; set; }
        public int XpReward { get; set; }
        public int CurrentProgress { get; set; }
        public int OpponentProgress { get; set; }
        public bool Completed { get; set; }
        public DateTime EndDate { get; set; }
        public int CreatedByUserId { get; set; }
        public string? CreatedByUsername { get; set; }
        public int? OpponentUserId { get; set; }
        public string? OpponentUsername { get; set; }
        public string Status { get; set; } = ChallengeStatus.Open;
        public int? WinnerUserId { get; set; }
        public DateTime CreatedAt { get; set; }

        public static ChallengeDto From(Challenge c, IReadOnlyDictionary<int, string> usernames)
        {
            return new ChallengeDto
            {
                ChallengeId = c.ChallengeId,
                Title = c.Title,
                Description = c.Description,
                Type = c.Type,
                Target = c.Target,
                XpReward = c.XpReward,
                CurrentProgress = c.CurrentProgress,
                OpponentProgress = c.OpponentProgress,
                Completed = c.Completed,
                EndDate = c.EndDate,
                CreatedByUserId = c.CreatedByUserId,
                CreatedByUsername = usernames.TryGetValue(c.CreatedByUserId, out var creator) ? creator : null,
                OpponentUserId = c.OpponentUserId,
                OpponentUsername = c.OpponentUserId.HasValue && usernames.TryGetValue(c.OpponentUserId.Value, out var opp) ? opp : null,
                Status = c.Status,
                WinnerUserId = c.WinnerUserId,
                CreatedAt = c.CreatedAt
            };
        }
    }

    /// <summary>Body of POST api/challenges.</summary>
    public class CreateChallengeRequest
    {
        public string Title { get; set; } = string.Empty;
        public string? Description { get; set; }
        public string Type { get; set; } = string.Empty;
        public int Target { get; set; }
        public int XpReward { get; set; }
        public DateTime EndDate { get; set; }
        public int CreatedByUserId { get; set; }

        // Optional: challenge one specific person instead of leaving it open to everyone
        public int? OpponentUserId { get; set; }
    }

    /// <summary>Body of POST api/devicetokens.</summary>
    public class RegisterDeviceTokenRequest
    {
        public int UserId { get; set; }
        public string Token { get; set; } = string.Empty;
    }
}
