namespace GymLeagueAPI.Services
{
    /// <summary>
    /// Turns a user's total XP into a level. Every 100 XP is one level, starting at level 1.
    /// (A workout earns 10 XP per set, so a 3-set exercise is 30 XP.)
    /// </summary>
    public static class LevelCalculator
    {
        public const int XpPerLevel = 100;

        public static int LevelForXp(int xp)
        {
            if (xp < 0)
            {
                xp = 0;
            }

            return 1 + (xp / XpPerLevel);
        }
    }
}
