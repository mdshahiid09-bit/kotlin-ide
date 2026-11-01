package com.kotlinide.shahid.app.ui.screens.editor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.ForkRight
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.ReportProblem
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
import com.kotlinide.shahid.app.data.api.GitHubIssue
import com.kotlinide.shahid.app.data.api.GitHubPullRequest
import com.kotlinide.shahid.app.data.model.Project
import com.kotlinide.shahid.app.ui.theme.DarkBackground
import com.kotlinide.shahid.app.ui.theme.DarkBorder
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.DarkSurfaceHighlight
import com.kotlinide.shahid.app.ui.theme.DarkTextPrimary
import com.kotlinide.shahid.app.ui.theme.DarkTextSecondary
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import com.kotlinide.shahid.app.util.AuditLogger
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

    // Pull Requests
    val prsList = remember { mutableStateListOf<GitHubPullRequest>() }
    var isLoadingPrs by remember { mutableStateOf(false) }
    var showCreatePrDialog by remember { mutableStateOf(false) }
    var prTitle by remember { mutableStateOf("") }
    var prBody by remember { mutableStateOf("") }
    var prHeadBranch by remember { mutableStateOf("") }
    var prBaseBranch by remember { mutableStateOf("main") }

    // Issues
    val issuesList = remember { mutableStateListOf<GitHubIssue>() }
    var isLoadingIssues by remember { mutableStateOf(false) }
    var showCreateIssueDialog by remember { mutableStateOf(false) }
    var issueTitle by remember { mutableStateOf("") }
    var issueBody by remember { mutableStateOf("") }

    val changedFiles = remember { mutableStateListOf<File>() }

    fun scanChangedFiles() {
        changedFiles.clear()
        if (projectRoot.exists()) {
            for (file in projectRoot.walkTopDown()) {
                if (file.isFile && !file.name.startsWith(".") && !file.path.contains("/build/") && !file.path.contains("/.gradle/")) {
                    changedFiles.add(file)
                }
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
        // Load PRs
        coroutineScope.launch {
            val prRes = apiService.listPullRequests(project.repoOwner, project.repoName)
            prRes.onSuccess {
                prsList.clear()
                prsList.addAll(it)
            }
        }
        // Load Issues
        coroutineScope.launch {
            val issRes = apiService.listIssues(project.repoOwner, project.repoName)
            issRes.onSuccess {
                issuesList.clear()
                issuesList.addAll(it)
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
                    Text("Git Operations & Management", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
                        text = { Text("Sync", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
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
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Pull Requests", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("Issues", fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) }
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
                                                AuditLogger.log(
                                                    context = context,
                                                    eventType = "CODE_PULLED",
                                                    details = "Pulled branch '$currentBranch' files to local project",
                                                    projectId = project.id,
                                                    repoName = project.repoName
                                                )
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
                                            statusText = "Successfully pushed $successCount files to $currentBranch!"
                                            AuditLogger.log(
                                                context = context,
                                                eventType = "CODE_PUSHED",
                                                details = "Pushed $successCount files to branch '$currentBranch' ($commitMessage)",
                                                projectId = project.id,
                                                repoName = project.repoName
                                            )
                                            scanChangedFiles()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                                    enabled = !isPushing && !isPulling && changedFiles.isNotEmpty()
                                ) {
                                    if (isPushing) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Commit & Push")
                                    }
                                }
                            }
                        }
                    }

                    1 -> { // Commits History
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
                                        colors = CardDefaults.cardColors(containerColor = DarkBackground),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(commitItem.commit.message, fontWeight = FontWeight.SemiBold, color = DarkTextPrimary, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(commitItem.commit.author?.name ?: "Unknown", color = DarkTextSecondary, fontSize = 11.sp)
                                                Text(commitItem.sha.take(7), fontFamily = FontFamily.Monospace, color = NeonGreen, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // Branches
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Available Branches (${branchesList.size}):", style = MaterialTheme.typography.labelSmall)
                                IconButton(onClick = { showNewBranchDialog = true }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = "New Branch", tint = NeonGreen)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(branchesList) { branch ->
                                    val isCurrent = branch.name == currentBranch
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = if (isCurrent) NeonGreen.copy(alpha = 0.1f) else DarkBackground),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().clickable { currentBranch = branch.name }
                                    ) {
                                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ForkRight, contentDescription = null, tint = if (isCurrent) NeonGreen else DarkTextSecondary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(branch.name, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = if (isCurrent) NeonGreen else DarkTextPrimary, fontSize = 13.sp)
                                            if (isCurrent) {
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                            }
                                        }
                                    }
                                }
                            }

                            if (currentBranch != "main") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            statusText = "Merging $currentBranch into main..."
                                            val mergeRes = apiService.mergeBranch(
                                                owner = project.repoOwner,
                                                repo = project.repoName,
                                                base = "main",
                                                head = currentBranch,
                                                commitMessage = "Merge $currentBranch into main via Kotlin IDE"
                                            )
                                            mergeRes.onSuccess {
                                                statusText = "Merged successfully!"
                                                AuditLogger.log(
                                                    context = context,
                                                    eventType = "BRANCH_MERGED",
                                                    details = "Merged $currentBranch into main",
                                                    projectId = project.id,
                                                    repoName = project.repoName
                                                )
                                            }.onFailure {
                                                statusText = "Merge error: ${it.message}"
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight, contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Merge, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Merge $currentBranch into main")
                                }
                            }
                        }
                    }

                    3 -> { // Pull Requests
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Pull Requests (${prsList.size}):", style = MaterialTheme.typography.labelSmall)
                                IconButton(onClick = { showCreatePrDialog = true }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = "New PR", tint = NeonGreen)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            if (prsList.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("No pull requests found.", color = DarkTextSecondary)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(prsList) { pr ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("#${pr.number} ${pr.title}", fontWeight = FontWeight.Bold, color = DarkTextPrimary, fontSize = 13.sp)
                                                    Text(pr.state.uppercase(), fontWeight = FontWeight.Bold, color = NeonGreen, fontSize = 10.sp)
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("by @${pr.user?.login ?: "unknown"}", fontSize = 11.sp, color = DarkTextSecondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    4 -> { // Issues
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Issues (${issuesList.size}):", style = MaterialTheme.typography.labelSmall)
                                IconButton(onClick = { showCreateIssueDialog = true }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = "New Issue", tint = NeonGreen)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            if (issuesList.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("No open issues found.", color = DarkTextSecondary)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(issuesList) { issue ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("#${issue.number} ${issue.title}", fontWeight = FontWeight.Bold, color = DarkTextPrimary, fontSize = 13.sp)
                                                    Text(issue.state.uppercase(), fontWeight = FontWeight.Bold, color = if (issue.state == "open") NeonGreen else DarkTextSecondary, fontSize = 10.sp)
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("${issue.comments} comments", fontSize = 11.sp, color = DarkTextSecondary)
                                            }
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
            title = { Text("Create New Branch", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Branch will branch off from latest commit on $currentBranch.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newBranchName,
                        onValueChange = { newBranchName = it },
                        label = { Text("Branch Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bName = newBranchName.trim()
                        if (bName.isNotBlank()) {
                            coroutineScope.launch {
                                val shaRes = apiService.getLatestCommitSha(project.repoOwner, project.repoName, currentBranch)
                                val sha = shaRes.getOrNull()
                                if (sha != null) {
                                    val createRes = apiService.createBranch(project.repoOwner, project.repoName, bName, sha)
                                    if (createRes.isSuccess) {
                                        currentBranch = bName
                                        showNewBranchDialog = false
                                        newBranchName = ""
                                        AuditLogger.log(
                                            context = context,
                                            eventType = "BRANCH_CREATED",
                                            details = "Created branch '$bName' from $currentBranch",
                                            projectId = project.id,
                                            repoName = project.repoName
                                        )
                                        // Refresh branch list
                                        apiService.listBranches(project.repoOwner, project.repoName).onSuccess {
                                            branchesList.clear()
                                            branchesList.addAll(it)
                                        }
                                        Toast.makeText(context, "Branch '$bName' created!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed: ${createRes.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                ) {
                    Text("Create Branch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewBranchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreatePrDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePrDialog = false },
            title = { Text("Create Pull Request", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = prTitle,
                        onValueChange = { prTitle = it },
                        label = { Text("PR Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = if (prHeadBranch.isBlank()) currentBranch else prHeadBranch,
                        onValueChange = { prHeadBranch = it },
                        label = { Text("Head Branch (source)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prBaseBranch,
                        onValueChange = { prBaseBranch = it },
                        label = { Text("Base Branch (target)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prBody,
                        onValueChange = { prBody = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = prTitle.trim()
                        val head = if (prHeadBranch.isBlank()) currentBranch else prHeadBranch.trim()
                        val base = prBaseBranch.trim()
                        if (title.isNotBlank() && head.isNotBlank() && base.isNotBlank()) {
                            coroutineScope.launch {
                                val res = apiService.createPullRequest(
                                    owner = project.repoOwner,
                                    repo = project.repoName,
                                    title = title,
                                    head = head,
                                    base = base,
                                    body = prBody
                                )
                                if (res.isSuccess) {
                                    val newPr = res.getOrThrow()
                                    prsList.add(0, newPr)
                                    showCreatePrDialog = false
                                    prTitle = ""
                                    prBody = ""
                                    AuditLogger.log(
                                        context = context,
                                        eventType = "PR_CREATED",
                                        details = "Created PR #${newPr.number}: $title",
                                        projectId = project.id,
                                        repoName = project.repoName
                                    )
                                    Toast.makeText(context, "Pull request #${newPr.number} created!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "PR failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                ) {
                    Text("Create PR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePrDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreateIssueDialog) {
        AlertDialog(
            onDismissRequest = { showCreateIssueDialog = false },
            title = { Text("Create New Issue", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = issueTitle,
                        onValueChange = { issueTitle = it },
                        label = { Text("Issue Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = issueBody,
                        onValueChange = { issueBody = it },
                        label = { Text("Issue Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = issueTitle.trim()
                        if (title.isNotBlank()) {
                            coroutineScope.launch {
                                val res = apiService.createIssue(
                                    owner = project.repoOwner,
                                    repo = project.repoName,
                                    title = title,
                                    body = issueBody
                                )
                                if (res.isSuccess) {
                                    val newIssue = res.getOrThrow()
                                    issuesList.add(0, newIssue)
                                    showCreateIssueDialog = false
                                    issueTitle = ""
                                    issueBody = ""
                                    AuditLogger.log(
                                        context = context,
                                        eventType = "ISSUE_CREATED",
                                        details = "Created Issue #${newIssue.number}: $title",
                                        projectId = project.id,
                                        repoName = project.repoName
                                    )
                                    Toast.makeText(context, "Issue #${newIssue.number} created!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Issue failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                ) {
                    Text("Create Issue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateIssueDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
