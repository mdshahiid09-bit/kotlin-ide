package com.kotlinide.shahid.app.ui.screens.editor

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlinide.shahid.app.data.api.GitHubApiException
import com.kotlinide.shahid.app.data.api.GitHubApiService
import com.kotlinide.shahid.app.data.api.QueuedSyncAction
import com.kotlinide.shahid.app.data.api.SyncActionType
import com.kotlinide.shahid.app.data.model.BuildErrorLine
import com.kotlinide.shahid.app.data.model.BuildInfo
import com.kotlinide.shahid.app.data.model.EditorTab
import com.kotlinide.shahid.app.data.model.FileNode
import com.kotlinide.shahid.app.data.model.Project
import com.kotlinide.shahid.app.data.security.TokenStorage
import com.kotlinide.shahid.app.data.storage.DraftStorage
import com.kotlinide.shahid.app.data.storage.ProjectStorage
import com.kotlinide.shahid.app.data.sync.SyncQueueManager
import com.kotlinide.shahid.app.ui.screens.build.BuildLogPanel
import com.kotlinide.shahid.app.ui.theme.DarkBackground
import com.kotlinide.shahid.app.ui.theme.DarkBorder
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.DarkSurfaceHighlight
import com.kotlinide.shahid.app.ui.theme.DarkSurfaceVariant
import com.kotlinide.shahid.app.ui.theme.DarkTextPrimary
import com.kotlinide.shahid.app.ui.theme.DarkTextSecondary
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import com.kotlinide.shahid.app.ui.theme.OrangeWarning
import com.kotlinide.shahid.app.ui.theme.RedError
import com.kotlinide.shahid.app.util.CrashReportingHelper
import com.kotlinide.shahid.app.util.NotificationHelper
import com.kotlinide.shahid.app.util.safeIoCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class EditorSyncStatus {
    SYNCED,
    SYNCING,
    OFFLINE,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: String,
    projectStorage: ProjectStorage,
    tokenStorage: TokenStorage,
    draftStorage: DraftStorage,
    syncQueueManager: SyncQueueManager,
    onBack: () -> Unit,
    onOpenBuildHistory: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val apiService = remember { GitHubApiService(tokenStorage) }

    var project by remember { mutableStateOf<Project?>(null) }
    val openTabs = remember { mutableStateListOf<EditorTab>() }
    var activeTabIndex by remember { mutableIntStateOf(0) }

    var fileTree by remember { mutableStateOf<List<FileNode>>(emptyList()) }
    val projectDir = remember(projectId) { projectStorage.getProjectDirectory(projectId) }

    // Sync State
    var syncStatus by remember { mutableStateOf(EditorSyncStatus.SYNCED) }
    var pendingQueueCount by remember { mutableIntStateOf(0) }
    var syncErrorMessage by remember { mutableStateOf<String?>(null) }
    var showSyncErrorDialog by remember { mutableStateOf(false) }

    // Remote Polling & Conflict State
    var lastKnownRemoteSha by remember { mutableStateOf<String?>(null) }
    var showRemoteChangesBanner by remember { mutableStateOf(false) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictAction by remember { mutableStateOf<QueuedSyncAction?>(null) }
    var conflictRemoteContent by remember { mutableStateOf("") }
    var conflictRemoteSha by remember { mutableStateOf("") }

    // Editor configurations
    var fontSizeSp by remember { mutableIntStateOf(13) }
    var isWordWrapEnabled by remember { mutableStateOf(false) }

    // Search & Replace
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var showReplaceAllDialog by remember { mutableStateOf(false) }

    // Dialogs
    var showNewItemDialog by remember { mutableStateOf(false) }
    var isCreatingFolder by remember { mutableStateOf(false) }
    var targetFolderForNewItem by remember { mutableStateOf(projectDir) }

    var selectedNodeForAction by remember { mutableStateOf<FileNode?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var deleteFromGitHubAlso by remember { mutableStateOf(true) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var actionInputText by remember { mutableStateOf("") }

    var showGitDialog by remember { mutableStateOf(false) }
    var showDashboardDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Build flow
    var isBuilding by remember { mutableStateOf(false) }
    var showBuildLogPanel by remember { mutableStateOf(false) }
    var currentBuildInfo by remember { mutableStateOf<BuildInfo?>(null) }

    fun refreshFileTree() {
        coroutineScope.launch(Dispatchers.IO) {
            fun buildTree(dir: File, relPrefix: String): List<FileNode> {
                val list = mutableListOf<FileNode>()
                val files = dir.listFiles()?.sortedWith(
                    compareBy<File> { !it.isDirectory }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                ) ?: return list

                for (f in files) {
                    if (f.name == ".git" || f.name == "build" || f.name == ".gradle" || f.name == ".idea") continue
                    val relPath = if (relPrefix.isEmpty()) f.name else "$relPrefix/${f.name}"
                    if (f.isDirectory) {
                        list.add(
                            FileNode(
                                name = f.name,
                                path = relPath,
                                isDirectory = true,
                                children = buildTree(f, relPath).toMutableList()
                            )
                        )
                    } else {
                        list.add(
                            FileNode(
                                name = f.name,
                                path = relPath,
                                isDirectory = false
                            )
                        )
                    }
                }
                return list
            }

            val tree = buildTree(projectDir, "")
            withContext(Dispatchers.Main) {
                fileTree = tree
            }
        }
    }

    fun openFileInTab(relPath: String, targetLine: Int = -1) {
        val existingIndex = openTabs.indexOfFirst { it.filePath == relPath }
        if (existingIndex >= 0) {
            activeTabIndex = existingIndex
        } else {
            coroutineScope.launch(Dispatchers.IO) {
                val file = File(projectDir, relPath)
                if (file.exists() && !file.isDirectory) {
                    val draft = draftStorage.getDraft(projectId, relPath)
                    val diskContent = file.readText()
                    val initialContent = draft ?: diskContent
                    val hadDraft = (draft != null && draft != diskContent)

                    withContext(Dispatchers.Main) {
                        val newTab = EditorTab(
                            filePath = relPath,
                            fileName = file.name,
                            content = initialContent,
                            savedContent = diskContent,
                            isDirty = hadDraft
                        )
                        if (openTabs.size >= 8) {
                            openTabs.removeAt(0)
                        }
                        openTabs.add(newTab)
                        activeTabIndex = openTabs.lastIndex

                        if (hadDraft) {
                            Toast.makeText(context, "Restored unsaved draft for ${file.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    fun saveActiveFile() {
        if (openTabs.isEmpty() || activeTabIndex !in openTabs.indices) return
        val tab = openTabs[activeTabIndex]
        val currentProj = project

        coroutineScope.launch(Dispatchers.IO) {
            val file = File(projectDir, tab.filePath)
            file.parentFile?.mkdirs()
            file.writeText(tab.content)
            withContext(Dispatchers.Main) {
                tab.markSaved()
            }

            if (currentProj != null && currentProj.repoOwner.isNotBlank() && currentProj.repoName.isNotBlank()) {
                withContext(Dispatchers.Main) { syncStatus = EditorSyncStatus.SYNCING }
                val res = apiService.createOrUpdateFile(
                    owner = currentProj.repoOwner,
                    repo = currentProj.repoName,
                    path = tab.filePath,
                    contentString = tab.content,
                    message = "Update ${tab.fileName} from Kotlin IDE"
                )

                if (res.isSuccess) {
                    draftStorage.clearDraft(projectId, tab.filePath)
                    val count = syncQueueManager.getPendingCount(projectId)
                    withContext(Dispatchers.Main) {
                        syncStatus = EditorSyncStatus.SYNCED
                        pendingQueueCount = count
                        Toast.makeText(context, "Saved & Synced ${tab.fileName}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val ex = res.exceptionOrNull()
                    if (ex is GitHubApiException && (ex.statusCode == 409 || ex.statusCode == 422)) {
                        val remote = apiService.getFileDetails(currentProj.repoOwner, currentProj.repoName, tab.filePath).getOrNull()
                        val remoteContent = remote?.content?.let { apiService.decodeBase64(it) }.orEmpty()
                        val remoteSha = remote?.sha.orEmpty()
                        withContext(Dispatchers.Main) {
                            conflictAction = QueuedSyncAction(
                                projectId = projectId,
                                owner = currentProj.repoOwner,
                                repo = currentProj.repoName,
                                path = tab.filePath,
                                actionType = SyncActionType.CREATE_OR_UPDATE,
                                content = tab.content
                            )
                            conflictRemoteContent = remoteContent
                            conflictRemoteSha = remoteSha
                            showConflictDialog = true
                            syncStatus = EditorSyncStatus.ERROR
                            syncErrorMessage = "Conflict on ${tab.fileName} (Remote was changed on GitHub)"
                        }
                    } else {
                        syncQueueManager.enqueue(
                            QueuedSyncAction(
                                projectId = projectId,
                                owner = currentProj.repoOwner,
                                repo = currentProj.repoName,
                                path = tab.filePath,
                                actionType = SyncActionType.CREATE_OR_UPDATE,
                                content = tab.content
                            )
                        )
                        val count = syncQueueManager.getPendingCount(projectId)
                        withContext(Dispatchers.Main) {
                            syncStatus = EditorSyncStatus.OFFLINE
                            pendingQueueCount = count
                            Toast.makeText(context, "Saved locally (Offline - queued for sync)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                draftStorage.clearDraft(projectId, tab.filePath)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Saved ${tab.fileName}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Debounced Auto-Save Effect
    LaunchedEffect(openTabs.map { it.content }) {
        delay(2500)
        val dirtyTabs = openTabs.filter { it.isDirty }
        if (dirtyTabs.isNotEmpty()) {
            val currentProj = project
            coroutineScope.launch(Dispatchers.IO) {
                dirtyTabs.forEach { tab ->
                    val file = File(projectDir, tab.filePath)
                    file.parentFile?.mkdirs()
                    file.writeText(tab.content)
                    withContext(Dispatchers.Main) {
                        tab.markSaved()
                    }

                    if (currentProj != null && currentProj.repoOwner.isNotBlank() && currentProj.repoName.isNotBlank()) {
                        withContext(Dispatchers.Main) { syncStatus = EditorSyncStatus.SYNCING }
                        val res = apiService.createOrUpdateFile(
                            owner = currentProj.repoOwner,
                            repo = currentProj.repoName,
                            path = tab.filePath,
                            contentString = tab.content,
                            message = "Update ${tab.fileName} from Kotlin IDE"
                        )

                        if (res.isSuccess) {
                            draftStorage.clearDraft(projectId, tab.filePath)
                            val count = syncQueueManager.getPendingCount(projectId)
                            withContext(Dispatchers.Main) {
                                syncStatus = EditorSyncStatus.SYNCED
                                pendingQueueCount = count
                            }
                        } else {
                            val ex = res.exceptionOrNull()
                            if (ex is GitHubApiException && (ex.statusCode == 409 || ex.statusCode == 422)) {
                                val remote = apiService.getFileDetails(currentProj.repoOwner, currentProj.repoName, tab.filePath).getOrNull()
                                val remoteContent = remote?.content?.let { apiService.decodeBase64(it) }.orEmpty()
                                val remoteSha = remote?.sha.orEmpty()
                                withContext(Dispatchers.Main) {
                                    conflictAction = QueuedSyncAction(
                                        projectId = projectId,
                                        owner = currentProj.repoOwner,
                                        repo = currentProj.repoName,
                                        path = tab.filePath,
                                        actionType = SyncActionType.CREATE_OR_UPDATE,
                                        content = tab.content
                                    )
                                    conflictRemoteContent = remoteContent
                                    conflictRemoteSha = remoteSha
                                    showConflictDialog = true
                                    syncStatus = EditorSyncStatus.ERROR
                                    syncErrorMessage = "Conflict on ${tab.fileName}"
                                }
                            } else {
                                syncQueueManager.enqueue(
                                    QueuedSyncAction(
                                        projectId = projectId,
                                        owner = currentProj.repoOwner,
                                        repo = currentProj.repoName,
                                        path = tab.filePath,
                                        actionType = SyncActionType.CREATE_OR_UPDATE,
                                        content = tab.content
                                    )
                                )
                                val count = syncQueueManager.getPendingCount(projectId)
                                withContext(Dispatchers.Main) {
                                    syncStatus = EditorSyncStatus.OFFLINE
                                    pendingQueueCount = count
                                }
                            }
                        }
                    } else {
                        draftStorage.clearDraft(projectId, tab.filePath)
                    }
                }
            }
        }
    }

    // Remote commit polling (every 45s)
    LaunchedEffect(projectId, project?.repoOwner, project?.repoName) {
        val currentProj = project ?: return@LaunchedEffect
        if (currentProj.repoOwner.isBlank() || currentProj.repoName.isBlank()) return@LaunchedEffect

        val initialSha = apiService.getLatestCommitSha(currentProj.repoOwner, currentProj.repoName).getOrNull()
        lastKnownRemoteSha = initialSha

        while (true) {
            delay(45000)
            try {
                val latestShaRes = apiService.getLatestCommitSha(currentProj.repoOwner, currentProj.repoName)
                if (latestShaRes.isSuccess) {
                    val latestSha = latestShaRes.getOrThrow()
                    if (lastKnownRemoteSha != null && lastKnownRemoteSha != latestSha) {
                        showRemoteChangesBanner = true
                    }
                    lastKnownRemoteSha = latestSha
                }
            } catch (_: Exception) {}
        }
    }

    // Initial project load and queue check
    LaunchedEffect(projectId) {
        val p = projectStorage.getProject(projectId)
        project = p
        refreshFileTree()

        val count = syncQueueManager.getPendingCount(projectId)
        pendingQueueCount = count
        if (count > 0) {
            syncStatus = EditorSyncStatus.OFFLINE
            // Attempt background queue drain
            coroutineScope.launch(Dispatchers.IO) {
                syncQueueManager.drainQueue(
                    projectId = projectId,
                    apiService = apiService,
                    onConflict = { action, remoteContent, remoteSha ->
                        withContext(Dispatchers.Main) {
                            conflictAction = action
                            conflictRemoteContent = remoteContent
                            conflictRemoteSha = remoteSha
                            showConflictDialog = true
                            syncStatus = EditorSyncStatus.ERROR
                            syncErrorMessage = "Sync conflict on ${action.path}"
                        }
                    }
                )
                val remaining = syncQueueManager.getPendingCount(projectId)
                withContext(Dispatchers.Main) {
                    pendingQueueCount = remaining
                    if (remaining == 0 && syncStatus == EditorSyncStatus.OFFLINE) {
                        syncStatus = EditorSyncStatus.SYNCED
                    }
                }
            }
        }

        // Open MainActivity by default if no tab is open
        coroutineScope.launch(Dispatchers.IO) {
            val mainActivity = projectDir.walkTopDown().firstOrNull { it.name == "MainActivity.kt" }
            if (mainActivity != null) {
                val rel = mainActivity.relativeTo(projectDir).path
                withContext(Dispatchers.Main) {
                    openFileInTab(rel)
                }
            }
        }
    }

    fun startBuild(cleanBuild: Boolean = false) {
        val currentProj = project ?: return
        if (currentProj.repoOwner.isBlank() || currentProj.repoName.isBlank()) {
            Toast.makeText(context, "Cannot build: Project is not synced to a GitHub repository", Toast.LENGTH_LONG).show()
            return
        }

        isBuilding = true
        showBuildLogPanel = true

        coroutineScope.launch {
            saveActiveFile()
            val dispatchRes = apiService.triggerWorkflow(
                owner = currentProj.repoOwner,
                repo = currentProj.repoName,
                workflowId = "build-apk.yml",
                cleanBuild = cleanBuild
            )

            if (dispatchRes.isFailure) {
                isBuilding = false
                Toast.makeText(context, "Failed to dispatch build: ${dispatchRes.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                return@launch
            }

            delay(4000)
            var attempts = 0
            var runId: Long = 0L

            while (attempts < 30) {
                val runsRes = apiService.listWorkflowRuns(currentProj.repoOwner, currentProj.repoName)
                val latestRun = runsRes.getOrNull()?.firstOrNull()
                if (latestRun != null) {
                    runId = latestRun.id
                    if (latestRun.status == "completed") {
                        val isSuccess = latestRun.conclusion == "success"
                        val logs = apiService.getWorkflowRunLogs(currentProj.repoOwner, currentProj.repoName, runId).getOrDefault("")

                        val parsedErrors = mutableListOf<BuildErrorLine>()
                        logs.lines().forEach { line ->
                            if (line.contains("e: ") || line.contains("error:")) {
                                val match = Regex("""([a-zA-Z0-9_.-]+\.kt):\((\d+),\s*(\d+)\):\s*(.+)""").find(line)
                                if (match != null) {
                                    val (fileName, lineNum, colNum, msg) = match.destructured
                                    parsedErrors.add(
                                        BuildErrorLine(
                                            filePath = fileName,
                                            fileName = fileName,
                                            lineNumber = lineNum.toIntOrNull() ?: 1,
                                            columnNumber = colNum.toIntOrNull() ?: 1,
                                            message = msg
                                        )
                                    )
                                }
                            }
                        }

                        val simpleError = if (!isSuccess) {
                            parsedErrors.firstOrNull()?.let { "${it.fileName}:${it.lineNumber} - ${it.message}" }
                                ?: "Compilation failed during Gradle assembly. Tap below for full logs."
                        } else null

                        val newBuild = BuildInfo(
                            id = runId,
                            projectId = projectId,
                            runNumber = latestRun.runNumber,
                            versionCode = currentProj.versionCode,
                            versionName = currentProj.versionName,
                            status = "completed",
                            conclusion = latestRun.conclusion,
                            rawLog = logs,
                            simpleErrorMessage = simpleError,
                            errors = parsedErrors
                        )

                        currentBuildInfo = newBuild
                        projectStorage.saveBuild(newBuild)
                        projectStorage.saveProject(currentProj.copy(lastBuildStatus = if (isSuccess) "SUCCESS" else "FAILURE"))
                        isBuilding = false

                        NotificationHelper.showBuildNotification(context, currentProj.name, isSuccess, runId)
                        break
                    }
                }
                delay(5000)
                attempts++
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkSurface,
                modifier = Modifier.width(300.dp)
            ) {
                ProjectExplorer(
                    projectName = project?.name ?: "Project",
                    nodes = fileTree,
                    activeFilePath = openTabs.getOrNull(activeTabIndex)?.filePath,
                    onFileClick = { relPath ->
                        openFileInTab(relPath)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onAddNewItem = { targetNode, isFolder ->
                        targetFolderForNewItem = if (targetNode != null && targetNode.isDirectory) {
                            File(projectDir, targetNode.path)
                        } else {
                            projectDir
                        }
                        isCreatingFolder = isFolder
                        showNewItemDialog = true
                    },
                    onRefresh = { refreshFileTree() },
                    onNodeAction = { node, action ->
                        selectedNodeForAction = node
                        when (action) {
                            "rename" -> {
                                actionInputText = node.name
                                showRenameDialog = true
                            }
                            "duplicate" -> {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val srcFile = File(projectDir, node.path)
                                    val dupFile = File(srcFile.parentFile, srcFile.nameWithoutExtension + "_copy." + srcFile.extension)
                                    if (srcFile.isDirectory) srcFile.copyRecursively(dupFile) else srcFile.copyTo(dupFile)
                                    refreshFileTree()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Duplicated to ${dupFile.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            "copy_path" -> {
                                clipboardManager.setText(AnnotatedString(node.path))
                                Toast.makeText(context, "Copied: ${node.path}", Toast.LENGTH_SHORT).show()
                            }
                            "details" -> {
                                showDetailsDialog = true
                            }
                            "delete" -> {
                                deleteFromGitHubAlso = true
                                showDeleteConfirmDialog = true
                            }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column(modifier = Modifier.padding(start = 2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = project?.name ?: "Editor",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Sync Status Chip
                                    SyncStatusChip(
                                        status = syncStatus,
                                        pendingCount = pendingQueueCount,
                                        onClick = {
                                            if (syncStatus == EditorSyncStatus.ERROR) {
                                                showSyncErrorDialog = true
                                            } else if (syncStatus == EditorSyncStatus.OFFLINE) {
                                                Toast.makeText(context, "$pendingQueueCount offline change(s) queued to sync.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }

                                // Single-line breadcrumb with tap-to-copy
                                val currentPath = openTabs.getOrNull(activeTabIndex)?.filePath
                                if (currentPath != null) {
                                    Text(
                                        text = formatBreadcrumb(currentPath),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DarkTextSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable {
                                            clipboardManager.setText(AnnotatedString(currentPath))
                                            Toast.makeText(context, "Copied path: $currentPath", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                                IconButton(onClick = { coroutineScope.launch { drawerState.open() } }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Menu, contentDescription = "Explorer")
                                }
                            }
                        },
                        actions = {
                            // Find / Replace
                            IconButton(onClick = { showSearchBar = !showSearchBar }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Find/Replace",
                                    tint = if (showSearchBar) NeonGreen else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Git Sync Dialog
                            IconButton(onClick = { showGitDialog = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Git Sync", tint = NeonGreen, modifier = Modifier.size(20.dp))
                            }
                            // Save Active File
                            val isDirty = openTabs.getOrNull(activeTabIndex)?.isDirty == true
                            IconButton(
                                onClick = { saveActiveFile() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = "Save",
                                    tint = if (isDirty) NeonGreen else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Run Build
                            IconButton(
                                onClick = { startBuild(cleanBuild = false) },
                                enabled = !isBuilding,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Build",
                                    tint = if (isBuilding) Color.Gray else NeonGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            // Overflow Menu
                            Box {
                                IconButton(onClick = { showOverflowMenu = true }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(20.dp))
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false },
                                    modifier = Modifier.background(DarkSurface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Project Dashboard", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.Dashboard, contentDescription = null, tint = NeonGreen) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showDashboardDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Build History", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = NeonGreen) },
                                        onClick = {
                                            showOverflowMenu = false
                                            onOpenBuildHistory(projectId)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(if (isWordWrapEnabled) "Disable Word Wrap" else "Enable Word Wrap", color = Color.White)
                                                if (isWordWrapEnabled) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            isWordWrapEnabled = !isWordWrapEnabled
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Font Size: ${fontSizeSp}sp (+)", color = Color.White) },
                                        onClick = {
                                            fontSizeSp = if (fontSizeSp >= 20) 11 else fontSizeSp + 2
                                        }
                                    )
                                    if (pendingQueueCount > 0) {
                                        DropdownMenuItem(
                                            text = { Text("Retry Sync Queue ($pendingQueueCount)", color = OrangeWarning) },
                                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = OrangeWarning) },
                                            onClick = {
                                                showOverflowMenu = false
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    syncQueueManager.drainQueue(
                                                        projectId = projectId,
                                                        apiService = apiService,
                                                        onConflict = { action, remoteContent, remoteSha ->
                                                            withContext(Dispatchers.Main) {
                                                                conflictAction = action
                                                                conflictRemoteContent = remoteContent
                                                                conflictRemoteSha = remoteSha
                                                                showConflictDialog = true
                                                                syncStatus = EditorSyncStatus.ERROR
                                                            }
                                                        }
                                                    )
                                                    val rem = syncQueueManager.getPendingCount(projectId)
                                                    withContext(Dispatchers.Main) {
                                                        pendingQueueCount = rem
                                                        if (rem == 0) syncStatus = EditorSyncStatus.SYNCED
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                    )

                    // Remote Changes Notification Banner
                    AnimatedVisibility(visible = showRemoteChangesBanner) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceHighlight)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New remote commits on GitHub", fontSize = 12.sp, color = Color.White)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        showRemoteChangesBanner = false
                                        showGitDialog = true
                                    },
                                    modifier = Modifier.height(28.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                                ) {
                                    Text("Pull", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { showRemoteChangesBanner = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = DarkTextSecondary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column {
                    if (openTabs.isNotEmpty()) {
                        CodingAccessoryBar(
                            onInsertSymbol = { symbol ->
                                val activeTab = openTabs.getOrNull(activeTabIndex)
                                if (activeTab != null) {
                                    val current = activeTab.content
                                    val pos = activeTab.cursorPosition.coerceIn(0, current.length)
                                    val newContent = current.substring(0, pos) + symbol + current.substring(pos)
                                    activeTab.updateContent(newContent)
                                    activeTab.cursorPosition = pos + symbol.length
                                    coroutineScope.launch {
                                        draftStorage.saveDraft(projectId, activeTab.filePath, newContent)
                                    }
                                }
                            }
                        )
                    }

                    if (showBuildLogPanel) {
                        BuildLogPanel(
                            buildInfo = currentBuildInfo,
                            isBuilding = isBuilding,
                            onClose = { showBuildLogPanel = false },
                            onRetry = { startBuild(cleanBuild = false) },
                            onCleanBuild = { startBuild(cleanBuild = true) },
                            onErrorLineClick = { filePath, lineNum ->
                                openFileInTab(filePath, lineNum)
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DarkBackground)
            ) {
                // Tab Row
                if (openTabs.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = activeTabIndex.coerceIn(0, openTabs.lastIndex),
                        containerColor = DarkSurface,
                        contentColor = NeonGreen,
                        edgePadding = 0.dp
                    ) {
                        openTabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = activeTabIndex == index,
                                onClick = { activeTabIndex = index },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tab.fileName,
                                            fontWeight = if (activeTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                            color = if (activeTabIndex == index) NeonGreen else DarkTextSecondary
                                        )
                                        if (tab.isDirty) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .background(NeonGreen, CircleShape)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close",
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    openTabs.removeAt(index)
                                                    if (activeTabIndex >= openTabs.size) {
                                                        activeTabIndex = openTabs.lastIndex.coerceAtLeast(0)
                                                    }
                                                }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // In-file Find & Replace Bar
                if (showSearchBar && openTabs.isNotEmpty()) {
                    val currentTab = openTabs[activeTabIndex]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceVariant)
                            .padding(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Find in file", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(46.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedTextField(
                                value = replaceQuery,
                                onValueChange = { replaceQuery = it },
                                placeholder = { Text("Replace with", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(46.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = {
                                    if (searchQuery.isNotEmpty()) {
                                        val idx = currentTab.content.indexOf(searchQuery)
                                        if (idx >= 0) {
                                            val newContent = currentTab.content.replaceFirst(searchQuery, replaceQuery)
                                            currentTab.updateContent(newContent)
                                            coroutineScope.launch {
                                                draftStorage.saveDraft(projectId, currentTab.filePath, newContent)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Replace", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { showReplaceAllDialog = true },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Replace All", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Editor Controls Bar
                if (openTabs.isNotEmpty()) {
                    val activeTab = openTabs[activeTabIndex]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { activeTab.undo() }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { activeTab.redo() }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Redo, contentDescription = "Redo", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                val formatted = activeTab.content.lines().joinToString("\n") { it.trimEnd() }
                                activeTab.updateContent(formatted)
                                coroutineScope.launch {
                                    draftStorage.saveDraft(projectId, activeTab.filePath, formatted)
                                }
                                Toast.makeText(context, "Code Formatted", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.FormatAlignLeft, contentDescription = "Format", modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = activeTab.filePath,
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Main Code Area with Fixed Line Numbers Column
                if (openTabs.isNotEmpty() && activeTabIndex in openTabs.indices) {
                    val currentTab = openTabs[activeTabIndex]
                    val lines = currentTab.content.lines()
                    val extension = currentTab.fileName.substringAfterLast('.', "")
                    val highlighter = remember(extension) { SyntaxHighlighter(extension) }

                    val verticalScrollState = rememberScrollState()
                    val horizontalScrollState = rememberScrollState()

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScrollState)
                    ) {
                        // Fixed line numbers column on left
                        Column(
                            modifier = Modifier
                                .background(DarkSurface)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            for (i in 1..lines.size.coerceAtLeast(1)) {
                                Text(
                                    text = "$i",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = fontSizeSp.sp,
                                    color = DarkTextSecondary,
                                    lineHeight = (fontSizeSp + 6).sp
                                )
                            }
                        }

                        // Code Editor BasicTextField Box
                        val codeBoxModifier = if (isWordWrapEnabled) {
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        } else {
                            Modifier
                                .fillMaxSize()
                                .horizontalScroll(horizontalScrollState)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        }

                        Box(modifier = codeBoxModifier) {
                            var textFieldValue by remember(currentTab.filePath) {
                                mutableStateOf(TextFieldValue(currentTab.content, TextRange(currentTab.cursorPosition.coerceIn(0, currentTab.content.length))))
                            }

                            if (textFieldValue.text != currentTab.content) {
                                textFieldValue = textFieldValue.copy(
                                    text = currentTab.content,
                                    selection = TextRange(currentTab.cursorPosition.coerceIn(0, currentTab.content.length))
                                )
                            }

                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = { newVal ->
                                    val oldText = currentTab.content
                                    val newText = newVal.text
                                    currentTab.cursorPosition = newVal.selection.start

                                    // Auto-indent on newline
                                    if (newText.length > oldText.length && newText.endsWith("\n")) {
                                        val lastLine = oldText.lines().lastOrNull().orEmpty()
                                        val leadingSpaces = " ".repeat(lastLine.takeWhile { it == ' ' }.length)
                                        val extraIndent = if (lastLine.trimEnd().endsWith("{")) "    " else ""
                                        val indented = newText + leadingSpaces + extraIndent
                                        currentTab.updateContent(indented)
                                        textFieldValue = newVal.copy(text = indented, selection = TextRange(indented.length))
                                        coroutineScope.launch {
                                            draftStorage.saveDraft(projectId, currentTab.filePath, indented)
                                        }
                                    } else {
                                        currentTab.updateContent(newText)
                                        textFieldValue = newVal
                                        coroutineScope.launch {
                                            draftStorage.saveDraft(projectId, currentTab.filePath, newText)
                                        }
                                    }
                                },
                                visualTransformation = highlighter,
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = fontSizeSp.sp,
                                    lineHeight = (fontSizeSp + 6).sp,
                                    color = DarkTextPrimary
                                ),
                                cursorBrush = SolidColor(NeonGreen),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Open a file from the explorer drawer to edit code", color = DarkTextSecondary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                            ) {
                                Text("Open Explorer")
                            }
                        }
                    }
                }
            }
        }
    }

    // New Item Creation Dialog
    if (showNewItemDialog) {
        NewItemCreationDialog(
            isFolder = isCreatingFolder,
            targetDirectory = targetFolderForNewItem,
            projectRoot = projectDir,
            onDismiss = { showNewItemDialog = false },
            onItemCreated = { newFile ->
                showNewItemDialog = false
                refreshFileTree()
                if (!newFile.isDirectory) {
                    val rel = newFile.relativeTo(projectDir).path
                    openFileInTab(rel)

                    // Also push or queue new file
                    val currentProj = project
                    if (currentProj != null && currentProj.repoOwner.isNotBlank() && currentProj.repoName.isNotBlank()) {
                        coroutineScope.launch(Dispatchers.IO) {
                            withContext(Dispatchers.Main) { syncStatus = EditorSyncStatus.SYNCING }
                            val res = apiService.createOrUpdateFile(
                                owner = currentProj.repoOwner,
                                repo = currentProj.repoName,
                                path = rel,
                                contentString = newFile.readText(),
                                message = "Create $rel from Kotlin IDE"
                            )
                            if (res.isSuccess) {
                                withContext(Dispatchers.Main) { syncStatus = EditorSyncStatus.SYNCED }
                            } else {
                                syncQueueManager.enqueue(
                                    QueuedSyncAction(
                                        projectId = projectId,
                                        owner = currentProj.repoOwner,
                                        repo = currentProj.repoName,
                                        path = rel,
                                        actionType = SyncActionType.CREATE_OR_UPDATE,
                                        content = newFile.readText()
                                    )
                                )
                                val count = syncQueueManager.getPendingCount(projectId)
                                withContext(Dispatchers.Main) {
                                    syncStatus = EditorSyncStatus.OFFLINE
                                    pendingQueueCount = count
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // Git Workflow Dialog
    if (showGitDialog && project != null) {
        GitHubGitDialog(
            project = project!!,
            projectRoot = projectDir,
            apiService = apiService,
            onDismiss = { showGitDialog = false },
            onPullComplete = {
                refreshFileTree()
                openTabs.forEach { tab ->
                    val f = File(projectDir, tab.filePath)
                    if (f.exists()) {
                        tab.content = f.readText()
                        tab.savedContent = tab.content
                    }
                }
                syncStatus = EditorSyncStatus.SYNCED
            }
        )
    }

    // Project Dashboard Dialog
    if (showDashboardDialog && project != null) {
        ProjectDashboardDialog(
            project = project!!,
            projectStorage = projectStorage,
            onDismiss = { showDashboardDialog = false },
            onTriggerBuild = { startBuild(cleanBuild = false) }
        )
    }

    // Conflict Resolution Dialog (HTTP 409/422)
    if (showConflictDialog && conflictAction != null) {
        val action = conflictAction!!
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = OrangeWarning) },
            title = { Text("Conflict on ${action.path}") },
            text = {
                Column {
                    Text(
                        text = "The file '${action.path}' was modified on GitHub since your last sync.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Select an option to resolve:",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkTextSecondary
                    )
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val latestSha = apiService.getFileDetails(action.owner, action.repo, action.path).getOrNull()?.sha
                                val res = apiService.createOrUpdateFile(
                                    owner = action.owner,
                                    repo = action.repo,
                                    path = action.path,
                                    contentString = action.content.orEmpty(),
                                    message = "Force update ${action.path} (resolved conflict)",
                                    sha = latestSha
                                )
                                if (res.isSuccess) {
                                    syncQueueManager.removeAction(action.id)
                                    val count = syncQueueManager.getPendingCount(projectId)
                                    withContext(Dispatchers.Main) {
                                        showConflictDialog = false
                                        syncStatus = EditorSyncStatus.SYNCED
                                        pendingQueueCount = count
                                        Toast.makeText(context, "Resolved: Kept your version", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                    ) {
                        Text("Keep Mine (Overwrite GitHub)")
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val file = File(projectDir, action.path)
                                file.writeText(conflictRemoteContent)
                                syncQueueManager.removeAction(action.id)
                                draftStorage.clearDraft(projectId, action.path)
                                val count = syncQueueManager.getPendingCount(projectId)
                                withContext(Dispatchers.Main) {
                                    val tabIndex = openTabs.indexOfFirst { it.filePath == action.path }
                                    if (tabIndex >= 0) {
                                        openTabs[tabIndex].content = conflictRemoteContent
                                        openTabs[tabIndex].markSaved()
                                    }
                                    showConflictDialog = false
                                    syncStatus = EditorSyncStatus.SYNCED
                                    pendingQueueCount = count
                                    Toast.makeText(context, "Resolved: Kept GitHub version", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight, contentColor = Color.White)
                    ) {
                        Text("Keep GitHub's (Overwrite Mine)")
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val merged = buildString {
                                    append("<<<<<<< LOCAL (Your phone)\n")
                                    append(action.content.orEmpty())
                                    append("\n=======\n")
                                    append(conflictRemoteContent)
                                    append("\n>>>>>>> GITHUB (Remote)\n")
                                }
                                val file = File(projectDir, action.path)
                                file.writeText(merged)
                                syncQueueManager.removeAction(action.id)
                                val count = syncQueueManager.getPendingCount(projectId)
                                withContext(Dispatchers.Main) {
                                    val tabIndex = openTabs.indexOfFirst { it.filePath == action.path }
                                    if (tabIndex >= 0) {
                                        openTabs[tabIndex].updateContent(merged)
                                    } else {
                                        openFileInTab(action.path)
                                    }
                                    showConflictDialog = false
                                    syncStatus = EditorSyncStatus.SYNCED
                                    pendingQueueCount = count
                                    Toast.makeText(context, "Merged with conflict markers in editor", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight, contentColor = NeonGreen)
                    ) {
                        Text("Show Both / Merge")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showConflictDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sync Error Dialog
    if (showSyncErrorDialog) {
        AlertDialog(
            onDismissRequest = { showSyncErrorDialog = false },
            icon = { Icon(Icons.Default.Error, contentDescription = null, tint = RedError) },
            title = { Text("Sync Status Details") },
            text = {
                Column {
                    Text(syncErrorMessage ?: "An error occurred while communicating with GitHub.", color = DarkTextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Offline changes will be retained safely on device and automatically retried when network recovers.", style = MaterialTheme.typography.bodySmall, color = DarkTextSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showSyncErrorDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog && selectedNodeForAction != null) {
        val node = selectedNodeForAction!!
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename ${node.name}") },
            text = {
                OutlinedTextField(
                    value = actionInputText,
                    onValueChange = { actionInputText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newName = actionInputText.trim()
                        if (newName.isNotBlank() && newName != node.name) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val src = File(projectDir, node.path)
                                val dst = File(src.parentFile, newName)
                                if (src.renameTo(dst)) {
                                    val relDst = dst.relativeTo(projectDir).path
                                    // Update tabs
                                    withContext(Dispatchers.Main) {
                                        val tabIndex = openTabs.indexOfFirst { it.filePath == node.path }
                                        if (tabIndex >= 0) {
                                            openTabs[tabIndex] = openTabs[tabIndex].copy(filePath = relDst, fileName = dst.name)
                                        }
                                        refreshFileTree()
                                        showRenameDialog = false
                                        Toast.makeText(context, "Renamed to ${dst.name}", Toast.LENGTH_SHORT).show()
                                    }

                                    // Queue rename to GitHub
                                    val currentProj = project
                                    if (currentProj != null && currentProj.repoOwner.isNotBlank() && !node.isDirectory) {
                                        syncQueueManager.enqueue(
                                            QueuedSyncAction(
                                                projectId = projectId,
                                                owner = currentProj.repoOwner,
                                                repo = currentProj.repoName,
                                                path = relDst,
                                                oldPath = node.path,
                                                actionType = SyncActionType.RENAME,
                                                content = dst.readText()
                                            )
                                        )
                                        val count = syncQueueManager.getPendingCount(projectId)
                                        withContext(Dispatchers.Main) {
                                            pendingQueueCount = count
                                        }
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && selectedNodeForAction != null) {
        val node = selectedNodeForAction!!
        val isRepoConnected = project?.repoOwner.orEmpty().isNotBlank()

        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete ${node.name}?") },
            text = {
                Column {
                    Text("Are you sure you want to delete '${node.name}'? This action cannot be undone.")
                    if (isRepoConnected) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteFromGitHubAlso = !deleteFromGitHubAlso }
                        ) {
                            Checkbox(
                                checked = deleteFromGitHubAlso,
                                onCheckedChange = { deleteFromGitHubAlso = it },
                                colors = CheckboxDefaults.colors(checkedColor = NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Also delete from GitHub repository", fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val target = File(projectDir, node.path)
                            target.deleteRecursively()
                            draftStorage.clearDraft(projectId, node.path)

                            withContext(Dispatchers.Main) {
                                openTabs.removeAll { it.filePath.startsWith(node.path) }
                                refreshFileTree()
                                showDeleteConfirmDialog = false
                                Toast.makeText(context, "Deleted ${node.name}", Toast.LENGTH_SHORT).show()
                            }

                            // GitHub delete
                            val currentProj = project
                            if (deleteFromGitHubAlso && currentProj != null && currentProj.repoOwner.isNotBlank() && !node.isDirectory) {
                                syncQueueManager.enqueue(
                                    QueuedSyncAction(
                                        projectId = projectId,
                                        owner = currentProj.repoOwner,
                                        repo = currentProj.repoName,
                                        path = node.path,
                                        actionType = SyncActionType.DELETE
                                    )
                                )
                                val count = syncQueueManager.getPendingCount(projectId)
                                withContext(Dispatchers.Main) {
                                    pendingQueueCount = count
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError, contentColor = Color.White)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    // File Details Dialog
    if (showDetailsDialog && selectedNodeForAction != null) {
        val node = selectedNodeForAction!!
        val file = File(projectDir, node.path)
        val sizeStr = if (file.isDirectory) "${file.listFiles()?.size ?: 0} items" else "${file.length() / 1024} KB"
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text(node.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Relative Path: ${node.path}", style = MaterialTheme.typography.bodySmall)
                    Text("• Type: ${if (node.isDirectory) "Directory" else "File"}", style = MaterialTheme.typography.bodySmall)
                    Text("• Size: $sizeStr", style = MaterialTheme.typography.bodySmall)
                    Text("• Last Modified: ${java.util.Date(file.lastModified())}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) { Text("Close") }
            }
        )
    }

    // Replace All Confirmation Dialog
    if (showReplaceAllDialog && openTabs.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showReplaceAllDialog = false },
            title = { Text("Confirm Replace All") },
            text = { Text("Replace all occurrences of '$searchQuery' with '$replaceQuery'?") },
            confirmButton = {
                Button(
                    onClick = {
                        val activeTab = openTabs[activeTabIndex]
                        val replaced = activeTab.content.replace(searchQuery, replaceQuery)
                        activeTab.updateContent(replaced)
                        coroutineScope.launch {
                            draftStorage.saveDraft(projectId, activeTab.filePath, replaced)
                        }
                        showReplaceAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                ) { Text("Replace All") }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceAllDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SyncStatusChip(
    status: EditorSyncStatus,
    pendingCount: Int,
    onClick: () -> Unit
) {
    val (bgColor, textColor, icon) = when (status) {
        EditorSyncStatus.SYNCED -> Triple(NeonGreen.copy(alpha = 0.15f), NeonGreen, Icons.Default.CheckCircle)
        EditorSyncStatus.SYNCING -> Triple(OrangeWarning.copy(alpha = 0.15f), OrangeWarning, Icons.Default.Sync)
        EditorSyncStatus.OFFLINE -> Triple(Color.Gray.copy(alpha = 0.2f), Color(0xFFC0C0C0), Icons.Default.CloudOff)
        EditorSyncStatus.ERROR -> Triple(RedError.copy(alpha = 0.15f), RedError, Icons.Default.Error)
    }

    val label = when (status) {
        EditorSyncStatus.SYNCED -> "Synced"
        EditorSyncStatus.SYNCING -> "Syncing..."
        EditorSyncStatus.OFFLINE -> if (pendingCount > 0) "Offline ($pendingCount)" else "Offline"
        EditorSyncStatus.ERROR -> "Error"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(11.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = label, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

fun formatBreadcrumb(path: String): String {
    val parts = path.split("/")
    return when {
        parts.size <= 2 -> path
        else -> "${parts.first()}/.../${parts.last()}"
    }
}
