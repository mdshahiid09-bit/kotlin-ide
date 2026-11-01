package com.kotlinide.shahid.app.ui.screens.editor

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlinide.shahid.app.ui.theme.DarkBorder
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.DarkSurfaceHighlight
import com.kotlinide.shahid.app.ui.theme.NeonGreen
import com.kotlinide.shahid.app.ui.theme.OrangeWarning

@Composable
fun CodingAccessoryBar(
    onInsertSymbol: (String) -> Unit,
    suggestions: List<String> = emptyList(),
    linterStatus: String? = null,
    onLinterClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val symbols = listOf(
        "Tab" to "    ",
        "{ }" to "{}",
        "( )" to "()",
        "[ ]" to "[]",
        "\" \"" to "\"\"",
        "' '" to "''",
        ";" to ";",
        "." to ".",
        "=" to " = ",
        "->" to " -> ",
        "<" to "<",
        ">" to ">",
        "-" to " - ",
        "+" to " + ",
        "*" to "*",
        "/" to "/",
        "&" to "&",
        "|" to "|",
        "!" to "!",
        "?" to "?",
        ":" to ":",
        "@" to "@",
        "#" to "#",
        "$" to "$",
        "%" to "%",
        "^" to "^",
        "~" to "~",
        "_" to "_",
        "\\" to "\\",
        "," to ", "
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
    ) {
        // Suggestions and Linter Bar
        if (!linterStatus.isNullOrBlank() || suggestions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(Color(0xFF161B22))
                    .padding(horizontal = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!linterStatus.isNullOrBlank()) {
                    val isOk = linterStatus.startsWith("✔")
                    val tint = if (isOk) NeonGreen else OrangeWarning
                    Row(
                        modifier = Modifier
                            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .clickable(enabled = onLinterClick != null) { onLinterClick?.invoke() }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = linterStatus,
                            color = tint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(DarkBorder))
                    Spacer(modifier = Modifier.width(2.dp))
                }

                suggestions.forEach { suggestion ->
                    Box(
                        modifier = Modifier
                            .background(DarkSurfaceHighlight, RoundedCornerShape(4.dp))
                            .clickable { onInsertSymbol(suggestion) }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = suggestion,
                            color = NeonGreen,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Primary Symbols Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.padding(start = 6.dp))

            symbols.forEach { (label, value) ->
                Box(
                    modifier = Modifier
                        .background(DarkSurfaceHighlight, RoundedCornerShape(6.dp))
                        .clickable { onInsertSymbol(value) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (label == "Tab") NeonGreen else Color(0xFFE6EDF3),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Box(modifier = Modifier.padding(end = 16.dp))
        }
    }
}
