package com.kotlinide.shahid.app.ui.screens.editor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlinide.shahid.app.data.api.GitHubApiService
import com.kotlinide.shahid.app.data.model.BuildErrorLine
import com.kotlinide.shahid.app.data.model.BuildInfo
import com.kotlinide.shahid.app.data.model.EditorTab
import com.kotlinide.shahid.app.data.model.FileNode
import com.kotlinide.shahid.app.data.model.Project
import com.kotlinide.shahid.app.data.security.TokenStorage
import com.kotlinide.shahid.app.data.storage.ProjectStorage
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
import com.kotlinide.shahid.app.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: String,
    projectStorage: ProjectStorage,
    tokenStorage: TokenStorage,
    onBack: () -> Unit,
    onOpenBuildHistory: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val apiService = remember { GitHubApiService(tokenStorage) }

    var project by remember { mutableStateOf<Project?>(null) }
    val openTabs = remember { mutableStateListOf<EditorTab>() }
    var activeTabIndex by remember { mutableIntStateOf(0) }

    var fileTree by remember { mutableStateOf<List<FileNode>>(emptyList()) }

    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var showReplaceAllDialog by remember { mutableStateOf(false) }

    var selectedNodeForAction by remember { mutableStateOf<FileNode?>(null) }
    var showFileActionDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var actionInputText by remember { mutableStateOf("") }

    var isBuilding by remember { mutableStateOf(false) }
    var showBuildLogPanel by remember { mutableStateOf(false) }
    var currentBuildInfo by remember { mutableStateOf<BuildInfo?>(null) }

    fun refreshFileTree() {
        val projectDir = projectStorage.getProjectDirectory(projectId)
        fun buildTree(dir: File, relPrefix: String): List<FileNode> {
            val list = mutableListOf<FileNode>()
            val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: return list
            for (f in files) {
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
        fileTree = buildTree(projectDir, "")
    }

    fun openFileInTab(relPath: String, targetLine: Int = -1) {
        val existingIndex = openTabs.indexOfFirst { it.filePath == relPath }
        if (existingIndex >= 0) {
            activeTabIndex = existingIndex
        } else {
            val projectDir = projectStorage.getProjectDirectory(projectId)
            val file = File(projectDir, relPath)
            if (file.exists() && !file.isDirectory) {
                val content = file.readText()
                val newTab = EditorTab(
                    filePath = relPath,
                    fileName = file.name,
                    content = content,
                    savedContent = content
                )
                if (openTabs.size >= 6) {
                    openTabs.removeAt(0)
                }
                openTabs.add(newTab)
                activeTabIndex = openTabs.lastIndex
            }
        }
    }

    fun saveActiveFile() {
        if (openTabs.isEmpty() || activeTabIndex !in openTabs.indices) return
        val tab = openTabs[activeTabIndex]
        val projectDir = projectStorage.getProjectDirectory(projectId)
        val file = File(projectDir, tab.filePath)
        file.parentFile?.mkdirs()
        file.writeText(tab.content)
        tab.markSaved()

        val currentProj = project
        if (currentProj != null && currentProj.repoOwner.isNotEmpty()) {
            coroutineScope.launch {
                apiService.createOrUpdateFile(
                    owner = currentProj.repoOwner,
                    repo = currentProj.repoName,
                    path = tab.filePath,
                    contentString = tab.content,
                    message = "Update ${tab.fileName} from Kotlin IDE"
                )
            }
        }
        Toast.makeText(context, "Saved ${tab.fileName}", Toast.LENGTH_SHORT).show()
    }

    fun startBuild(cleanBuild: Boolean = false) {
        val currentProj = project ?: return
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

    LaunchedEffect(projectId) {
        val p = projectStorage.getProject(projectId)
        project = p
        refreshFileTree()
        val projectDir = projectStorage.getProjectDirectory(projectId)
        val mainActivity = projectDir.walkTopDown().firstOrNull { it.name == "MainActivity.kt" }
        if (mainActivity != null) {
            val rel = mainActivity.relativeTo(projectDir).path
            openFileInTab(rel)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkSurface,
                modifier = Modifier.width(280.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Text(
                        text = "Project Files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyFileTree(
                        nodes = fileTree,
                        onFileClick = { relPath ->
                            openFileInTab(relPath)
                            coroutineScope.launch { drawerState.close() }
                        },
                        onNodeLongClick = { node ->
                            selectedNodeForAction = node
                            showFileActionDialog = true
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(project?.name ?: "Editor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(project?.repoName.orEmpty(), style = MaterialTheme.typography.labelSmall, color = DarkTextSecondary)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "File Tree")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearchBar = !showSearchBar }) {
                            Icon(Icons.Default.Search, contentDescription = "Find/Replace")
                        }
                        IconButton(onClick = { saveActiveFile() }) {
                            Icon(Icons.Default.Save, contentDescription = "Save File")
                        }
                        IconButton(onClick = { startBuild(cleanBuild = false) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Build", tint = NeonGreen)
                        }
                        IconButton(onClick = { onOpenBuildHistory(projectId) }) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )
            },
            bottomBar = {
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
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DarkBackground)
            ) {
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
                                            contentDescription = "Close Tab",
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
                                placeholder = { Text("Find") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(48.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedTextField(
                                value = replaceQuery,
                                onValueChange = { replaceQuery = it },
                                placeholder = { Text("Replace") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(48.dp)
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
                                        }
                                    }
                                },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Replace")
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { showReplaceAllDialog = true },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Replace All")
                            }
                        }
                    }
                }

                if (openTabs.isNotEmpty()) {
                    val activeTab = openTabs[activeTabIndex]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { activeTab.undo() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { activeTab.redo() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Redo, contentDescription = "Redo", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                val formatted = activeTab.content.lines().joinToString("\n") { it.trimEnd() }
                                activeTab.updateContent(formatted)
                                Toast.makeText(context, "Code Formatted", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.FormatAlignLeft, contentDescription = "Format", modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = activeTab.filePath,
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkTextSecondary
                        )
                    }
                }

                if (openTabs.isNotEmpty() && activeTabIndex in openTabs.indices) {
                    val currentTab = openTabs[activeTabIndex]
                    val lines = currentTab.content.lines()
                    val extension = currentTab.fileName.substringAfterLast('.', "")
                    val highlighter = remember(extension) { SyntaxHighlighter(extension) }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Column(
                            modifier = Modifier
                                .background(DarkSurface)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            for (i in 1..lines.size) {
                                Text(
                                    text = "$i",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = DarkTextSecondary,
                                    lineHeight = 19.sp
                                )
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                            BasicTextField(
                                value = currentTab.content,
                                onValueChange = { newText ->
                                    if (newText.length > currentTab.content.length && newText.endsWith("\n")) {
                                        val lastLine = currentTab.content.lines().lastOrNull().orEmpty()
                                        val leadingSpaces = " ".repeat(lastLine.takeWhile { it == ' ' }.length)
                                        val extraIndent = if (lastLine.trimEnd().endsWith("{")) "    " else ""
                                        currentTab.updateContent(newText + leadingSpaces + extraIndent)
                                    } else {
                                        currentTab.updateContent(newText)
                                    }
                                },
                                visualTransformation = highlighter,
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = DarkTextPrimary
                                ),
                                cursorBrush = SolidColor(NeonGreen),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a file from the left drawer to edit", color = DarkTextSecondary)
                    }
                }
            }
        }

        if (showReplaceAllDialog && openTabs.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showReplaceAllDialog = false },
                title = { Text("Confirm Replace All") },
                text = { Text("Are you sure you want to replace all occurrences of '$searchQuery' with '$replaceQuery'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val activeTab = openTabs[activeTabIndex]
                            activeTab.updateContent(activeTab.content.replace(searchQuery, replaceQuery))
                            showReplaceAllDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                    ) {
                        Text("Replace All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReplaceAllDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showFileActionDialog && selectedNodeForAction != null) {
            val node = selectedNodeForAction!!
            AlertDialog(
                onDismissRequest = { showFileActionDialog = false },
                title = { Text(node.name) },
                text = {
                    Column {
                        TextButton(
                            onClick = {
                                showFileActionDialog = false
                                actionInputText = node.name
                                showRenameDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Rename")
                        }
                        if (node.isDirectory) {
                            TextButton(
                                onClick = {
                                    showFileActionDialog = false
                                    actionInputText = ""
                                    showNewFileDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("New File")
                            }
                            TextButton(
                                onClick = {
                                    showFileActionDialog = false
                                    actionInputText = ""
                                    showNewFolderDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("New Folder")
                            }
                        }
                        TextButton(
                            onClick = {
                                showFileActionDialog = false
                                val projectDir = projectStorage.getProjectDirectory(projectId)
                                val target = File(projectDir, node.path)
                                target.deleteRecursively()
                                openTabs.removeAll { it.filePath.startsWith(node.path) }
                                refreshFileTree()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete", color = RedError)
                        }
                    }
                },
                confirmButton = {}
            )
        }

        if (showRenameDialog && selectedNodeForAction != null) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename") },
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
                            val node = selectedNodeForAction!!
                            val projectDir = projectStorage.getProjectDirectory(projectId)
                            val oldFile = File(projectDir, node.path)
                            val newFile = File(oldFile.parentFile, actionInputText.trim())
                            oldFile.renameTo(newFile)
                            showRenameDialog = false
                            refreshFileTree()
                        }
                    ) { Text("Rename") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showNewFileDialog && selectedNodeForAction != null) {
            AlertDialog(
                onDismissRequest = { showNewFileDialog = false },
                title = { Text("New File") },
                text = {
                    OutlinedTextField(
                        value = actionInputText,
                        onValueChange = { actionInputText = it },
                        placeholder = { Text("e.g. MyComponent.kt") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val node = selectedNodeForAction!!
                            val projectDir = projectStorage.getProjectDirectory(projectId)
                            val parentDir = File(projectDir, node.path)
                            val newFile = File(parentDir, actionInputText.trim())
                            newFile.createNewFile()
                            showNewFileDialog = false
                            refreshFileTree()
                        }
                    ) { Text("Create File") }
                },
                dismissButton = {
                    TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showNewFolderDialog && selectedNodeForAction != null) {
            AlertDialog(
                onDismissRequest = { showNewFolderDialog = false },
                title = { Text("New Folder") },
                text = {
                    OutlinedTextField(
                        value = actionInputText,
                        onValueChange = { actionInputText = it },
                        placeholder = { Text("e.g. components") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val node = selectedNodeForAction!!
                            val projectDir = projectStorage.getProjectDirectory(projectId)
                            val parentDir = File(projectDir, node.path)
                            val newFolder = File(parentDir, actionInputText.trim())
                            newFolder.mkdirs()
                            showNewFolderDialog = false
                            refreshFileTree()
                        }
                    ) { Text("Create Folder") }
                },
                dismissButton = {
                    TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun LazyFileTree(
    nodes: List<FileNode>,
    onFileClick: (String) -> Unit,
    onNodeLongClick: (FileNode) -> Unit,
    level: Int = 0
) {
    Column {
        nodes.forEach { node ->
            var isExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (node.isDirectory) {
                            isExpanded = !isExpanded
                        } else {
                            onFileClick(node.path)
                        }
                    }
                    .padding(start = (level * 14).dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (node.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (node.isDirectory) OrangeWarning else if (node.name.endsWith(".kt")) NeonGreen else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkTextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            if (node.isDirectory && isExpanded) {
                LazyFileTree(
                    nodes = node.children,
                    onFileClick = onFileClick,
                    onNodeLongClick = onNodeLongClick,
                    level = level + 1
                )
            }
        }
    }
}
