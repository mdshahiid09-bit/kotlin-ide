package com.kotlinide.shahid.app.ui.screens.projects.wizard

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ViewQuilt
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kotlinide.shahid.app.data.api.GitHubApiService
import com.kotlinide.shahid.app.data.model.Project
import com.kotlinide.shahid.app.data.model.ProjectTemplate
import com.kotlinide.shahid.app.data.security.TokenStorage
import com.kotlinide.shahid.app.data.storage.AppPreferences
import com.kotlinide.shahid.app.data.storage.ProjectStorage
import com.kotlinide.shahid.app.data.templates.TemplateProvider
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.NeonGreen
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

    var appName by remember { mutableStateOf("MyAwesomeApp") }
    var packageName by remember { mutableStateOf("com.example.myawesomeapp") }
    var versionName by remember { mutableStateOf("1.0.0") }
    var versionCodeInput by remember { mutableStateOf("1") }

    var selectedTemplateId by remember { mutableStateOf("empty_activity") }
    var minSdk by remember { mutableIntStateOf(26) }
    var targetSdk by remember { mutableIntStateOf(35) }
    var isPrivateRepo by remember { mutableStateOf(true) }

    var isCreating by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                                isCreating = true
                                errorMessage = null
                                statusText = "Creating GitHub repository..."
                                val vCode = versionCodeInput.toIntOrNull() ?: 1
                                val projectId = UUID.randomUUID().toString()
                                val cleanRepoName = appName.replace(Regex("[^a-zA-Z0-9_-]"), "-")

                                coroutineScope.launch {
                                    val repoRes = apiService.createRepository(
                                        name = cleanRepoName,
                                        description = "$appName - Built with Kotlin IDE on Android",
                                        isPrivate = isPrivateRepo
                                    )

                                    if (repoRes.isFailure) {
                                        isCreating = false
                                        errorMessage = repoRes.exceptionOrNull()?.message ?: "Failed to create GitHub repository"
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
                                        targetSdk = targetSdk
                                    )

                                    val projectDir = projectStorage.getProjectDirectory(projectId)
                                    files.forEach { (relPath, content) ->
                                        val f = File(projectDir, relPath)
                                        f.parentFile?.mkdirs()
                                        f.writeText(content)
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
                                    Toast.makeText(context, "Project Created!", Toast.LENGTH_SHORT).show()
                                    onProjectCreated(projectId)
                                }
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
                            Text(if (step == 3) "Create Project" else "Next", fontWeight = FontWeight.Bold)
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
                    Text("Standard adaptive mipmap icons will be configured automatically.", color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(DarkSurface, RoundedCornerShape(20.dp))
                            .border(2.dp, NeonGreen, RoundedCornerShape(20.dp))
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(44.dp))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Project Summary:", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Name: $appName\n• Package: $packageName\n• Version: $versionName ($versionCodeInput)\n• Visibility: ${if (isPrivateRepo) "Private" else "Public"}\n• Cloud CI/CD: Automated GitHub Actions workflow")
                        }
                    }

                    if (isCreating) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = NeonGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(statusText, style = MaterialTheme.typography.bodyMedium, color = NeonGreen)
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
