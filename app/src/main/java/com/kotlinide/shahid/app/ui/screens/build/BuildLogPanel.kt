package com.kotlinide.shahid.app.ui.screens.build

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlinide.shahid.app.data.model.BuildErrorLine
import com.kotlinide.shahid.app.data.model.BuildInfo
import com.kotlinide.shahid.app.ui.theme.DarkBackground
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.DarkSurfaceVariant
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import com.kotlinide.shahid.app.ui.theme.OrangeWarning
import com.kotlinide.shahid.app.ui.theme.RedError

@Composable
fun BuildLogPanel(
    buildInfo: BuildInfo?,
    isBuilding: Boolean,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onCleanBuild: () -> Unit,
    onErrorLineClick: (filePath: String, lineNumber: Int) -> Unit
) {
    val context = LocalContext.current
    var showRawDetails by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 340.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (statusColor, statusIcon) = when {
                        isBuilding -> Pair(OrangeWarning, Icons.Default.HourglassTop)
                        buildInfo?.conclusion == "success" -> Pair(NeonGreen, Icons.Default.CheckCircle)
                        buildInfo?.conclusion == "failure" -> Pair(RedError, Icons.Default.Error)
                        else -> Pair(Color.Gray, Icons.Default.HourglassTop)
                    }
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBuilding) "Building on GitHub Actions..." else "Build ${buildInfo?.conclusion?.uppercase() ?: "OUTPUT"}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row {
                    if (buildInfo?.rawLog?.isNotBlank() == true) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Gradle Log", buildInfo.rawLog))
                                Toast.makeText(context, "Log copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy log", modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (isBuilding) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp), color = NeonGreen)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Assembling Debug APK & Release Bundle...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            if (!isBuilding && buildInfo != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val isSuccess = buildInfo.conclusion == "success"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSuccess) NeonGreen.copy(alpha = 0.1f) else RedError.copy(alpha = 0.12f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = if (isSuccess) "Build Successful!" else "Build Failed",
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccess) NeonGreen else RedError
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildInfo.simpleErrorMessage
                                ?: if (isSuccess) "Debug APK compiled cleanly. You can download and install it directly."
                                else "Compilation error encountered during Gradle build. See below for exact line.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (buildInfo.errors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Errors (tap to jump):", style = MaterialTheme.typography.labelSmall, color = OrangeWarning)
                    Spacer(modifier = Modifier.height(4.dp))
                    buildInfo.errors.take(4).forEach { err ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onErrorLineClick(err.filePath, err.lineNumber) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${err.fileName}:${err.lineNumber}",
                                color = NeonGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = err.message,
                                maxLines = 1,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (!isSuccess) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onCleanBuild,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clean Build")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRawDetails = !showRawDetails }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Technical Gradle Output", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Icon(
                        if (showRawDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showRawDetails) {
                    val logLines = buildInfo.rawLog.lines()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(DarkBackground, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        items(logLines) { line ->
                            val isErrorLine = line.contains("e: ") || line.contains("ERROR:") || line.contains("FAILED")
                            Text(
                                text = line,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (isErrorLine) RedError else Color(0xFFC9D1D9),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isErrorLine) RedError.copy(alpha = 0.15f) else Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}
