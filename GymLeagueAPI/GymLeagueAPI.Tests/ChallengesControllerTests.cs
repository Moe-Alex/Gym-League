using GymLeagueAPI.Controllers;
using GymLeagueAPI.Models;
using GymLeagueAPI.Models.Dtos;
using Microsoft.AspNetCore.Mvc;

namespace GymLeagueAPI.Tests
{
    public class ChallengesControllerTests
    {
        private static ChallengesController CreateController(
            GymLeagueAPI.Data.GymLeagueDbContext context,
            FakePushNotificationService push)
        {
            return new ChallengesController(context, push, TestSupport.Logger<ChallengesController>());
        }

        private static CreateChallengeRequest Request(int creatorId, int? opponentId = null, int target = 2)
        {
            return new CreateChallengeRequest
            {
                Title = "Squat Showdown",
                Description = "First to the target wins",
                Type = "MostWorkouts",
                Target = target,
                XpReward = 150,
                EndDate = DateTime.UtcNow.Date.AddDays(3),
                CreatedByUserId = creatorId,
                OpponentUserId = opponentId
            };
        }

        private static ChallengeDto Body(ActionResult<ChallengeDto> result)
        {
            if (result.Value != null)
            {
                return result.Value;
            }

            var created = Assert.IsType<CreatedAtActionResult>(result.Result);
            return Assert.IsType<ChallengeDto>(created.Value);
        }

        [Fact]
        public async Task Create_WithoutOpponent_IsOpenToEveryone_AndSendsNoPush()
        {
            using var context = TestSupport.CreateContext();
            var push = new FakePushNotificationService();
            var moe = TestSupport.AddUser(context, "moe");

            var result = await CreateController(context, push).CreateChallenge(Request(moe.UserId));

            var dto = Body(result);
            Assert.Equal(ChallengeStatus.Open, dto.Status);
            Assert.Equal("moe", dto.CreatedByUsername);
            Assert.Empty(push.Sent);
        }

        [Fact]
        public async Task Create_WithOpponent_IsPending_AndNotifiesTheOpponent()
        {
            using var context = TestSupport.CreateContext();
            var push = new FakePushNotificationService();
            var moe = TestSupport.AddUser(context, "moe");
            var caleb = TestSupport.AddUser(context, "caleb");

            var result = await CreateController(context, push)
                .CreateChallenge(Request(moe.UserId, caleb.UserId));

            var dto = Body(result);
            Assert.Equal(ChallengeStatus.Pending, dto.Status);
            Assert.Equal("caleb", dto.OpponentUsername);

            var sent = Assert.Single(push.Sent);
            Assert.Equal(caleb.UserId, sent.UserId);
            Assert.Contains("moe", sent.Title);
            Assert.Equal("challenge_invite", sent.Data!["type"]);
        }

        [Fact]
        public async Task Create_AgainstYourself_IsRejected()
        {
            using var context = TestSupport.CreateContext();
            var moe = TestSupport.AddUser(context, "moe");

            var result = await CreateController(context, new FakePushNotificationService())
                .CreateChallenge(Request(moe.UserId, moe.UserId));

            Assert.IsType<BadRequestObjectResult>(result.Result);
        }

        [Fact]
        public async Task Create_WithUnknownUser_IsRejected()
        {
            using var context = TestSupport.CreateContext();

            var result = await CreateController(context, new FakePushNotificationService())
                .CreateChallenge(Request(999));

            Assert.IsType<BadRequestObjectResult>(result.Result);
        }

        [Theory]
        [InlineData("", 3, 100)]
        [InlineData("Title", 0, 100)]
        [InlineData("Title", 3, 0)]
        [InlineData("Title", 3, 100000)]
        public async Task Create_WithInvalidValues_IsRejected(string title, int target, int xp)
        {
            using var context = TestSupport.CreateContext();
            var moe = TestSupport.AddUser(context, "moe");
            var request = Request(moe.UserId);
            request.Title = title;
            request.Target = target;
            request.XpReward = xp;

            var result = await CreateController(context, new FakePushNotificationService())
                .CreateChallenge(request);

            Assert.IsType<BadRequestObjectResult>(result.Result);
            Assert.Empty(context.Challenges);
        }

        [Fact]
        public async Task Create_WithEndDateInThePast_IsRejected()
        {
            using var context = TestSupport.CreateContext();
            var moe = TestSupport.AddUser(context, "moe");
            var request = Request(moe.UserId);
            request.EndDate = DateTime.UtcNow.Date.AddDays(-2);

            var result = await CreateController(context, new FakePushNotificationService())
                .CreateChallenge(request);

            Assert.IsType<BadRequestObjectResult>(result.Result);
        }

        [Fact]
        public async Task Accept_DirectChallenge_ActivatesIt_AndNotifiesTheCreator()
        {
            using var context = TestSupport.CreateContext();
            var push = new FakePushNotificationService();
            var moe = TestSupport.AddUser(context, "moe");
            var caleb = TestSupport.AddUser(context, "caleb");
            var controller = CreateController(context, push);
            var created = Body(await controller.CreateChallenge(Request(moe.UserId, caleb.UserId)));
            push.Sent.Clear();

            var result = await controller.AcceptChallenge(created.ChallengeId, caleb.UserId);

            Assert.Equal(ChallengeStatus.Active, Body(result).Status);
            var sent = Assert.Single(push.Sent);
            Assert.Equal(moe.UserId, sent.UserId);
            Assert.Equal("challenge_accepted", sent.Data!["type"]);
        }

        [Fact]
        public async Task Accept_ByTheCreator_IsRejected()
        {
            using var context = TestSupport.CreateContext();
            var moe = TestSupport.AddUser(context, "moe");
            var controller = CreateController(context, new FakePushNotificationService());
            var created = Body(await controller.CreateChallenge(Request(moe.UserId)));

            var result = await controller.AcceptChallenge(created.ChallengeId, moe.UserId);

            Assert.IsType<BadRequestObjectResult>(result.Result);
        }

        [Fact]
        public async Task Accept_UnknownChallenge_ReturnsNotFound()
        {
            using var context = TestSupport.CreateContext();
            var moe = TestSupport.AddUser(context, "moe");

            var result = await CreateController(context, new FakePushNotificationService())
                .AcceptChallenge(12345, moe.UserId);

            Assert.IsType<NotFoundResult>(result.Result);
        }

        [Fact]
        public async Task Decline_DirectChallenge_NotifiesTheCreator()
        {
            using var context = TestSupport.CreateContext();
            var push = new FakePushNotificationService();
            var moe = TestSupport.AddUser(context, "moe");
            var caleb = TestSupport.AddUser(context, "caleb");
            var controller = CreateController(context, push);
            var created = Body(await controller.CreateChallenge(Request(moe.UserId, caleb.UserId)));
            push.Sent.Clear();

            var result = await controller.DeclineChallenge(created.ChallengeId, caleb.UserId);

            Assert.Equal(ChallengeStatus.Declined, Body(result).Status);
            Assert.Equal("challenge_declined", Assert.Single(push.Sent).Data!["type"]);
        }

        [Fact]
        public async Task LogProgress_WinningTheBattle_AwardsXp_RaisesTheLevel_AndNotifiesTheLoser()
        {
            using var context = TestSupport.CreateContext();
            var push = new FakePushNotificationService();
            var moe = TestSupport.AddUser(context, "moe", xp: 80);
            var caleb = TestSupport.AddUser(context, "caleb");
            var controller = CreateController(context, push);
            var created = Body(await controller.CreateChallenge(Request(moe.UserId, caleb.UserId, target: 2)));
            await controller.AcceptChallenge(created.ChallengeId, caleb.UserId);
            push.Sent.Clear();

            await controller.LogProgress(created.ChallengeId, moe.UserId);
            var final = Body(await controller.LogProgress(created.ChallengeId, moe.UserId));

            Assert.True(final.Completed);
            Assert.Equal(ChallengeStatus.Completed, final.Status);
            Assert.Equal(moe.UserId, final.WinnerUserId);

            var winner = await context.Users.FindAsync(moe.UserId);
            Assert.Equal(230, winner!.XP);   // 80 + 150 reward
            Assert.Equal(3, winner.Level);   // 1 + 230 / 100

            var loser = await context.Users.FindAsync(caleb.UserId);
            Assert.Equal(0, loser!.XP);

            var sent = Assert.Single(push.Sent);
            Assert.Equal(caleb.UserId, sent.UserId);
            Assert.Equal("challenge_completed", sent.Data!["type"]);
        }

        [Fact]
        public async Task LogProgress_BeforeAccepting_IsRejected()
        {
            using var context = TestSupport.CreateContext();
            var moe = TestSupport.AddUser(context, "moe");
            var controller = CreateController(context, new FakePushNotificationService());
            var created = Body(await controller.CreateChallenge(Request(moe.UserId)));

            var result = await controller.LogProgress(created.ChallengeId, moe.UserId);

            Assert.IsType<BadRequestObjectResult>(result.Result);
        }

        [Fact]
        public async Task GetChallenges_ForAUser_ShowsTheirChallengesAndOpenOnes_ButNotOthers()
        {
            using var context = TestSupport.CreateContext();
            var push = new FakePushNotificationService();
            var moe = TestSupport.AddUser(context, "moe");
            var caleb = TestSupport.AddUser(context, "caleb");
            var erin = TestSupport.AddUser(context, "erin");
            var controller = CreateController(context, push);

            // Open challenge by Moe: visible to everyone
            await controller.CreateChallenge(Request(moe.UserId));
            // Private challenge between Caleb and Erin: Moe must not see it
            await controller.CreateChallenge(Request(caleb.UserId, erin.UserId));

            var forMoe = (await controller.GetChallenges(moe.UserId)).Value!.ToList();
            var forErin = (await controller.GetChallenges(erin.UserId)).Value!.ToList();
            var everything = (await controller.GetChallenges()).Value!.ToList();

            Assert.Single(forMoe);
            Assert.Equal(2, forErin.Count);
            Assert.Equal(2, everything.Count);
        }
    }
}
