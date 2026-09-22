using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace GymLeagueAPI.Migrations
{
    /// <inheritdoc />
    public partial class AddExerciseOwnership : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<int>(
                name: "CreatedByUserId",
                table: "Exercises",
                type: "integer",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "CreatedByUserId",
                table: "Exercises");
        }
    }
}
