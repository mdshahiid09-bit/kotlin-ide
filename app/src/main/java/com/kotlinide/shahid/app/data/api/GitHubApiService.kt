package com.kotlinide.shahid.app.data.api

import android.util.Base64
import com.kotlinide.shahid.app.data.security.TokenStorage
import com.kotlinide.shahid.app.util.CrashReportingHelper
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
                // Non-JSON raw body
            }
        }

        val currentToken = cleanCurrentToken()
        val tokenType = detectTokenType(currentToken)

        val userFriendlyMessage = when (statusCode) {
            401 -> "GitHub token is invalid or expired. Please generate a new personal access token and sign in again."
            403 -> {
                val isNotAccessible = gitHubMsg?.contains("Resource not accessible", ignoreCase = true) == true
                val isRateLimit = gitHubMsg?.contains("rate limit", ignoreCase = true) == true

                when {
                    isRateLimit -> "GitHub API rate limit exceeded. Please wait a few moments before retrying."
                    isNotAccessible && tokenType == TokenType.FINE_GRAINED -> {
                        "Token permissions insufficient (HTTP 403).\n\n" +
                        "Note: GitHub fine-grained PATs cannot create personal user repositories via REST API.\n" +
                        "Solution: Sign in using a Classic Personal Access Token with 'repo' and 'workflow' scopes."
                    }
                    isNotAccessible && tokenType == TokenType.CLASSIC -> {
                        "Token permissions insufficient (HTTP 403).\n\n" +
                        "Solution: Ensure your Classic PAT has both 'repo' (Full control of private repositories) " +
                        "and 'workflow' (Update GitHub Action workflows) scopes enabled in GitHub Settings."
                    }
                    else -> "GitHub access forbidden (HTTP 403): ${gitHubMsg ?: "Permission denied"}"
                }
            }
            404 -> "Resource not found (HTTP 404): ${gitHubMsg ?: "Endpoint or repository unavailable"}"
            409 -> "Git conflict (HTTP 409): The file or repository branch has conflicting changes on GitHub."
            422 -> "Validation error (HTTP 422): ${errorDetails ?: gitHubMsg ?: "Name already exists or invalid parameters"}"
            else -> "GitHub API error (HTTP $statusCode): ${gitHubMsg ?: "Unexpected error"}"
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

                    val (hasRepo, hasWorkflow, warning) = when (tokenType) {
                        TokenType.CLASSIC -> {
                            val r = scopes.contains("repo")
                            val w = scopes.contains("workflow")
                            val warn = when {
                                !r && !w -> "Classic PAT is missing 'repo' and 'workflow' scopes."
                                !r -> "Classic PAT is missing 'repo' scope."
                                !w -> "Classic PAT is missing 'workflow' scope."
                                else -> null
                            }
                            Triple(r, w, warn)
                        }
                        TokenType.FINE_GRAINED -> {
                            Triple(
                                false,
                                false,
                                "Fine-grained token detected. Fine-grained tokens cannot create user-level repositories on GitHub. A Classic token with 'repo' and 'workflow' scopes is recommended."
                            )
                        }
                        TokenType.UNKNOWN -> {
                            Triple(false, false, "Unknown token format. Ensure token has repository and workflow scopes.")
                        }
                    }

                    Result.success(
                        TokenValidationInfo(
                            user = user,
                            tokenType = tokenType,
                            scopes = if (tokenType == TokenType.FINE_GRAINED) listOf("fine-grained-permissions") else scopes,
                            hasRepoScope = hasRepo,
                            hasWorkflowScope = hasWorkflow,
                            scopeWarning = warning
                        )
                    )
                } else {
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("GitHubApiService", "Network error in verifyToken", e)
            Result.failure(
                GitHubApiException(
                    statusCode = 0,
                    userFriendlyMessage = "Network error: ${e.message ?: "Failed to connect to GitHub"}",
                    rawResponse = e.stackTraceToString()
                )
            )
        }
    }

    suspend fun validateTokenDetails(tokenToTest: String? = null): Result<TokenValidationInfo> =
        verifyToken(tokenToTest)

    suspend fun requestDeviceCode(
        clientId: String,
        scope: String = "repo,workflow,read:org,user"
    ): Result<DeviceCodeResponse> = withContext(Dispatchers.IO) {
        val formBody = "client_id=$clientId&scope=$scope"
        val request = Request.Builder()
            .url("https://github.com/login/device/code")
            .header("Accept", "application/json")
            .header("User-Agent", "Kotlin-IDE-Android-Client")
            .post(formBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val codeResponse = json.decodeFromString<DeviceCodeResponse>(body)
                    Result.success(codeResponse)
                } else {
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("GitHubApiService", "OAuth Device Code error", e)
            Result.failure(e)
        }
    }

    suspend fun pollDeviceAccessToken(
        clientId: String,
        deviceCode: String
    ): Result<OAuthAccessTokenResponse> = withContext(Dispatchers.IO) {
        val formBody = "client_id=$clientId&device_code=$deviceCode&grant_type=urn:ietf:params:oauth:grant-type:device_code"
        val request = Request.Builder()
            .url("https://github.com/login/oauth/access_token")
            .header("Accept", "application/json")
            .header("User-Agent", "Kotlin-IDE-Android-Client")
            .post(formBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val tokenResponse = json.decodeFromString<OAuthAccessTokenResponse>(body)
                Result.success(tokenResponse)
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("GitHubApiService", "OAuth Poll Access Token error", e)
            Result.failure(e)
        }
    }

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
                    break
                }
                page++
            } catch (e: Exception) {
                CrashReportingHelper.logHandledException("GitHubApiService", "getAllUserRepositories failed on page $page", e)
                return@withContext Result.failure(e)
            }
        }

        Result.success(allRepos)
    }

    suspend fun createRepository(
        request: CreateRepoRequest,
        org: String? = null
    ): Result<GitHubRepoResponse> = withContext(Dispatchers.IO) {
        val endpoint = if (!org.isNullOrBlank()) {
            "https://api.github.com/orgs/$org/repos"
        } else {
            "https://api.github.com/user/repos"
        }

        val bodyString = json.encodeToString(request)
        val httpRequest = newRequestBuilder(endpoint)
            .post(bodyString.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(httpRequest).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.code == 201) {
                    val repo = json.decodeFromString<GitHubRepoResponse>(body)
                    Result.success(repo)
                } else {
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("GitHubApiService", "createRepository failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteRepository(owner: String, repo: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$owner/$repo"
        val request = newRequestBuilder(url).delete().build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 204 || response.code == 404) {
                    Result.success(true)
                } else {
                    val body = response.body?.string().orEmpty()
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("GitHubApiService", "deleteRepository failed", e)
            Result.failure(e)
        }
    }

    suspend fun getFileDetails(
        owner: String,
        repo: String,
        path: String,
        ref: String = "main"
    ): Result<FileContentResponse> = withContext(Dispatchers.IO) {
        val cleanPath = path.trimStart('/')
        val url = "https://api.github.com/repos/$owner/$repo/contents/$cleanPath?ref=$ref"
        val request = newRequestBuilder(url).get().build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val fileData = json.decodeFromString<FileContentResponse>(body)
                    Result.success(fileData)
                } else {
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("GitHubApiService", "getFileDetails failed for $path", e)
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateFile(
        owner: String,
        repo: String,
        path: String,
        contentString: String,
        message: String,
        branch: String = "main",
        sha: String? = null
    ): Result<FileContentResponse> = withContext(Dispatchers.IO) {
        val base64Content = Base64.encodeToString(contentString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val fileReq = FileContentRequest(
            message = message,
            content = base64Content,
            branch = branch,
            sha = sha
        )

        val cleanPath = path.trimStart('/')
        val url = "https://api.github.com/repos/$owner/$repo/contents/$cleanPath"
        val bodyString = json.encodeToString(fileReq)
        val request = newRequestBuilder(url)
            .put(bodyString.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.code == 200 || response.code == 201) {
                    val fileData = json.decodeFromString<FileContentResponse>(body)
                    Result.success(fileData)
                } else {
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("GitHubApiService", "createOrUpdateFile failed for $path", e)
            Result.failure(e)
        }
    }

    suspend fun deleteFile(
        owner: String,
        repo: String,
        path: String,
        sha: String,
        message: String,
        branch: String = "main"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanPath = path.trimStart('/')
        val deleteReq = DeleteFileRequest(message = message, sha = sha, branch = branch)
        val bodyString = json.encodeToString(deleteReq)
        val url = "https://api.github.com/repos/$owner/$repo/contents/$cleanPath"
        val request = newRequestBuilder(url)
            .delete(bodyString.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 200) {
                    Result.success(true)
                } else {
                    val body = response.body?.string().orEmpty()
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("GitHubApiService", "deleteFile failed for $path", e)
            Result.failure(e)
        }
    }

    suspend fun getGitTree(
        owner: String,
        repo: String,
        branch: String = "main",
        recursive: Boolean = true
    ): Result<GitTreeResponse> = withContext(Dispatchers.IO) {
        val recParam = if (recursive) "?recursive=1" else ""
        val url = "https://api.github.com/repos/$owner/$repo/git/trees/$branch$recParam"
        val request = newRequestBuilder(url).get().build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val tree = json.decodeFromString<GitTreeResponse>(body)
                    Result.success(tree)
                } else {
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("GitHubApiService", "getGitTree failed", e)
            Result.failure(e)
        }
    }

    suspend fun getCommitHistory(
        owner: String,
        repo: String,
        branch: String = "main",
        limit: Int = 30
    ): Result<List<GitHubCommitItem>> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$owner/$repo/commits?sha=$branch&per_page=$limit"
        val request = newRequestBuilder(url).get().build()

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
            CrashReportingHelper.logHandledException("GitHubApiService", "getCommitHistory failed", e)
            Result.failure(e)
        }
    }

    suspend fun listBranches(
        owner: String,
        repo: String
    ): Result<List<GitHubBranchItem>> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$owner/$repo/branches?per_page=50"
        val request = newRequestBuilder(url).get().build()

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
            CrashReportingHelper.logHandledException("GitHubApiService", "listBranches failed", e)
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
            CrashReportingHelper.logHandledException("GitHubApiService", "createBranch failed", e)
            Result.failure(e)
        }
    }

    suspend fun mergeBranch(
        owner: String,
        repo: String,
        base: String,
        head: String,
        commitMessage: String? = null
    ): Result<GitHubMergeResult> = withContext(Dispatchers.IO) {
        val req = GitHubMergeRequest(base = base, head = head, commitMessage = commitMessage)
        val bodyString = json.encodeToString(req)
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/merges")
            .post(bodyString.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful || response.code == 201) {
                    val mergeResult = json.decodeFromString<GitHubMergeResult>(body)
                    Result.success(mergeResult)
                } else {
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("GitHubApiService", "mergeBranch failed", e)
            Result.failure(e)
        }
    }

    suspend fun listPullRequests(
        owner: String,
        repo: String,
        state: String = "open"
    ): Result<List<GitHubPullRequest>> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$owner/$repo/pulls?state=$state&per_page=30"
        val request = newRequestBuilder(url).get().build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val prs = json.decodeFromString<List<GitHubPullRequest>>(body)
                    Result.success(prs)
                } else {
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("GitHubApiService", "listPullRequests failed", e)
            Result.failure(e)
        }
    }

    suspend fun createPullRequest(
        owner: String,
        repo: String,
        title: String,
        head: String,
        base: String,
        body: String? = null
    ): Result<GitHubPullRequest> = withContext(Dispatchers.IO) {
        val req = CreatePullRequestRequest(title = title, head = head, base = base, body = body)
        val bodyString = json.encodeToString(req)
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/pulls")
            .post(bodyString.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val resBody = response.body?.string().orEmpty()
                if (response.code == 201) {
                    val pr = json.decodeFromString<GitHubPullRequest>(resBody)
                    Result.success(pr)
                } else {
                    Result.failure(parseGitHubError(response.code, resBody))
                }
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("GitHubApiService", "createPullRequest failed", e)
            Result.failure(e)
        }
    }

    suspend fun listIssues(
        owner: String,
        repo: String,
        state: String = "open"
    ): Result<List<GitHubIssue>> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$owner/$repo/issues?state=$state&per_page=30"
        val request = newRequestBuilder(url).get().build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val issues = json.decodeFromString<List<GitHubIssue>>(body)
                    Result.success(issues)
                } else {
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("GitHubApiService", "listIssues failed", e)
            Result.failure(e)
        }
    }

    suspend fun createIssue(
        owner: String,
        repo: String,
        title: String,
        body: String? = null
    ): Result<GitHubIssue> = withContext(Dispatchers.IO) {
        val req = CreateIssueRequest(title = title, body = body)
        val bodyString = json.encodeToString(req)
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/issues")
            .post(bodyString.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val resBody = response.body?.string().orEmpty()
                if (response.code == 201) {
                    val issue = json.decodeFromString<GitHubIssue>(resBody)
                    Result.success(issue)
                } else {
                    Result.failure(parseGitHubError(response.code, resBody))
                }
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("GitHubApiService", "createIssue failed", e)
            Result.failure(e)
        }
    }

    suspend fun triggerWorkflow(
        owner: String,
        repo: String,
        workflowId: String = "build.yml",
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
            CrashReportingHelper.logHandledException("GitHubApiService", "triggerWorkflow failed", e)
            Result.failure(e)
        }
    }

    suspend fun listWorkflowRuns(
        owner: String,
        repo: String,
        workflowId: String = "build.yml"
    ): Result<List<WorkflowRun>> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$owner/$repo/actions/workflows/$workflowId/runs?per_page=20"
        val request = newRequestBuilder(url).get().build()

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
            CrashReportingHelper.logHandledException("GitHubApiService", "listWorkflowRuns failed", e)
            Result.failure(e)
        }
    }

    suspend fun getWorkflowRun(
        owner: String,
        repo: String,
        runId: Long
    ): Result<WorkflowRun> = withContext(Dispatchers.IO) {
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
            CrashReportingHelper.logHandledException("GitHubApiService", "getWorkflowRun failed", e)
            Result.failure(e)
        }
    }

    suspend fun getWorkflowRunJobs(
        owner: String,
        repo: String,
        runId: Long
    ): Result<List<WorkflowJob>> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("https://api.github.com/repos/$owner/$repo/actions/runs/$runId/jobs")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val jobsRes = json.decodeFromString<WorkflowJobsResponse>(body)
                    Result.success(jobsRes.jobs)
                } else {
                    Result.failure(parseGitHubError(response.code, body))
                }
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("GitHubApiService", "getWorkflowRunJobs failed", e)
            Result.failure(e)
        }
    }

    suspend fun getWorkflowRunLogs(
        owner: String,
        repo: String,
        runId: Long
    ): Result<String> = withContext(Dispatchers.IO) {
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
            CrashReportingHelper.logHandledException("GitHubApiService", "getWorkflowRunLogs failed", e)
            Result.failure(e)
        }
    }

    suspend fun listArtifacts(
        owner: String,
        repo: String,
        runId: Long
    ): Result<List<ArtifactItem>> = withContext(Dispatchers.IO) {
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
            CrashReportingHelper.logHandledException("GitHubApiService", "listArtifacts failed", e)
            Result.failure(e)
        }
    }

    suspend fun downloadArtifact(
        downloadUrl: String,
        destinationFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
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
            CrashReportingHelper.logHandledException("GitHubApiService", "downloadArtifact failed", e)
            Result.failure(e)
        }
    }
}
