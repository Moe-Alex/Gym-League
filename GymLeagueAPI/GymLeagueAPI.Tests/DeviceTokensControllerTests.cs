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
    }
}
