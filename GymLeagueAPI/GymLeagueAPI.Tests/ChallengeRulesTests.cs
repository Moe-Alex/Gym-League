using GymLeagueAPI.Models;
using GymLeagueAPI.Services;

namespace GymLeagueAPI.Tests
{
    public class ChallengeRulesTests
    {
        private static readonly DateTime Now = new(2026, 11, 4, 12, 0, 0, DateTimeKind.Utc);

        private static Challenge NewChallenge(
            string status = ChallengeStatus.Open,
            int creator = 1,
            int? opponent = null,
            int target = 3)
        {
            return new Challenge
            {
                ChallengeId = 10,
                Title = "7-Day Showdown",
                Target = target,
                XpReward = 150,
                CreatedByUserId = creator,
                OpponentUserId = opponent,
                Status = status,
                EndDate = Now.Date.AddDays(7)
            };
        }

        // ---------- Accept ----------

        [Fact]
        public void Accept_OpenChallenge_ByAnotherUser_StartsTheBattle()
        {
            var challenge = NewChallenge();

            var error = ChallengeRules.TryAccept(challenge, 2, Now);

            Assert.Null(error);
            Assert.Equal(ChallengeStatus.Active, challenge.Status);
            Assert.Equal(2, challenge.OpponentUserId);
        }

        [Fact]
        public void Accept_OwnChallenge_IsRefused()
        {
            var challenge = NewChallenge();

            Assert.NotNull(ChallengeRules.TryAccept(challenge, 1, Now));
            Assert.Equal(ChallengeStatus.Open, challenge.Status);
        }

        [Fact]
        public void Accept_DirectChallenge_OnlyTheInvitedUserMayAccept()
        {
            var challenge = NewChallenge(ChallengeStatus.Pending, opponent: 2);

            Assert.NotNull(ChallengeRules.TryAccept(challenge, 3, Now));
            Assert.Null(ChallengeRules.TryAccept(challenge, 2, Now));
            Assert.Equal(ChallengeStatus.Active, challenge.Status);
        }

        [Fact]
        public void Accept_AlreadyActiveChallenge_IsRefused()
        {
            var challenge = NewChallenge(ChallengeStatus.Active, opponent: 2);

            Assert.NotNull(ChallengeRules.TryAccept(challenge, 3, Now));
        }

        [Fact]
        public void Accept_EndedChallenge_IsRefused()
        {
            var challenge = NewChallenge();
            challenge.EndDate = Now.Date.AddDays(-1);

            Assert.NotNull(ChallengeRules.TryAccept(challenge, 2, Now));
        }

        // ---------- Decline ----------

        [Fact]
        public void Decline_DirectChallenge_ByInvitedUser_MarksItDeclined()
        {
            var challenge = NewChallenge(ChallengeStatus.Pending, opponent: 2);

            var error = ChallengeRules.TryDecline(challenge, 2);

            Assert.Null(error);
            Assert.Equal(ChallengeStatus.Declined, challenge.Status);
        }

        [Fact]
        public void Decline_OpenChallenge_IsRefused()
        {
            var challenge = NewChallenge();

            Assert.NotNull(ChallengeRules.TryDecline(challenge, 2));
        }

        [Fact]
        public void Decline_ByTheWrongUser_IsRefused()
        {
            var challenge = NewChallenge(ChallengeStatus.Pending, opponent: 2);

            Assert.NotNull(ChallengeRules.TryDecline(challenge, 3));
            Assert.Equal(ChallengeStatus.Pending, challenge.Status);
        }

        // ---------- Progress ----------

        [Fact]
        public void LogProgress_Creator_IncreasesOnlyTheCreatorsProgress()
        {
            var challenge = NewChallenge(ChallengeStatus.Active, opponent: 2);

            var error = ChallengeRules.TryLogProgress(challenge, 1, Now, out var completed);

            Assert.Null(error);
            Assert.False(completed);
            Assert.Equal(1, challenge.CurrentProgress);
            Assert.Equal(0, challenge.OpponentProgress);
        }

        [Fact]
        public void LogProgress_Opponent_IncreasesOnlyTheOpponentsProgress()
        {
            var challenge = NewChallenge(ChallengeStatus.Active, opponent: 2);

            ChallengeRules.TryLogProgress(challenge, 2, Now, out _);

            Assert.Equal(0, challenge.CurrentProgress);
            Assert.Equal(1, challenge.OpponentProgress);
        }

        [Fact]
        public void LogProgress_FirstToReachTheTarget_WinsTheChallenge()
        {
            var challenge = NewChallenge(ChallengeStatus.Active, opponent: 2, target: 2);

            ChallengeRules.TryLogProgress(challenge, 2, Now, out _);
            ChallengeRules.TryLogProgress(challenge, 1, Now, out _);
            var error = ChallengeRules.TryLogProgress(challenge, 2, Now, out var completed);

            Assert.Null(error);
            Assert.True(completed);
            Assert.True(challenge.Completed);
            Assert.Equal(ChallengeStatus.Completed, challenge.Status);
            Assert.Equal(2, challenge.WinnerUserId);
            Assert.Equal(2, challenge.OpponentProgress);
        }

        [Fact]
        public void LogProgress_AfterTheChallengeIsWon_IsRefused()
        {
            var challenge = NewChallenge(ChallengeStatus.Active, opponent: 2, target: 1);
            ChallengeRules.TryLogProgress(challenge, 1, Now, out _);

            Assert.NotNull(ChallengeRules.TryLogProgress(challenge, 2, Now, out _));
            Assert.Equal(1, challenge.CurrentProgress);
            Assert.Equal(0, challenge.OpponentProgress);
        }

        [Fact]
        public void LogProgress_ByANonParticipant_IsRefused()
        {
            var challenge = NewChallenge(ChallengeStatus.Active, opponent: 2);

            Assert.NotNull(ChallengeRules.TryLogProgress(challenge, 3, Now, out _));
        }

        [Theory]
        [InlineData(ChallengeStatus.Open)]
        [InlineData(ChallengeStatus.Pending)]
        [InlineData(ChallengeStatus.Declined)]
        public void LogProgress_BeforeTheChallengeIsAccepted_IsRefused(string status)
        {
            var challenge = NewChallenge(status, opponent: 2);

            Assert.NotNull(ChallengeRules.TryLogProgress(challenge, 1, Now, out _));
            Assert.Equal(0, challenge.CurrentProgress);
        }

        [Fact]
        public void LogProgress_AfterTheEndDate_IsRefused()
        {
            var challenge = NewChallenge(ChallengeStatus.Active, opponent: 2);
            challenge.EndDate = Now.Date.AddDays(-1);

            Assert.NotNull(ChallengeRules.TryLogProgress(challenge, 1, Now, out _));
        }

        [Fact]
        public void HasEnded_IsFalseThroughoutTheEndDateItself()
        {
            var challenge = NewChallenge();
            challenge.EndDate = Now.Date;

            Assert.False(ChallengeRules.HasEnded(challenge, Now.Date.AddHours(23)));
            Assert.True(ChallengeRules.HasEnded(challenge, Now.Date.AddDays(1)));
        }
    }
}
