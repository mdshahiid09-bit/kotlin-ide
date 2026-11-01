package com.kotlinide.shahid.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: MutableList<FileNode> = mutableListOf(),
    val sha: String? = null
) {
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "")

    fun findNode(targetPath: String): FileNode? {
        if (path == targetPath) return this
        if (isDirectory) {
            for (child in children) {
                val found = child.findNode(targetPath)
                if (found != null) return found
            }
        }
        return null
    }

    fun removeNode(targetPath: String): Boolean {
        if (!isDirectory) return false
        val iterator = children.iterator()
        while (iterator.hasNext()) {
            val child = iterator.next()
            if (child.path == targetPath) {
                iterator.remove()
                return true
            }
            if (child.isDirectory && child.removeNode(targetPath)) {
                return true
            }
        }
        return false
    }
}
