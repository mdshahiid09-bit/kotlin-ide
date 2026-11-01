plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val ciRunNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.kotlinide.shahid.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kotlinide.shahid.app"
        minSdk = 26
        targetSdk = 35
        versionCode = ciRunNumber
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val ksFile = System.getenv("KEYSTORE_FILE")?.trim()
    val ksPass = (System.getenv("RESOLVED_KEYSTORE_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD"))?.trim()
    val kAlias = (System.getenv("RESOLVED_KEY_ALIAS") ?: System.getenv("KEY_ALIAS"))?.trim()
    val kPass = (System.getenv("RESOLVED_KEY_PASSWORD") ?: System.getenv("KEY_PASSWORD") ?: ksPass)?.trim()

    val hasReleaseSigning = !ksFile.isNullOrBlank() &&
            file(ksFile).exists() &&
            !ksPass.isNullOrBlank() &&
            !kAlias.isNullOrBlank() &&
            !kPass.isNullOrBlank()

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(ksFile)
                storePassword = ksPass
                keyAlias = kAlias
                keyPassword = kPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
}
