package com.kotlinide.shahid.app.ui.screens.projects

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.kotlinide.shahid.app.data.model.Project
import com.kotlinide.shahid.app.data.security.TokenStorage
import com.kotlinide.shahid.app.data.storage.ProjectStorage
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import com.kotlinide.shahid.app.ui.theme.OrangeWarning
import com.kotlinide.shahid.app.ui.theme.RedError
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    projectStorage: ProjectStorage,
    tokenStorage: TokenStorage,
    onOpenProject: (String) -> Unit,
    onCreateNewProject: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotes: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { GitHubApiService(tokenStorage) }

    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }
    var confirmationInput by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }

    fun refreshProjects() {
        coroutineScope.launch {
            projects = projectStorage.getProjects()
        }
    }

    LaunchedEffect(Unit) {
        refreshProjects()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = NeonGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Kotlin IDE", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNewProject,
                containerColor = NeonGreen,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Project")
            }
        }
    ) { innerPadding ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Android,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No projects yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the + button below to create your first Jetpack Compose Android app.",
                        style = MaterialTheme.typography.bodyMedium,
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
                items(projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onOpenProject(project.id) },
                        onNotes = { onOpenNotes(project.id) },
                        onExportZip = {
                            coroutineScope.launch {
                                val downloadsDir = File(context.getExternalFilesDir(null), "Backups").apply { mkdirs() }
                                val zipFile = File(downloadsDir, "${project.name}_backup.zip")
                                val success = projectStorage.exportProjectToZip(project.id, zipFile)
                                if (success) {
                                    Toast.makeText(context, "Exported: ${zipFile.name}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDelete = {
                            projectToDelete = project
                            confirmationInput = ""
                        }
                    )
                }
            }
        }

        if (projectToDelete != null) {
            val target = projectToDelete!!
            AlertDialog(
                onDismissRequest = { if (!isDeleting) projectToDelete = null },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = RedError) },
                title = { Text("Delete Project & GitHub Repo", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "This action is PERMANENT. It will delete the local project and remove the repository '${target.repoOwner}/${target.repoName}' from GitHub.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Type '${target.name}' to confirm:",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmationInput,
                            onValueChange = { confirmationInput = it },
                            placeholder = { Text(target.name) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isDeleting = true
                            coroutineScope.launch {
                                if (target.repoOwner.isNotEmpty() && target.repoName.isNotEmpty()) {
                                    apiService.deleteRepository(target.repoOwner, target.repoName)
                                }
                                projectStorage.deleteProject(target.id)
                                isDeleting = false
                                projectToDelete = null
                                refreshProjects()
                                Toast.makeText(context, "Project deleted permanently", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = (confirmationInput.trim() == target.name) && !isDeleting,
                        colors = ButtonDefaults.buttonColors(containerColor = RedError, contentColor = Color.White)
                    ) {
                        Text("Permanently Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { projectToDelete = null },
                        enabled = !isDeleting
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onNotes: () -> Unit,
    onExportZip: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                BuildStatusBadge(status = project.lastBuildStatus)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = project.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "v${project.versionName} (${project.versionCode}) • SDK ${project.minSdk}-${project.targetSdk}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    IconButton(onClick = onNotes, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Note, contentDescription = "Notes", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onExportZip, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Archive, contentDescription = "Backup ZIP", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedError, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BuildStatusBadge(status: String) {
    val (color, icon, text) = when (status) {
        "SUCCESS" -> Triple(NeonGreen, Icons.Default.CheckCircle, "Built")
        "FAILURE" -> Triple(RedError, Icons.Default.Error, "Failed")
        "IN_PROGRESS", "QUEUED" -> Triple(OrangeWarning, Icons.Default.HourglassTop, "Building")
        else -> Triple(Color.Gray, Icons.Default.Build, "Ready")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
