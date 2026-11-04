using System;
using Microsoft.EntityFrameworkCore.Migrations;
using Npgsql.EntityFrameworkCore.PostgreSQL.Metadata;

#nullable disable

namespace GymLeagueAPI.Migrations
{
    /// <inheritdoc />
    public partial class BattleArenaAndDeviceTokens : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<int>(
                name: "OpponentProgress",
                table: "Challenges",
                type: "integer",
                nullable: false,
                defaultValue: 0);

            migrationBuilder.AddColumn<int>(
                name: "OpponentUserId",
                table: "Challenges",
                type: "integer",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "Status",
                table: "Challenges",
                type: "text",
                nullable: false,
                defaultValue: "Open");

            migrationBuilder.AddColumn<int>(
                name: "WinnerUserId",
                table: "Challenges",
                type: "integer",
                nullable: true);

            migrationBuilder.CreateTable(
                name: "DeviceTokens",
                columns: table => new
                {
                    DeviceTokenId = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    UserId = table.Column<int>(type: "integer", nullable: false),
                    Token = table.Column<string>(type: "text", nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_DeviceTokens", x => x.DeviceTokenId);
                });

            migrationBuilder.CreateIndex(
                name: "IX_DeviceTokens_Token",
                table: "DeviceTokens",
                column: "Token",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "DeviceTokens");

            migrationBuilder.DropColumn(
                name: "OpponentProgress",
                table: "Challenges");

            migrationBuilder.DropColumn(
                name: "OpponentUserId",
                table: "Challenges");

            migrationBuilder.DropColumn(
                name: "Status",
                table: "Challenges");

            migrationBuilder.DropColumn(
                name: "WinnerUserId",
                table: "Challenges");
        }
    }
}
