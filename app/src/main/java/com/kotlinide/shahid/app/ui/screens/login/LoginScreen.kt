package com.kotlinide.shahid.app.ui.screens.login

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlinide.shahid.app.data.api.GitHubApiException
import com.kotlinide.shahid.app.data.api.GitHubApiService
import com.kotlinide.shahid.app.data.security.TokenStorage
import com.kotlinide.shahid.app.data.storage.AppPreferences
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.DarkSurfaceHighlight
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import com.kotlinide.shahid.app.util.AuditLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    tokenStorage: TokenStorage,
    appPreferences: AppPreferences,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var tokenInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // OAuth Device Flow states
    var showDeviceDialog by remember { mutableStateOf(false) }
    var deviceCodeInputClientId by remember { mutableStateOf("") }
    var deviceUserCode by remember { mutableStateOf("") }
    var deviceVerificationUri by remember { mutableStateOf("https://github.com/login/device") }
    var isPollingDevice by remember { mutableStateOf(false) }
    var devicePollingStatus by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { GitHubApiService(tokenStorage) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
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

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Sign in to Kotlin IDE",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Authenticate with GitHub to manage Android repositories, commit code, and trigger CI/CD builds.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Recommended Authentication: Classic PAT",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create a Classic Personal Access Token with two scopes:\n" +
                                "• repo (Full control of private repositories)\n" +
                                "• workflow (Update and manage GitHub Action builds)\n\n" +
                                "Fine-grained tokens are also supported for existing repositories, but cannot create personal user repositories via API.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = tokenInput,
                onValueChange = {
                    tokenInput = it
                    errorMessage = null
                },
                label = { Text("Personal Access Token (PAT)") },
                placeholder = { Text("ghp_... or github_pat_...") },
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

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val cleanToken = TokenStorage.cleanToken(tokenInput)
                    if (cleanToken.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        tokenStorage.saveToken(cleanToken)
                        coroutineScope.launch {
                            val res = apiService.validateTokenDetails()
                            isLoading = false
                            res.onSuccess { info ->
                                appPreferences.githubUsername = info.user.login
                                AuditLogger.log(
                                    context = context,
                                    eventType = "AUTH_LOGIN",
                                    details = "User @${info.user.login} signed in via Personal Access Token"
                                )
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
                        errorMessage = "Please enter your GitHub Personal Access Token."
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
                    Text("Sign In with Token", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    showDeviceDialog = true
                    deviceUserCode = ""
                    devicePollingStatus = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign In with Device Flow")
            }
        }
    }

    if (showDeviceDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isPollingDevice) showDeviceDialog = false
            },
            title = { Text("GitHub Device Flow", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (deviceUserCode.isBlank()) {
                        Text(
                            "Sign in securely using GitHub's Device Flow. Enter your OAuth App Client ID (or your organization's OAuth Client ID):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = deviceCodeInputClientId,
                            onValueChange = { deviceCodeInputClientId = it },
                            label = { Text("OAuth Client ID") },
                            placeholder = { Text("e.g. Iv1.xxx or Client ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (devicePollingStatus != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(devicePollingStatus!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text("1. Copy this code:", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceHighlight, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = deviceUserCode,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Device Code", deviceUserCode))
                                    Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Code", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deviceVerificationUri)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open GitHub", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeonGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(devicePollingStatus ?: "Waiting for confirmation in browser...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                if (deviceUserCode.isBlank()) {
                    Button(
                        onClick = {
                            val cId = deviceCodeInputClientId.trim()
                            if (cId.isBlank()) {
                                devicePollingStatus = "Client ID is required for Device Flow."
                                return@Button
                            }
                            coroutineScope.launch {
                                devicePollingStatus = "Requesting device code..."
                                val reqRes = apiService.requestDeviceCode(cId)
                                reqRes.onSuccess { codeRes ->
                                    deviceUserCode = codeRes.userCode
                                    deviceVerificationUri = codeRes.verificationUri
                                    isPollingDevice = true
                                    devicePollingStatus = "Waiting for authorization..."

                                    // Polling loop
                                    var attempts = 0
                                    val maxAttempts = codeRes.expiresIn / codeRes.interval
                                    while (attempts < maxAttempts && isPollingDevice) {
                                        delay(codeRes.interval * 1000L)
                                        val pollRes = apiService.pollDeviceAccessToken(cId, codeRes.deviceCode)
                                        if (pollRes.isSuccess) {
                                            val tokenData = pollRes.getOrThrow()
                                            if (!tokenData.accessToken.isNullOrBlank()) {
                                                tokenStorage.saveToken(tokenData.accessToken)
                                                val valRes = apiService.validateTokenDetails()
                                                valRes.onSuccess { info ->
                                                    appPreferences.githubUsername = info.user.login
                                                    AuditLogger.log(
                                                        context = context,
                                                        eventType = "AUTH_LOGIN",
                                                        details = "User @${info.user.login} signed in via OAuth Device Flow"
                                                    )
                                                    showDeviceDialog = false
                                                    onLoginSuccess()
                                                }
                                                break
                                            }
                                        }
                                        attempts++
                                    }
                                }.onFailure { err ->
                                    devicePollingStatus = "Failed: ${err.message}"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                    ) {
                        Text("Continue")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isPollingDevice = false
                        showDeviceDialog = false
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}
