# SAT Messenger

> A modern messaging platform built by SAT Limited.

SAT Messenger is a messaging application currently under development, designed to provide fast, simple and reliable communication between users.

The project is being built with Android, Firebase and modern cloud technologies.

---

## 🚧 Development Status

**SAT Messenger is currently under active development.**

Features are being added and improved continuously.

### Current progress

- [x] Android project foundation
- [x] Firebase project integration
- [x] Firebase Authentication
- [x] Email and password registration
- [x] Email and password login
- [x] User profiles
- [x] Find People
- [x] Private chat foundation
- [x] Firestore message storage
- [x] Real-time message listener
- [x] GitHub Actions APK building
- [ ] Recent conversations
- [ ] Online/offline status
- [ ] Last seen
- [ ] Message delivery status
- [ ] Read receipts
- [ ] Profile pictures
- [ ] Push notifications
- [ ] Group chats
- [ ] Image and file sharing
- [ ] Message deletion
- [ ] Dark mode
- [ ] Settings
- [ ] More features

---

## 📱 Features

### 🔐 Authentication

Users can create an account and securely sign in using:

- Email
- Password
- Firebase Authentication

### 👤 User Profiles

Each account can have:

- Display name
- Email address
- Profile picture
- About/status
- Online status
- Last seen information

### 👥 Find People

Users can search for other registered SAT Messenger users and start conversations.

### 💬 Private Messaging

SAT Messenger supports one-to-one conversations using Firebase Firestore.

Messages are stored in real time and displayed inside the chat interface.

### ☁️ Firebase

Firebase is used as the backend infrastructure for:

- Authentication
- Cloud Firestore
- Cloud Storage
- Firebase Cloud Messaging

### ⚙️ Automated Builds

GitHub Actions is used to automatically build the Android application.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Java | Android application development |
| Android SDK | Mobile application platform |
| Firebase Authentication | User authentication |
| Cloud Firestore | Users, chats and messages |
| Firebase Storage | Media storage |
| Firebase Cloud Messaging | Push notifications |
| Gradle | Android build system |
| GitHub Actions | Automated builds |
| GitHub | Source code management |

---

## 📂 Project Structure

```text
SAT-Messenger/
│
├── app/
│   ├── build.gradle
│   │
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           │
│           ├── java/
│           │   └── com/
│           │       └── sat_limited/
│           │           └── satmessenger/
│           │               ├── MainActivity.java
│           │               ├── RegisterActivity.java
│           │               ├── HomeActivity.java
│           │               ├── UsersActivity.java
│           │               ├── ChatActivity.java
│           │               ├── User.java
│           │               ├── UserAdapter.java
│           │               ├── Message.java
│           │               └── MessageAdapter.java
│           │
│           └── res/
│               ├── layout/
│               └── ...
│
├── .github/
│   └── workflows/
│       └── build.yml
│
├── build.gradle
├── settings.gradle
├── gradle.properties
├── .gitignore
├── LICENSE
└── README.md


---

🔥 Firebase Architecture

SAT Messenger uses Cloud Firestore to organize application data.

Users

users/
└── userId/
    ├── displayName
    ├── email
    ├── photoUrl
    ├── about
    ├── online
    └── lastSeen

Chats

chats/
└── chatId/
    ├── participants
    ├── lastMessage
    ├── updatedAt
    │
    └── messages/
        └── messageId/
            ├── senderId
            ├── text
            └── timestamp


---

🔒 Security

SAT Messenger uses Firebase Authentication and Firestore Security Rules to control access to application data.

Firebase configuration files containing private project configuration are intentionally excluded from the public repository.

The project will continue to receive security improvements as development progresses.


---

🧪 Development

This project is currently intended for development and testing.

The application should not be considered production-ready until authentication, database rules, messaging, media handling, notifications and other security-sensitive components have been fully reviewed.


---

📦 Building

The project can be built automatically through GitHub Actions.

The workflow builds a debug APK and makes the resulting APK available as a GitHub Actions artifact.


---

🗺️ Roadmap

Phase 1 — Foundation

Android project

Firebase integration

Authentication

User profiles


Phase 2 — Messaging

Find People

Private conversations

Real-time messages

Recent conversations


Phase 3 — Communication

Online status

Last seen

Delivery status

Read receipts

Push notifications


Phase 4 — Media

Profile pictures

Image sharing

File sharing


Phase 5 — Social Messaging

Group chats

Group management

Group profiles


Phase 6 — Polish

Dark mode

Settings

Improved UI

Performance improvements

Security review



---

📸 Screenshots

Screenshots will be added as the application UI develops.


---

🤝 Contributing

SAT Messenger is currently under active development.

Suggestions, bug reports and improvements are welcome as the project grows.


---

📄 License

This project is licensed under the Apache License 2.0.

See the LICENSE file for details.


---

🏢 SAT Limited

SAT Messenger is a project developed by SAT Limited.

> Connect. Chat. Stay connected.

---

SAT Messenger — Still in development. 🚀