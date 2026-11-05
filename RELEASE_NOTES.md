\# GymLeague Release Notes



\## Version 1.0

\*\*Release Date:\*\* 6 November 2026



\### New



\- Added biometric unlock using Android fingerprint or face authentication.

\- Added Battle Arena challenges for competitive user interaction.

\- Added Muscle Volume tracking to help users monitor training volume.

\- Added push notifications for important GymLeague updates and challenge activity.

\- Added offline support using Room Database so users can continue accessing saved data without an internet connection.

\- Added background synchronisation for offline workout data.

\- Added multi-language support for English, Afrikaans and isiZulu.

\- Added improved profile and settings functionality.

\- Added support for secure Google authentication.



\### Improved



\- Improved the overall user interface for a more consistent GymLeague experience.

\- Improved navigation across the main application screens.

\- Improved light and dark mode support.

\- Improved loading, empty and error states across the application.

\- Improved offline handling and feedback when network connectivity is unavailable.

\- Improved authentication logging to avoid exposing personal or sensitive user information.

\- Improved startup security by protecting logged-in sessions with optional biometric authentication.



\### Fixed



\- Fixed inconsistent hard-coded text by moving user-facing text into language resources.

\- Fixed settings persistence for appearance and language preferences.

\- Fixed session handling when users log out.

\- Fixed offline data handling so saved information can still be shown without network access.

\- Fixed application startup so biometric authentication is required before protected app data is displayed when biometric unlock is enabled.



\## Known Notes



\- Biometric unlock requires a compatible Android device with fingerprint or face authentication enrolled.

\- Some application data still requires an internet connection for live server updates.

\- The release APK should be tested on a physical Android device before final submission.

