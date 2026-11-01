package com.kotlinide.shahid.app.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlinide.shahid.app.data.model.FileNode
import com.kotlinide.shahid.app.ui.theme.DarkBorder
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.DarkSurfaceHighlight
import com.kotlinide.shahid.app.ui.theme.DarkTextPrimary
import com.kotlinide.shahid.app.ui.theme.DarkTextSecondary
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import com.kotlinide.shahid.app.ui.theme.OrangeWarning
import com.kotlinide.shahid.app.ui.theme.RedError

@Composable
fun ProjectExplorer(
    projectName: String,
    nodes: List<FileNode>,
    activeFilePath: String?,
    onFileClick: (String) -> Unit,
    onAddNewItem: (FileNode?, Boolean) -> Unit, // (targetParent, isFolder)
    onRefresh: () -> Unit,
    onNodeAction: (FileNode, String) -> Unit, // "rename", "duplicate", "delete", "copy_path", "details"
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showRootMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val sortedTree = remember(nodes) {
        sortNodes(nodes)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurface)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showRootMenu = true
                            }
                        )
                    }
            ) {
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Project Files",
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkTextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { isSearchExpanded = !isSearchExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search Files",
                        tint = if (isSearchExpanded) NeonGreen else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showRootMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Root Options",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showRootMenu,
                        onDismissRequest = { showRootMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("New File at Root", color = Color.White) },
                            leadingIcon = {
                                Icon(Icons.Default.NoteAdd, contentDescription = null, tint = NeonGreen)
                            },
                            onClick = {
                                showRootMenu = false
                                onAddNewItem(null, false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Folder at Root", color = Color.White) },
                            leadingIcon = {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = OrangeWarning)
                            },
                            onClick = {
                                showRootMenu = false
                                onAddNewItem(null, true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Refresh Explorer", color = Color.White) },
                            leadingIcon = {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                            },
                            onClick = {
                                showRootMenu = false
                                onRefresh()
                            }
                        )
                    }
                }
            }
        }

        if (isSearchExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter files...", fontSize = 12.sp, color = DarkTextSecondary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .height(48.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = DarkTextPrimary,
                    unfocusedTextColor = DarkTextPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Recursive tree
        val displayNodes = if (searchQuery.isBlank()) sortedTree else filterNodes(sortedTree, searchQuery.trim())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp)
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showRootMenu = true
                        }
                    )
                }
        ) {
            if (displayNodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No files match '$searchQuery'" else "No files in project",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary
                    )
                }
            } else {
                displayNodes.forEach { node ->
                    ExplorerNodeItem(
                        node = node,
                        level = 0,
                        activeFilePath = activeFilePath,
                        onFileClick = onFileClick,
                        onAddNewItem = onAddNewItem,
                        onRefresh = onRefresh,
                        onNodeAction = onNodeAction
                    )
                }
            }

            // Extra space at bottom allowing easy long-press on root
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

fun sortNodes(nodes: List<FileNode>): List<FileNode> {
    return nodes.sortedWith(
        compareBy<FileNode> { !it.isDirectory }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    ).map { node ->
        if (node.isDirectory) {
            node.copy(children = sortNodes(node.children).toMutableList())
        } else {
            node
        }
    }
}

private fun filterNodes(nodes: List<FileNode>, query: String): List<FileNode> {
    val result = mutableListOf<FileNode>()
    for (node in nodes) {
        if (node.name.contains(query, ignoreCase = true)) {
            result.add(node)
        } else if (node.isDirectory) {
            val filteredChildren = filterNodes(node.children, query)
            if (filteredChildren.isNotEmpty()) {
                result.add(node.copy(children = filteredChildren.toMutableList()))
            }
        }
    }
    return result
}

@Composable
fun ExplorerNodeItem(
    node: FileNode,
    level: Int,
    activeFilePath: String?,
    onFileClick: (String) -> Unit,
    onAddNewItem: (FileNode?, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onNodeAction: (FileNode, String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(level < 1) }
    var showMenu by remember { mutableStateOf(false) }
    val isActive = !node.isDirectory && node.path == activeFilePath
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) DarkSurfaceHighlight else Color.Transparent, RoundedCornerShape(6.dp))
            .pointerInput(node.path) {
                detectTapGestures(
                    onTap = {
                        if (node.isDirectory) {
                            isExpanded = !isExpanded
                        } else {
                            onFileClick(node.path)
                        }
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
            }
            .padding(start = (level * 14).dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        val (iconVector, iconTint) = getFileIconAndTint(node, isExpanded)
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = node.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) NeonGreen else DarkTextPrimary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = DarkTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(DarkSurface)
            ) {
                if (node.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("New File", color = Color.White) },
                        leadingIcon = {
                            Icon(Icons.Default.NoteAdd, contentDescription = null, tint = NeonGreen)
                        },
                        onClick = {
                            showMenu = false
                            onAddNewItem(node, false)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Folder", color = Color.White) },
                        leadingIcon = {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = OrangeWarning)
                        },
                        onClick = {
                            showMenu = false
                            onAddNewItem(node, true)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Refresh Folder", color = Color.White) },
                        leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        },
                        onClick = {
                            showMenu = false
                            onRefresh()
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Rename", color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = Color.White)
                    },
                    onClick = {
                        showMenu = false
                        onNodeAction(node, "rename")
                    }
                )

                if (!node.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("Duplicate", color = Color.White) },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White)
                        },
                        onClick = {
                            showMenu = false
                            onNodeAction(node, "duplicate")
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Copy Path", color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color.White)
                    },
                    onClick = {
                        showMenu = false
                        onNodeAction(node, "copy_path")
                    }
                )

                DropdownMenuItem(
                    text = { Text("Details", color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                    },
                    onClick = {
                        showMenu = false
                        onNodeAction(node, "details")
                    }
                )

                DropdownMenuItem(
                    text = { Text("Delete", color = RedError) },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = RedError)
                    },
                    onClick = {
                        showMenu = false
                        onNodeAction(node, "delete")
                    }
                )
            }
        }
    }

    if (node.isDirectory && isExpanded) {
        Column {
            node.children.forEach { child ->
                ExplorerNodeItem(
                    node = child,
                    level = level + 1,
                    activeFilePath = activeFilePath,
                    onFileClick = onFileClick,
                    onAddNewItem = onAddNewItem,
                    onRefresh = onRefresh,
                    onNodeAction = onNodeAction
                )
            }
        }
    }
}

private fun getFileIconAndTint(node: FileNode, isExpanded: Boolean): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    if (node.isDirectory) {
        val icon = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder
        return Pair(icon, OrangeWarning)
    }

    val ext = node.extension.lowercase()
    return when (ext) {
        "kt", "kts" -> Pair(Icons.Default.Code, NeonGreen)
        "java" -> Pair(Icons.Default.Code, Color(0xFFFFA657))
        "xml", "html", "svg" -> Pair(Icons.Default.Code, Color(0xFF58A6FF))
        "json" -> Pair(Icons.Default.DataObject, Color(0xFFF2CC60))
        "md", "txt" -> Pair(Icons.Default.Description, Color(0xFF79C0FF))
        "gradle", "properties", "toml", "pro" -> Pair(Icons.Default.Settings, Color(0xFFBC8CFF))
        "png", "jpg", "jpeg", "webp" -> Pair(Icons.Default.Image, Color(0xFFD2A8FF))
        else -> Pair(Icons.Default.InsertDriveFile, DarkTextSecondary)
    }
}
