# K&B Nexus — Independent Android App

This repository is the working copy of Google-Ai-Nexus. All changes here are isolated from the original Google-Ai-Nexus repository.

## Existing app modules

The current Compose layout is retained. It includes:

- **Admin:** dashboard, employee approvals and management, attendance/GPS, shift overrides, CRM and ISP plans, invoicing, finance, inventory, tasks, complaints, requests, announcements and team chat.
- **Employee:** shift attendance, live GPS status, assigned tasks, support tickets, photo/video media, team chat, requests, profile and salary information.
- **Media:** photo/video records, categories, albums and approval states.

## Clean data and account security

- No demo staff, customers, invoices, shifts, coordinates, chats or media are seeded.
- First launch presents a one-time initial Super Admin setup on the Admin Login tab.
- Employee registration creates employee-only pending accounts.
- Passwords are stored as salted PBKDF2-HMAC-SHA256 hashes, not plaintext.
- Local business records are serialized and encrypted with AES-GCM using a key held in Android Keystore.
- Sensitive repository operations include role and ownership checks.
- Android app backup is disabled so the encrypted local database is not copied without its Keystore key.

## Shift GPS behavior

- A shift starts only after the phone returns a real GPS fix; no default or sample coordinates are used.
- Normal staff shift start is limited to 8:00 AM–7:00 PM (Asia/Kolkata).
- Admins can create off-hours shift overrides with a reason.
- Regular and early-start override shifts auto-close at 7:00 PM. An admin override started after 7:00 PM is auto-closed after eight hours.
- A native location foreground service stores the active shift identity in encrypted local state and uses START_STICKY to restore after eligible Android process restarts.
- The service keeps running when the app task is swiped away, monitors the device Location toggle, and reports GPS-off, permission-denied, waiting-for-fix and live states truthfully.
- Live points and timestamped breadcrumbs are stored locally. Admins can open a received coordinate in Google Maps.

Android can still stop background work after a user force-stops the app, revokes permissions, or applies aggressive manufacturer battery restrictions. No Android app can guarantee restart after a force-stop.

## Current backend boundary

This build currently uses encrypted **on-device** persistence. It does not yet synchronize employee records or GPS points between different phones. Cross-device live GPS, remote admin shift commands, shared chat and centrally enforced authorization require a configured backend (Firebase Auth/Realtime Database or another secured API), deployed security rules, and a registered Google Maps Android API key. Do not treat local role checks as server-side authorization.

## Build and test

GitHub Actions runs unit tests and builds a signed debug APK on pushes to main:

- Workflow: [Build Android Test APK](.github/workflows/build-test-apk.yml)
- Download the `K-B-Nexus-test-apk` artifact from a successful run.