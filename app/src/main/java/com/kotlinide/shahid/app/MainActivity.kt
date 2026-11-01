package com.kotlinide.shahid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kotlinide.shahid.app.ui.navigation.AppNavHost
import com.kotlinide.shahid.app.ui.theme.KotlinIDETheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as KotlinIdeApplication
        val appPreferences = app.appPreferences
        val tokenStorage = app.tokenStorage
        val projectStorage = app.projectStorage
        val draftStorage = app.draftStorage
        val syncQueueManager = app.syncQueueManager

        setContent {
            // Dark theme enforced by default (Android Studio Darcula style)
            KotlinIDETheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    AppNavHost(
                        navController = navController,
                        tokenStorage = tokenStorage,
                        appPreferences = appPreferences,
                        projectStorage = projectStorage,
                        draftStorage = draftStorage,
                        syncQueueManager = syncQueueManager,
                        onThemeToggle = { /* Enforced Dark Theme */ }
                    )
                }
            }
        }
    }
}
