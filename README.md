# Kotlin IDE - Build Android apps from your phone

> **Tagline:** Build Android apps from your phone  
> **Application ID / Package:** `com.kotlinide.shahid.app`  
> **Repository:** [https://github.com/mdshahiid09-bit/kotlin-ide](https://github.com/mdshahiid09-bit/kotlin-ide)

---

## 🚀 Overview
**Kotlin IDE** is a native Android application built with **Jetpack Compose** and **Material 3** that empowers you to create, edit, manage, and build Android projects directly from your smartphone. 

Projects run through automated **GitHub Actions CI/CD workflows** in the cloud, generating installable **Debug APKs** and **Release AABs** without requiring a PC or Android Studio.

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
| **Java JDK** | `17` (Zulu) | Required standard for AGP 8.x |
| **Min SDK** | `26` (Android 8.0) | Broad Android device compatibility |
| **Target / Compile SDK** | `35` (Android 15) | Current Google Play & Android target standard |

---

## 📦 How to Download and Install the Debug APK

### Step 1: Automatic Cloud Build
Every push to `main` triggers `.github/workflows/build.yml` on GitHub Actions. You can also trigger it manually anytime under the **Actions** tab by selecting **Build Android Debug APK** -> **Run workflow**.

### Step 2: Download the APK
You have two easy ways to download the compiled `.apk`:
1. **GitHub Releases (Direct 1-Click Download):**
   Go to:  
   👉 **[https://github.com/mdshahiid09-bit/kotlin-ide/releases/tag/v1.0.0](https://github.com/mdshahiid09-bit/kotlin-ide/releases/tag/v1.0.0)**  
   Click on the `.apk` file under **Assets** to download it directly to your phone.
2. **GitHub Actions Artifacts:**
   Go to the [GitHub Actions tab](https://github.com/mdshahiid09-bit/kotlin-ide/actions), click the latest completed workflow run, scroll down to **Artifacts**, and tap `kotlin-ide-debug.zip`.

### Step 3: Install on Your Phone
1. Once downloaded, tap the `.apk` file on your phone.
2. If Android prompts **"Install unknown apps"** or **"Unknown sources"**:
   - Tap **Settings** in the popup.
   - Toggle **Allow from this source** for your browser or file manager.
3. Tap **Install** and open **Kotlin IDE**!

---

## 📱 Features Included

- **First-Run Setup Guide:** 3-step setup guide with progress indicator ("1 of 3", "2 of 3", "3 of 3"), "Next" and "Skip for now" on each step (GitHub token, Keystore backup warning, Google Play Console information).
- **GitHub PAT Authentication:** Secure login using Personal Access Token, encrypted with **Android Keystore** via `EncryptedSharedPreferences`. Shows masked preview (`ghp_************abcd`) and complete deletion on logout.
- **Projects Wizard (3 Screens):**
  - Screen 1: App Info (Name, Package, Version Name, Manual Version Code).
  - Screen 2: Template selection (`Empty Activity`, `Bottom Navigation`, `Login + Home`), Min SDK & Target SDK, Public/Private repository toggle.
  - Screen 3: Adaptive Mipmap Icon generation & GitHub repo creation.
- **Android Studio-Style Editor:**
  - File tree drawer on the left with expandable directories and file icons.
  - Context menu on long-press: *Rename*, *Delete*, *New File*, *New Folder*.
  - Tab row with unsaved-changes dot (capped at 6 open tabs, closing oldest).
  - Syntax highlighting for Kotlin (`.kt`), Gradle (`.kts`), and XML (`.xml`).
  - Search within file, and Search & Replace (single and replace all with confirmation dialog).
  - Per-tab Undo and Redo history stack.
  - Auto-indentation and bracket completion.
  - Code formatter button.
  - Editable `build.gradle.kts`, `libs.versions.toml`, and `.github/workflows/build-apk.yml`.
- **Cloud Build & Gradle Logs Panel:**
  - Manual "Build" and "Clean Build" buttons.
  - Status indicator (queued, running, completed) with polling.
  - Two-layer build log: beginner-friendly summary layer + expandable technical raw log.
  - Error lines highlighted in red; tapping an error jumps directly to that file and line in the editor!
  - Copy log button and manual Retry button on failure.
- **Build History & Artifacts:**
  - Keeps last 10 builds with date, version code, duration, and status.
  - Direct download of debug APK and release AAB into phone's storage.
  - Build logs auto-cleaned after 30 days while downloaded files remain intact.
  - High-priority system notification on build completion.
- **Notes & README Editor:** Create and edit Markdown notes inside each project.
- **Design & Theme:** Dark developer theme default (`#0D1117`) with neon green accent (`#00FF66`), switchable light theme. Custom adaptive vector icon featuring Android robot with `</>` code brackets.
- **Privacy & Crash Reporting:** Safe opt-in crash reporter with complete redaction of tokens, passwords, keystores, and user code.

---

## 📋 Features Simplified or Skipped (As Requested)

| Feature | Status | Reason |
| :--- | :--- | :--- |
| **Google Play Store Direct Upload** | Skipped in Phase 1 | Per specification, Play Store direct API upload is deferred to a future phase. |
| **Real-time Error Underlining** | Simplified to Build Log | Live Kotlin LSP / AST compiler daemon requires heavy RAM/SDK on device; compilation errors appear in the build log with line-jump navigation. |
| **Emulator / Terminal / Offline Mode** | Excluded | Explicitly out of scope per user specification. Internet connection is required for GitHub API and Actions CI/CD builds. |
| **Search / Sort in Project List** | Excluded | Plain list retained per user specification. |
| **AI Code Generation** | Reserved for Phase 2 | Scheduled for Phase 2 with Gemini & Groq integration. |
