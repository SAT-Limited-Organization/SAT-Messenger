# SAT Messenger

[![Android](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase%20Auth%20%26%20Firestore-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)
[![Version](https://img.shields.io/badge/Version-1.2.07.11.2026.14.5.0-0A2540)](app/build.gradle.kts)

**SAT Messenger** is a native Kotlin and Jetpack Compose enterprise & team messaging application for **SAT Limited**, built on Material Design 3, Firebase Authentication (Google Credential Manager), and real-time Cloud Firestore synchronization.

---

## Download APK (GitHub)

> **Important Setup Note:** Replace `<OWNER>/<REPO>` below with your GitHub username and repository name after pushing this project to GitHub. A live download URL is created only after the project is pushed to your GitHub repository and either a GitHub Release is published or the included GitHub Actions workflow completes.

[![Download SAT Messenger APK](https://img.shields.io/badge/Download-SAT%20Messenger%20APK-00BFA5?style=for-the-badge&logo=android&logoColor=white)](https://github.com/SAT-Limited-Organization/SAT-Messenger/releases/latest/download/sat-messenger-debug.apk)

- **Direct Release Asset Link (after pushing to GitHub):**  
  `https://github.com/SAT-Limited-Organization/SAT-Messenger/releases/latest/download/sat-messenger-debug.apk`
- **All GitHub Releases:**  
  `https://github.com/SAT-Limited-Organization/SAT-Messenger/releases`
- **Application ID:** `com.aistudio.satmessenger.satlim`
- **Version:** `1.2.07.11.2026.14.5.0` (`versionCode: 1`)
- **Minimum Android Version:** Android 7.0 (`minSdk 24`, `targetSdk 36`)

---

## How to Get the APK & Publish the GitHub Download Link

This repository includes an automated release workflow at [`.github/workflows/release-apk.yml`](.github/workflows/release-apk.yml). Follow either path below depending on how you deploy:

### Option 1: Automatic GitHub Release Download Link (Recommended)
1. **Ensure Firebase & Keystore config are available to GitHub Actions:**
   - Either commit `app/google-services.json` and `debug.keystore.base64` to the repository, **or** add them under **GitHub Repository → Settings → Secrets and variables → Actions**:
     - `GOOGLE_SERVICES_JSON`: Full JSON (or base64) contents of `app/google-services.json`.
     - `DEBUG_KEYSTORE_BASE64` *(optional, required for Google Sign-In SHA-1 matching)*: Base64 contents of `debug.keystore.base64` (if omitted, CI generates a fallback debug keystore automatically).
2. **Push the project to GitHub** (from Google AI Studio **Settings -> Push to GitHub**, or using `git push` to `main`).
3. Once pushed, GitHub Actions automatically runs **Build & Release Android APK** ([`.github/workflows/release-apk.yml`](.github/workflows/release-apk.yml)):
   - Restores `app/google-services.json` and `debug.keystore`.
   - Compiles the APK via `gradle :app:assembleDebug`.
   - Publishes a GitHub Release tagged `latest` (or the pushed `v*` tag) with **`sat-messenger-debug.apk`** attached.
4. Download the APK directly from **Releases → `sat-messenger-debug.apk`** using the badge link above.

### Option 2: Generate APK Directly in Google AI Studio
1. Open the **Settings** menu in Google AI Studio Build.
2. Select **Generate APK / AAB** to build and download `app-debug.apk` (also produced locally at `app/build/outputs/apk/debug/app-debug.apk`).
3. On GitHub, open your repository -> **Releases -> Draft a new release**, attach the downloaded APK as `sat-messenger-debug.apk`, and publish the release.

### Option 3: Build Locally via Gradle
```bash
# Build signed debug APK
gradle :app:assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Build release APK (requires release keystore env vars)
gradle :app:assembleRelease
```

> **Android Sideloading Note:** When installing `.apk` builds outside Google Play, allow **"Install unknown apps"** for your browser or file manager when prompted by Android.

---

## Key Features

### Authentication & Onboarding
- **Google Identity Sign-In:** Authenticate securely with **Google** (Jetpack `CredentialManager` + Firebase Auth) with automatic Cloud Firestore profile provisioning.
- **Pre-Auth & In-App Manual:** A synchronized **App Manual & User Guide** is available directly on the Sign-In / Registration screen as well as inside the authenticated app, searchable by feature and category.

### Contacts & Real-Time Availability
- **Firestore-Synced Contact Directory:** Syncs registered user profiles in real time under `users/{userId}`.
- **Live Availability Statuses:** Broadcast and view `Available`, `Busy`, `Away`, `In a Meeting`, `Do Not Disturb`, and `Offline` (with relative `Last seen` timestamps) across Contacts, Chats, Conversation Headers, and Profile.
- **Friends & Filtering:** Star/unstar favorite contacts into your private `/users/{userId}/contacts/{contactId}` Friends list, filter by **All**, **Online**, **Friends**, or **Colleagues**, and search by name, `@handle`, email, phone, or availability.

### SAT Limited AI Assistant
- **Powered by Firebase AI Logic & App Check:** Conversational AI assistant (`SAT Limited AI`) built directly into the Chats screen with multi-turn context, streaming answers, and zero hardcoded API keys.
- **Model Modes & Multimodal Input:** Switch between **Smart** (`gemini-3.8-flash`), **Fast** (`gemini-3.5-flash-lite`), and **Reasoning** (`gemini-4-argon`), and attach photos via the Android Photo Picker for multimodal visual Q&A.
- **Google Search Grounding:** Toggle **Google Search** grounding to enrich answers with live web searches and clickable citation source links.

### Messaging, Reactions, Media & Voice Clips
- **1-on-1 & Group Chats:** Real-time messages with live typing indicators, read receipts, unread badges, quoted replies, and `MessageBubble` styling for incoming/outgoing bubbles.
- **Emoji Reactions:** Long-press any message to add or toggle emoji reactions (`👍`, `❤️`, `😂`, `😮`, `😢`, `🙏`, `🔥`, `🎉`, `👏`, `🚀`, plus an extended emoji picker) stored in Firestore.
- **Voice Recording & Playback:** Record inline audio clips up to 90 seconds using the device microphone (`RECORD_AUDIO` runtime permission), preview before sending, and play/scrub directly inside chat bubbles.
- **Photo, Video & Document Sharing:** Zero-permission Android Photo Picker (`ImageAndVideo`) and document attachment composer in Chats and Status, full-screen image viewer & inline `VideoView` clip player, and message deletion/chat clearing.

### Admin Promotion, Flexible Channels, Communities & Status
- **Admin Promotion (Groups, Channels & Communities):** Creators and existing admins can promote or dismiss fellow members as Admins in **Group Rooms**, **Channels**, and **Communities**—backed by Cloud Firestore security rules that protect creators and prevent privilege escalation.
- **Flexible Broadcast Channels:** Create public or invite-only channels with customizable categories, follower posting permissions, post reactions, and admin role management.
- **Communities:** Group related departmental rooms and an announcements hub under shared communities with member/admin delegation.
- **Shareable Invite Links & Deep Links:** Generate, copy, revoke/reset, and redeem invite codes and `satmessenger://invite/<code>` / `https://satmessenger.app/invite/<code>` links for **Channels**, **Group Rooms**, and **Communities**.
- **24-Hour Status Updates:** Share text, photos, or short video clips as status stories with viewer tracking and inline replies.

### Phone Notification Center Alerts
- **System Notifications:** Dedicated Android notification channels (`Direct & Group Messages` and `Channels & Alerts`) with runtime `POST_NOTIFICATIONS` permission handling on Android 13+ and deep-link navigation straight into the relevant chat.

---

## Architecture & Project Structure

The app follows **MVVM** + **Clean Repository Architecture** with reactive Kotlin `StateFlow` pipelines and Jetpack Compose Material 3:

```text
├── .github/workflows/
│   └── release-apk.yml               # Automated APK build & GitHub Release publisher
├── app/
│   ├── build.gradle.kts              # Android app build config & dependencies
│   ├── google-services.json          # Firebase client configuration
│   └── src/main/java/com/example/
│       ├── MainActivity.kt           # Edge-to-edge host, lifecycle presence & permissions
│       ├── audio/                    # VoiceRecorderManager & VoicePlayerManager
│       ├── data/
│       │   ├── model/                # Firestore models & AvailabilityStatus
│       │   └── repository/           # Auth, User, Chat, SocialFeatures, Invite & SatAi repositories
│       ├── notifications/            # AppNotificationManager & channels
│       ├── util/                     # App Check init & MediaAttachmentHelper
│       └── ui/
│           ├── components/           # MessageBubble, VoiceComposer, Avatars & Status pills
│           ├── screens/              # AI, Auth, Chats, Chat/Details, Contacts, Channels, Communities, Status, Profile, Search, Dialogs, Manual
│           ├── theme/                # Material 3 color scheme & typography
│           └── viewmodel/            # MessengerViewModel
├── firebase-blueprint.json           # Firestore schema & security architecture blueprint
├── firestore.rules                   # Zero-trust Firestore security rules
└── firestore.test.js                 # Node emulator security rule tests
```

---

## Tech Stack

| Layer | Technology |
| :--- | :--- |
| Language & Async | Kotlin `2.2.10`, Coroutines & Flow |
| UI Framework | Jetpack Compose (BOM `2024.09.00`), Material Design 3 |
| AI & Grounding | Firebase AI Logic (`firebase-ai`), Firebase App Check, Google Search Grounding |
| Authentication | Firebase Auth, Jetpack `CredentialManager`, Google Identity |
| Cloud Database | Cloud Firestore with real-time snapshot listeners & server timestamps |
| Media & Audio | Coil Compose `2.7.0`, Android Photo Picker (Image & Video), `VideoView`, `MediaRecorder` / `MediaPlayer` |
| Testing | JUnit 4, Robolectric `4.16.1`, Compose UI Test, Firebase Auth & Firestore Emulators |

---

## Permissions

| Permission | Usage |
| :--- | :--- |
| `INTERNET` / `ACCESS_NETWORK_STATE` | Real-time Firebase Authentication & Cloud Firestore sync |
| `POST_NOTIFICATIONS` | Incoming message alerts in the Android Notification Center (Android 13+) |
| `RECORD_AUDIO` | Runtime permission requested only when recording voice clips in chat |

Image selection uses the Android Photo Picker (`PickVisualMedia`) and requires no broad storage permissions.

---

## Verification & Testing

Run both the Firestore security rules suite and Android Robolectric/Compose unit tests locally against the Firebase Emulators:

```bash
# Run Firestore rules tests and Android debug unit tests together
firebase emulators:exec --only auth,firestore --project demo-no-project \
  "node --test firestore.test.js && gradle :app:testDebugUnitTest"
```

---

## Configuration Notes

- **Firebase:** Client configuration is stored in `app/google-services.json` (project `sat-limited`). Deploy rule updates with `firebase deploy --only firestore:rules`.
- **Release Signing:** Configure `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` environment variables or CI secrets when building a Play-signed release APK/AAB (`gradle :app:assembleRelease`).
- **Secrets:** Environment keys belong in AI Studio's **Secrets** panel (mapped via `.env.example`), never hardcoded in source files.
