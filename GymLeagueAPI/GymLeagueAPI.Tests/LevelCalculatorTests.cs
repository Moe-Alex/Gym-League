using GymLeagueAPI.Services;

namespace GymLeagueAPI.Tests
{
    public class LevelCalculatorTests
    {
        [Theory]
        [InlineData(0, 1)]
        [InlineData(99, 1)]
        [InlineData(100, 2)]
        [InlineData(250, 3)]
        [InlineData(1000, 11)]
        public void LevelForXp_UsesOneLevelPerHundredXp(int xp, int expectedLevel)
        {
            Assert.Equal(expectedLevel, LevelCalculator.LevelForXp(xp));
        }

        [Fact]
        public void LevelForXp_NeverGoesBelowLevelOne()
        {
            Assert.Equal(1, LevelCalculator.LevelForXp(-50));
        }
    }
}
