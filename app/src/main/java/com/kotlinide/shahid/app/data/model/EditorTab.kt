package com.kotlinide.shahid.app.data.model

data class EditorTab(
    val filePath: String,
    val fileName: String,
    var content: String,
    var savedContent: String,
    var isDirty: Boolean = false,
    val undoStack: MutableList<String> = mutableListOf(),
    val redoStack: MutableList<String> = mutableListOf(),
    var cursorPosition: Int = 0,
    val openedAt: Long = System.currentTimeMillis()
) {
    fun updateContent(newContent: String) {
        if (newContent != content) {
            undoStack.add(content)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
            content = newContent
            isDirty = (content != savedContent)
        }
    }

    fun undo(): String? {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(content)
            content = prev
            isDirty = (content != savedContent)
            return content
        }
        return null
    }

    fun redo(): String? {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(content)
            content = next
            isDirty = (content != savedContent)
            return content
        }
        return null
    }

    fun markSaved() {
        savedContent = content
        isDirty = false
    }
}
