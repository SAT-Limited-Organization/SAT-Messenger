# SAT Messenger Documentation (GitHub Pages)

Welcome to the `/docs` portal for **SAT Messenger** (`com.aistudio.satmessenger.satlim`, version `4.2.07.11.2026.14.5.1`, `versionCode: 5`).

- **Live Documentation Website:** [`docs/index.html`](index.html) *(enable GitHub Pages from branch `main` → folder `/docs`)*
- **Interactive Web User Manual:** [`docs/manual.html`](manual.html)
- **Architecture, Firestore Schema & CI/CD:** [`docs/architecture.html`](architecture.html)
- **Privacy Policy:** [`docs/privacy.html`](privacy.html)

---

## Quick App Summary

**SAT Messenger** is a native Kotlin and Jetpack Compose enterprise & team messaging application for **SAT Limited**, built on Material Design 3, Firebase Authentication (Google Credential Manager), Firebase AI Logic (`SAT Limited AI`), and real-time Cloud Firestore synchronization.

### Downloads (GitHub & Other Services)

| Package / Artifact | Intended Service / Use Case | Direct Latest Link |
| :--- | :--- | :--- |
| **Signed Release APK** | Direct install, Obtainium, APKPure, Uptodown, MDM, mirrors | [`sat-messenger-release.apk`](https://github.com/SAT-Limited-Organization/SAT-Messenger/releases/latest/download/sat-messenger-release.apk) |
| **Universal Debug APK** | Instant GitHub sideloading & Firebase QA testing | [`sat-messenger-debug.apk`](https://github.com/SAT-Limited-Organization/SAT-Messenger/releases/latest/download/sat-messenger-debug.apk) |
| **Android App Bundle** | Google Play Console submission | [`sat-messenger-release.aab`](https://github.com/SAT-Limited-Organization/SAT-Messenger/releases/latest/download/sat-messenger-release.aab) |
| **SHA-256 Checksums** | Integrity verification across download mirrors | [`SHA256SUMS.txt`](https://github.com/SAT-Limited-Organization/SAT-Messenger/releases/latest/download/SHA256SUMS.txt) |

- **Application ID:** `com.aistudio.satmessenger.satlim`
- **Version:** `4.2.07.11.2026.14.5.1` (`versionCode: 5`)
- **Minimum Android Version:** Android 7.0 (`minSdk 24`, `targetSdk 36`)
- **Repository Releases:** `https://github.com/SAT-Limited-Organization/SAT-Messenger/releases`

---

## Core Capabilities

1. **Google Identity & Pre-Auth Manual:** Google Sign-In via Jetpack `CredentialManager` with automatic Cloud Firestore profile provisioning and a searchable manual accessible directly from the Sign-In screen.
2. **Contacts Sync, Find Friends & Real-Time Availability:** Live synced colleague directory, device `ContactsContract` friend matching, 5-field global search (name, email, phone, primary & secondary `@username`), friend requests, and real-time status indicators.
3. **SAT Limited AI Assistant:** Powered by Firebase AI Logic & App Check with **Smart** (`gemini-3.8-flash`), **Fast** (`gemini-3.5-flash-lite`), and **Reasoning** (`gemini-4-argon`) modes, live **Google Search** grounding with clickable source links, and multimodal photo Q&A.
4. **1-on-1 & Group Messaging, Reactions, Voice & Media:** Typing indicators, read receipts, quoted replies, emoji reactions, 90-second inline voice clips (`RECORD_AUDIO`), and zero-permission Android Photo Picker photo, video, and document sharing.
5. **Custom Icons, Admin Promotion, Channels, Communities & Updates:** Change/remove Profile photos and Group, Channel, and Community icons, manage admin delegation (`adminIds` + protected creators), share deep links (`satmessenger://invite/<code>`), post 24-hour status stories, and check GitHub Releases in-app.

---

## Enabling GitHub Pages for This Folder

1. Open your repository on GitHub: **Settings → Pages**.
2. Under **Build and deployment → Source**, choose **Deploy from a branch**.
3. Select branch **`main`** (or **`master`**) and folder **`/docs`**, then click **Save**.
4. Your documentation site will be published at `https://sat-limited-organization.github.io/SAT-Messenger/`.
