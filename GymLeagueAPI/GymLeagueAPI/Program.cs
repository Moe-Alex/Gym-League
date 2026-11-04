using Microsoft.EntityFrameworkCore;
using GymLeagueAPI.Data;
using GymLeagueAPI.Services;
using FirebaseAdmin;
using Google.Apis.Auth.OAuth2;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.ReferenceHandler =
            System.Text.Json.Serialization.ReferenceHandler.IgnoreCycles;
    });
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();
builder.Services.AddHttpClient();

builder.Services.AddDbContext<GymLeagueDbContext>(options =>
    options.UseNpgsql(builder.Configuration.GetConnectionString("DefaultConnection")));

// Push notifications (Firebase Cloud Messaging).
// The service-account key is never stored in the repository. On Render it is supplied as the
// environment variable FIREBASE_CREDENTIALS_BASE64 (the key file, base64 encoded) or
// FIREBASE_CREDENTIALS_JSON (the raw JSON). With neither set, notifications are skipped.
var firebaseJson = builder.Configuration["FIREBASE_CREDENTIALS_JSON"];
var firebaseBase64 = builder.Configuration["FIREBASE_CREDENTIALS_BASE64"];

if (string.IsNullOrWhiteSpace(firebaseJson) && !string.IsNullOrWhiteSpace(firebaseBase64))
{
    firebaseJson = System.Text.Encoding.UTF8.GetString(Convert.FromBase64String(firebaseBase64));
}

var pushEnabled = !string.IsNullOrWhiteSpace(firebaseJson);

if (pushEnabled)
{
    FirebaseApp.Create(new AppOptions
    {
        Credential = GoogleCredential.FromJson(firebaseJson)
    });

    builder.Services.AddScoped<IPushNotificationService, FcmPushNotificationService>();
}
else
{
    builder.Services.AddScoped<IPushNotificationService, NoOpPushNotificationService>();
}

var app = builder.Build();

app.Logger.LogInformation(
    "Push notifications are {State}.",
    pushEnabled ? "ENABLED (Firebase Cloud Messaging)" : "DISABLED (no Firebase credentials found)");

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();
app.UseAuthorization();
app.MapControllers();
app.Run();