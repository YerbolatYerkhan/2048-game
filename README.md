# 2048 Game for Android

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Language-Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![AdMob](https://img.shields.io/badge/Monetization-AdMob-EA4335?style=for-the-badge&logo=googleadmob&logoColor=white)

A feature-rich, smooth, and modern implementation of the classic **2048 puzzle game** designed specifically for Android. Built with native Android technologies, clean architecture, and custom animations to deliver a seamless user experience.

---

## ✨ Features

- 🎮 **Classic 4x4 Gameplay** — Intuitive swipe gestures, fluid tile animations, and real-time score calculation.
- 💡 **Lifelines & Power-ups** — Custom game mechanics to help players extend gameplay and reach higher milestones.
- 🏆 **Global & Local Leaderboards** — Real-time high score saving and user rankings integrated with Firebase.
- 🔔 **Push Notifications** — Engagement campaigns powered by Firebase Cloud Messaging (FCM).
- 🎨 **Modern Material Design UI** — Responsive layout supporting various screen densities and orientations.
- 📊 **Analytics & Stability** — Comprehensive crash reporting and performance metrics via Firebase Crashlytics and Analytics.
- ⚙️ **Dynamic Remote Config** — Live app configuration changes managed via Firebase Remote Config.

---

## 🛠 Tech Stack & Architecture

- **Language:** Java
- **Minimum SDK:** 28 (Android 9.0 Pie)
- **Target SDK:** 35 (Android 15)
- **Architecture:** Android Jetpack / Model-View Pattern
- **UI Framework:** ConstraintLayout, Material Components, AndroidX
- **Backend & Cloud:**
  - Firebase Realtime Database
  - Firebase Cloud Messaging (FCM)
  - Firebase Crashlytics & Analytics
  - Firebase Remote Config
- **Monetization:** Google AdMob (Rewarded & Interstitial Ads)
- **Build System:** Gradle (Kotlin DSL - build.gradle.kts)

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (Ladybug or newer recommended)
- **JDK 17** or higher
- **Android SDK 35** installed

### Installation & Setup

1. **Clone the repository:**
   git clone https://github.com/YerbolatYerkhan/2048-game.git

2. **Open in Android Studio:**
   Launch Android Studio and select Open..., then navigate to the cloned project directory.

3. **Firebase Setup:**
   Ensure your local google-services.json is correctly configured inside the app/ directory if connecting to a personal Firebase instance.

4. **Build and Run:**
   Sync project Gradle files and run the application on an emulator or a connected physical Android device (Shift + F10).

---

## 🔒 Security & Best Practices

- Key store files (*.jks, *.keystore) and build outputs (*.apk, *.aab) are strictly excluded from version control via .gitignore.
- Configuration parameters and private credentials are kept secure during local builds.

---

## 📜 Privacy Policy

You can read the full official Privacy Policy for 2048:
https://docs.google.com/document/d/10mp4N8yAxRxLo31uIsWS3eZZi4NmEOpMxe38kMUjsMU/edit?usp=drive_web

---

## 👤 Author

Yerkhan Yerbolatuly
GitHub: https://github.com/YerbolatYerkhan
