package com.kotlinide.shahid.app.data.sync

import android.content.Context
import com.kotlinide.shahid.app.data.api.GitHubApiException
import com.kotlinide.shahid.app.data.api.GitHubApiService
import com.kotlinide.shahid.app.data.api.QueuedSyncAction
import com.kotlinide.shahid.app.data.api.SyncActionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class SyncQueueManager(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val queueFile = File(context.filesDir, "sync_queue.json")

    @Synchronized
    private fun readQueue(): MutableList<QueuedSyncAction> {
        if (!queueFile.exists()) return mutableListOf()
        return try {
            json.decodeFromString<List<QueuedSyncAction>>(queueFile.readText()).toMutableList()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    @Synchronized
    private fun writeQueue(list: List<QueuedSyncAction>) {
        try {
            queueFile.writeText(json.encodeToString(list))
        } catch (_: Exception) {}
    }

    suspend fun enqueue(action: QueuedSyncAction) = withContext(Dispatchers.IO) {
        val list = readQueue()
        // If there is already an update for the same file in the queue, replace or update it
        val existingIndex = list.indexOfFirst { it.projectId == action.projectId && it.path == action.path }
        if (existingIndex >= 0 && action.actionType == SyncActionType.CREATE_OR_UPDATE && list[existingIndex].actionType == SyncActionType.CREATE_OR_UPDATE) {
            list[existingIndex] = action
        } else {
            list.add(action)
        }
        writeQueue(list)
    }

    suspend fun getPendingCount(projectId: String): Int = withContext(Dispatchers.IO) {
        readQueue().count { it.projectId == projectId }
    }

    suspend fun getPendingActions(projectId: String): List<QueuedSyncAction> = withContext(Dispatchers.IO) {
        readQueue().filter { it.projectId == projectId }
    }

    suspend fun removeAction(actionId: String) = withContext(Dispatchers.IO) {
        val list = readQueue()
        list.removeAll { it.id == actionId }
        writeQueue(list)
    }

    suspend fun clearQueueForProject(projectId: String) = withContext(Dispatchers.IO) {
        val list = readQueue()
        list.removeAll { it.projectId == projectId }
        writeQueue(list)
    }

    /**
     * Drains pending queued sync actions for the given project.
     * Returns the count of successfully synced items.
     * If a conflict occurs (HTTP 409/422), calls onConflict callback and pauses queue for that action.
     */
    suspend fun drainQueue(
        projectId: String,
        apiService: GitHubApiService,
        onConflict: (action: QueuedSyncAction, remoteContent: String, remoteSha: String) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        val actions = getPendingActions(projectId)
        if (actions.isEmpty()) return@withContext Result.success(0)

        var syncedCount = 0
        for (action in actions) {
            try {
                when (action.actionType) {
                    SyncActionType.CREATE_OR_UPDATE -> {
                        val content = action.content.orEmpty()
                        val res = apiService.createOrUpdateFile(
                            owner = action.owner,
                            repo = action.repo,
                            path = action.path,
                            contentString = content,
                            message = "Update ${action.path} from Kotlin IDE",
                            sha = action.sha
                        )

                        if (res.isSuccess) {
                            removeAction(action.id)
                            syncedCount++
                        } else {
                            val ex = res.exceptionOrNull()
                            if (ex is GitHubApiException && (ex.statusCode == 409 || ex.statusCode == 422)) {
                                // SHA Mismatch conflict! Fetch remote content
                                val remote = apiService.getFileDetails(action.owner, action.repo, action.path).getOrNull()
                                val remoteContent = remote?.content?.let { apiService.decodeBase64(it) }.orEmpty()
                                val remoteSha = remote?.sha.orEmpty()
                                onConflict(action, remoteContent, remoteSha)
                                break // Stop queue draining until conflict is resolved
                            } else {
                                // Network or other failure, stop queue and retry later
                                return@withContext Result.failure(ex ?: Exception("Failed to sync ${action.path}"))
                            }
                        }
                    }

                    SyncActionType.DELETE -> {
                        val sha = action.sha ?: apiService.getFileDetails(action.owner, action.repo, action.path).getOrNull()?.sha
                        if (sha != null) {
                            val res = apiService.deleteFile(
                                owner = action.owner,
                                repo = action.repo,
                                path = action.path,
                                sha = sha,
                                message = "Delete ${action.path} from Kotlin IDE"
                            )
                            if (res.isSuccess) {
                                removeAction(action.id)
                                syncedCount++
                            } else {
                                val ex = res.exceptionOrNull()
                                return@withContext Result.failure(ex ?: Exception("Failed to delete ${action.path}"))
                            }
                        } else {
                            removeAction(action.id) // Already not on GitHub
                        }
                    }

                    SyncActionType.RENAME -> {
                        // Create at new path, delete old path
                        val content = action.content.orEmpty()
                        val createRes = apiService.createOrUpdateFile(
                            owner = action.owner,
                            repo = action.repo,
                            path = action.path,
                            contentString = content,
                            message = "Rename to ${action.path} from Kotlin IDE"
                        )
                        if (createRes.isSuccess) {
                            if (!action.oldPath.isNullOrBlank()) {
                                val oldSha = apiService.getFileDetails(action.owner, action.repo, action.oldPath).getOrNull()?.sha
                                if (oldSha != null) {
                                    apiService.deleteFile(
                                        owner = action.owner,
                                        repo = action.repo,
                                        path = action.oldPath,
                                        sha = oldSha,
                                        message = "Delete ${action.oldPath} after rename"
                                    )
                                }
                            }
                            removeAction(action.id)
                            syncedCount++
                        } else {
                            val ex = createRes.exceptionOrNull()
                            return@withContext Result.failure(ex ?: Exception("Failed to rename ${action.path}"))
                        }
                    }
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

        Result.success(syncedCount)
    }
}
