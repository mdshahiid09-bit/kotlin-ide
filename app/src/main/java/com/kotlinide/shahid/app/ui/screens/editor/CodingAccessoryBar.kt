package com.kotlinide.shahid.app.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlinide.shahid.app.ui.theme.DarkSurface
import com.kotlinide.shahid.app.ui.theme.DarkSurfaceHighlight
import com.kotlinide.shahid.app.ui.theme.NeonGreen

@Composable
fun CodingAccessoryBar(
    onInsertSymbol: (String) -> Unit,
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(DarkSurface)
            .padding(vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading breathing room
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

        // Trailing padding to ensure last item is never clipped
        Box(modifier = Modifier.padding(end = 16.dp))
    }
}
