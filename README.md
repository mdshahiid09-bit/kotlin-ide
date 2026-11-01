# Kotlin IDE - Build Android apps from your phone

> **Tagline:** Build Android apps from your phone  
> **Application ID / Package:** `com.kotlinide.shahid.app`  
> **Target Repository:** [https://github.com/mdshahiid09-bit/kotlin-ide](https://github.com/mdshahiid09-bit/kotlin-ide)  
> **UI Theme:** Android Studio Darcula / Modern Dark UI (Enforced)

---

## 🚀 Overview
**Kotlin IDE** is a full-featured, enterprise-grade mobile development environment that allows developers to create, edit, manage, and build Android applications directly from an Android smartphone.

There is **no need for a PC or Android Studio**. Projects are synchronized directly with GitHub and compiled in the cloud through automated **GitHub Actions CI/CD workflows**, generating both installable **Debug APKs** and **Signed Release APKs** ready for immediate download and installation.

---

## 🌟 Key Architecture & Capabilities

### 1. 🛡️ Native Keystore Encryption (Zero Secret Leakage)
- GitHub Personal Access Tokens (PATs) and sensitive credentials are encrypted using **AndroidKeyStore AES-256 GCM** with hardware-backed security.
- Tokens are never stored in plaintext, never logged to logcat, and never displayed unmasked in the UI.
- All stack traces and error reports run through a rigorous sanitization pipeline that strips Classic PATs (`ghp_*`), Fine-Grained PATs (`github_pat_*`), Bearer tokens, and keystore passwords.

### 2. 🗄️ Relational SQLite Persistence (`KotlinIdeDatabaseHelper`)
- Local project metadata, build run history, notes, cached repositories, and security audit logs are stored in a relational SQLite database with full ACID transactions.
- Offline resilience: changes are automatically mirrored locally so editing continues without interruptions even when disconnected.

### 3. 🔄 Real-Time Git & GitHub REST Integration
- **Direct GitHub REST API**: Full repository creation, tree retrieval, commit tracking, branches, merge operations, pull requests, and issues management.
- **Offline Sync Queue**: Any edit made while offline is queued locally with UUID-tracked pending operations and automatically retried upon reconnection.
- **Smart 3-Way Conflict Resolution**: Detects remote GitHub changes (HTTP 409/422) and offers visual resolution: *Keep Mine*, *Keep GitHub's*, or *Merge with standard conflict markers*.
- **OAuth Device Flow & Classic PAT**: Supports instant QR / browser authentication (`github.com/login/device`) as well as Classic PATs with fine-grained advisory checks.

### 4. 💻 Advanced Mobile Code Editor
- **Android Studio Look & Feel**: Styled after Jetpack Compose and Darcula theme.
- **Coding Accessory Bar**: One-tap symbol insertion (`Tab`, `{ }`, `( )`, `[ ]`, `->`, `=`, etc.) and intelligent context-aware autocomplete suggestions for Jetpack Compose, Kotlin keywords, and Android XML.
- **Real-Time Syntax Linter**: Instant on-device linting detecting unclosed delimiters, mismatched brackets, unclosed string literals, and missing package headers.
- **Fixed Line Numbers Column**: Synchronized line numbers aligned with code editor scrolling.
- **In-File Search & Replace**: Case-sensitive search with single and bulk "Replace All" actions.
- **Multi-Tab File Management**: Up to 8 concurrent active tabs with dirty-state indicators and persistent unsaved drafts.

### 5. 📲 In-App APK Extraction & Installation
- Built APK artifacts from GitHub Actions workflow runs can be downloaded directly inside the app.
- Automated ZIP extraction and secure intent launching via Android `FileProvider` (`content://com.kotlinide.shahid.app.fileprovider`), with pre-flight `canRequestPackageInstalls` permission handling.

### 6. 🎨 Project Templates & PNG Mipmap Generator
- **Empty Compose Activity** (`empty_compose`): Modern Jetpack Compose Material 3 starter project with Kotlin 2.0 compiler plugin.
- **Empty Views Activity** (`empty_views`): Classic XML layout (`activity_main.xml`) with ConstraintLayout and AppCompatActivity.
- **App Icon Creator**: Upload custom PNG icons directly on device; automatically scales and exports exact mipmap densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) into the project source tree.

---

## 🛠 Exact Versions & Tech Stack

All versions are pinned in `gradle/libs.versions.toml`:

| Component | Pinned Version | Why Compatible |
| :--- | :--- | :--- |
| **Gradle** | `8.9` | Official stable Gradle release supporting Java 17 and AGP 8.7+ |
| **Android Gradle Plugin (AGP)** | `8.7.2` | Latest stable AGP supporting Android 15 (API 35) & compileSdk 35 |
| **Kotlin** | `2.0.21` | Modern standard Kotlin release with first-party Compose compiler plugin |
| **Compose Compiler Plugin** | `org.jetbrains.kotlin.plugin.compose` (Kotlin 2.0.21) | Native Kotlin 2.0 compiler plugin (replaces deprecated Jetpack compiler) |
| **Compose BOM** | `2024.10.01` | Fully certified Compose BOM for Material 3 and Lifecycle 2.8+ |
| **Java JDK** | `17` (Temurin) | Required standard for AGP 8.x |
| **Min SDK** | `26` (Android 8.0) | Broad Android device compatibility (95%+ of active devices) |
| **Target / Compile SDK** | `35` (Android 15) | Current Google Play & Android target standard |

---

## 📦 How to Download and Install

### Option A: Direct 1-Click Release Download
Download the latest pre-compiled APK directly:  
👉 **[https://github.com/mdshahiid09-bit/kotlin-ide/releases/latest](https://github.com/mdshahiid09-bit/kotlin-ide/releases/latest)**

- **`KotlinIDE-release.apk`**: Production release signed with repository secrets.
- **`KotlinIDE-debug.apk`**: Developer debug build.

### Option B: GitHub Actions Artifacts
1. Go to the [Actions tab](https://github.com/mdshahiid09-bit/kotlin-ide/actions).
2. Tap the latest green run on `main`.
3. Under **Artifacts**, download `kotlin-ide-release-signed` or `kotlin-ide-debug`.

---

## ⚙️ Initial GitHub Token Setup

To enable repository creation, file editing, and GitHub Actions build triggering:

1. Visit [GitHub Settings → Developer Settings → Personal Access Tokens → Tokens (classic)](https://github.com/settings/tokens).
2. Click **Generate new token (classic)**.
3. Select the following required scopes:
   - `repo` (Full control of private repositories)
   - `workflow` (Update GitHub Action workflows)
4. Copy the generated token, open **Kotlin IDE**, and paste it into the Sign In screen.
5. *(Optional)* Alternatively, tap **Sign In with Browser (Device Flow)** and enter the 8-digit verification code at `github.com/login/device`.

> [!NOTE]  
> If using a **Fine-grained Personal Access Token**, note that GitHub REST API restricts creating personal user repositories via fine-grained tokens. A Classic PAT with `repo` and `workflow` scopes is strongly recommended.

---

## 📱 Device & OEM Troubleshooting (MIUI / HyperOS / EMUI)

If running Kotlin IDE on Xiaomi (MIUI/HyperOS), Huawei (EMUI), or OnePlus (OxygenOS):
1. **Background Build Notifications:**  
   Go to *Settings → Apps → Kotlin IDE → Battery Saver* and set to **"No restrictions"**. This ensures build completion notifications appear reliably while compiling large projects in the background.
2. **In-App APK Installation:**  
   Go to *Settings → Apps → Kotlin IDE → Install unknown apps* and enable **"Allow from this source"**.
