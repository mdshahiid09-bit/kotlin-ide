package com.kotlinide.shahid.app.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotlinide.shahid.app.data.api.GitHubApiException
import com.kotlinide.shahid.app.data.api.GitHubApiService
import com.kotlinide.shahid.app.data.security.TokenStorage
import com.kotlinide.shahid.app.data.storage.AppPreferences
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import com.kotlinide.shahid.app.ui.theme.OrangeWarning
import kotlinx.coroutines.launch

@Composable
fun FirstRunGuideScreen(
    tokenStorage: TokenStorage,
    appPreferences: AppPreferences,
    onCompleteGuide: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var tokenInput by remember { mutableStateOf(tokenStorage.getToken() ?: "") }
    var isValidating by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var validationSuccess by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { GitHubApiService(tokenStorage) }

    fun finishGuide() {
        appPreferences.isFirstRunComplete = true
        onCompleteGuide()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "First-Run Setup Guide",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$currentStep of 3",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { currentStep / 3f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = NeonGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (currentStep < 3) currentStep++ else finishGuide()
                    }
                ) {
                    Text(text = "Skip for now")
                }

                Button(
                    onClick = {
                        if (currentStep == 1) {
                            val cleanToken = TokenStorage.cleanToken(tokenInput)
                            if (cleanToken.isNotBlank()) {
                                isValidating = true
                                validationError = null
                                tokenStorage.saveToken(cleanToken)
                                coroutineScope.launch {
                                    val result = apiService.validateToken()
                                    isValidating = false
                                    result.onSuccess { user ->
                                        appPreferences.githubUsername = user.login
                                        validationSuccess = "Connected as @${user.login}"
                                        currentStep = 2
                                    }.onFailure { err ->
                                        validationError = if (err is GitHubApiException) {
                                            err.userFriendlyMessage
                                        } else {
                                            err.message ?: "Authentication failed."
                                        }
                                    }
                                }
                            } else {
                                currentStep = 2
                            }
                        } else if (currentStep < 3) {
                            currentStep++
                        } else {
                            finishGuide()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color.Black
                    ),
                    enabled = !isValidating
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.Black
                        )
                    } else {
                        Text(
                            text = if (currentStep == 3) "Get Started" else "Next",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            when (currentStep) {
                1 -> {
                    StepHeader(
                        icon = Icons.Default.Code,
                        stepNumber = "Step 1",
                        title = "Connect Your GitHub Account",
                        subtitle = "Kotlin IDE uses GitHub Actions to build your apps in the cloud without needing a PC or Android Studio."
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "How to generate a Personal Access Token:",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. Visit github.com/settings/tokens (classic)\n2. Generate new token\n3. Select scopes: 'repo' (Full control) and 'workflow'\n4. Copy and paste it below",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = {
                            tokenInput = it
                            validationError = null
                        },
                        label = { Text("GitHub Personal Access Token") },
                        placeholder = { Text("ghp_xxxxxxxxxxxxxxxxxxxx") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = validationError != null
                    )

                    if (validationError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (validationSuccess != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = validationSuccess!!,
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                2 -> {
                    StepHeader(
                        icon = Icons.Default.Key,
                        stepNumber = "Step 2",
                        title = "App Signing Keystore",
                        subtitle = "Each project gets a release keystore for secure signing on GitHub Actions."
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1A11)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = OrangeWarning,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "CRITICAL BACKUP WARNING",
                                    color = OrangeWarning,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Always back up your keystore file and its password! If you lose your signing key, you will permanently lose the ability to publish updates to the Google Play Store for that app.",
                                    color = Color(0xFFFFD5B8),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Keystores are generated when you create a project and securely uploaded directly to your repository's GitHub Secrets. Neither your private key nor your password remains on the phone after upload.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                3 -> {
                    StepHeader(
                        icon = Icons.Default.Shop,
                        stepNumber = "Step 3",
                        title = "Google Play Console (Upcoming)",
                        subtitle = "Prepare for publishing your built apps to the Play Store."
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Play Store Upload in Next Phase",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "In this phase, Kotlin IDE builds both an installable debug APK (for immediate testing on your phone) and a release AAB (Android App Bundle).\n\nDirect Play Console API uploads will be introduced in an upcoming release. You can skip this step now.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    stepNumber: String,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = NeonGreen)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = stepNumber.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
