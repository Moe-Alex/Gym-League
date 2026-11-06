# GymLeague

**Train. Compete. Level up.**

GymLeague is a gamified gym-workout tracker for Android. Users log workouts, earn XP and levels, join squads, challenge each other in the Battle Arena and see how much training volume they put into each muscle group. It is a native Android app (Kotlin, MVVM) backed by our own ASP.NET Core REST API and a cloud PostgreSQL database.

Built for **PROG7314 – Final POE (Part 3)**.

| Student | Student number |
|---|---|
| Caleb Forbes | ST10438433 |
| Moegammad Alexander | ST10453511 |
| Erin Sarjoo | ST10450571 |
| Keeran Perumel | ST10367702 |

**Demo video:** 
https://youtu.be/EnU0XXdKcec?si=UiCQWOQR3ChOE_pG
---

## Contents

1. [Quick start for markers](#1-quick-start-for-markers)
2. [Features](#2-features)
3. [Final POE requirements and where to find them](#3-final-poe-requirements-and-where-to-find-them)
4. [Architecture](#4-architecture)
5. [REST API](#5-rest-api)
6. [Running the project](#6-running-the-project)
7. [Testing and CI/CD](#7-testing-and-cicd)
8. [Release build and Play Store readiness](#8-release-build-and-play-store-readiness)
9. [Team contributions](#9-team-contributions)
10. [Known limitations](#10-known-limitations)
11. [Use of AI](#11-use-of-ai)

---

## 1. Quick start for markers

1. **Use a device with Google Play services.** A physical Android phone (Android 8.0 / API 26 or higher) or an emulator image that says *Google APIs / Google Play*. Huawei phones without Google services cannot sign in with Google.
2. **Install the signed release app:** `release/app-release.apk` (copy it to the phone and open it; allow "install unknown apps" if asked). An `app-release.aab` (Play Store bundle) is in the same folder.
3. **Sign in with Google.** This is the only login method (single sign-on). The first sign-in creates your GymLeague profile automatically.
4. **Wait for the first request.** The API runs on a free Render instance that sleeps when idle, so the very first call can take about 30 to 60 seconds. After that it is fast.
5. **Try biometrics:** Home, tap the gear icon (Settings), switch on **Use biometric unlock**, confirm with your fingerprint or face, then close the app completely and open it again. The system biometric prompt appears.
6. **Try Battle Arena with two accounts:** one account creates a challenge, a second account accepts it. Both devices receive a push notification.

---

## 2. Features

### Sign-in and security
- **Google single sign-on** through Firebase Authentication and Android Credential Manager. There is no email/password login by design (the brief asks for SSO).
- **Biometric unlock** (fingerprint or face, `androidx.biometric`). It is opt-in from Settings. When enabled, the app asks for the biometric on every cold start before any data is shown. Logging out switches it off again.

### Home
Welcome card with your level, total XP and workout count, plus quick access to Settings (gear icon).

### Train (Exercise Library and workout logging)
- Browse and search a shared exercise catalogue, filtered by muscle group.
- Add your own custom exercises. Only the creator can delete a custom exercise; the base catalogue is protected.
- Log a workout (sets, reps, weight, duration) with a built-in timer and rest timer. Logging earns XP, and level is calculated as `1 + XP / 100`.

### Squad
Create a squad or join one, add real registered users by username, see the shared streak and member list, and leave at any time.

### Battle Arena (User Defined Feature 4)
Head-to-head and open challenges between users.
- **Types:** Most Workouts, Longest Streak, Most XP, Most Reps.
- **Create** an open challenge (anyone can accept) or a direct challenge against a specific opponent. The end date is picked from a calendar.
- **Accept / decline** challenges, **log progress**, and the first to reach the target wins the **XP reward** (1 to 500 XP).
- **Rules enforced on the server:** you cannot challenge yourself, only the invited opponent can accept or decline a direct challenge, the end date is inclusive, and a finished challenge cannot be changed.
- **Push notifications** are sent when you are challenged, when a challenge goes live, when it is declined and when it ends.

### Muscle Group Volume (User Defined Feature 5)
Shows total training volume (sets × reps × weight) for each muscle group, with the biggest muscle group highlighted and each group's share of the total. It has its own tab in the bottom navigation.

### Ranks (Personal Records)
Calculates your estimated one-rep max per exercise (Epley formula) from your logged workouts and ranks your lifts from strongest to weakest.

### Settings
Profile summary, notification toggle, appearance (light, dark or system), **language** (English, Afrikaans, isiZulu) and the **biometric unlock** switch. Preferences are saved to the backend, and the language and biometric choice are also kept on the device.

### Offline mode with sync
- Data you have viewed is cached in a **Room** database, so the app still shows exercises, squads, workouts, challenges, records and muscle volume with no internet.
- Workouts logged **offline are saved on the phone** and marked as pending. When the connection returns, **WorkManager** syncs them automatically. The app checks the server first so nothing is sent twice.
- An **offline banner** appears when the phone has no connection. Actions that need the server (for example accepting a challenge) show a clear message instead of failing silently.
- Logging out clears the cached data but keeps workouts that have not synced yet, so nothing is lost.

### Push notifications
Firebase Cloud Messaging. The app registers its device token with our API, and the API sends real-time pushes for Battle Arena events. Android 13+ notification permission is requested at runtime.

### Multi-language
English, **Afrikaans** and **isiZulu**, switched at runtime with `AppCompatDelegate.setApplicationLocales`. All user-facing text lives in `values/`, `values-af/` and `values-zu/` string resources.

---

## 3. Final POE requirements and where to find them

| Requirement | Where it is |
|---|---|
| Runs on a physical device | Demo video; push tested on a real phone |
| Biometric authentication | `biometric/BiometricLockPolicy.kt`, `SplashActivity.kt`, `SettingsFragment.kt` |
| Offline mode with Room and sync | `database/`, `sync/`, `util/OfflineLoader.kt`, `util/NetworkMonitor.kt` |
| Real-time push notifications (FCM) | `GymLeagueMessagingService.kt`, `PushTokenManager.kt`, API `Services/FcmPushNotificationService.cs` |
| Multi-language, 2 South African languages | `res/values-af/`, `res/values-zu/` |
| User Defined Feature 4: Battle Arena | `BattleActivity.kt`, `viewmodel/ChallengeViewModel.kt`, API `ChallengesController.cs` |
| User Defined Feature 5: Muscle Group Volume | `MuscleVolumeActivity.kt`, API `WorkoutsController.cs` (`musclegroupvolume`) |
| Single sign-on | `LoginActivity.kt` (Google, Firebase) |
| Own REST API and database | `GymLeagueAPI/` (ASP.NET Core + PostgreSQL) |
| Settings screen | `SettingsFragment.kt` |
| Custom app icon | `res/mipmap-*/` |
| Logging | `Log.d/w/e` calls with tags such as `GymLeagueAuth`, `GymLeagueBattle`, `GymLeagueOffline`, `GymLeagueBiometric` |
| Unit tests and CI | `app/src/test`, `GymLeagueAPI.Tests`, `.github/workflows/` |
| Signed APK | `release/app-release.apk`, `release/app-release.aab` |
| Release notes | `RELEASE_NOTES.md` |
| Play Store material | `PLAY_STORE_LISTING.md`, `PRIVACY_POLICY.md`, `DATA_SAFETY_NOTES.md` |

---

## 4. Architecture

```
Activity / Fragment  ->  ViewModel  ->  Repository  ->  Retrofit  ->  ASP.NET Core API  ->  PostgreSQL
                                           |
                                           +-> Room (offline cache and pending-workout outbox)
```

- **Android:** Kotlin, MVVM, Kotlin coroutines with StateFlow/SharedFlow, Retrofit + Gson, Room, WorkManager, Firebase Auth and Cloud Messaging, Credential Manager, AndroidX Biometric, Material components. `minSdk 26`, `targetSdk 37`.
- **Offline pattern:** network first. A successful response is saved to Room; if the network fails, the last saved copy is shown with a notice. Pending workouts live in the Room `workouts` table with a `synced` flag and are sent by `SyncWorker`.
- **Backend:** ASP.NET Core Web API (.NET 10), Entity Framework Core, Npgsql. Firebase Admin SDK sends push notifications; if the credentials are missing the API starts with a no-op sender instead of failing.
- **Database:** PostgreSQL hosted on Neon.
- **Hosting:** the API is built into a Docker image by GitHub Actions, pushed to Docker Hub and deployed to Render through a deploy hook.

Main source folders (`GymLeagueApp/app/src/main/java/`):

| Folder | Contents |
|---|---|
| `com/gymleague/app/` | Activities, fragments, session, push and theme helpers |
| `com/gymleague/app/viewmodel/` | ViewModels |
| `com/gymleague/app/repository/` | Repositories (API plus Room) |
| `com/gymleague/app/network/` | Retrofit client and API interface |
| `com/gymleague/app/models/` | Data classes |
| `com/gymleague/app/adapter/` | RecyclerView adapters |
| `com/gymleague/app/biometric/` | Biometric lock rule |
| `com/gymleague/app/util/` | Offline rules, network monitor, error parsing, helpers |
| `database/`, `sync/` | Room database, entities, DAOs and the WorkManager sync |

---

## 5. REST API

Base URL: `https://gymleague-api.onrender.com/` (set in `RetrofitClient.kt`).

| Resource | Endpoints |
|---|---|
| Users | `GET api/users`, `GET api/users/{id}`, `GET api/users/firebase/{firebaseUid}`, `GET api/users/username/{username}`, `POST api/users` |
| User settings | `GET api/usersettings/user/{userId}`, `POST api/usersettings`, `PUT api/usersettings/{id}` |
| Exercises | `GET api/exercises`, `GET api/exercises/{id}`, `GET api/exercises/musclegroup/{muscleGroup}`, `POST`, `PUT {id}`, `DELETE {id}` |
| Workouts | `GET api/workouts/user/{userId}`, `GET api/workouts/{id}`, `POST api/workouts`, `DELETE api/workouts/{id}`, `GET api/workouts/musclegroupvolume/{userId}` |
| Personal records | `GET api/personalrecords/user/{userId}`, `GET api/personalrecords/user/{userId}/exercise/{exerciseId}` |
| Squads | `GET api/squads`, `GET api/squads/{id}`, `POST api/squads`, `POST api/squads/{squadId}/members`, `DELETE api/squads/{squadId}/members/{userId}` |
| Challenges (Battle Arena) | `GET api/challenges`, `GET api/challenges/{id}`, `POST api/challenges`, `POST api/challenges/{id}/accept`, `POST {id}/decline`, `POST {id}/progress`, `DELETE {id}` |
| Device tokens (push) | `POST api/devicetokens`, `DELETE api/devicetokens` |

Swagger UI is enabled when the API runs locally in the Development environment.

---

## 6. Running the project

### Android app
1. Clone the repository and open the `GymLeagueApp/` folder in Android Studio (latest stable, JDK 17 or newer).
2. Let Gradle sync. `google-services.json` for our Firebase project is already in `app/`.
3. Use the **debug** build. It is signed with the `debug.keystore` that is committed on purpose, so every team member has the same fingerprint, which is registered in Firebase for Google sign-in. (This is a shared test key only; the release keystore is **not** in the repository.)
4. Run on a device or emulator with Google Play services and a Google account added.

To use a different Firebase project, replace `app/google-services.json` and add your debug SHA-1 in the Firebase console.

### API (optional, the live one is already deployed)
```
cd GymLeagueAPI/GymLeagueAPI
dotnet run
```
Set the `ConnectionStrings__DefaultConnection` environment variable for your own PostgreSQL database, and `FIREBASE_CREDENTIALS_BASE64` (base64 of a Firebase service-account key) to enable push notifications. Apply database migrations with `dotnet ef database update`.

---

## 7. Testing and CI/CD

| Suite | Count | Run with |
|---|---|---|
| Android unit tests (JUnit) | 64 | `cd GymLeagueApp && ./gradlew test` |
| API unit tests (xUnit) | 54 | `cd GymLeagueAPI/GymLeagueAPI.Tests && dotnet test` |

What is covered: offline and sync rules, JSON list helpers, API error parsing, challenge helpers, muscle-volume maths and the biometric lock rule on Android; and the Battle Arena rules, level calculation, challenge and device-token controllers and workout controller on the API.

GitHub Actions (`.github/workflows/`):
- **Android CI**: unit tests and debug build on every push and pull request.
- **API CI**: builds the API and runs its tests.
- **Deploy API**: builds the Docker image, pushes it to Docker Hub and triggers the Render deploy when API files change.

---

## 8. Release build and Play Store readiness

- **Signed release APK and AAB:** `release/app-release.apk` and `release/app-release.aab`, version 1.0 (`versionCode 1`), package `com.gymleague.app`, signed with a private GymLeague release key.
- **Release notes:** `RELEASE_NOTES.md`
- **Store listing text:** `PLAY_STORE_LISTING.md`
- **Privacy policy:** `PRIVACY_POLICY.md`
- **Data safety answers:** `DATA_SAFETY_NOTES.md`
- Custom launcher icon, adaptive icon, light/dark/system theme and runtime permission handling (notifications) are included.

---

## 9. Team contributions

| Member | Main responsibilities |
|---|---|
| **Moegammad Alexander** | REST API, database and deployment, Firebase push (FCM), Battle Arena (UD4), Muscle Group Volume (UD5), API tests and CI/CD, README, final release |
| **Caleb Forbes** | Offline mode: Room database, caching, offline workout logging, WorkManager sync, network monitor and offline banner |
| **Erin Sarjoo** | Multi-language support (Afrikaans, isiZulu), UI polish, custom app icon |
| **Keeran Perumel** | Biometric authentication, Play Store readiness, privacy policy, data-safety notes, release notes, signed APK/AAB |

Every member committed to the repository; see the commit history for detail.

---

## 10. Known limitations

- **Google account and Google Play services are required.** Phones without them (for example newer Huawei models) cannot sign in. There is no email/password login by design.
- **Free hosting:** the Render instance sleeps when idle, so the first request after a quiet period can take 30 to 60 seconds.
- **No server-side authentication tokens.** The API trusts the `userId` sent by the app. This is acceptable for the POE but would need token validation in production.
- **Battle Arena actions need internet.** Offline the saved list can be viewed, but creating, accepting, declining and logging progress require a connection.
- **Biometric lock is checked on cold start only,** not when returning from the background, and it is opt-in from Settings.
- **Push taps:** tapping a notification when the app is closed opens the app; it does not jump to the specific challenge.
- **Some text stays in English:** exercise and muscle-group names that come from the database, push notification text built on the server, and a few raw server error messages.
- **Language preference** is applied when Settings is saved. On a new device it takes effect after the user saves Settings once.
- **Accepting the same open challenge twice at the same instant** is only partly guarded on the server.
- **Swagger** is available only in local development, not on the live deployment.

---
