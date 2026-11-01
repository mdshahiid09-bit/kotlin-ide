# Changelog - Kotlin IDE

All notable changes to the **Kotlin IDE** mobile development environment are documented in this file.

---

## [1.0.0] - 2026-11-01

### 🚀 Enterprise Mobile IDE Overhaul

#### Security & Hardware-Backed Keystore
- **Native AndroidKeyStore Encryption**: Implemented zero-dependency AES-256 GCM encryption for user authentication tokens in `TokenStorage.kt`, completely removing insecure plaintext storage.
- **Self-Contained DER X.509 Certificate Generator**: Built internal DER ASN.1 certificate encoder in `DerX509Generator.kt` to generate compliant PKCS12 release keystores dynamically on demand without deprecated BouncyCastle APIs.
- **Sanitization Pipeline**: Enhanced `CrashReportingHelper` to automatically redact GitHub Classic PATs (`ghp_*`), Fine-Grained PATs (`github_pat_*`), Authorization Bearer headers, and Keystore passwords before writing logs to disk.
- **Local Security Audit Trail**: Added SQLite-backed `audit_logs` tracking all authentication, project creation, Git syncs, and builds, with one-tap CSV export functionality.

#### Relational Database Architecture
- **Relational SQLite Persistence**: Added `KotlinIdeDatabaseHelper` providing robust ACID transactions across 5 core tables:
  - `projects`: Project metadata, SDK targets, and sync tracking.
  - `build_records`: Complete GitHub Actions run metrics, compilation timings, and log paths.
  - `notes`: Per-project development notes and checklists.
  - `audit_logs`: Rolling 500-event local security audit log.
  - `cached_repos`: GitHub repository cache for instant offline browsing.
- **Storage Conservation**: Automatic log truncation and max 10 builds retention per project to avoid filling device storage.

#### Mobile Code Editor & Linting
- **Accessory Symbol & Autocomplete Bar**: Upgraded `CodingAccessoryBar` with one-tap coding symbols and dynamic auto-completion for Jetpack Compose APIs (`Column`, `Row`, `Box`, `Scaffold`, `remember`), Kotlin keywords, and Android XML elements.
- **Real-Time On-Device Linter**: Created `EditorLinter` verifying bracket matching (`{}`, `()`, `[]`), quote closures, and XML tags with direct visual warning indicators above the keyboard.
- **Fixed Line Numbers Column**: Synchronized line numbering column aligned with vertical code scrolling.
- **Draft Persistence**: Unsaved editor changes are backed up locally in real-time (`DraftStorage`), surviving app crashes or accidental closure.
- **Debounced Auto-Save & Remote Polling**: Automatic file save after 2.5s pause; background polling detects remote commits made directly on GitHub and alerts the user.

#### Git Operations & Conflict Resolution
- **Full GitHub REST Operations**: Support for branch creation, branch switching, merge requests, pull request listings, and GitHub issues tracking directly from the phone.
- **3-Way Conflict Dialog**: Added visual resolution for HTTP 409/422 conflicts offering *Keep Mine*, *Keep GitHub's*, or *Merge with standard conflict markers*.
- **Offline Sync Queue**: Added `SyncQueueManager` that queues offline modifications locally and retries them automatically when network connection returns.

#### In-App APK Extraction & Installation
- **Automated ZIP Extraction**: Downloaded GitHub Actions build artifact ZIPs are unpacked directly on device.
- **FileProvider Integration**: Seamless in-app APK installer via `content://com.kotlinide.shahid.app.fileprovider` with pre-flight check for unknown sources permission.

#### Starter Templates & PNG Mipmap Generator
- **Empty Compose Activity**: Modern Material 3 Jetpack Compose starter with Kotlin 2.0 compiler plugin.
- **Empty Views Activity**: Classic Android layout using `activity_main.xml` and AppCompat.
- **PNG Icon Generator**: Automatically scales user-uploaded PNG icons into standard Android mipmap densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).

#### UI & OEM Compatibility
- **Darcula / Dark Theme Enforced**: Unified Android Studio dark visual design across all dialogs and screens.
- **Xiaomi / MIUI Resilience**: Battery saver guidance and notification channel setup for background builds.
