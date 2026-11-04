using GymLeagueAPI.Controllers;
using GymLeagueAPI.Models.Dtos;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace GymLeagueAPI.Tests
{
    public class DeviceTokensControllerTests
    {
        private static DeviceTokensController CreateController(GymLeagueAPI.Data.GymLeagueDbContext context)
        {
            return new DeviceTokensController(context, TestSupport.Logger<DeviceTokensController>());
        }

        [Fact]
        public async Task Register_StoresTheToken()
        {
            using var context = TestSupport.CreateContext();
            var user = TestSupport.AddUser(context, "moe");

            var result = await CreateController(context)
                .Register(new RegisterDeviceTokenRequest { UserId = user.UserId, Token = "token-1" });

            Assert.IsType<NoContentResult>(result);
            var saved = await context.DeviceTokens.SingleAsync();
            Assert.Equal(user.UserId, saved.UserId);
        }

        [Fact]
        public async Task Register_SameTokenTwice_DoesNotCreateADuplicate()
        {
            using var context = TestSupport.CreateContext();
            var user = TestSupport.AddUser(context, "moe");
            var controller = CreateController(context);

            await controller.Register(new RegisterDeviceTokenRequest { UserId = user.UserId, Token = "token-1" });
            await controller.Register(new RegisterDeviceTokenRequest { UserId = user.UserId, Token = "token-1" });

            Assert.Equal(1, await context.DeviceTokens.CountAsync());
        }

        [Fact]
        public async Task Register_TokenSignedInByAnotherUser_MovesToThatUser()
        {
            using var context = TestSupport.CreateContext();
            var moe = TestSupport.AddUser(context, "moe");
            var caleb = TestSupport.AddUser(context, "caleb");
            var controller = CreateController(context);

            await controller.Register(new RegisterDeviceTokenRequest { UserId = moe.UserId, Token = "shared-phone" });
            await controller.Register(new RegisterDeviceTokenRequest { UserId = caleb.UserId, Token = "shared-phone" });

            var saved = await context.DeviceTokens.SingleAsync();
            Assert.Equal(caleb.UserId, saved.UserId);
        }

        [Fact]
        public async Task Register_TwoRequestsRaceWithTheSameNewToken_BothSucceed()
        {
            // Both contexts share one in-memory database, like two requests share one real database
            var databaseName = Guid.NewGuid().ToString();
            var options = new DbContextOptionsBuilder<GymLeagueAPI.Data.GymLeagueDbContext>()
                .UseInMemoryDatabase(databaseName)
                .Options;

            using var setup = new GymLeagueAPI.Data.GymLeagueDbContext(options);
            var user = TestSupport.AddUser(setup, "moe");

            using var racing = new RacingContext(options, userId: user.UserId, token: "token-race");

            var result = await CreateController(racing)
                .Register(new RegisterDeviceTokenRequest { UserId = user.UserId, Token = "token-race" });

            Assert.IsType<NoContentResult>(result);

            using var check = new GymLeagueAPI.Data.GymLeagueDbContext(options);
            var saved = await check.DeviceTokens.SingleAsync();
            Assert.Equal("token-race", saved.Token);
            Assert.Equal(user.UserId, saved.UserId);
        }

        [Fact]
        public async Task Register_ForUnknownUser_IsRejected()
        {
            using var context = TestSupport.CreateContext();

            var result = await CreateController(context)
                .Register(new RegisterDeviceTokenRequest { UserId = 42, Token = "token-1" });

            Assert.IsType<BadRequestObjectResult>(result);
        }

        [Fact]
        public async Task Register_WithBlankToken_IsRejected()
        {
            using var context = TestSupport.CreateContext();
            var user = TestSupport.AddUser(context, "moe");

            var result = await CreateController(context)
                .Register(new RegisterDeviceTokenRequest { UserId = user.UserId, Token = "  " });

            Assert.IsType<BadRequestObjectResult>(result);
        }

        [Fact]
        public async Task Unregister_RemovesTheToken()
        {
            using var context = TestSupport.CreateContext();
            var user = TestSupport.AddUser(context, "moe");
            var controller = CreateController(context);
            await controller.Register(new RegisterDeviceTokenRequest { UserId = user.UserId, Token = "token-1" });

            await controller.Unregister("token-1");

            Assert.Empty(context.DeviceTokens);
        }

    /// <summary>
    /// Pretends a second request saved the same token first: on its first save it inserts the token
    /// through another context and then fails the way a unique index violation would.
    /// </summary>
    internal class RacingContext : GymLeagueAPI.Data.GymLeagueDbContext
    {
        private readonly DbContextOptions<GymLeagueAPI.Data.GymLeagueDbContext> _options;
        private readonly int _userId;
        private readonly string _token;
        private bool _raced;

        public RacingContext(
            DbContextOptions<GymLeagueAPI.Data.GymLeagueDbContext> options,
            int userId,
            string token) : base(options)
        {
            _options = options;
            _userId = userId;
            _token = token;
        }

        public override async Task<int> SaveChangesAsync(CancellationToken cancellationToken = default)
        {
            if (!_raced)
            {
                _raced = true;

                using var other = new GymLeagueAPI.Data.GymLeagueDbContext(_options);
                other.DeviceTokens.Add(new GymLeagueAPI.Models.DeviceToken
                {
                    UserId = _userId,
                    Token = _token,
                    UpdatedAt = DateTime.UtcNow
                });
                await other.SaveChangesAsync(cancellationToken);

                throw new DbUpdateException("Duplicate token (simulated unique index violation).");
            }

            return await base.SaveChangesAsync(cancellationToken);
        }
    }
}
}
