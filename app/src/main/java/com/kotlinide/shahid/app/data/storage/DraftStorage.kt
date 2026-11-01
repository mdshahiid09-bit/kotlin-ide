package com.kotlinide.shahid.app.data.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Dedicated local store for unsaved editor drafts.
 * Prevents code loss during navigation, orientation change, or unexpected process death.
 */
class DraftStorage(private val context: Context) {

    private val draftsRoot = File(context.filesDir, "unsaved_drafts").apply { mkdirs() }

    private fun getDraftFile(projectId: String, relativePath: String): File {
        val projDir = File(draftsRoot, projectId).apply { mkdirs() }
        val hash = hashPath(relativePath)
        return File(projDir, "$hash.draft")
    }

    private fun hashPath(path: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(path.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun saveDraft(projectId: String, relativePath: String, content: String) = withContext(Dispatchers.IO) {
        try {
            val file = getDraftFile(projectId, relativePath)
            file.writeText(content)
        } catch (e: Exception) {}
    }

    suspend fun getDraft(projectId: String, relativePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = getDraftFile(projectId, relativePath)
            if (file.exists() && file.isFile) file.readText() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clearDraft(projectId: String, relativePath: String) = withContext(Dispatchers.IO) {
        try {
            val file = getDraftFile(projectId, relativePath)
            if (file.exists()) file.delete()
        } catch (e: Exception) {}
    }

    suspend fun clearAllDraftsForProject(projectId: String) = withContext(Dispatchers.IO) {
        try {
            val projDir = File(draftsRoot, projectId)
            if (projDir.exists()) projDir.deleteRecursively()
        } catch (e: Exception) {}
    }

    suspend fun hasDraft(projectId: String, relativePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = getDraftFile(projectId, relativePath)
        file.exists() && file.length() > 0
    }
}
