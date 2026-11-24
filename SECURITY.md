# Security Policy

## 🛡️ Supported Versions

Security updates and bug fixes are actively provided for the latest release branch of **RNA. TV**:

| Version | Status | Support Details |
| :--- | :---: | :--- |
| **`v3.7.6`** *(Latest)* | ✅ | Active security patches and bug fixes |
| **`< v3.7`** | ❌ | Unsupported — please upgrade via in-app updater |

---

## 🔒 Security & Privacy Architecture

**RNA. TV** is engineered around local-first security and strict user privacy:

* **100% Local Storage:** All user profiles, browser sessions, clipboard history, AI conversation logs, and preferences remain strictly on your device—no cloud accounts or tracking telemetry are collected.
* **Zero Hardcoded Developer API Keys:** Free AI routing relies on keyless public endpoints or user-supplied API keys stored locally on-device.
* **Verified Releases:** Official APKs and source archives are published exclusively on [GitHub Releases](https://github.com/frosty-sys/rnatv/releases) with GPG-signed commits.
* **Secure In-App Updates:** The built-in updater (`Controls > Functions > Updates`) fetches APKs exclusively over **HTTPS** from the official GitHub Releases repository, relying on Android's package installer to verify signing certificates.
* **Minimal, Purpose-Bound Permissions:** Accessibility Service, Overlay (`SYSTEM_ALERT_WINDOW`), and IME permissions are used solely for Home/Back button interception, custom TV keyboard input, and on-screen AI agent overlays.

---

## 🚨 Reporting a Vulnerability

> [!CAUTION]
> **Please do not open a public GitHub issue for security vulnerabilities.** Public disclosure before a patch is available puts users at risk.

If you discover a security issue affecting WebView isolation, `.rna` web bundles, local Wi-Fi controller pairing, or Accessibility/IME services, please report it privately using one of the following methods:

1. **GitHub Private Vulnerability Reporting *(Recommended)*:** Navigate to the [**Security Advisories**](https://github.com/frosty-sys/rnatv/security/advisories) tab and click **"Report a vulnerability"** to submit a confidential report.
2. **Direct Maintainer Contact:** Reach out directly to [`@frosty-sys`](https://github.com/frosty-sys) with details of the issue.

### 📋 What to Include in Your Report

* **Vulnerability Description:** Summary of the issue and its potential security impact.
* **Reproduction Steps:** Clear step-by-step instructions or a proof-of-concept.
* **Environment Details:** Affected RNA. TV version (e.g., `v3.7.6`), TV device model, and Android TV OS version.

### ⏱️ Response Timeline

* **Initial Acknowledgment:** Within **48 hours** of receiving your report.
* **Patch & Disclosure:** Once verified, a fix will be developed and published in a new release before any public security advisory is disclosed.
