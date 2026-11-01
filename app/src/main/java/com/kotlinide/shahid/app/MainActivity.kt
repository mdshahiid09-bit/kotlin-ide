package com.kotlinide.shahid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.kotlinide.shahid.app.ui.navigation.AppNavHost
import com.kotlinide.shahid.app.ui.theme.KotlinIDETheme
import com.kotlinide.shahid.app.ui.theme.NeonGreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as KotlinIdeApplication
        val appPreferences = app.appPreferences
        val tokenStorage = app.tokenStorage
        val projectStorage = app.projectStorage

        setContent {
            var isDarkTheme by remember { mutableStateOf(appPreferences.isDarkTheme) }
            var showCrashDialog by remember { mutableStateOf(!appPreferences.crashlyticsDialogAnswered) }

            KotlinIDETheme(darkTheme = isDarkTheme) {
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
                        onThemeToggle = { isDark -> isDarkTheme = isDark }
                    )

                    if (showCrashDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                appPreferences.crashlyticsDialogAnswered = true
                                showCrashDialog = false
                            },
                            title = { Text("Help Improve Kotlin IDE") },
                            text = {
                                Text(
                                    "Would you like to share anonymized crash logs to help improve Kotlin IDE? " +
                                            "Your personal access token, project code, keystore data, and passwords are NEVER included in crash reports."
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        appPreferences.isCrashlyticsOptIn = true
                                        appPreferences.crashlyticsDialogAnswered = true
                                        showCrashDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                                ) {
                                    Text("Allow")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        appPreferences.isCrashlyticsOptIn = false
                                        appPreferences.crashlyticsDialogAnswered = true
                                        showCrashDialog = false
                                    }
                                ) {
                                    Text("No thanks")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
