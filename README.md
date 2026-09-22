# GymLeague

Caleb Forbes- ST10438433
Moegammad Alexander- ST10453511
Erin Sarjoo- ST10450571
Keeran Perumel- ST10367702

GymLeague is a native Android fitness tracking app with an ASP.NET Core Web API backend, built for PROG7314 (POE Part 2).

## Important — Login Requirement

**You must sign in with a Google account to use this app.** Authentication is handled through Firebase using Google Sign-In only — there is no email/password login. When testing or demoing the app, make sure the device or emulator has a Google account added under Settings, and that Google Play Services is available (a standard emulator image with Google APIs, or a physical Android device, both work).

## Architecture

- **Android app**: Kotlin, MVVM (View → ViewModel → Repository → Retrofit → API)
- **Backend API**: ASP.NET Core Web API (.NET 10), Entity Framework Core, PostgreSQL
- **Authentication**: Firebase Authentication (Google Sign-In)
- **Hosting**: Render (free tier), deployed via Docker

## Tech Stack

- Kotlin, Retrofit2 + Gson, Firebase Auth, Credential Manager (Google Identity)
- ASP.NET Core, EF Core, PostgreSQL
- GitHub Actions CI (build + unit tests on every push)

## Features

- **Google Sign-In** — secure authentication via Firebase, no separate GymLeague password required
- **Exercise Library & Workout Logging** — browse exercises, log sets/reps/weight per workout session
- **Squad** — connect with other users, view squad members
- **Personal Records** — automatically calculates your estimated 1-rep max (Epley formula) per exercise from your logged workouts, ranked from strongest to weakest lift
- **Settings** — appearance (light/dark/system), notifications, language, logout

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
- Scope was narrowed from the original Part 1 prototype to focus on Exercise Library/Workouts, Squad, and Personal Records
