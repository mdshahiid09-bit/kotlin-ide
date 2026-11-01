package com.kotlinide.shahid.app.ui.screens.projects.wizard

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.unit.sp
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
import com.kotlinide.shahid.app.util.AuditLogger
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

    // App Information
    var appName by remember { mutableStateOf("My Application") }
    var packageName by remember { mutableStateOf("com.example.myapplication") }
    var versionName by remember { mutableStateOf("1.0.0") }
    var versionCodeInput by remember { mutableStateOf("1") }

    // Template & SDK
    var selectedTemplateId by remember { mutableStateOf("empty_compose") }
    var minSdk by remember { mutableIntStateOf(26) }
    var targetSdk by remember { mutableIntStateOf(35) }
    var isPrivateRepo by remember { mutableStateOf(true) }

    // App Icon
    var iconBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Status & Error
    var isCreating by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorRawDetails by remember { mutableStateOf<String?>(null) }
    var isDetailsExpanded by remember { mutableStateOf(false) }

    // Token capability warning
    var tokenInfo by remember { mutableStateOf<TokenValidationInfo?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                        decoder.allocatorSchema = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                iconBitmap = bitmap
                Toast.makeText(context, "Icon loaded. Mipmap densities will be generated.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load icon: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        val testRes = apiService.validateTokenDetails()
        testRes.onSuccess { info ->
            tokenInfo = info
        }
    }

    fun executeRepositoryCreation() {
        if (appName.isBlank() || packageName.isBlank()) {
            errorMessage = "App Name and Package Name are required."
            return
        }

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

            AuditLogger.log(
                context = context,
                eventType = "PROJECT_CREATED",
                details = "Created project '$appName' linked to repository ${repo.owner.login}/${repo.name}",
                projectId = projectId,
                repoName = repo.name
            )

            isCreating = false
            Toast.makeText(context, "Repository & Project Created Successfully!", Toast.LENGTH_SHORT).show()
            onProjectCreated(projectId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Android Project", fontWeight = FontWeight.Bold) },
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

            // Token Capability Alert
            if (tokenInfo?.tokenType == TokenType.FINE_GRAINED) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = OrangeWarning.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = OrangeWarning, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Fine-grained Personal Access Token detected. Note that GitHub disallows creating personal user repositories via REST API. If creation fails with 403, please switch to a Classic PAT with 'repo' and 'workflow' scopes in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OrangeWarning
                        )
                    }
                }
            }

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
                    Text("2. Template & Architecture", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Select a ready starter template and repository visibility.", color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00FF66),
                                checkedTrackColor = Color(0xFF1B382B)
                            )
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
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (iconBitmap != null) "Change PNG" else "Upload PNG")
                            }

                            if (iconBitmap != null) {
                                TextButton(
                                    onClick = { iconBitmap = null },
                                    enabled = !isCreating
                                ) {
                                    Text("Reset to Default")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Project Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("• App Name: $appName", style = MaterialTheme.typography.bodyMedium)
                            Text("• Package: $packageName", style = MaterialTheme.typography.bodyMedium)
                            Text("• Version: $versionName (Code $versionCodeInput)", style = MaterialTheme.typography.bodyMedium)
                            Text("• Template: ${if (selectedTemplateId == "empty_views") "Empty Views Activity" else "Empty Compose Activity"}", style = MaterialTheme.typography.bodyMedium)
                            Text("• Repository: ${if (isPrivateRepo) "Private" else "Public"}", style = MaterialTheme.typography.bodyMedium)
                            Text("• Icon: ${if (iconBitmap != null) "Custom PNG (Mipmaps generated)" else "Default Adaptive Icon"}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (isCreating) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonGreen, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(statusText, style = MaterialTheme.typography.bodyMedium, color = NeonGreen)
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Repository Creation Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(errorMessage!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)

                                if (!errorRawDetails.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isDetailsExpanded = !isDetailsExpanded },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isDetailsExpanded) "Hide details" else "Show details",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            if (isDetailsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    AnimatedVisibility(visible = isDetailsExpanded) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                                .background(DarkBackground, RoundedCornerShape(6.dp))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = errorRawDetails!!,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = Color.LightGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
