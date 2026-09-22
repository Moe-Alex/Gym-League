# GymLeague

Caleb Forbes - ST10438433
Moegammad Alexander - ST10453511
Erin Sarjoo - ST10450571
Keeran Perumel - ST10367702

GymLeague is a native Android fitness tracking app with an ASP.NET Core Web API backend, built for PROG7314 (POE Part 2). Train, log your workouts, compete with a squad, and track your personal records — all backed by a live REST API and cloud-hosted PostgreSQL database.

## Important — Login Requirement

**You must sign in with a Google account to use this app.** Authentication is handled through Firebase using Google Sign-In only — there is no email/password login. When testing or demoing the app, make sure the device or emulator has a Google account added under Settings, and that Google Play Services is available (a standard emulator image with Google APIs, or a physical Android device, both work).

## Architecture

- **Android app**: Kotlin, MVVM (View → ViewModel → Repository → Retrofit → API)
- **Backend API**: ASP.NET Core Web API (.NET 10), Entity Framework Core, PostgreSQL
- **Authentication**: Firebase Authentication (Google Sign-In)
- **Hosting**: Render (free tier), deployed via Docker
- **Design**: Custom purple/blue branded theme applied app-wide, with a persistent bottom navigation bar shared across every screen

## Tech Stack

- Kotlin, Retrofit2 + Gson, Firebase Auth, Credential Manager (Google Identity)
- ASP.NET Core, EF Core, PostgreSQL
- GitHub Actions CI (build + unit tests on every push)

## Core Features

### Exercise Library & Workout Logging
Browse and search a shared exercise catalog filterable by muscle group. Users can add their own custom exercises (tracked by who created them) and log a real workout session against any exercise — sets, reps, weight, and duration, with a built-in rest timer. Only the user who created a custom exercise can delete it; the base exercise catalog is protected from deletion.

### Squad
Create a squad or join one by searching for and inviting a real registered user by username. View your squad's shared streak and member list, and leave a squad you're part of at any time. All squad actions read and write directly to the live database.

### Personal Records
Automatically calculates your estimated one-rep max (Epley formula) per exercise from your logged workout history, ranked from strongest to weakest lift. Updates immediately as new workouts are logged.

## Settings

Accessible from the More tab — shows your live profile (level, total XP, workouts logged), and lets you manage notifications, appearance (light/dark/system), and language. Changes are saved to the backend and persist across sessions.

## Setup

1. Clone the repo
2. Open `GymLeagueApp/` in Android Studio
3. Make sure a Google account is signed in on your test device/emulator
4. Add your own `google-services.json` if testing with a different Firebase project
5. Run on an emulator (with Google APIs) or a physical device (API 24+)
6. On first launch, sign in with Google — this creates your GymLeague profile automatically

## API

The backend is hosted on Render. Base URL is configured in `RetrofitClient.kt`. Note: the free Render tier spins down when idle, so the first request after inactivity can take up to ~60 seconds — the app's network timeout is configured accordingly.

## Version Control & CI

This repo uses feature branches merged into `main` via pull request. GitHub Actions runs unit tests and a debug build on every push and pull request (see `.github/workflows/android-ci.yml`).

## Known Limitations

- Requires a Google account — no offline or email/password login is supported
- Swagger UI is only enabled in local development, not on the live Render deployment
- The live database is on Render's free tier and expires 30 days after creation — if testing after this window, the API may require redeployment with a fresh database

Youtube Link: https://youtu.be/4vE5qfamKGI
