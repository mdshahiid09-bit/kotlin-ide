package com.kotlinide.shahid.app.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlinide.shahid.app.data.db.AuditLogEntry
import com.kotlinide.shahid.app.data.security.TokenStorage
import com.kotlinide.shahid.app.data.storage.AppPreferences
import com.kotlinide.shahid.app.ui.theme.DarkBackground
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import com.kotlinide.shahid.app.ui.theme.RedError
import com.kotlinide.shahid.app.util.AuditLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    tokenStorage: TokenStorage,
    appPreferences: AppPreferences,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onThemeToggle: (Boolean) -> Unit,
    onOpenCrashLog: () -> Unit
) {
    val context = LocalContext.current
    val maskedToken = remember { tokenStorage.getMaskedToken() ?: "No token stored" }
    val username = remember { appPreferences.githubUsername.ifBlank { "Not connected" } }
    var showAuditLogsDialog by remember { mutableStateOf(false) }
    var auditLogs by remember { mutableStateOf<List<AuditLogEntry>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Identity Header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = NeonGreen)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Kotlin IDE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Build Android apps from your phone", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Text("GitHub Authentication", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = NeonGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Encrypted Token (Android KeyStore)", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Authenticated User: @$username",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = maskedToken,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            AuditLogger.log(
                                context = context,
                                eventType = "AUTH_LOGOUT",
                                details = "User @$username logged out and deleted token from Keystore"
                            )
                            tokenStorage.clearToken()
                            appPreferences.githubUsername = ""
                            Toast.makeText(context, "Logged out. Token deleted from Keystore.", Toast.LENGTH_SHORT).show()
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedError, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Token & Log Out")
                    }
                }
            }

            Text("Security & Audit Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = NeonGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Local Audit Trail", fontWeight = FontWeight.SemiBold)
                            Text("All Git, CI, and build activities are recorded locally.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                auditLogs = AuditLogger.getLogs(context, 100)
                                showAuditLogsDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View Logs")
                        }

                        Button(
                            onClick = {
                                val csvFile = AuditLogger.exportAsCsv(context)
                                if (csvFile != null) {
                                    Toast.makeText(context, "Exported to ${csvFile.absolutePath}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to export audit logs", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV")
                        }
                    }
                }
            }

            Text("Appearance & Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Enforced Dark Theme Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = NeonGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("IDE Theme", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Darcula / Modern Dark UI (Enforced)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Crash Log Screen Navigation Entry
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCrashLog() }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = NeonGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Crash & Exception Logs", fontWeight = FontWeight.SemiBold)
                                Text("Inspect last 50 sanitized on-device error logs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showAuditLogsDialog) {
        AlertDialog(
            onDismissRequest = { showAuditLogsDialog = false },
            title = { Text("Local Security Audit Logs", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    if (auditLogs.isEmpty()) {
                        Text("No audit log records found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(auditLogs) { entry ->
                                val dateStr = dateFormat.format(Date(entry.timestamp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkBackground, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                Zhi) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(entry.eventType, fontWeight = FontWeight.Bold, color = NeonGreen, fontSize = 12.sp)
                                            Text(dateStr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(entry.details, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAuditLogsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
