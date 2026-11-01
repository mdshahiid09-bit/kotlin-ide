package com.kotlinide.shahid.app.ui.screens.projects

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.kotlinide.shahid.app.ui.theme.DarkBackground
import com.kotlinide.shahid.app.ui.theme.DarkBorder
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.DarkSurfaceHighlight
import com.kotlinide.shahid.app.ui.theme.DarkTextPrimary
import com.kotlinide.shahid.app.ui.theme.DarkTextSecondary
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import com.kotlinide.shahid.app.ui.theme.OrangeWarning
import com.kotlinide.shahid.app.ui.theme.RedError
import com.kotlinide.shahid.app.util.safeIoCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val snackbarHostState = remember { SnackbarHostState() }

    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Delete dialog
    var projectToDelete by remember { mutableStateOf<Project?>(null) }
    var deleteFromGitHubAlso by remember { mutableStateOf(true) }
    var isDeleting by remember { mutableStateOf(false) }

    // Push to GitHub dialog
    var projectToPush by remember { mutableStateOf<Project?>(null) }
    var pushRepoName by remember { mutableStateOf("") }
    var pushIsPrivate by remember { mutableStateOf(true) }
    var isPushing by remember { mutableStateOf(false) }

    fun loadAndSync(forceRemote: Boolean = true, showToast: Boolean = false) {
        coroutineScope.launch(Dispatchers.IO) {
            // First load local projects instantly
            val localProjects = projectStorage.getProjects()
            withContext(Dispatchers.Main) {
                projects = localProjects
            }

            if (!tokenStorage.hasToken() || !forceRemote) return@launch

            withContext(Dispatchers.Main) { isRefreshing = true }

            val reposResult = apiService.getAllUserRepositories()
            if (reposResult.isSuccess) {
                val remoteRepos = reposResult.getOrThrow()
                val remoteNames = remoteRepos.map { "${it.owner.login}/${it.name}".lowercase() }.toSet()

                // Check local projects against remote
                var deletedCount = 0
                val updatedList = localProjects.toMutableList()

                for (p in localProjects) {
                    if (p.repoOwner.isNotBlank() && p.repoName.isNotBlank()) {
                        val key = "${p.repoOwner}/${p.repoName}".lowercase()
                        if (!remoteNames.contains(key)) {
                            // Repo was deleted on GitHub!
                            projectStorage.deleteProject(p.id)
                            updatedList.removeAll { it.id == p.id }
                            deletedCount++
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("'${p.name}' was deleted on GitHub and removed from the app.")
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    projects = updatedList
                    isRefreshing = false
                    if (showToast) {
                        Toast.makeText(context, "Projects synced with GitHub", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Fetch failed (network / rate limit / offline).
                // CRITICAL: NEVER delete local projects on network failure!
                withContext(Dispatchers.Main) {
                    isRefreshing = false
                    if (showToast) {
                        Toast.makeText(context, "GitHub sync offline. Showing local projects.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAndSync(forceRemote = true)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Android, contentDescription = null, tint = NeonGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Kotlin IDE", fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { loadAndSync(forceRemote = true, showToast = true) },
                            enabled = !isRefreshing
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync Repos", tint = Color.White)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )

                if (isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = NeonGreen,
                        trackColor = DarkSurfaceHighlight
                    )
                }
            }
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
        if (projects.isEmpty() && !isRefreshing) {
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
                            coroutineScope.launch(Dispatchers.IO) {
                                val downloadsDir = File(context.getExternalFilesDir(null), "Backups").apply { mkdirs() }
                                val zipFile = File(downloadsDir, "${project.name}_backup.zip")
                                val success = projectStorage.exportProjectToZip(project.id, zipFile)
                                withContext(Dispatchers.Main) {
                                    if (success) {
                                        Toast.makeText(context, "Exported backup: ${zipFile.name}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onPushToGitHub = {
                            projectToPush = project
                            pushRepoName = project.repoName.ifBlank { project.name.lowercase().replace(" ", "-") }
                            pushIsPrivate = project.isPrivate
                        },
                        onDelete = {
                            projectToDelete = project
                            deleteFromGitHubAlso = project.repoOwner.isNotBlank()
                        }
                    )
                }
            }
        }

        // Push to GitHub Dialog for Local-Only projects
        if (projectToPush != null) {
            val target = projectToPush!!
            AlertDialog(
                onDismissRequest = { if (!isPushing) projectToPush = null },
                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = NeonGreen) },
                title = { Text("Push '${target.name}' to GitHub", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Create a remote repository on GitHub and sync your project code.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pushRepoName,
                            onValueChange = { pushRepoName = it },
                            label = { Text("Repository Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                cursorColor = NeonGreen
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pushIsPrivate = !pushIsPrivate }
                        ) {
                            Checkbox(
                                checked = pushIsPrivate,
                                onCheckedChange = { pushIsPrivate = it },
                                colors = CheckboxDefaults.colors(checkedColor = NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Make repository private", fontSize = 13.sp, color = DarkTextPrimary)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isPushing = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val cleanRepoName = pushRepoName.trim()
                                val createRes = apiService.createRepository(
                                    name = cleanRepoName,
                                    description = "Built with Kotlin IDE on Android",
                                    isPrivate = pushIsPrivate
                                )

                                if (createRes.isSuccess) {
                                    val repo = createRes.getOrThrow()
                                    val projDir = projectStorage.getProjectDirectory(target.id)

                                    // Push all files to the new repo using for-loop (supports suspend functions)
                                    for (file in projDir.walkTopDown()) {
                                        if (file.isFile && !file.name.startsWith(".")) {
                                            val rel = file.relativeTo(projDir).path
                                            apiService.createOrUpdateBinaryFile(
                                                owner = repo.owner.login,
                                                repo = repo.name,
                                                path = rel,
                                                bytes = file.readBytes(),
                                                message = "Initial push $rel"
                                            )
                                        }
                                    }

                                    val updatedProject = target.copy(
                                        repoOwner = repo.owner.login,
                                        repoName = repo.name,
                                        repoUrl = repo.htmlUrl,
                                        isPrivate = pushIsPrivate
                                    )
                                    projectStorage.saveProject(updatedProject)

                                    withContext(Dispatchers.Main) {
                                        isPushing = false
                                        projectToPush = null
                                        loadAndSync(forceRemote = false)
                                        Toast.makeText(context, "Successfully pushed to GitHub!", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        isPushing = false
                                        val err = createRes.exceptionOrNull()?.message ?: "Failed to create repository"
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = pushRepoName.isNotBlank() && !isPushing,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                    ) {
                        Text(if (isPushing) "Pushing..." else "Push Now")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { projectToPush = null },
                        enabled = !isPushing
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Dialog
        if (projectToDelete != null) {
            val target = projectToDelete!!
            val hasRemote = target.repoOwner.isNotBlank() && target.repoName.isNotBlank()

            AlertDialog(
                onDismissRequest = { if (!isDeleting) projectToDelete = null },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = RedError) },
                title = { Text("Delete '${target.name}'", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Are you sure you want to delete '${target.name}' from your phone? Local source files and build history will be removed.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (hasRemote) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteFromGitHubAlso = !deleteFromGitHubAlso }
                            ) {
                                Checkbox(
                                    checked = deleteFromGitHubAlso,
                                    onCheckedChange = { deleteFromGitHubAlso = it },
                                    colors = CheckboxDefaults.colors(checkedColor = RedError)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Also delete '${target.repoOwner}/${target.repoName}' from GitHub",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = RedError
                                    )
                                    Text(
                                        text = "Permanent action: Repository on GitHub will be deleted",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DarkTextSecondary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isDeleting = true
                            coroutineScope.launch(Dispatchers.IO) {
                                if (deleteFromGitHubAlso && hasRemote) {
                                    apiService.deleteRepository(target.repoOwner, target.repoName)
                                }
                                projectStorage.deleteProject(target.id)

                                withContext(Dispatchers.Main) {
                                    isDeleting = false
                                    projectToDelete = null
                                    loadAndSync(forceRemote = false)
                                    Toast.makeText(context, "Project deleted", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isDeleting,
                        colors = ButtonDefaults.buttonColors(containerColor = RedError, contentColor = Color.White)
                    ) {
                        Text(if (isDeleting) "Deleting..." else "Delete Project")
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
    onPushToGitHub: () -> Unit,
    onDelete: () -> Unit
) {
    val isLocalOnly = project.repoOwner.isBlank() || project.repoName.isBlank()

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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isLocalOnly) {
                        Box(
                            modifier = Modifier
                                .background(OrangeWarning.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(text = "Not on GitHub", color = OrangeWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    BuildStatusBadge(status = project.lastBuildStatus)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = project.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isLocalOnly) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onPushToGitHub,
                        colors = ButtonDefaults.textButtonColors(contentColor = NeonGreen),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Push to GitHub", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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

                // Action buttons with minimum 48dp touch targets
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNotes,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Note, contentDescription = "Project Notes", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onExportZip,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = "Backup ZIP", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Project", tint = RedError, modifier = Modifier.size(20.dp))
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
