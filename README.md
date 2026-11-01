# Kotlin IDE - Build Android apps from your phone

> **Tagline:** Build Android apps from your phone  
> **Application ID / Package:** `com.kotlinide.shahid.app`  
> **Repository:** [https://github.com/mdshahiid09-bit/kotlin-ide](https://github.com/mdshahiid09-bit/kotlin-ide)

---

## 🚀 Overview
**Kotlin IDE** is a native Android application built with **Jetpack Compose** and **Material 3** that empowers you to create, edit, manage, and build Android projects directly from your smartphone. 

Projects run through automated **GitHub Actions CI/CD workflows** in the cloud, generating installable **Debug APKs** and **Signed Release APKs** without requiring a PC or Android Studio.

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
| **Min SDK** | `26` (Android 8.0) | Broad Android device compatibility |
| **Target / Compile SDK** | `35` (Android 15) | Current Google Play & Android target standard |

---

## 📦 How to Download and Install the APKs

### Step 1: Automatic Cloud Build
Every push to `main` triggers `.github/workflows/build.yml` on GitHub Actions. It automatically compiles both:
1. **`KotlinIDE-debug.apk`**: Developer debug build.
2. **`KotlinIDE-release.apk`**: Production release APK signed with your repository Keystore secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).

### Step 2: Download the APK
You have two easy ways to download the compiled `.apk`:
1. **GitHub Releases (Direct 1-Click Download):**
   Go to:  
   👉 **[https://github.com/mdshahiid09-bit/kotlin-ide/releases/latest](https://github.com/mdshahiid09-bit/kotlin-ide/releases/latest)**  
   Download either:
   - `KotlinIDE-release.apk` (Signed Release APK)
   - `KotlinIDE-debug.apk` (Debug APK)
2. **GitHub Actions Artifacts:**
   Go to the [GitHub Actions tab](https://github.com/mdshahiid09-bit/kotlin-ide/actions), click the latest completed workflow run, scroll down to **Artifacts**, and tap `kotlin-ide-release-signed` or `kotlin-ide-debug`.

### Step 3: Install on Your Phone
1. Once downloaded, tap the `.apk` file on your phone.
2. If Android prompts **"Install unknown apps"** or **"Unknown sources"**:
   - Tap **Settings** in the popup.
   - Toggle **Allow from this source** for your browser or file manager.
3. Tap **Install** and open **Kotlin IDE**!

---

## 🔐 Keystore Secrets & Security
Release signing is fully automated and secure:
- Keystore credentials (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) are loaded strictly from GitHub Repository Secrets.
- Secrets are never printed in build logs.
- Temporary JKS files are placed outside the repository with restricted permissions (`chmod 600`) and securely destroyed (`shred -u` + `rm -rf`) immediately after the build completes.
