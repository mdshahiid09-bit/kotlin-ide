package com.kotlinide.shahid.app.editor

object EditorAutoComplete {

    private val COMPOSE_SYMBOLS = listOf(
        "Column", "Row", "Box", "Text", "Button", "Spacer",
        "Modifier", "remember", "mutableStateOf", "mutableIntStateOf",
        "Scaffold", "LazyColumn", "items", "Card", "Icon",
        "IconButton", "OutlinedTextField", "Surface", "Alignment",
        "Arrangement", "dp", "sp", "Color", "MaterialTheme",
        "fillMaxSize()", "fillMaxWidth()", "padding(", "height(", "width("
    )

    private val KOTLIN_KEYWORDS = listOf(
        "fun", "val", "var", "class", "interface", "object",
        "override", "import", "package", "return", "if", "else",
        "when", "while", "for", "private", "public", "internal",
        "suspend", "data class", "sealed class", "companion object"
    )

    private val XML_TAGS = listOf(
        "<TextView", "<Button", "<ImageView", "<EditText",
        "<androidx.constraintlayout.widget.ConstraintLayout",
        "<LinearLayout", "<FrameLayout", "<ScrollView",
        "android:layout_width=\"match_parent\"",
        "android:layout_height=\"wrap_content\"",
        "android:id=\"@+id/\"",
        "android:text=\"\"",
        "android:orientation=\"vertical\""
    )

    fun getSuggestions(
        content: String,
        cursorPosition: Int,
        fileExtension: String
    ): List<String> {
        val ext = fileExtension.lowercase()
        val safeCursor = cursorPosition.coerceIn(0, content.length)
        val textBeforeCursor = content.substring(0, safeCursor)
        val currentWord = textBeforeCursor.takeLastWhile { it.isLetterOrDigit() || it == '_' || it == '<' || it == ':' }

        val pool = when (ext) {
            "kt", "kts" -> COMPOSE_SYMBOLS + KOTLIN_KEYWORDS
            "xml" -> XML_TAGS
            else -> COMPOSE_SYMBOLS.take(10)
        }

        if (currentWord.isBlank()) {
            return pool.take(8)
        }

        val filtered = pool.filter {
            it.startsWith(currentWord, ignoreCase = true) && !it.equals(currentWord, ignoreCase = true)
        }

        return if (filtered.isNotEmpty()) filtered.take(8) else pool.take(6)
    }
}
