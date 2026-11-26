# Contributing to RNA. TV

Thank you for your interest in contributing to **RNA. TV**! Whether you are reporting a bug on a specific Android TV box, proposing a new AI tool or launcher layout, or submitting a pull request, your contributions help make RNA. TV faster, smarter, and better for everyone.

---

## 🧭 Code of Conduct

Please keep all discussions, issues, and code reviews respectful, constructive, and welcoming. Focus on technical merit and helping fellow contributors.

---

## 🐞 Reporting Bugs

Before opening a new issue, please search existing [Issues](https://github.com/frosty-sys/rnatv/issues) to avoid duplicates. When submitting a bug report, please include:

* **Device & OS Details:** TV model or streaming box (e.g., Chromecast with Google TV, Nvidia Shield, Mi Box, Sony Bravia) and Android TV version (Android 10+).
* **App Version:** The exact release tag (e.g., `v3.7.7`) or commit hash.
* **Steps to Reproduce:** Clear step-by-step instructions (including whether Accessibility Service or RNA. Keyboard IME was active).
* **Expected vs. Actual Behavior:** What you expected to happen and what actually occurred.
* **Logs / Screenshots:** Relevant `adb logcat` snippets or screenshots/photos of the TV screen if applicable.

---

## 💡 Suggesting Features

We welcome ideas for new launcher widgets, remote remapping actions, `.rna` web container APIs, and keyless AI improvements. Open a feature request issue describing:

* The problem or TV workflow you want to improve.
* How the feature should behave using a standard TV D-Pad remote.
* Any performance or memory considerations for low-RAM TV devices.

---

## 🛠️ Setting Up Your Development Environment

### Prerequisites

* **Android Studio** (latest stable version with Kotlin 2.4+ support)
* **JDK 17** or newer
* **Android SDK 29+** (Android 10 minimum target)
* An **Android TV / Google TV Emulator** or a physical Android TV device connected via ADB (`adb connect <TV_IP>:5555`)

### Local Setup

1. **Fork & Clone** the repository:
   ```bash
   git clone https://github.com/frosty-sys/rnatv.git
   cd rn-tv
   ```
2. **Environment Configuration:** If your local build uses environment properties, copy `.env.example` to `.env` (never commit real secrets or keys).
3. **Sync & Build:** Open the project in Android Studio, let Gradle (`build.gradle.kts`) sync dependencies, and run the `app` configuration on your Android TV target.

---

## 🧪 Testing

Run the JVM and instrumented test suites before opening a pull request:

```bash
./gradlew test
./gradlew connectedAndroidTest
```

* `app/src/test/` contains local unit and Robolectric tests.
* `app/src/androidTest/` contains on-device instrumented tests.
* Test on both a 1080p TV emulator and a 4K physical or virtual target when changing layouts or TV navigation.
* Verify D-Pad navigation using `Up`, `Down`, `Left`, `Right`, `Center/OK`, and `Back`.
* Build the debug APK locally with:

```bash
./gradlew assembleDebug
```

For UI changes, include screenshots or a short recording that demonstrates D-Pad navigation and TV behavior.

---

## 📁 Project Structure

```text
rnatv/
├── app/
│   ├── src/
│   │   ├── main/                 # Application source and resources
│   │   │   ├── assets/           # Wallpapers and live wallpaper media
│   │   │   ├── java/com/example/ # Activities, services, UI, data, and utilities
│   │   │   └── res/              # Android resources, themes, icons, and XML configs
│   │   ├── test/                 # Local JVM and Robolectric tests
│   │   └── androidTest/          # Instrumented Android tests
│   └── build.gradle.kts          # App module configuration
├── assets/                       # Repository release assets
├── fastlane/                     # Android store metadata
├── public/                       # Public web assets
├── gradle/                       # Gradle version catalog and wrapper
├── build.gradle.kts              # Project-level Gradle configuration
└── settings.gradle.kts           # Gradle module settings
```

Key areas in `app/src/main/java/com/example/`:

* `ui/tabs/` — Main TV tab screens and phone controller UI.
* `ui/tv/` — Launcher, AI agent, browser, remapper, controller bridge, and wallpaper features.
* `ui/theme/` — Material 3 Expressive Compose theme definitions.
* `ui/wizard/` — App creation wizard and logo builder components.
* `data/` — Room database entities, DAO, and repository logic.
* `util/` — Package, shortcut, app-check, and helper utilities.

---

## 🎨 Coding & TV UX Standards

Because **RNA. TV** runs on living-room televisions with varying hardware specs, all pull requests must follow these core principles:

1. **100% D-Pad First Navigation:** Every new screen, dialog, keyboard overlay, and button must be fully navigable and clearly highlighted using only a 5-way D-Pad (`Up`, `Down`, `Left`, `Right`, `Center/OK`, `Back`) without requiring a mouse or touchscreen.
2. **Low-RAM & Performance Mode Friendly:** Respect the global **Performance Mode** toggle by disabling heavy animations when enabled, and avoid memory leaks in background services (Accessibility, IME, Live Wallpapers, and WebView containers).
3. **Zero Hardcoded Developer API Keys:** Never commit API keys, tokens, or credentials. Any new AI provider or service integration must work via keyless public endpoints or user-supplied keys stored locally on-device.
4. **Local-First Privacy:** User profiles, browser sessions, clipboard history, and telemetry must remain 100% local on the user's device.
5. **Idiomatic Kotlin & Jetpack Compose:** Follow standard Kotlin style conventions, keep composables modular and stateless where possible, and test across both 1080p and 4K TV aspect ratios.

---

## 🔀 Pull Request Process

1. Create a descriptive feature or fix branch from `main`:
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. Keep commits focused, atomic, and clearly written.
3. Verify that the project builds cleanly without new warnings or regressions on an Android TV target.
4. Open a **Pull Request** against `main` with a clear summary of your changes, linked issues, and screenshots/videos demonstrating D-Pad navigation on TV.

---

## ⚖️ License

By contributing to **RNA. TV**, you agree that your contributions will be licensed under the **GNU General Public License v3.0 (GPL-3.0)** — see the [LICENSE](LICENSE) file for full details.
