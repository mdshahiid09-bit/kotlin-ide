package com.kotlinide.shahid.app.data.api

import android.util.Base64
import com.kotlinide.shahid.app.data.security.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class GitHubApiService(private val tokenStorage: TokenStorage) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun newRequestBuilder(url: String): Request.Builder {
        val token = tokenStorage.getToken() ?: ""
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "Kotlin-IDE-Android-Client")
    }

    suspend fun validateToken(): Result<GitHubUser> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("https://api.github.com/user").get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val user = json.decodeFromString<GitHubUser>(body)
                    Result.success(user)
                } else {
                    val errorMsg = when (response.code) {
                        401 -> "Invalid or expired GitHub Personal Access Token. Please re-enter a valid token with 'repo' and 'workflow' scopes."
                        403 -> "API rate limit exceeded or insufficient permissions."
                        else -> "GitHub API Error: HTTP ${response.code}"
                    }
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRepository(
        name: String,
        description: String = "Built with Kotlin IDE on Android",
        isPrivate: Boolean = true
    ): Result<GitHubRepoResponse> = withContext(Dispatchers.IO) {
        val reqBody = CreateRepoRequest(
            name = name,
            description = description,
            private = isPrivate,
            autoInit = false
        )
        val bodyString = json.encodeToString(reqBody)
        val request = newRequestBuilder("https://api.github.com/user/repos")
            .post(bodyString.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val repo = json.decodeFromString<GitHubRepoResponse>(respBody)
                    Result.success(repo)
                } else {
                    Result.failure(Exception("Failed to create repository: HTTP ${response.code} - $respBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRepository(owner: String, repo: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo")
            .delete()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 204) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Failed to delete repository: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateFile(
        owner: String,
        repo: String,
        path: String,
        contentString: String,
        message: String = "Update $path",
        branch: String = "main",
        sha: String? = null
    ): Result<FileContentResponse> = withContext(Dispatchers.IO) {
        val base64Content = Base64.encodeToString(contentString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val reqBody = FileContentRequest(
            message = message,
            content = base64Content,
            branch = branch,
            sha = sha
        )
        val bodyString = json.encodeToString(reqBody)
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/contents/$path")
            .put(bodyString.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val res = json.decodeFromString<FileContentResponse>(respBody)
                    Result.success(res)
                } else {
                    Result.failure(Exception("Failed to commit $path: HTTP ${response.code} - $respBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFileContent(
        owner: String,
        repo: String,
        path: String,
        ref: String = "main"
    ): Result<String> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/contents/$path?ref=$ref")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respBody = response.body?.string().orEmpty()
                    val res = json.decodeFromString<FileContentResponse>(respBody)
                    val rawBase64 = res.content?.replace("\n", "").orEmpty()
                    val decoded = String(Base64.decode(rawBase64, Base64.DEFAULT), Charsets.UTF_8)
                    Result.success(decoded)
                } else {
                    Result.failure(Exception("File not found: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTree(
        owner: String,
        repo: String,
        ref: String = "main"
    ): Result<List<GitTreeItem>> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/git/trees/$ref?recursive=1")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respBody = response.body?.string().orEmpty()
                    val treeResp = json.decodeFromString<GitTreeResponse>(respBody)
                    Result.success(treeResp.tree)
                } else {
                    Result.failure(Exception("Failed to fetch tree: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun triggerWorkflow(
        owner: String,
        repo: String,
        workflowId: String = "build-apk.yml",
        ref: String = "main",
        cleanBuild: Boolean = false
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val inputs = if (cleanBuild) mapOf("clean" to "true") else emptyMap()
        val reqBody = WorkflowDispatchRequest(ref = ref, inputs = inputs)
        val bodyString = json.encodeToString(reqBody)
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/actions/workflows/$workflowId/dispatches")
            .post(bodyString.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 204) {
                    Result.success(true)
                } else {
                    val err = response.body?.string().orEmpty()
                    Result.failure(Exception("Failed to trigger build: HTTP ${response.code} - $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listWorkflowRuns(owner: String, repo: String): Result<List<WorkflowRun>> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/actions/runs?per_page=15")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val runs = json.decodeFromString<WorkflowRunsResponse>(body)
                    Result.success(runs.workflowRuns)
                } else {
                    Result.failure(Exception("Failed to fetch workflow runs: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWorkflowRun(owner: String, repo: String, runId: Long): Result<WorkflowRun> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/actions/runs/$runId")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val run = json.decodeFromString<WorkflowRun>(body)
                    Result.success(run)
                } else {
                    Result.failure(Exception("Failed to fetch run: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWorkflowRunLogs(owner: String, repo: String, runId: Long): Result<String> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/actions/runs/$runId/logs")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code} fetching logs"))
                }
                val inputStream = response.body?.byteStream()
                    ?: return@withContext Result.failure(Exception("Empty log body"))

                val logBuilder = StringBuilder()
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && (entry.name.contains("gradle") || entry.name.contains("build") || entry.name.contains("Assemble"))) {
                            val text = zis.bufferedReader(Charsets.UTF_8).readText()
                            logBuilder.append("=== Log: ${entry.name} ===\n")
                            logBuilder.append(text)
                            logBuilder.append("\n\n")
                        }
                        entry = zis.nextEntry
                    }
                }

                val finalLog = if (logBuilder.isEmpty()) "Build completed. Detailed logs archived." else logBuilder.toString()
                Result.success(finalLog)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listArtifacts(owner: String, repo: String, runId: Long): Result<List<ArtifactItem>> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/actions/runs/$runId/artifacts")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val res = json.decodeFromString<ArtifactsResponse>(body)
                    Result.success(res.artifacts)
                } else {
                    Result.failure(Exception("Failed to fetch artifacts: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadArtifact(downloadUrl: String, destinationFile: File): Result<File> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder(downloadUrl).get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
                    destinationFile.parentFile?.mkdirs()
                    FileOutputStream(destinationFile).use { fos ->
                        body.byteStream().copyTo(fos)
                    }
                    Result.success(destinationFile)
                } else {
                    Result.failure(Exception("Download failed: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
