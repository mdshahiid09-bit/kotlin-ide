package com.kotlinide.shahid.app.ui.screens.projects.wizard

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kotlinide.shahid.app.data.api.GitHubApiException
import com.kotlinide.shahid.app.data.api.GitHubApiService
import com.kotlinide.shahid.app.data.api.TokenType
import com.kotlinide.shahid.app.data.api.TokenValidationInfo
import com.kotlinide.shahid.app.data.model.Project
import com.kotlinide.shahid.app.data.model.ProjectTemplate
import com.kotlinide.shahid.app.data.security.TokenStorage
import com.kotlinide.shahid.app.data.storage.AppPreferences
import com.kotlinide.shahid.app.data.storage.ProjectStorage
import com.kotlinide.shahid.app.data.templates.TemplateProvider
import com.kotlinide.shahid.app.ui.theme.DarkBackground
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import com.kotlinide.shahid.app.ui.theme.OrangeWarning
import com.kotlinide.shahid.app.util.IconGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectWizardScreen(
    projectStorage: ProjectStorage,
    tokenStorage: TokenStorage,
    appPreferences: AppPreferences,
    onBack: () -> Unit,
    onProjectCreated: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { GitHubApiService(tokenStorage) }

    var step by remember { mutableIntStateOf(1) }

    // Preserved form data
    var appName by remember { mutableStateOf("MyAwesomeApp") }
    var packageName by remember { mutableStateOf("com.example.myawesomeapp") }
    var versionName by remember { mutableStateOf("1.0.0") }
    var versionCodeInput by remember { mutableStateOf("1") }

    var selectedTemplateId by remember { mutableStateOf("empty_activity") }
    var minSdk by remember { mutableIntStateOf(26) }
    var targetSdk by remember { mutableIntStateOf(35) }
    var isPrivateRepo by remember { mutableStateOf(true) }

    // Step 3 App Icon state
    var iconUri by remember { mutableStateOf<Uri?>(null) }
    var iconBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Token Validation & Checker state
    var tokenInfo by remember { mutableStateOf<TokenValidationInfo?>(null) }
    var isTestingToken by remember { mutableStateOf(false) }
    var tokenTestError by remember { mutableStateOf<String?>(null) }

    // Repository Creation state
    var isCreating by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorRawDetails by remember { mutableStateOf<String?>(null) }
    var isDetailsExpanded by remember { mutableStateOf(false) }

    fun testTokenPermissions() {
        isTestingToken = true
        tokenTestError = null
        coroutineScope.launch {
            val res = apiService.validateTokenDetails()
            isTestingToken = false
            res.onSuccess { info ->
                tokenInfo = info
                appPreferences.githubUsername = info.user.login
            }.onFailure { err ->
                tokenTestError = if (err is GitHubApiException) {
                    err.userFriendlyMessage
                } else {
                    err.message ?: "Token verification failed"
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        testTokenPermissions()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    iconBitmap = bitmap
                    iconUri = uri
                } else {
                    Toast.makeText(context, "Could not decode PNG image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun executeRepositoryCreation() {
        isCreating = true
        errorMessage = null
        errorRawDetails = null
        isDetailsExpanded = false
        statusText = "Verifying GitHub credentials & permissions..."
        val vCode = versionCodeInput.toIntOrNull() ?: 1
        val projectId = UUID.randomUUID().toString()
        val cleanRepoName = appName.replace(Regex("[^a-zA-Z0-9_-]"), "-")

        coroutineScope.launch {
            val repoRes = apiService.createRepository(
                name = cleanRepoName,
                description = "$appName - Android application project",
                isPrivate = isPrivateRepo
            )

            if (repoRes.isFailure) {
                isCreating = false
                val ex = repoRes.exceptionOrNull()
                if (ex is GitHubApiException) {
                    errorMessage = ex.userFriendlyMessage
                    errorRawDetails = ex.rawResponse
                } else {
                    errorMessage = ex?.message ?: "Failed to create GitHub repository"
                    errorRawDetails = null
                }
                return@launch
            }

            val repo = repoRes.getOrThrow()
            statusText = "Pushing project template files to GitHub..."

            val files = TemplateProvider.generateProjectFiles(
                appName = appName,
                packageName = packageName,
                versionName = versionName,
                versionCode = vCode,
                templateId = selectedTemplateId,
                minSdk = minSdk,
                targetSdk = targetSdk,
                hasCustomIcon = (iconBitmap != null)
            )

            val mipmapFiles = IconGenerator.generateMipmapIcons(iconBitmap)

            val projectDir = projectStorage.getProjectDirectory(projectId)
            files.forEach { (relPath, content) ->
                val f = File(projectDir, relPath)
                f.parentFile?.mkdirs()
                f.writeText(content)
            }
            mipmapFiles.forEach { (relPath, bytes) ->
                val f = File(projectDir, relPath)
                f.parentFile?.mkdirs()
                f.writeBytes(bytes)
            }

            for ((relPath, content) in files) {
                apiService.createOrUpdateFile(
                    owner = repo.owner.login,
                    repo = repo.name,
                    path = relPath,
                    contentString = content,
                    message = "Add $relPath"
                )
            }

            for ((relPath, bytes) in mipmapFiles) {
                apiService.createOrUpdateBinaryFile(
                    owner = repo.owner.login,
                    repo = repo.name,
                    path = relPath,
                    bytes = bytes,
                    message = "Add $relPath"
                )
            }

            val newProject = Project(
                id = projectId,
                name = appName,
                packageName = packageName,
                versionName = versionName,
                versionCode = vCode,
                templateId = selectedTemplateId,
                minSdk = minSdk,
                targetSdk = targetSdk,
                repoName = repo.name,
                repoOwner = repo.owner.login,
                repoUrl = repo.htmlUrl,
                isPrivate = isPrivateRepo
            )
            projectStorage.saveProject(newProject)

            isCreating = false
            Toast.makeText(context, "Repository & Project Created Successfully!", Toast.LENGTH_SHORT).show()
            onProjectCreated(projectId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Compose Project", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (step > 1) step-- else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                LinearProgressIndicator(
                    progress = { step / 3f },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = NeonGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 1) {
                        OutlinedButton(onClick = { step-- }, enabled = !isCreating) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (step < 3) {
                                step++
                            } else {
                                executeRepositoryCreation()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                        enabled = !isCreating && appName.isNotBlank() && packageName.isNotBlank()
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Creating...")
                        } else {
                            Text(if (step == 3) "Create Repository & Project" else "Next", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            when (step) {
                1 -> {
                    Text("1. App Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Configure your application identity and versioning.", color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = appName,
                        onValueChange = {
                            appName = it
                            packageName = "com.example." + it.lowercase().replace(Regex("[^a-z0-9]"), "")
                        },
                        label = { Text("Application Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Package Name / Application ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = versionName,
                            onValueChange = { versionName = it },
                            label = { Text("Version Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = versionCodeInput,
                            onValueChange = { versionCodeInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Version Code") },
                            placeholder = { Text("1") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Version Code is typed manually and never auto-incremented.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                2 -> {
                    Text("2. Template & SDK Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Select a ready Jetpack Compose starter template and target SDK.", color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(16.dp))

                    ProjectTemplate.ALL.forEach { template ->
                        val isSelected = selectedTemplateId == template.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { selectedTemplateId = template.id }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) NeonGreen else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) NeonGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                val icon = when (template.iconName) {
                                    "view_quilt" -> Icons.Default.ViewQuilt
                                    "lock" -> Icons.Default.Lock
                                    else -> Icons.Default.CropSquare
                                }
                                Icon(icon, contentDescription = null, tint = if (isSelected) NeonGreen else MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(template.name, fontWeight = FontWeight.Bold, color = if (isSelected) NeonGreen else MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(template.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = NeonGreen)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("GitHub Repository Visibility", fontWeight = FontWeight.SemiBold)
                            Text(if (isPrivateRepo) "Private (Recommended)" else "Public", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isPrivateRepo,
                            onCheckedChange = { isPrivateRepo = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                        )
                    }
                }

                3 -> {
                    Text("3. App Icon & Confirmation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Upload a PNG icon or use the default icon. All mipmap densities (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) will be generated automatically.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // App Icon Picker
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(DarkSurface, RoundedCornerShape(22.dp))
                                .border(2.dp, NeonGreen, RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (iconBitmap != null) {
                                Image(
                                    bitmap = iconBitmap!!.asImageBitmap(),
                                    contentDescription = "Custom App Icon",
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(RoundedCornerShape(22.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Android,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/png") },
                                enabled = !isCreating
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (iconBitmap != null) "Change PNG" else "Upload PNG Icon")
                            }

                            if (iconBitmap != null) {
                                OutlinedButton(
                                    onClick = {
                                        iconBitmap = null
                                        iconUri = null
                                    },
                                    enabled = !isCreating
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reset")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (iconBitmap != null) "Custom PNG selected" else "Using default developer neon icon",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (iconBitmap != null) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // GitHub Token Permission Checker & Account Status Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("GitHub Token & Permissions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }

                                OutlinedButton(
                                    onClick = { testTokenPermissions() },
                                    enabled = !isTestingToken && !isCreating
                                ) {
                                    if (isTestingToken) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = NeonGreen)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Testing...", style = MaterialTheme.typography.labelSmall)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Test Token", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (tokenInfo != null) {
                                val info = tokenInfo!!
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Authenticated as @${info.user.login}", fontWeight = FontWeight.SemiBold, color = NeonGreen)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                val typeLabel = when (info.tokenType) {
                                    TokenType.CLASSIC -> "Classic PAT (ghp_...)"
                                    TokenType.FINE_GRAINED -> "Fine-Grained PAT (github_pat_...)"
                                    TokenType.UNKNOWN -> "Personal Access Token"
                                }
                                Text("Token Type: $typeLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.height(6.dp))

                                if (info.tokenType == TokenType.CLASSIC) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val repoOk = info.hasRepoScope
                                        Icon(
                                            if (repoOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (repoOk) NeonGreen else OrangeWarning,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (repoOk) "Scope 'repo' detected (Full control)" else "Missing 'repo' scope! (Required for private repos)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (repoOk) MaterialTheme.colorScheme.onSurface else OrangeWarning
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val wfOk = info.hasWorkflowScope
                                        Icon(
                                            if (wfOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (wfOk) NeonGreen else OrangeWarning,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (wfOk) "Scope 'workflow' detected (GitHub Actions)" else "Missing 'workflow' scope (Recommended for CI/CD)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (wfOk) MaterialTheme.colorScheme.onSurface else OrangeWarning
                                        )
                                    }
                                } else if (info.tokenType == TokenType.FINE_GRAINED) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Fine-grained Token Requirements:",
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "• Repository access: 'All repositories' (Required to create new repos)\n• Administration: Read and write\n• Contents: Read and write\n• Workflows: Read and write",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else if (tokenTestError != null) {
                                Text(tokenTestError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("Testing token permissions...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Project Summary Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Project Configuration Summary:", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Name: $appName\n• Package: $packageName\n• Version: $versionName ($versionCodeInput)\n• Visibility: ${if (isPrivateRepo) "Private" else "Public"}\n• Icon: ${if (iconBitmap != null) "Custom PNG (Mipmaps Generated)" else "Default Adaptive Icon"}\n• Cloud Build: Automated GitHub Actions workflow")
                        }
                    }

                    if (isCreating) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = NeonGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(statusText, style = MaterialTheme.typography.bodyMedium, color = NeonGreen)
                        }
                    }

                    // Error Display with Details & Retry Button
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = errorMessage!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                if (!errorRawDetails.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .clickable { isDetailsExpanded = !isDetailsExpanded }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isDetailsExpanded) "Hide details ▲" else "Show details ▼",
                                            color = NeonGreen,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (isDetailsExpanded) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(DarkBackground, RoundedCornerShape(6.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = errorRawDetails!!,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { executeRepositoryCreation() },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                                        enabled = !isCreating
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
