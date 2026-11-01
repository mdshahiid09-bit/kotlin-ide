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

    private fun cleanCurrentToken(): String {
        return tokenStorage.getToken()?.let { TokenStorage.cleanToken(it) }.orEmpty()
    }

    fun detectTokenType(token: String): TokenType {
        val clean = TokenStorage.cleanToken(token)
        return when {
            clean.startsWith("github_pat_") -> TokenType.FINE_GRAINED
            clean.startsWith("ghp_") -> TokenType.CLASSIC
            else -> TokenType.UNKNOWN
        }
    }

    private fun newRequestBuilder(url: String): Request.Builder {
        val token = cleanCurrentToken()
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "Kotlin-IDE-Android-Client")
    }

    private fun parseGitHubError(statusCode: Int, rawBody: String?): GitHubApiException {
        var gitHubMsg: String? = null
        var errorDetails: String? = null
        if (!rawBody.isNullOrBlank()) {
            try {
                val parsed = json.decodeFromString<GitHubErrorResponse>(rawBody)
                gitHubMsg = parsed.message.ifBlank { null }
                val detailsList = parsed.errors.mapNotNull { it.message.ifBlank { null } }
                if (detailsList.isNotEmpty()) {
                    errorDetails = detailsList.joinToString(", ")
                }
            } catch (e: Exception) {
                // Not standard JSON
            }
        }

        val currentToken = cleanCurrentToken()
        val tokenType = detectTokenType(currentToken)

        val userFriendlyMessage = when (statusCode) {
            401 -> {
                "GitHub token invalid ya expired hai. Naya token banakar daalo."
            }
            403 -> {
                val isNotAccessible = gitHubMsg?.contains("Resource not accessible", ignoreCase = true) == true
                val isRateLimit = gitHubMsg?.contains("rate limit", ignoreCase = true) == true

                when {
                    isRateLimit -> {
                        "GitHub API rate limit exceeded. Please wait a few minutes before retrying."
                    }
                    isNotAccessible && tokenType == TokenType.FINE_GRAINED -> {
                        "Token mein repo banane ki permission nahi hai (HTTP 403).\n\n" +
                        "Fine-grained token setup:\n" +
                        "• Repository access: 'All repositories'\n" +
                        "• Administration: Read and write\n" +
                        "• Contents: Read and write\n" +
                        "• Workflows: Read and write"
                    }
                    isNotAccessible && tokenType == TokenType.CLASSIC -> {
                        "Token mein repo banane ki permission nahi hai (HTTP 403).\n\n" +
                        "Classic token ke liye 'repo' aur 'workflow' scopes select karein."
                    }
                    else -> {
                        "GitHub access forbidden (HTTP 403): ${gitHubMsg ?: "Permission denied"}"
                    }
                }
            }
            404 -> "Resource not found (HTTP 404): ${gitHubMsg ?: "Endpoint unavailable"}"
            409 -> "Conflict (HTTP 409): The file or repository has conflicting changes on GitHub."
            422 -> "Validation error (HTTP 422): ${errorDetails ?: gitHubMsg ?: "Name conflict or invalid parameters"}"
            else -> "GitHub API Error (HTTP $statusCode): ${gitHubMsg ?: "Unknown error"}"
        }

        return GitHubApiException(
            statusCode = statusCode,
            userFriendlyMessage = userFriendlyMessage,
            rawResponse = rawBody,
            gitHubErrorMessage = gitHubMsg
        )
    }

    fun decodeBase64(rawBase64: String): String {
        return try {
            val clean = rawBase64.replace("\n", "").replace("\r", "")
            String(Base64.decode(clean, Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: Exception) {
            rawBase64
        }
    }

    suspend fun verifyToken(tokenToTest: String? = null): Result<TokenValidationInfo> = withContext(Dispatchers.IO) {
        val token = tokenToTest?.let { TokenStorage.cleanToken(it) } ?: cleanCurrentToken()
        if (token.isBlank()) {
            return@withContext Result.failure(
                GitHubApiException(
                    statusCode = 401,
                    userFriendlyMessage = "No GitHub token provided. Please log in first.",
                    rawResponse = "{\"message\":\"No token provided\"}"
                )
            )
        }

        val request = Request.Builder()
            .url("https://api.github.com/user")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "Kotlin-IDE-Android-Client")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.code == 200) {
                    val user = json.decodeFromString<GitHubUser>(body)
                    val tokenType = detectTokenType(token)
                    val scopesHeader = response.header("X-OAuth-Scopes").orEmpty()
                    val scopes = scopesHeader.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val hasRepo = scopes.contains("repo") || tokenType == TokenType.FINE_GRAINED
                    val hasWorkflow = scopes.contains("workflow") || tokenType == TokenType.FINE_GRAINED

                    Result.success(
                        TokenValidationInfo(
                            user = user,
                            tokenType = tokenType,
                            scopes = scopes,
                            hasRepoScope = hasRepo,
                            hasWorkflowScope = hasWorkflow
                        )
                    )
                } else {
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            Result.failure(
                GitHubApiException(
                    statusCode = 0,
                    userFriendlyMessage = "Network error: ${e.message ?: "Failed to connect to GitHub"}",
                    rawResponse = e.stackTraceToString()
                )
            )
        }
    }

    suspend fun validateTokenDetails(tokenToTest: String? = null): Result<TokenValidationInfo> = verifyToken(tokenToTest)

    suspend fun getAllUserRepositories(): Result<List<GitHubRepoResponse>> = withContext(Dispatchers.IO) {
        val allRepos = mutableListOf<GitHubRepoResponse>()
        var page = 1
        val perPage = 100

        while (true) {
            val url = "https://api.github.com/user/repos?affiliation=owner,collaborator,organization_member&sort=updated&per_page=$perPage&page=$page"
            val request = newRequestBuilder(url).get().build()

            try {
                val pageRepos = client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        json.decodeFromString<List<GitHubRepoResponse>>(body)
                    } else {
                        return@withContext Result.failure(parseGitHubError(response.code, body))
                    }
                }

                allRepos.addAll(pageRepos)
                if (pageRepos.size < perPage) {
                    break // Last page reached
                }
                page++
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

        Result.success(allRepos)
    }

    suspend fun createRepository(
        name: String,
        description: String = "Android application project",
        isPrivate: Boolean = true
    ): Result<GitHubRepoResponse> = withContext(Dispatchers.IO) {
        val token = cleanCurrentToken()
        if (token.isBlank()) {
            return@withContext Result.failure(
                GitHubApiException(
                    statusCode = 401,
                    userFriendlyMessage = "GitHub token invalid ya expired hai. Naya token banakar daalo."
                )
            )
        }

        val reqBody = CreateRepoRequest(
            name = name,
            description = description,
            private = isPrivate,
            autoInit = true
        )
        val bodyString = json.encodeToString(reqBody)
        val createRequest = newRequestBuilder("https://api.github.com/user/repos")
            .post(bodyString.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(createRequest).execute().use { response ->
                val respBody = response.body?.string().orEmpty()
                if (response.isSuccessful || response.code == 201) {
                    val repo = json.decodeFromString<GitHubRepoResponse>(respBody)
                    Result.success(repo)
                } else {
                    Result.failure(parseGitHubError(response.code, respBody))
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
                    val respBody = response.body?.string().orEmpty()
                    Result.failure(parseGitHubError(response.code, respBody))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateBinaryFile(
        owner: String,
        repo: String,
        path: String,
        bytes: ByteArray,
        message: String = "Add $path",
        branch: String = "main",
        sha: String? = null
    ): Result<FileContentResponse> = withContext(Dispatchers.IO) {
        var targetSha = sha
        if (targetSha == null) {
            val checkRes = getFileDetails(owner, repo, path, branch)
            targetSha = checkRes.getOrNull()?.sha
        }

        val base64Content = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val reqBody = FileContentRequest(
            message = message,
            content = base64Content,
            branch = branch,
            sha = targetSha
        )
        val bodyString = json.encodeToString(reqBody)
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/contents/$path")
            .put(bodyString.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string().orEmpty()
                if (response.isSuccessful || response.code == 200 || response.code == 201) {
                    val res = json.decodeFromString<FileContentResponse>(respBody)
                    Result.success(res)
                } else {
                    Result.failure(parseGitHubError(response.code, respBody))
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
    ): Result<FileContentResponse> = createOrUpdateBinaryFile(
        owner = owner,
        repo = repo,
        path = path,
        bytes = contentString.toByteArray(Charsets.UTF_8),
        message = message,
        branch = branch,
        sha = sha
    )

    suspend fun deleteFile(
        owner: String,
        repo: String,
        path: String,
        sha: String,
        message: String = "Delete $path",
        branch: String = "main"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val reqBody = DeleteFileRequest(message = message, sha = sha, branch = branch)
        val bodyString = json.encodeToString(reqBody)
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/contents/$path")
            .delete(bodyString.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 200 || response.code == 204) {
                    Result.success(true)
                } else {
                    val respBody = response.body?.string().orEmpty()
                    Result.failure(parseGitHubError(response.code, respBody))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFileDetails(
        owner: String,
        repo: String,
        path: String,
        ref: String = "main"
    ): Result<FileContentResponse> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/contents/$path?ref=$ref")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val res = json.decodeFromString<FileContentResponse>(respBody)
                    Result.success(res)
                } else {
                    Result.failure(parseGitHubError(response.code, respBody))
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
        val res = getFileDetails(owner, repo, path, ref)
        if (res.isSuccess) {
            val details = res.getOrThrow()
            Result.success(decodeBase64(details.content.orEmpty()))
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Failed to load content"))
        }
    }

    suspend fun getLatestCommitSha(
        owner: String,
        repo: String,
        branch: String = "main"
    ): Result<String> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/commits?sha=$branch&per_page=1")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val commits = json.decodeFromString<List<GitHubCommitItem>>(body)
                    val sha = commits.firstOrNull()?.sha
                    if (sha != null) {
                        Result.success(sha)
                    } else {
                        Result.failure(Exception("No commits found"))
                    }
                } else {
                    Result.failure(parseGitHubError(response.code, body))
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
                val respBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val treeResp = json.decodeFromString<GitTreeResponse>(respBody)
                    Result.success(treeResp.tree)
                } else {
                    Result.failure(parseGitHubError(response.code, respBody))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listCommits(
        owner: String,
        repo: String,
        branch: String = "main",
        perPage: Int = 15
    ): Result<List<GitHubCommitItem>> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/commits?sha=$branch&per_page=$perPage")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val commits = json.decodeFromString<List<GitHubCommitItem>>(body)
                    Result.success(commits)
                } else {
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listBranches(
        owner: String,
        repo: String
    ): Result<List<GitHubBranchItem>> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/branches")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val branches = json.decodeFromString<List<GitHubBranchItem>>(body)
                    Result.success(branches)
                } else {
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createBranch(
        owner: String,
        repo: String,
        branchName: String,
        fromSha: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val req = CreateGitRefRequest(ref = "refs/heads/$branchName", sha = fromSha)
        val bodyString = json.encodeToString(req)
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/git/refs")
            .post(bodyString.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 201) {
                    Result.success(true)
                } else {
                    val body = response.body?.string().orEmpty()
                    Result.failure(parseGitHubError(response.code, body))
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
                    Result.failure(parseGitHubError(response.code, err))
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
                    val body = response.body?.string().orEmpty()
                    Result.failure(parseGitHubError(response.code, body))
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
                    val body = response.body?.string().orEmpty()
                    Result.failure(parseGitHubError(response.code, body))
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
                    val body = response.body?.string().orEmpty()
                    return@withContext Result.failure(parseGitHubError(response.code, body))
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
                    val body = response.body?.string().orEmpty()
                    Result.failure(parseGitHubError(response.code, body))
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
                    val body = response.body?.string().orEmpty()
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
