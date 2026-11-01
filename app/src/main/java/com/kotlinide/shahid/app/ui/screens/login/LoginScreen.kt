package com.kotlinide.shahid.app.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    tokenStorage: TokenStorage,
    appPreferences: AppPreferences,
    onLoginSuccess: () -> Unit
) {
    var tokenInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { GitHubApiService(tokenStorage) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Sign in to Kotlin IDE",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Paste your GitHub Personal Access Token to manage repositories and build Android apps.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Required Token Permissions:",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• repo (Full repository control)\n• workflow (Manage GitHub Actions workflows)",
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
                    errorMessage = null
                },
                label = { Text("Personal Access Token (PAT)") },
                placeholder = { Text("ghp_...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val cleanToken = TokenStorage.cleanToken(tokenInput)
                    if (cleanToken.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        tokenStorage.saveToken(cleanToken)
                        coroutineScope.launch {
                            val res = apiService.validateToken()
                            isLoading = false
                            res.onSuccess { user ->
                                appPreferences.githubUsername = user.login
                                onLoginSuccess()
                            }.onFailure { err ->
                                tokenStorage.clearToken()
                                errorMessage = if (err is GitHubApiException) {
                                    err.userFriendlyMessage
                                } else {
                                    err.message ?: "Invalid or expired token."
                                }
                            }
                        }
                    } else {
                        errorMessage = "Please paste your GitHub Personal Access Token."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = Color.Black
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                } else {
                    Text("Authenticate & Continue", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
