package com.kotlinide.shahid.app.ui.screens.editor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val apiService = remember { GitHubApiService(tokenStorage) }

    var project by remember { mutableStateOf<Project?>(null) }
    val openTabs = remember { mutableStateListOf<EditorTab>() }
    var activeTabIndex by remember { mutableIntStateOf(0) }

    var fileTree by remember { mutableStateOf<List<FileNode>>(emptyList()) }
    val projectDir = remember(projectId) { projectStorage.getProjectDirectory(projectId) }

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
    var targetFolderForNewItem by remember { mutableStateOf(projectDir) }

    var selectedNodeForAction by remember { mutableStateOf<FileNode?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
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
        fun buildTree(dir: File, relPrefix: String): List<FileNode> {
            val list = mutableListOf<FileNode>()
            val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: return list
            for (f in files) {
                if (f.name == ".git" || f.name == "build" || f.name == ".gradle") continue
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

    // Auto-save debounce effect
    LaunchedEffect(openTabs.map { it.content }) {
        delay(2500)
        openTabs.forEach { tab ->
            if (tab.isDirty) {
                val file = File(projectDir, tab.filePath)
                file.parentFile?.mkdirs()
                file.writeText(tab.content)
                tab.markSaved()
            }
        }
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
                    onAddNewItem = { targetNode ->
                        targetFolderForNewItem = if (targetNode != null && targetNode.isDirectory) {
                            File(projectDir, targetNode.path)
                        } else {
                            projectDir
                        }
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
                                val srcFile = File(projectDir, node.path)
                                val dupFile = File(srcFile.parentFile, srcFile.nameWithoutExtension + "_copy." + srcFile.extension)
                                if (srcFile.isDirectory) srcFile.copyRecursively(dupFile) else srcFile.copyTo(dupFile)
                                refreshFileTree()
                                Toast.makeText(context, "Duplicated to ${dupFile.name}", Toast.LENGTH_SHORT).show()
                            }
                            "copy_path" -> {
                                clipboardManager.setText(AnnotatedString(node.path))
                                Toast.makeText(context, "Copied: ${node.path}", Toast.LENGTH_SHORT).show()
                            }
                            "details" -> {
                                showDetailsDialog = true
                            }
                            "delete" -> {
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
                TopAppBar(
                    title = {
                        Column {
                            Text(project?.name ?: "Editor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val currentPath = openTabs.getOrNull(activeTabIndex)?.filePath
                            Text(currentPath ?: project?.repoName.orEmpty(), style = MaterialTheme.typography.labelSmall, color = DarkTextSecondary)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Explorer")
                        }
                    },
                    actions = {
                        // Search / Replace
                        IconButton(onClick = { showSearchBar = !showSearchBar }) {
                            Icon(Icons.Default.Search, contentDescription = "Find/Replace", tint = if (showSearchBar) NeonGreen else Color.White)
                        }
                        // Git Push / Sync
                        IconButton(onClick = { showGitDialog = true }) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Git Sync", tint = NeonGreen)
                        }
                        // Save Active File
                        IconButton(onClick = { saveActiveFile() }) {
                            Icon(Icons.Default.Save, contentDescription = "Save File")
                        }
                        // Run Build
                        IconButton(onClick = { startBuild(cleanBuild = false) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Build", tint = NeonGreen)
                        }
                        // Overflow Menu
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
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
                                    text = { Text(if (isWordWrapEnabled) "Disable Word Wrap" else "Enable Word Wrap", color = Color.White) },
                                    onClick = {
                                        showOverflowMenu = false
                                        isWordWrapEnabled = !isWordWrapEnabled
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Font Size: ${fontSizeSp}sp (Tap to +)", color = Color.White) },
                                    onClick = {
                                        fontSizeSp = if (fontSizeSp >= 20) 11 else fontSizeSp + 2
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )
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
                                placeholder = { Text("Find in file") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(46.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedTextField(
                                value = replaceQuery,
                                onValueChange = { replaceQuery = it },
                                placeholder = { Text("Replace") },
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
                                        }
                                    }
                                },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Replace")
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { showReplaceAllDialog = true },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Replace All")
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
                            color = DarkTextSecondary
                        )
                    }
                }

                // Main Code Area
                if (openTabs.isNotEmpty() && activeTabIndex in openTabs.indices) {
                    val currentTab = openTabs[activeTabIndex]
                    val lines = currentTab.content.lines()
                    val extension = currentTab.fileName.substringAfterLast('.', "")
                    val highlighter = remember(extension) { SyntaxHighlighter(extension) }

                    val scrollModifier = if (isWordWrapEnabled) {
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    } else {
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())
                    }

                    Row(modifier = scrollModifier) {
                        // Line numbers column
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

                        // Code Editor BasicTextField
                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                            var textFieldValue by remember(currentTab.filePath) {
                                mutableStateOf(TextFieldValue(currentTab.content, TextRange(currentTab.cursorPosition)))
                            }

                            // Keep tab content synchronized
                            if (textFieldValue.text != currentTab.content) {
                                textFieldValue = textFieldValue.copy(text = currentTab.content)
                            }

                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = { newVal ->
                                    val oldText = currentTab.content
                                    val newText = newVal.text
                                    currentTab.cursorPosition = newVal.selection.start

                                    // Auto-indent & auto-close brackets
                                    if (newText.length > oldText.length && newText.endsWith("\n")) {
                                        val lastLine = oldText.lines().lastOrNull().orEmpty()
                                        val leadingSpaces = " ".repeat(lastLine.takeWhile { it == ' ' }.length)
                                        val extraIndent = if (lastLine.trimEnd().endsWith("{")) "    " else ""
                                        val indented = newText + leadingSpaces + extraIndent
                                        currentTab.updateContent(indented)
                                        textFieldValue = newVal.copy(text = indented, selection = TextRange(indented.length))
                                    } else {
                                        currentTab.updateContent(newText)
                                        textFieldValue = newVal
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

    // New Item Dialog
    if (showNewItemDialog) {
        NewItemDialog(
            projectRoot = projectDir,
            targetDirectory = targetFolderForNewItem,
            onDismiss = { showNewItemDialog = false },
            onItemCreated = { newFile, shouldOpen ->
                refreshFileTree()
                if (shouldOpen && !newFile.isDirectory) {
                    val rel = newFile.relativeTo(projectDir).path
                    openFileInTab(rel)
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
                        val src = File(projectDir, node.path)
                        val dst = File(src.parentFile, actionInputText.trim())
                        if (src.renameTo(dst)) {
                            // Update open tab if renamed
                            val tabIndex = openTabs.indexOfFirst { it.filePath == node.path }
                            if (tabIndex >= 0) {
                                val rel = dst.relativeTo(projectDir).path
                                openTabs[tabIndex] = openTabs[tabIndex].copy(filePath = rel, fileName = dst.name)
                            }
                            refreshFileTree()
                            showRenameDialog = false
                            Toast.makeText(context, "Renamed to ${dst.name}", Toast.LENGTH_SHORT).show()
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
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete ${node.name}?") },
            text = { Text("Are you sure you want to delete '${node.name}' permanently? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val target = File(projectDir, node.path)
                        target.deleteRecursively()
                        openTabs.removeAll { it.filePath.startsWith(node.path) }
                        refreshFileTree()
                        showDeleteConfirmDialog = false
                        Toast.makeText(context, "Deleted ${node.name}", Toast.LENGTH_SHORT).show()
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
                        activeTab.updateContent(activeTab.content.replace(searchQuery, replaceQuery))
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
