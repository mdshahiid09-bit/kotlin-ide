package com.kotlinide.shahid.app.ui.screens.build

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlinide.shahid.app.data.api.GitHubApiService
import com.kotlinide.shahid.app.data.model.BuildInfo
import com.kotlinide.shahid.app.data.model.Project
import com.kotlinide.shahid.app.data.security.TokenStorage
import com.kotlinide.shahid.app.data.storage.ProjectStorage
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import com.kotlinide.shahid.app.ui.theme.OrangeWarning
import com.kotlinide.shahid.app.ui.theme.RedError
import com.kotlinide.shahid.app.util.ApkInstaller
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildHistoryScreen(
    projectId: String,
    projectStorage: ProjectStorage,
    tokenStorage: TokenStorage,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { GitHubApiService(tokenStorage) }

    var project by remember { mutableStateOf<Project?>(null) }
    var builds by remember { mutableStateOf<List<BuildInfo>>(emptyList()) }
    var downloadingRunId by remember { mutableStateOf<Long?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    fun refreshBuilds() {
        coroutineScope.launch {
            project = projectStorage.getProject(projectId)
            builds = projectStorage.getBuilds(projectId)
        }
    }

    LaunchedEffect(projectId) {
        refreshBuilds()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Build History (Last 10)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { innerPadding ->
        if (builds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No builds recorded yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap the Play icon in the editor to trigger a cloud build.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(builds, key = { it.id }) { build ->
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    val dateString = dateFormat.format(Date(build.createdAt))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val (icon, tint) = when (build.conclusion) {
                                        "success" -> Pair(Icons.Default.CheckCircle, NeonGreen)
                                        "failure" -> Pair(Icons.Default.Error, RedError)
                                        else -> Pair(Icons.Default.HourglassTop, OrangeWarning)
                                    }
                                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Build #${build.runNumber}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }

                                Text(
                                    text = "Version Code: ${build.versionCode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = dateString,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (build.conclusion == "success") {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val hasLocalApk = !build.apkLocalPath.isNullOrBlank() && File(build.apkLocalPath).exists()

                                    if (hasLocalApk) {
                                        Button(
                                            onClick = {
                                                val apkFile = File(build.apkLocalPath!!)
                                                if (!ApkInstaller.canInstallApks(context)) {
                                                    showPermissionDialog = true
                                                } else {
                                                    val res = ApkInstaller.installApk(context, apkFile)
                                                    if (res.isFailure) {
                                                        Toast.makeText(context, "Install failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                                        ) {
                                            Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Install APK", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                val currentProj = project ?: return@Button
                                                downloadingRunId = build.id
                                                coroutineScope.launch {
                                                    val artifactsRes = apiService.listArtifacts(currentProj.repoOwner, currentProj.repoName, build.id)
                                                    val apkArtifact = artifactsRes.getOrNull()?.find { it.name.contains("apk") }
                                                    if (apkArtifact != null) {
                                                        val appDir = File(context.getExternalFilesDir(null), "Builds/${currentProj.name}").apply { mkdirs() }
                                                        val destZip = File(appDir, "build_${build.runNumber}_apk.zip")
                                                        val dlRes = apiService.downloadArtifact(apkArtifact.archiveDownloadUrl, destZip)
                                                        if (dlRes.isSuccess) {
                                                            val extractedApk = ApkInstaller.extractApkFromZip(destZip, appDir)
                                                            if (extractedApk != null) {
                                                                projectStorage.saveBuild(build.copy(apkLocalPath = extractedApk.absolutePath))
                                                                refreshBuilds()
                                                                Toast.makeText(context, "APK ready to install!", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, "Saved to ${destZip.absolutePath}", Toast.LENGTH_LONG).show()
                                                            }
                                                        } else {
                                                            Toast.makeText(context, "Failed to download APK artifact", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "No APK artifact found for this run", Toast.LENGTH_SHORT).show()
                                                    }
                                                    downloadingRunId = null
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                                            enabled = downloadingRunId != build.id
                                        ) {
                                            if (downloadingRunId == build.id) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Get APK", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val currentProj = project ?: return@Button
                                            downloadingRunId = build.id
                                            coroutineScope.launch {
                                                val artifactsRes = apiService.listArtifacts(currentProj.repoOwner, currentProj.repoName, build.id)
                                                val aabArtifact = artifactsRes.getOrNull()?.find { it.name.contains("aab") || it.name.contains("bundle") }
                                                if (aabArtifact != null) {
                                                    val appDir = File(context.getExternalFilesDir(null), "Builds/${currentProj.name}").apply { mkdirs() }
                                                    val destZip = File(appDir, "build_${build.runNumber}_aab.zip")
                                                    val dlRes = apiService.downloadArtifact(aabArtifact.archiveDownloadUrl, destZip)
                                                    if (dlRes.isSuccess) {
                                                        Toast.makeText(context, "Saved to ${destZip.absolutePath}", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        Toast.makeText(context, "Failed to download AAB artifact", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "No AAB artifact found for this run", Toast.LENGTH_SHORT).show()
                                                }
                                                downloadingRunId = null
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                        enabled = downloadingRunId != build.id
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Release AAB", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required", fontWeight = FontWeight.Bold) },
            text = {
                Text("To install built Android apps from Kotlin IDE, Android requires granting permission to install unknown apps. Please allow installation in Android Settings.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        ApkInstaller.openInstallPermissionSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
