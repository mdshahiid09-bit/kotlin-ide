package com.kotlinide.shahid.app.ui.screens.editor

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun ProjectExplorer(
    projectName: String,
    nodes: List<FileNode>,
    activeFilePath: String?,
    onFileClick: (String) -> Unit,
    onAddNewItem: (FileNode?) -> Unit,
    onRefresh: () -> Unit,
    onNodeAction: (FileNode, String) -> Unit, // "rename", "duplicate", "delete", "copy_path", "details"
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurface)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
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
                    Icon(Icons.Default.Search, contentDescription = "Search Files", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(NeonGreen, RoundedCornerShape(8.dp))
                        .clickable { onAddNewItem(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New File/Folder", tint = Color.Black, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (isSearchExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter files...", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .height(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Recursive tree
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val filteredNodes = if (searchQuery.isBlank()) nodes else filterNodes(nodes, searchQuery.trim())
            filteredNodes.forEach { node ->
                ExplorerNodeItem(
                    node = node,
                    level = 0,
                    activeFilePath = activeFilePath,
                    onFileClick = onFileClick,
                    onAddNewItem = onAddNewItem,
                    onNodeAction = onNodeAction
                )
            }
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
    onAddNewItem: (FileNode?) -> Unit,
    onNodeAction: (FileNode, String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(level < 1) }
    var showMenu by remember { mutableStateOf(false) }
    val isActive = !node.isDirectory && node.path == activeFilePath

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) DarkSurfaceHighlight else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable {
                if (node.isDirectory) {
                    isExpanded = !isExpanded
                } else {
                    onFileClick(node.path)
                }
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
            modifier = Modifier.weight(1f)
        )

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(22.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = DarkTextSecondary, modifier = Modifier.size(14.dp))
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(DarkSurface)
            ) {
                if (node.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("New File Inside", color = Color.White) },
                        onClick = {
                            showMenu = false
                            onAddNewItem(node)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Rename", color = Color.White) },
                    onClick = {
                        showMenu = false
                        onNodeAction(node, "rename")
                    }
                )
                if (!node.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("Duplicate", color = Color.White) },
                        onClick = {
                            showMenu = false
                            onNodeAction(node, "duplicate")
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Copy Path", color = Color.White) },
                    onClick = {
                        showMenu = false
                        onNodeAction(node, "copy_path")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Details", color = Color.White) },
                    onClick = {
                        showMenu = false
                        onNodeAction(node, "details")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color(0xFFFF7B72)) },
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
