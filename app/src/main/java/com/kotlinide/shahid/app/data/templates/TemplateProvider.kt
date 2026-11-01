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
        files["build.gradle.kts"] = """
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
""".trimIndent()

        // 3. gradle.properties
        files["gradle.properties"] = """
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
""".trimIndent()

        // 4. gradle/libs.versions.toml
        files["gradle/libs.versions.toml"] = """
[versions]
agp = "8.7.2"
kotlin = "2.0.21"
coreKtx = "1.15.0"
lifecycleRuntimeKtx = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.10.01"
navigationCompose = "2.8.3"

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
androidx-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
""".trimIndent()

        // 5. app/build.gradle.kts
        files["app/build.gradle.kts"] = """
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
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    debugImplementation(libs.androidx.ui.tooling)
}
""".trimIndent()

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
            android:exported="true"
            android:theme="@style/Theme.App">
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
        files["app/src/main/res/values/themes.xml"] = """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.App" parent="android:Theme.Material.NoActionBar" />
</resources>
""".trimIndent()

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

        // 11. Compose Theme files
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

        // 12. Template-specific MainActivity.kt
        when (templateId) {
            "bottom_navigation" -> {
                files["app/src/main/java/$packagePath/MainActivity.kt"] = generateBottomNavMainActivity(packageName, appName)
            }
            "login_home" -> {
                files["app/src/main/java/$packagePath/MainActivity.kt"] = generateLoginHomeMainActivity(packageName, appName)
            }
            else -> {
                files["app/src/main/java/$packagePath/MainActivity.kt"] = generateEmptyActivityMainActivity(packageName, appName)
            }
        }

        // 13. GitHub Actions Workflow (.github/workflows/build-apk.yml)
        files[".github/workflows/build-apk.yml"] = generateProjectWorkflow(appName)

        // 14. README.md
        files["README.md"] = """
# $appName

$appName was built and generated directly using **Kotlin IDE on Android**.

## Tech Stack
- Jetpack Compose with Material 3
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

    private fun generateEmptyActivityMainActivity(packageName: String, appName: String): String = """
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
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Welcome to your new Compose app!",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { counter++ }) {
            Text(text = "Clicked: ${'$'}counter times")
        }
    }
}
""".trimIndent()

    private fun generateBottomNavMainActivity(packageName: String, appName: String): String = """
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import $packageName.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                BottomNavApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavApp() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Home", "Dashboard", "Settings")
    val icons = listOf(Icons.Default.Home, Icons.Default.Dashboard, Icons.Default.Settings)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "$appName - " + tabs[selectedIndex]) })
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        icon = { Icon(icons[index], contentDescription = title) },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Active Tab: " + tabs[selectedIndex])
        }
    }
}
""".trimIndent()

    private fun generateLoginHomeMainActivity(packageName: String, appName: String): String = """
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
                    LoginHomeFlow()
                }
            }
        }
    }
}

@Composable
fun LoginHomeFlow() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    if (!isLoggedIn) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Sign In to $appName", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { if (email.isNotEmpty()) isLoggedIn = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log In")
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Welcome, ${'$'}email!", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { isLoggedIn = false }) {
                Text("Log Out")
            }
        }
    }
}
""".trimIndent()

    private fun generateProjectWorkflow(appName: String): String = """
name: Build Android App

on:
  push:
    branches: [ "main" ]
  workflow_dispatch:
    inputs:
      clean:
        description: 'Clean build (skip cache)'
        required: false
        default: 'false'

permissions:
  contents: write

jobs:
  build:
    name: Build APK and AAB
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'zulu'

      - name: Ensure Gradle Wrapper
        run: |
          mkdir -p gradle/wrapper
          curl -sSL -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar
          chmod +x gradlew || true

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.9'
          cache-disabled: ${'$'}{{ github.event.inputs.clean == 'true' }}

      - name: Generate Official Gradle Wrapper
        run: |
          gradle wrapper --gradle-version 8.9 --distribution-type all
          chmod +x gradlew

      - name: Build Debug APK
        run: ./gradlew assembleDebug --no-daemon 2>&1 | tee build_log.txt

      - name: Build Release Bundle
        run: ./gradlew bundleRelease --no-daemon 2>&1 | tee -a build_log.txt || true
        env:
          KEYSTORE_BASE64: ${'$'}{{ secrets.KEYSTORE_BASE64 }}
          KEYSTORE_PASSWORD: ${'$'}{{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${'$'}{{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${'$'}{{ secrets.KEY_PASSWORD }}

      - name: Upload Debug APK
        if: success()
        uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: app/build/outputs/apk/debug/*.apk

      - name: Upload Release Bundle
        if: success()
        uses: actions/upload-artifact@v4
        with:
          name: release-aab
          path: app/build/outputs/bundle/release/*.aab

      - name: Publish GitHub Release
        if: success()
        run: |
          APK_PATH=${'$'}(ls app/build/outputs/apk/debug/*.apk 2>/dev/null | head -n 1)
          if [ -n "${'$'}APK_PATH" ]; then
            gh release create "latest" "${'$'}APK_PATH" \
              --title "$appName Latest Build" \
              --notes "Automated build from Kotlin IDE on Android." \
              --clobber || gh release upload "latest" "${'$'}APK_PATH" --clobber || true
          fi
        env:
          GH_TOKEN: ${'$'}{{ secrets.GITHUB_TOKEN }}
""".trimIndent()
}
