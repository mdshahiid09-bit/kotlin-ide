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
            } catch (_: Exception) {
                // Not standard JSON or decode failed
            }
        }

        val currentToken = cleanCurrentToken()
        val tokenType = detectTokenType(currentToken)

        val userFriendlyMessage = when (statusCode) {
            401 -> {
                "Invalid or expired GitHub token (${gitHubMsg ?: "Bad credentials"}). Please re-enter a valid Personal Access Token."
            }
            403 -> {
                val isNotAccessible = gitHubMsg?.contains("Resource not accessible", ignoreCase = true) == true
                val isRateLimit = gitHubMsg?.contains("rate limit", ignoreCase = true) == true

                when {
                    isRateLimit -> {
                        "GitHub API rate limit exceeded. Please wait a few minutes before retrying."
                    }
                    isNotAccessible && tokenType == TokenType.FINE_GRAINED -> {
                        "Resource not accessible by personal access token (HTTP 403).\n\n" +
                        "Your Fine-grained token cannot create repositories. Please configure it with:\n" +
                        "• Repository access: 'All repositories' (Required to create new repos)\n" +
                        "• Administration: Read and write\n" +
                        "• Contents: Read and write\n" +
                        "• Workflows: Read and write"
                    }
                    isNotAccessible && tokenType == TokenType.CLASSIC -> {
                        "Resource not accessible by personal access token (HTTP 403).\n\n" +
                        "Your Classic token lacks the 'repo' scope. Please regenerate or edit the token on GitHub with:\n" +
                        "• repo (Full control of private repositories)\n" +
                        "• workflow (Manage GitHub Actions workflows)"
                    }
                    isNotAccessible -> {
                        "Resource not accessible by personal access token (HTTP 403).\n\n" +
                        "• Classic PAT (ghp_...): enable 'repo' and 'workflow' scopes.\n" +
                        "• Fine-grained PAT (github_pat_...): set Repository access to 'All repositories' with 'Administration' (Read/Write) and 'Contents' (Read/Write)."
                    }
                    else -> {
                        "GitHub 403 Forbidden: ${gitHubMsg ?: "Permission denied"}. Check token permissions, repository scope, or organization authorization policies."
                    }
                }
            }
            404 -> {
                "Resource unavailable or incorrect endpoint (HTTP 404): ${gitHubMsg ?: "Not Found"}."
            }
            422 -> {
                val extra = if (!errorDetails.isNullOrBlank()) " ($errorDetails)" else ""
                "Repository creation validation failed (HTTP 422): ${gitHubMsg ?: "Invalid request"}$extra. A repository with this name may already exist on your account."
            }
            else -> {
                "GitHub API Error (HTTP $statusCode): ${gitHubMsg ?: "Unexpected response from GitHub"}"
            }
        }

        return GitHubApiException(
            statusCode = statusCode,
            userFriendlyMessage = userFriendlyMessage,
            rawResponse = rawBody,
            gitHubErrorMessage = gitHubMsg
        )
    }

    suspend fun validateTokenDetails(): Result<TokenValidationInfo> = withContext(Dispatchers.IO) {
        val token = cleanCurrentToken()
        if (token.isBlank()) {
            return@withContext Result.failure(
                GitHubApiException(401, "No GitHub token provided. Please enter a token.", "{\"message\":\"Token is empty\"}")
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
                    val scopesHeader = response.header("X-OAuth-Scopes").orEmpty()
                    val scopes = if (scopesHeader.isNotBlank()) {
                        scopesHeader.split(",").map { it.trim() }
                    } else {
                        emptyList()
                    }
                    val tokenType = detectTokenType(token)
                    val hasRepoScope = scopes.contains("repo") || scopes.contains("public_repo")
                    val hasWorkflowScope = scopes.contains("workflow")
                    val expiration = response.header("github-authentication-token-expiration")

                    val info = TokenValidationInfo(
                        user = user,
                        tokenType = tokenType,
                        scopes = scopes,
                        hasRepoScope = hasRepoScope,
                        hasWorkflowScope = hasWorkflowScope,
                        expirationDate = expiration
                    )
                    Result.success(info)
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

    suspend fun validateToken(): Result<GitHubUser> = withContext(Dispatchers.IO) {
        validateTokenDetails().map { it.user }
    }

    suspend fun createRepository(
        name: String,
        description: String = "Built with Kotlin IDE on Android",
        isPrivate: Boolean = true
    ): Result<GitHubRepoResponse> = withContext(Dispatchers.IO) {
        val token = cleanCurrentToken()
        if (token.isBlank()) {
            return@withContext Result.failure(
                GitHubApiException(
                    statusCode = 401,
                    userFriendlyMessage = "GitHub token invalid ya expired hai. Naya token banakar daalo.",
                    rawResponse = "{\"message\":\"No token provided\"}"
                )
            )
        }

        // Pre-flight verify token
        val verifyRequest = Request.Builder()
            .url("https://api.github.com/user")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "Kotlin-IDE-Android-Client")
            .get()
            .build()

        try {
            client.newCall(verifyRequest).execute().use { verifyResponse ->
                val verifyBody = verifyResponse.body?.string().orEmpty()
                if (verifyResponse.code != 200) {
                    val err = parseGitHubError(verifyResponse.code, verifyBody)
                    return@withContext Result.failure(err)
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(
                GitHubApiException(
                    statusCode = 0,
                    userFriendlyMessage = "Network error: ${e.message ?: "Failed to connect to GitHub"}",
                    rawResponse = e.stackTraceToString()
                )
            )
        }

        // Token verified. Proceed to POST /user/repos
        val reqBody = CreateRepoRequest(
            name = name,
            description = description,
            private = isPrivate,
            autoInit = true
        )
        val bodyString = json.encodeToString(reqBody)
        val createRequest = Request.Builder()
            .url("https://api.github.com/user/repos")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("Content-Type", "application/json")
            .header("User-Agent", "Kotlin-IDE-Android-Client")
            .post(bodyString.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(createRequest).execute().use { response ->
                val respBody = response.body?.string().orEmpty()
                if (response.isSuccessful || response.code == 201) {
                    val repo = json.decodeFromString<GitHubRepoResponse>(respBody)
                    Result.success(repo)
                } else {
                    val err = parseGitHubError(response.code, respBody)
                    Result.failure(err)
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
            val checkReq = newRequestBuilder("https://api.github.com/repos/$owner/$repo/contents/$path?ref=$branch")
                .get()
                .build()
            try {
                client.newCall(checkReq).execute().use { checkResp ->
                    if (checkResp.isSuccessful) {
                        val checkBody = checkResp.body?.string().orEmpty()
                        val existingFile = json.decodeFromString<FileContentResponse>(checkBody)
                        if (existingFile.sha.isNotBlank()) {
                            targetSha = existingFile.sha
                        }
                    }
                }
            } catch (_: Exception) {}
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
                    val respBody = response.body?.string().orEmpty()
                    Result.failure(parseGitHubError(response.code, respBody))
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
                    val respBody = response.body?.string().orEmpty()
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
