package com.kotlinide.shahid.app.ui.screens.editor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.ForkRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.kotlinide.shahid.app.data.api.GitHubApiService
import com.kotlinide.shahid.app.data.api.GitHubBranchItem
import com.kotlinide.shahid.app.data.api.GitHubCommitItem
import com.kotlinide.shahid.app.data.model.Project
import com.kotlinide.shahid.app.ui.theme.DarkBackground
import com.kotlinide.shahid.app.ui.theme.DarkBorder
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.DarkSurfaceHighlight
import com.kotlinide.shahid.app.ui.theme.DarkTextPrimary
import com.kotlinide.shahid.app.ui.theme.DarkTextSecondary
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun GitHubGitDialog(
    project: Project,
    projectRoot: File,
    apiService: GitHubApiService,
    onDismiss: () -> Unit,
    onPullComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    var commitMessage by remember { mutableStateOf("Update project files") }
    var isPushing by remember { mutableStateOf(false) }
    var isPulling by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    val commitsList = remember { mutableStateListOf<GitHubCommitItem>() }
    var isLoadingCommits by remember { mutableStateOf(false) }

    val branchesList = remember { mutableStateListOf<GitHubBranchItem>() }
    var currentBranch by remember { mutableStateOf("main") }
    var newBranchName by remember { mutableStateOf("") }
    var showNewBranchDialog by remember { mutableStateOf(false) }

    // Scan for local uncommitted / changed files
    val changedFiles = remember { mutableStateListOf<File>() }

    fun scanChangedFiles() {
        changedFiles.clear()
        projectRoot.walkTopDown().forEach { f ->
            if (f.isFile && !f.path.contains("/.git/") && !f.path.contains("/build/") && !f.path.contains("/.gradle/")) {
                changedFiles.add(f)
            }
        }
    }

    LaunchedEffect(Unit) {
        scanChangedFiles()
        // Load commits
        isLoadingCommits = true
        coroutineScope.launch {
            val res = apiService.listCommits(project.repoOwner, project.repoName, currentBranch)
            isLoadingCommits = false
            res.onSuccess {
                commitsList.clear()
                commitsList.addAll(it)
            }
        }
        // Load branches
        coroutineScope.launch {
            val bRes = apiService.listBranches(project.repoOwner, project.repoName)
            bRes.onSuccess {
                branchesList.clear()
                branchesList.addAll(it)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Commit, contentDescription = null, tint = NeonGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("GitHub Git Workflow", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("${project.repoOwner}/${project.repoName} ($currentBranch)", style = MaterialTheme.typography.labelSmall, color = DarkTextSecondary)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = NeonGreen,
                    edgePadding = 0.dp
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Push / Sync", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("History", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Branches", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> { // Push & Sync
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Local Files Ready to Sync (${changedFiles.size}):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(DarkBackground, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                LazyColumn {
                                    items(changedFiles.take(40)) { file ->
                                        val relPath = file.relativeTo(projectRoot).path
                                        Text(
                                            text = "• $relPath",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = DarkTextPrimary,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = commitMessage,
                                onValueChange = { commitMessage = it },
                                label = { Text("Commit Message") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (statusText != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(statusText!!, color = NeonGreen, style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isPulling = true
                                        statusText = "Pulling latest files from GitHub..."
                                        coroutineScope.launch {
                                            val treeRes = apiService.getTree(project.repoOwner, project.repoName, currentBranch)
                                            isPulling = false
                                            treeRes.onSuccess { tree ->
                                                tree.forEach { item ->
                                                    if (item.type == "blob") {
                                                        val content = apiService.getFileContent(project.repoOwner, project.repoName, item.path, currentBranch).getOrNull()
                                                        if (content != null) {
                                                            val f = File(projectRoot, item.path)
                                                            f.parentFile?.mkdirs()
                                                            f.writeText(content)
                                                        }
                                                    }
                                                }
                                                statusText = "Pull completed successfully!"
                                                scanChangedFiles()
                                                onPullComplete()
                                            }.onFailure {
                                                statusText = "Pull failed: ${it.message}"
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight, contentColor = Color.White),
                                    enabled = !isPushing && !isPulling
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pull")
                                }

                                Button(
                                    onClick = {
                                        isPushing = true
                                        statusText = "Pushing commit to GitHub..."
                                        coroutineScope.launch {
                                            var successCount = 0
                                            val filesToPush = changedFiles.filter { !it.isDirectory }
                                            for (file in filesToPush) {
                                                val relPath = file.relativeTo(projectRoot).path
                                                val content = file.readBytes()
                                                val res = apiService.createOrUpdateBinaryFile(
                                                    owner = project.repoOwner,
                                                    repo = project.repoName,
                                                    path = relPath,
                                                    bytes = content,
                                                    message = commitMessage,
                                                    branch = currentBranch
                                                )
                                                if (res.isSuccess) successCount++
                                            }
                                            isPushing = false
                                            statusText = "Committed & Pushed $successCount files to $currentBranch!"
                                            Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                                    enabled = !isPushing && !isPulling
                                ) {
                                    if (isPushing) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                                    } else {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Push")
                                    }
                                }
                            }
                        }
                    }

                    1 -> { // Commit History
                        if (isLoadingCommits) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = NeonGreen)
                            }
                        } else if (commitsList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No commits found.", color = DarkTextSecondary)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(commitsList) { commitItem ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceHighlight),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = commitItem.commit.message,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = commitItem.commit.author?.name ?: "Author",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DarkTextSecondary
                                                )
                                                Text(
                                                    text = commitItem.sha.take(7),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = NeonGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // Branches
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Active Branch: $currentBranch", fontWeight = FontWeight.Bold, color = NeonGreen)
                                Button(
                                    onClick = { showNewBranchDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                                ) {
                                    Text("+ New Branch")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(branchesList) { branch ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (branch.name == currentBranch) NeonGreen.copy(alpha = 0.15f) else DarkSurfaceHighlight
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { currentBranch = branch.name }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.ForkRight, contentDescription = null, tint = if (branch.name == currentBranch) NeonGreen else Color.White)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(branch.name, fontWeight = FontWeight.SemiBold, color = if (branch.name == currentBranch) NeonGreen else Color.White)
                                        }
                                    }
                                }
                            }
                        }
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

    if (showNewBranchDialog) {
        AlertDialog(
            onDismissRequest = { showNewBranchDialog = false },
            title = { Text("Create New Branch") },
            text = {
                OutlinedTextField(
                    value = newBranchName,
                    onValueChange = { newBranchName = it },
                    label = { Text("Branch Name") },
                    placeholder = { Text("feature/ui-update") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newBranchName.trim()
                        if (name.isNotBlank()) {
                            val headSha = commitsList.firstOrNull()?.sha
                            if (headSha != null) {
                                coroutineScope.launch {
                                    val res = apiService.createBranch(project.repoOwner, project.repoName, name, headSha)
                                    res.onSuccess {
                                        currentBranch = name
                                        showNewBranchDialog = false
                                        Toast.makeText(context, "Created branch $name", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewBranchDialog = false }) { Text("Cancel") }
            }
        )
    }
}
