package com.kotlinide.shahid.app.ui.screens.editor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotlinide.shahid.app.data.model.Project
import com.kotlinide.shahid.app.data.storage.ProjectStorage
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.DarkSurfaceHighlight
import com.kotlinide.shahid.app.ui.theme.DarkTextPrimary
import com.kotlinide.shahid.app.ui.theme.DarkTextSecondary
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProjectDashboardDialog(
    project: Project,
    projectStorage: ProjectStorage,
    onDismiss: () -> Unit,
    onTriggerBuild: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val projectDir = projectStorage.getProjectDirectory(project.id)

    var fileCount = 0
    var folderCount = 0
    var totalBytes = 0L

    projectDir.walkTopDown().forEach {
        if (it.isDirectory) folderCount++
        else {
            fileCount++
            totalBytes += it.length()
        }
    }

    val sizeStr = if (totalBytes > 1024 * 1024) {
        String.format("%.2f MB", totalBytes / (1024f * 1024f))
    } else {
        String.format("%.1f KB", totalBytes / 1024f)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Dashboard, contentDescription = null, tint = NeonGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(project.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceHighlight),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• Package: ${project.packageName}", style = MaterialTheme.typography.bodySmall, color = DarkTextPrimary)
                        Text("• Version: ${project.versionName} (${project.versionCode})", style = MaterialTheme.typography.bodySmall, color = DarkTextPrimary)
                        Text("• Template: ${project.templateId}", style = MaterialTheme.typography.bodySmall, color = DarkTextPrimary)
                        Text("• SDK Range: API ${project.minSdk} - ${project.targetSdk}", style = MaterialTheme.typography.bodySmall, color = DarkTextPrimary)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceHighlight),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• Files: $fileCount items", style = MaterialTheme.typography.bodySmall, color = NeonGreen)
                        Text("• Folders: $folderCount directories", style = MaterialTheme.typography.bodySmall, color = DarkTextSecondary)
                        Text("• Size on Disk: $sizeStr", style = MaterialTheme.typography.bodySmall, color = DarkTextSecondary)
                    }
                }

                if (project.repoName.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceHighlight),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${project.repoOwner}/${project.repoName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val targetZip = File(context.cacheDir, "${project.name}.zip")
                                val success = projectStorage.exportProjectToZip(project.id, targetZip)
                                if (success) {
                                    Toast.makeText(context, "Exported ZIP to cache: ${targetZip.name}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export ZIP")
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onTriggerBuild()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Build")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
