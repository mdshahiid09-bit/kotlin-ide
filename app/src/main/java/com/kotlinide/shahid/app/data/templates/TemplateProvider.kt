package com.kotlinide.shahid.app.data.templates

object TemplateProvider {

    fun generateProjectFiles(
        appName: String,
        packageName: String,
        versionName: String,
        versionCode: Int,
        templateId: String,
        minSdk: Int,
        targetSdk: Int,
        hasCustomIcon: Boolean = false
    ): Map<String, String> {
        val files = mutableMapOf<String, String>()
        val packagePath = packageName.replace('.', '/')
        val isViewsTemplate = templateId == "empty_views"

        // 1. Root settings.gradle.kts
        files["settings.gradle.kts"] = """
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "$appName"
include(":app")
""".trimIndent()

        // 2. Root build.gradle.kts
        files["build.gradle.kts"] = if (isViewsTemplate) {
            """
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
""".trimIndent()
        } else {
            """
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
""".trimIndent()
        }

        // 3. gradle.properties
        files["gradle.properties"] = """
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
""".trimIndent()

        // 4. gradle/libs.versions.toml
        files["gradle/libs.versions.toml"] = if (isViewsTemplate) {
            """
[versions]
agp = "8.7.2"
kotlin = "2.0.21"
coreKtx = "1.15.0"
appcompat = "1.7.0"
material = "1.12.0"
constraintlayout = "2.2.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
""".trimIndent()
        } else {
            """
[versions]
agp = "8.7.2"
kotlin = "2.0.21"
coreKtx = "1.15.0"
lifecycleRuntimeKtx = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.10.01"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
""".trimIndent()
        }

        // 5. app/build.gradle.kts
        files["app/build.gradle.kts"] = if (isViewsTemplate) {
            """
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "$packageName"
    compileSdk = $targetSdk

    defaultConfig {
        applicationId = "$packageName"
        minSdk = $minSdk
        targetSdk = $targetSdk
        versionCode = $versionCode
        versionName = "$versionName"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
}
""".trimIndent()
        } else {
            """
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "$packageName"
    compileSdk = $targetSdk

    defaultConfig {
        applicationId = "$packageName"
        minSdk = $minSdk
        targetSdk = $targetSdk
        versionCode = $versionCode
        versionName = "$versionName"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    debugImplementation(libs.androidx.ui.tooling)
}
""".trimIndent()
        }

        // 6. app/proguard-rules.pro
        files["app/proguard-rules.pro"] = "# Project Proguard Rules\n"

        // 7. AndroidManifest.xml
        files["app/src/main/AndroidManifest.xml"] = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.App">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
""".trimIndent()

        // 8. strings.xml
        files["app/src/main/res/values/strings.xml"] = """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">$appName</string>
</resources>
""".trimIndent()

        // 9. themes.xml
        files["app/src/main/res/values/themes.xml"] = if (isViewsTemplate) {
            """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.App" parent="Theme.Material3.DayNight.NoActionBar" />
</resources>
""".trimIndent()
        } else {
            """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.App" parent="android:Theme.Material.NoActionBar" />
</resources>
""".trimIndent()
        }

        // 10. Default adaptive launcher icons if not custom icon
        if (!hasCustomIcon) {
            files["app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"] = """
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
""".trimIndent()

            files["app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml"] = """
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
""".trimIndent()

            files["app/src/main/res/drawable/ic_launcher_background.xml"] = """
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#121212"
        android:pathData="M0,0h108v108h-108z" />
</vector>
""".trimIndent()

            files["app/src/main/res/drawable/ic_launcher_foreground.xml"] = """
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#00FF66"
        android:pathData="M31,44 L20,54 L31,64 M77,44 L88,54 L77,64 M60,34 L48,74"
        android:strokeColor="#00FF66"
        android:strokeWidth="5"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
""".trimIndent()
        }

        if (isViewsTemplate) {
            // Layout activity_main.xml
            files["app/src/main/res/layout/activity_main.xml"] = """
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="24dp">

    <TextView
        android:id="@+id/tvTitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Welcome to $appName!"
        android:textSize="22sp"
        android:textStyle="bold"
        app:layout_constraintBottom_toTopOf="@+id/btnClick"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintVertical_chainStyle="packed" />

    <Button
        android:id="@+id/btnClick"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        android:text="Click Me"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@+id/tvTitle" />

</androidx.constraintlayout.widget.ConstraintLayout>
""".trimIndent()

            // MainActivity.kt for Views
            files["app/src/main/java/$packagePath/MainActivity.kt"] = """
package $packageName

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var clickCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val btnClick = findViewById<Button>(R.id.btnClick)

        btnClick.setOnClickListener {
            clickCount++
            tvTitle.text = "Button clicked ${'$'}clickCount times!"
        }
    }
}
""".trimIndent()

        } else {
            // Compose Theme files
            files["app/src/main/java/$packagePath/ui/theme/Color.kt"] = """
package $packageName.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val NeonAccent = Color(0xFF00FF66)
""".trimIndent()

            files["app/src/main/java/$packagePath/ui/theme/Theme.kt"] = """
package $packageName.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonAccent,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
""".trimIndent()

            // MainActivity.kt for Compose
            files["app/src/main/java/$packagePath/MainActivity.kt"] = """
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import $packageName.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var counter by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$appName",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Built with Kotlin IDE on Android",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { counter++ }) {
            Text("Button tapped: ${'$'}counter times")
        }
    }
}
""".trimIndent()
        }

        // 13. GitHub Actions Workflow (.github/workflows/build-apk.yml)
        files[".github/workflows/build-apk.yml"] = """
name: Build Android APK

on:
  push:
    branches: [ "main" ]
  workflow_dispatch:
    inputs:
      clean:
        description: 'Perform clean build'
        required: false
        default: 'false'

permissions:
  contents: read
  actions: read

jobs:
  build:
    name: Build Debug APK & Release AAB
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.9'

      - name: Ensure Executable Gradlew
        run: chmod +x gradlew || true

      - name: Build Debug APK
        run: ./gradlew assembleDebug --no-daemon --stacktrace

      - name: Build Release Bundle (AAB)
        run: ./gradlew bundleRelease --no-daemon --stacktrace || true

      - name: Upload Debug APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-debug-apk
          path: app/build/outputs/apk/debug/*.apk
          if-no-files-found: error
          retention-days: 14

      - name: Upload Release AAB Artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-release-aab
          path: app/build/outputs/bundle/release/*.aab
          if-no-files-found: ignore
          retention-days: 14
""".trimIndent()

        // 14. README.md
        files["README.md"] = """
# $appName

$appName was built and generated directly using **Kotlin IDE on Android**.

## Tech Stack
- ${if (isViewsTemplate) "Classic Android Views with Material 3" else "Jetpack Compose with Material 3"}
- Kotlin 2.0.21
- Android Gradle Plugin 8.7.2
- Minimum SDK: $minSdk
- Target SDK: $targetSdk

## Build Instructions
This repository includes a GitHub Actions CI/CD workflow (`.github/workflows/build-apk.yml`) that automatically compiles:
1. **Debug APK**: Installable directly on any Android device.
2. **Release AAB**: Android App Bundle ready for Google Play Console.

Trigger the build anytime via **Kotlin IDE** on your phone or in the **Actions** tab on GitHub!
""".trimIndent()

        return files
    }
}
