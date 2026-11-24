# Security Policy

## 🛡️ Supported Versions

Security updates and bug fixes are actively provided for the latest release of **RNA. TV**:

| Version | Supported |
| ------- | --------- |
| `v3.7.x` (Latest) | ✅ Yes |
| `< v3.7.0` | ❌ No |

---

#### 🔒 Security & Privacy Architecture

**RNA. TV** is designed around local-first security and strict user privacy: 

* **100% Local Storage:** All user profiles, browser sessions, clipboard history, AI conversation logs, and preferences remain strictly on your device—no cloud accounts or tracking telemetry are collected.
* **Zero Hardcoded Developer API Keys:** Free AI routing relies on keyless public endpoints or user-supplied API keys stored locally on-device.
* **Verified Releases:** Official APKs and source archives are published exclusively on the [GitHub Releases](https://github.com/frosty-sys/rnatv/releases) page with GPG-signed commits.
* **Secure In-App Updates:** The updater (Controls > Functions > Updates) fetches APKs over **HTTPS** from the official GitHub Releases repository, relying on Android's package installer to verify signing certificates.
* **Sensitive Permissions:** Accessibility Service, Overlay (SYSTEM_ALERT_WINDOW), and IME permissions are used solely for Home/Back button interception, custom TV keyboard input, and on-screen AI agent overlays.

---

#### 🚨 Reporting a Vulnerability

If you discover a security vulnerability in **RNA. TV** (such as an issue affecting WebView isolation, .rna web bundles, local Wi-Fi phone controller pairing, or Accessibility/IME services), **please do not open a public GitHub issue**. 

Instead, please report it privately using one of the following methods: 

1. **GitHub Private Vulnerability Reporting:** Go to the [**Security**](https://github.com/frosty-sys/rnatv/security/advisories) tab of this repository and click **"Report a vulnerability"** to submit a private advisory.
2. **Direct Contact:** Reach out directly to the repository maintainer (@frosty-sys) with details of the issue.
   
---

#### What to Include in Your Report

* Description of the vulnerability and potential impact.
* Steps to reproduce or a proof-of-concept.
* Affected RNA. TV version (v3.7.6, etc.) and Android TV OS version.

#### Response Timeline

* **Initial Acknowledgment:** Within **48 hours** of receiving your report.
* **Fix & Disclosure:** Once verified, a patch will be developed and released in a new version before any public advisory is published.
