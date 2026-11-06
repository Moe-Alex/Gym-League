# GymLeague Play Console Data Safety Notes

## Application

App name: GymLeague

Application ID: com.gymleague.app

Version: 1.0

## Data Collected

GymLeague may collect the following information when a user uses the application:

- Google account name
- E-mail address

- Profile information supplied through Google Sign-In
- Workout data

- Exercise activity
- Personal records

- XP and level information
- Squad-related data

- Battle Arena challenge activity
- Application preferences

- Firebase Cloud Messaging device token

## Purpose of Data Collection

The collected information is used to:

- authenticate users;
- maintain GymLeague user accounts;

- save workout and fitness progress;
- provide rankings and XP functionality;

- provide squad functionality;
- provide Battle Arena challenges;

- deliver push notifications;
- synchronise data between the app and server;

- support offline functionality; and
- save user preferences.

## Data Sharing

GymLeague does not sell user data.

The application relies on external services required for its normal operation, including:

- Firebase for authentication and push notifications;
- Render for hosting the GymLeague API; and

- Neon for database storage.

These services process information required to provide GymLeague functionality.

The final Play Console Data Safety answers should be checked against Google's current definitions of "collected" and "shared" before submission.

## Encryption

Data sent between GymLeague and the API is transmitted using HTTPS.

This means network communication with the GymLeague API is encrypted in transit.

## Local Data

GymLeague uses Room Database to store selected application data locally on the Android device.

This allows selected information to remain available when the device is offline and supports later synchronisation when an internet connection returns.

## Biometric Data

GymLeague does not collect or store fingerprint or facial biometric data.

Biometric authentication is handled by the Android operating system.

GymLeague only stores whether the user has chosen to enable biometric unlock.

## Push Notification Data

GymLeague stores a Firebase Cloud Messaging device token so that notifications can be delivered to the user's device.

The token is removed or unregistered as part of the logout flow where supported by the application.

## Permissions

### INTERNET

Reason:

GymLeague needs internet access to communicate with the GymLeague API, Firebase Authentication, Firebase Cloud Messaging and other online services.

### ACCESS_NETWORK_STATE

Reason:

GymLeague checks network availability to support offline mode and synchronisation.

### POST_NOTIFICATIONS

Reason:

GymLeague uses notifications for challenge activity and other relevant application updates.

This permission is only required on Android versions that require runtime notification permission.

### USE_BIOMETRIC

Reason:

GymLeague allows the user to optionally protect their logged-in session using fingerprint or face authentication supported by Android.

The application does not access or store the user's biometric information directly.

## Account and Data Deletion

Users may request deletion of their GymLeague account and associated personal information through the GymLeague project team.

The privacy policy should provide the final contact method used for deletion requests.

## Content Rating Preparation

GymLeague is a fitness, workout tracking and social competition application.

The application contains:

- workout tracking;
- fitness statistics;

- user profiles;
- squads;

- rankings; and
- user challenges through Battle Arena.

There is no gambling functionality and no real-money competition.

The final content-rating questionnaire must be completed using the current Play Console questions at the time of submission.

## Final Review Before Play Console Submission

Before completing the Play Console forms:

1. Confirm the final permissions in AndroidManifest.xml.
2. Confirm the privacy policy URL is public and accessible.

3. Confirm the final application features match these notes.
4. Confirm whether any analytics or additional Firebase services were added.

5. Review Google's current definitions of collected data and shared data.
6. Confirm that the signed release APK has been tested on a physical Android device.
