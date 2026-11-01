package com.kotlinide.shahid.app.ui.screens.editor

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlinide.shahid.app.ui.theme.DarkBackground
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import com.kotlinide.shahid.app.ui.theme.RedError
import com.kotlinide.shahid.app.util.safeIoCall
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun NewItemCreationDialog(
    isFolder: Boolean,
    targetDirectory: File,
    projectRoot: File,
    onDismiss: () -> Unit,
    onItemCreated: (File) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var nameInput by remember { mutableStateOf("") }
    var selectedExtension by remember { mutableStateOf(if (isFolder) "" else ".kt") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isCreating by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    val relTargetDir = remember(targetDirectory, projectRoot) {
        val rel = targetDirectory.relativeToOrNull(projectRoot)?.path.orEmpty()
        if (rel.isEmpty()) "(Project Root)" else rel
    }

    val resultingFileName = remember(nameInput, selectedExtension, isFolder) {
        val cleanName = nameInput.trim()
        if (cleanName.isEmpty()) ""
        else if (isFolder) cleanName
        else if (cleanName.contains(".")) cleanName
        else "$cleanName$selectedExtension"
    }

    val resultingRelativePath = remember(relTargetDir, resultingFileName) {
        if (resultingFileName.isEmpty()) ""
        else if (relTargetDir == "(Project Root)") resultingFileName
        else "$relTargetDir/$resultingFileName"
    }

    fun validate(input: String) {
        val trimmed = input.trim()
        val invalidChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        when {
            trimmed.isEmpty() -> {
                validationError = "Name cannot be empty"
            }
            trimmed.any { it in invalidChars } -> {
                validationError = "Name contains invalid characters (/ \\ : * ? \" < > |)"
            }
            File(targetDirectory, resultingFileName).exists() -> {
                validationError = "An item with this name already exists in this folder"
            }
            else -> {
                validationError = null
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        icon = {
            Icon(
                if (isFolder) Icons.Default.CreateNewFolder else Icons.Default.NoteAdd,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = if (isFolder) "New Folder" else "New File",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Inside: $relTargetDir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        validate(it)
                    },
                    label = { Text(if (isFolder) "Folder Name" else "File Name") },
                    placeholder = { Text(if (isFolder) "components" else "MyScreen") },
                    singleLine = true,
                    isError = validationError != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        cursorColor = NeonGreen
                    )
                )

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (!isFolder) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Quick Extensions:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val extensions = listOf(".kt", ".xml", ".json", ".gradle.kts", ".md", ".properties", ".txt")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        extensions.forEach { ext ->
                            val isSelected = selectedExtension == ext
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) NeonGreen else DarkSurface,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        selectedExtension = ext
                                        validate(nameInput)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = ext,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                if (resultingRelativePath.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("Full Resulting Path:", style = MaterialTheme.typography.labelSmall, color = NeonGreen)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = resultingRelativePath,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validationError != null || resultingFileName.isEmpty()) return@Button
                    isCreating = true

                    coroutineScope.launch {
                        val result = safeIoCall("createItem") {
                            val targetFile = File(targetDirectory, resultingFileName)
                            if (isFolder) {
                                targetFile.mkdirs()
                                // Add .gitkeep so git tracks empty folders
                                val gitkeep = File(targetFile, ".gitkeep")
                                if (!gitkeep.exists()) gitkeep.createNewFile()
                            } else {
                                targetFile.parentFile?.mkdirs()
                                targetFile.createNewFile()
                                // Pre-fill basic template if .kt
                                if (targetFile.name.endsWith(".kt")) {
                                    val pkg = calculatePackageName(targetFile, projectRoot)
                                    if (pkg.isNotEmpty()) {
                                        targetFile.writeText("package $pkg\n\n")
                                    }
                                }
                            }
                            targetFile
                        }

                        isCreating = false
                        result.onSuccess { createdFile ->
                            Toast.makeText(context, "Created ${createdFile.name}", Toast.LENGTH_SHORT).show()
                            onItemCreated(createdFile)
                            onDismiss()
                        }.onError { msg, _ ->
                            Toast.makeText(context, "Failed: $msg", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = validationError == null && nameInput.trim().isNotEmpty() && !isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
            ) {
                Text(if (isCreating) "Creating..." else "Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun calculatePackageName(file: File, projectRoot: File): String {
    val absPath = file.parentFile?.absolutePath ?: return ""
    val marker = "src/main/java/"
    val idx = absPath.indexOf(marker)
    return if (idx >= 0) {
        absPath.substring(idx + marker.length).replace('/', '.')
    } else ""
}
