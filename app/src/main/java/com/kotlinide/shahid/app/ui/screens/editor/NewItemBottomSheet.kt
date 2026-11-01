package com.kotlinide.shahid.app.ui.screens.editor

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotlinide.shahid.app.ui.theme.DarkBackground
import com.kotlinide.shahid.app.ui.theme.DarkBorder
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.DarkSurfaceHighlight
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewItemDialog(
    projectRoot: File,
    targetDirectory: File,
    onDismiss: () -> Unit,
    onItemCreated: (File, Boolean) -> Unit // (file, isOpenInEditor)
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    var fileNameInput by remember { mutableStateOf("") }
    var selectedExt by remember { mutableStateOf("kt") }

    var folderNameInput by remember { mutableStateOf("") }
    var packageNameInput by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_file.txt"
                val destFile = File(targetDirectory, fileName)
                destFile.outputStream().use { out ->
                    inputStream?.copyTo(out)
                }
                inputStream?.close()
                Toast.makeText(context, "Imported $fileName", Toast.LENGTH_SHORT).show()
                onItemCreated(destFile, true)
                onDismiss()
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val quickExtensions = listOf("kt", "java", "xml", "json", "gradle.kts", "md", "txt")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Create / Import Project Item", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "In: " + targetDirectory.relativeTo(projectRoot).path.ifEmpty { "(root)" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = NeonGreen,
                    edgePadding = 0.dp
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("File", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Folder", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Package", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Import", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTab) {
                    0 -> { // New File
                        OutlinedTextField(
                            value = fileNameInput,
                            onValueChange = { fileNameInput = it },
                            label = { Text("File Name") },
                            placeholder = { Text("MyScreen") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Extension:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickExtensions.forEach { ext ->
                                val isSelected = selectedExt == ext
                                Box(
                                    modifier = Modifier
                                        .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else DarkSurfaceHighlight, RoundedCornerShape(6.dp))
                                        .border(1.dp, if (isSelected) NeonGreen else DarkBorder, RoundedCornerShape(6.dp))
                                        .clickable { selectedExt = ext }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = ".$ext",
                                        color = if (isSelected) NeonGreen else Color.White,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    1 -> { // New Folder
                        OutlinedTextField(
                            value = folderNameInput,
                            onValueChange = { folderNameInput = it },
                            label = { Text("Folder Name") },
                            placeholder = { Text("components") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    2 -> { // Create Package
                        OutlinedTextField(
                            value = packageNameInput,
                            onValueChange = { packageNameInput = it },
                            label = { Text("Package Path") },
                            placeholder = { Text("com.example.app.ui") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Automatically creates nested folders (e.g. com/example/app/ui)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    3 -> { // Import File
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = NeonGreen)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Import a file from phone storage into this folder.", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                                ) {
                                    Text("Browse Files")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab < 3) {
                Button(
                    onClick = {
                        when (selectedTab) {
                            0 -> { // Create file
                                val rawName = fileNameInput.trim()
                                if (rawName.isBlank()) {
                                    Toast.makeText(context, "Please enter a file name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val fullName = if (rawName.contains('.')) rawName else "$rawName.$selectedExt"
                                val newFile = File(targetDirectory, fullName)
                                if (newFile.exists()) {
                                    Toast.makeText(context, "File already exists", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                targetDirectory.mkdirs()
                                newFile.createNewFile()
                                Toast.makeText(context, "Created $fullName", Toast.LENGTH_SHORT).show()
                                onItemCreated(newFile, true)
                                onDismiss()
                            }
                            1 -> { // Create folder
                                val name = folderNameInput.trim()
                                if (name.isBlank()) {
                                    Toast.makeText(context, "Please enter a folder name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val newFolder = File(targetDirectory, name)
                                if (newFolder.exists()) {
                                    Toast.makeText(context, "Folder already exists", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                newFolder.mkdirs()
                                Toast.makeText(context, "Created folder $name", Toast.LENGTH_SHORT).show()
                                onItemCreated(newFolder, false)
                                onDismiss()
                            }
                            2 -> { // Create package
                                val pkg = packageNameInput.trim()
                                if (pkg.isBlank()) {
                                    Toast.makeText(context, "Please enter a package name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val relPath = pkg.replace('.', '/')
                                val pkgDir = File(targetDirectory, relPath)
                                pkgDir.mkdirs()
                                Toast.makeText(context, "Created package $pkg", Toast.LENGTH_SHORT).show()
                                onItemCreated(pkgDir, false)
                                onDismiss()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                ) {
                    Text(
                        when (selectedTab) {
                            0 -> "Create File"
                            1 -> "Create Folder"
                            2 -> "Create Package"
                            else -> "Done"
                        }
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
