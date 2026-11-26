<div align="center">
  <img src="app/metadata/android/en-US/images/cropped_circle_image.png" alt="RNA. TV circular icon" width="128" height="128">

  # RNA. TV

  ***The ultimate Android TV & Google TV launcher, web container, and AI ecosystem***

  [![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)
  [![Latest release](assets/release-badge.svg)](https://github.com/frosty-sys/rnatv/releases/latest)
  [![Android 10+](https://img.shields.io/badge/Android-10%2B%20(API%2029)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/about/versions/10)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
  [![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
  [![Issues](https://img.shields.io/github/issues/frosty-sys/rnatv?style=flat-square)](https://github.com/frosty-sys/rnatv/issues)
</div>

<br>

![RNA. TV banner](app/src/main/res/drawable/ic_tv_banner2.png)

**RNA. TV** is a high-performance, ad-free Android TV / Google TV launcher and application runtime built with Kotlin and Jetpack Compose. It combines native Android capabilities with a built-in web browser, offline profiles, remote control tools, a phone controller, live wallpapers, and an integrated AI assistant.

---

## 🌟 Key Features

* **Ad-Free Google TV Launcher:** A clean, fast home screen with customizable single-row or two-row app docks, recent-app history, and instant app launching.
* **Remote Button Remapper:** Remap single, double, triple, and long remote-button presses to apps, AI features, Recent Apps, Quick Actions, or other actions.
* **43+ Bundled 4K Wallpapers:** Choose from bundled 4K preset wallpapers or use your own photos. Native live wallpaper support is also included.
* **Multi-User Offline Profiles:** Create separate profiles with 28 preset avatars or local photos, with isolated or shared browser data.
* **Integrated TV Web Browser:** Browse the web with an on-screen D-Pad keyboard, full-screen mode, downloads, and one-tap conversion of websites into standalone TV apps.
* **RNA. AI Assistant & Agentic Control:** Use 52+ free keyless AI models or an optional user-provided key, retain local conversation history, and control supported TV actions through the assistant.
* **RNA. Controller & Gamepad Bridge:** Control the TV from a phone over local Wi-Fi using a Material 3 Expressive remote/gamepad interface and remappable Android or Xbox-style buttons.
* **Performance Mode:** Disable animations from the Control Panel for faster response on resource-constrained TV boxes.

---

## 📋 System Requirements

* Android 10 or later (API 29+)
* Android TV or Google TV device
* An internet connection for browsing, updates, controller pairing, and cloud AI features
* Accessibility, overlay, usage-access, or installer permissions may be required for specific features
* A D-Pad remote or game controller is recommended
* Google Play Services are not required for all local launcher features

> Hardware requirements vary depending on device specifications and enabled features.

---

## 📥 Installation

### Recommended: Download a Release

1. Open the [latest RNA. TV release](https://github.com/frosty-sys/rnatv/releases/latest).
2. Download the GitHub APK:
   **`RNA_v3.7.7-github.apk`**
3. Transfer the APK to your Android TV or Google TV device.

The `-github` build supports in-app updates. F-Droid builds may receive updates only through F-Droid or another F-Droid-compatible store.

---

### Option 1: Transfer with Send Files to TV

#### Step 1: Install Send Files to TV

Install **Send Files to TV (SFTV)** on both devices:

* Android TV or Google TV
* Android phone or tablet

#### Step 2: Transfer the APK

1. Download the APK on your phone.
2. Open SFTV on both devices.
3. Select **Receive** on the TV and **Send** on the phone.
4. Select the RNA. TV APK and send it to the TV.

#### Step 3: Install the APK

1. Open your file manager or the file explorer inside SFTV.
2. Open the transferred APK.
3. Allow installation from unknown sources for the file manager if prompted.
4. Select **Install**.

> **Play Protect notice:** If Play Protect repeatedly blocks the installer for a sideloaded build, review its warning and verify the APK came from this repository’s official release page. Do not disable Android security features unless you understand and accept the associated risks.

---

### Option 2: Install Over ADB

```bash
adb connect <TV_IP_ADDRESS>:5555
adb install RNA_v3.7.7-github.apk
```

If RNA. TV is already installed, update it with:

```bash
adb install -r RNA_v3.7.7-github.apk
```

### Step 4: Enable Accessibility

Pressing **Home** or **Back** should keep you inside RNA. TV after the launcher has been enabled.

1. Open the TV **Settings**.
2. Navigate to **Accessibility** or **Device Preferences → Accessibility**.
3. Open the RNA. TV accessibility service.
4. Enable **RNA. Home Interceptor Service**.
5. Grant overlay, usage-access, and other requested permissions if you want to use the related features.

You can usually open the TV launcher settings and select RNA. TV as the default home app.

> Accessibility permissions and settings can vary significantly by TV manufacturer and Android TV build.

---

## 🔐 Permissions & Privacy

RNA. TV stores profiles, browser sessions, conversations, wallpapers, and preferences locally on the device.

Common permissions include:

* **Accessibility Service:** Detects Home and Back button actions.
* **Display over other apps:** Enables AI assistant and quick-action overlays.
* **Usage Access:** Provides recent-app detection and fast app switching.
* **Storage / media access:** Allows custom wallpapers and local file selection.
* **Network access:** Supports browsing, downloads, updates, AI features, remote-controller communication, and device discovery.
* **Package installation:** Required for the built-in update installer.

RNA. TV provides keyless AI fallback options and supports optional user-provided keys. Never add real API keys or credentials to source files or `.env` files before contributing.

---

## 🛠️ Control Panel

Open **Controls** in the top bar to access:

* **YT Search Widget**
* **Startup Wallpaper Splash Screen**
* **Performance Mode**
* **Sleep timers**
* **Blue-light warmth filters**
* **Voice assistant settings**
* **Remote Remapper**
* **Updates**

The Updates section can download and install new GitHub releases directly on the device.

---

## 🔧 Development Setup

### Prerequisites

* Android Studio with Kotlin and Jetpack Compose support
* JDK 17 or newer
* Android SDK 29 or later
* Android SDK 36 recommended for the current project configuration
* A physical Android TV / Google TV device or an emulator
* ADB access for device testing

### Clone and Build

```bash
git clone https://github.com/frosty-sys/rnatv.git
cd rnatv
```

Open the repository in Android Studio and allow Gradle to sync.

Copy `.env.example` to `.env` only if local environment configuration is needed. Never commit secrets, tokens, or API keys.

Build the debug APK:

```bash
./gradlew assembleDebug
```

Install it on a connected Android TV or Google TV device:

```bash
adb install -r app/build/outputs/apk/github/debug/app-github-debug.apk
```

Release APK output names and signing requirements may vary by build configuration.

---

## 🧪 Testing

Run local JVM tests:

```bash
./gradlew test
```

Run on-device instrumented tests:

```bash
./gradlew connectedAndroidTest
```

Test locations:

* `app/src/test/java/` — local unit and Robolectric tests
* `app/src/androidTest/java/` — on-device instrumented tests

Before submitting a UI or navigation change, test:

* 1080p and 4K layouts
* D-Pad movement, focus, and Back behavior
* Center/OK and remote-button activation
* Profile switching
* Browser and keyboard behavior
* Accessibility and overlay interactions
* Performance Mode
* Physical Android TV devices and emulators
* Low-memory or resource-constrained devices

Include screenshots, a recording, device model, Android version, and reproduction steps in the pull request when relevant.

---

## 📂 Project Structure

```text
rnatv/
├── app/
│   ├── metadata/                  # Store metadata and listing assets
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/            # Bundled application assets
│   │   │   ├── java/com/example/  # Activities, services, UI, data, and utilities
│   │   │   ├── res/               # Android resources, icons, themes, and banners
│   │   │   └── AndroidManifest.xml
│   │   ├── test/java/             # Local JVM and Robolectric tests
│   │   └── androidTest/java/      # Instrumented Android tests
│   └── build.gradle.kts           # App module configuration
├── assets/                        # Repository-level badges and release assets
├── fastlane/                      # Android store and release metadata
├── gradle/                        # Gradle wrapper and version catalog
├── public/                        # Public web assets
├── build.gradle.kts               # Project-level Gradle configuration
├── settings.gradle.kts            # Gradle modules
├── CONTRIBUTING.md
├── LICENSE
└── README.md
```

Main source areas:

* `MainActivity.kt` — primary launcher activity
* `RnaAiActivity.kt` — AI assistant UI
* `WebContainerActivity.kt` — web browser and website-container behavior
* `RnaHomeInterceptorService.kt` — Home and Back interception
* `RnaTvKeyboardActivity.kt` / `RnaTvInputMethodService.kt` — TV keyboard support
* `PackageInstallerActivity.kt` / update receivers — release installation and update handling
* `data/` — databases, entities, repositories, and application data
* `ui/` — launcher, tabs, themes, TV screens, browser UI, and Compose components
* `util/` — package, app, shortcut, and helper utilities

---

## 🛠️ Troubleshooting

### RNA. TV does not replace the stock launcher

Enable RNA. TV in the device’s Accessibility settings, grant overlay and usage-access permissions, and select RNA. TV under **Settings → Apps → Default apps → Home app** if supported.

### Home or Back does not work correctly

Check that **RNA. Home Interceptor Service** is enabled. TV manufacturers and third-party remotes may use custom intercept rules, so test the exact remote and Android TV build in use.

### The APK cannot be installed

* Verify that the APK came from the official [RNA. TV releases page](https://github.com/frosty-sys/rnatv/releases).
* Enable installation from unknown sources for the selected file manager.
* Remove an incompatible older package if the package signature or application ID changed.
* Check that the device has sufficient free storage.

### UI or video playback is slow

Enable **Performance Mode**, close unused apps, and test while the TV is connected to a stable network. Live wallpapers and heavy overlays can increase memory usage.

### Remote navigation does not reach a control

Use only the D-Pad and report the screen, device model, Android version, and remote type in an issue.

---

## ↩️ Restoring the Stock Launcher

To return to the original launcher:

1. Open **Settings → Apps**.
2. Find the current home/launcher settings.
3. Select **Default apps → Home app**.
4. Choose the manufacturer’s stock launcher.
5. Disable the RNA. TV Accessibility service if it is no longer needed.
6. Uninstall RNA. TV only if you no longer want to keep it installed.

Exact menu names vary by Android TV manufacturer.

---

## 🤝 Contributing

Contributions, bug reports, feature requests, documentation fixes, and test improvements are welcome.

Before opening a pull request:

1. Read [CONTRIBUTING.md](CONTRIBUTING.md).
2. Search existing [issues](https://github.com/frosty-sys/rnatv/issues).
3. Keep changes focused and atomic.
4. Follow D-Pad-first and low-memory TV UX practices.
5. Never commit API keys, passwords, keystores, or personal data.
6. Test on an Android TV or Google TV device or emulator.

Development discussions and bug reports can be opened through the repository:

* [Report a bug](https://github.com/frosty-sys/rnatv/issues/new)
* [Request a feature](https://github.com/frosty-sys/rnatv/issues/new)
* [View open pull requests](https://github.com/frosty-sys/rnatv/pulls)

---

## ⚖️ License & Open Source

RNA. TV is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.

Profiles, browser sessions, preferences, and supported local app data are designed to remain on the device. Third-party network services used by browsing or AI features are governed by their own privacy policies.

See [LICENSE](LICENSE) for the complete license text.
