package com.kotlinide.shahid.app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class TokenType {
    CLASSIC,
    FINE_GRAINED,
    UNKNOWN
}

data class TokenValidationInfo(
    val user: GitHubUser,
    val tokenType: TokenType,
    val scopes: List<String> = emptyList(),
    val hasRepoScope: Boolean = false,
    val hasWorkflowScope: Boolean = false,
    val scopeWarning: String? = null,
    val canCreateRepositories: Boolean? = null
)

class GitHubApiException(
    val statusCode: Int,
    val userFriendlyMessage: String,
    val rawResponse: String? = null,
    val gitHubErrorMessage: String? = null
) : Exception(userFriendlyMessage)

@Serializable
data class GitHubErrorResponse(
    val message: String = "",
    val errors: List<GitHubErrorItem> = emptyList(),
    @SerialName("documentation_url") val documentationUrl: String = ""
)

@Serializable
data class GitHubErrorItem(
    val resource: String = "",
    val field: String = "",
    val code: String = "",
    val message: String = ""
)

@Serializable
data class GitHubUser(
    val login: String,
    val id: Long,
    @SerialName("avatar_url") val avatarUrl: String = "",
    val name: String? = null,
    val email: String? = null,
    @SerialName("html_url") val htmlUrl: String = ""
)

@Serializable
data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("expires_in") val expiresIn: Int = 900,
    val interval: Int = 5
)

@Serializable
data class OAuthAccessTokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    val scope: String? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null
)

@Serializable
data class CreateRepoRequest(
    val name: String,
    val description: String = "",
    val private: Boolean = true,
    @SerialName("auto_init") val autoInit: Boolean = true
)

@Serializable
data class GitHubRepoResponse(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val private: Boolean,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("clone_url") val cloneUrl: String = "",
    @SerialName("default_branch") val defaultBranch: String = "main",
    val owner: GitHubUser,
    val permissions: GitHubPermissions? = null
)

@Serializable
data class GitHubPermissions(
    val admin: Boolean = false,
    val push: Boolean = false,
    val pull: Boolean = true
)

@Serializable
data class FileContentRequest(
    val message: String,
    val content: String,
    val branch: String = "main",
    val sha: String? = null
)

@Serializable
data class DeleteFileRequest(
    val message: String,
    val sha: String,
    val branch: String = "main"
)

@Serializable
data class FileContentResponse(
    val name: String = "",
    val path: String = "",
    val sha: String = "",
    val size: Long = 0,
    @SerialName("download_url") val downloadUrl: String? = null,
    val content: String? = null,
    val encoding: String? = null
)

@Serializable
data class GitTreeItem(
    val path: String,
    val mode: String,
    val type: String,
    val sha: String,
    val size: Long = 0
)

@Serializable
data class GitTreeResponse(
    val sha: String,
    val tree: List<GitTreeItem> = emptyList(),
    val truncated: Boolean = false
)

@Serializable
data class GitHubCommitItem(
    val sha: String,
    val commit: GitHubCommitData,
    val author: GitHubUser? = null,
    @SerialName("html_url") val htmlUrl: String = ""
)

@Serializable
data class GitHubCommitData(
    val message: String,
    val author: GitHubCommitAuthor? = null
)

@Serializable
data class GitHubCommitAuthor(
    val name: String = "",
    val email: String = "",
    val date: String = ""
)

@Serializable
data class GitHubBranchItem(
    val name: String,
    val commit: GitHubBranchCommit
)

@Serializable
data class GitHubBranchCommit(
    val sha: String
)

@Serializable
data class CreateGitRefRequest(
    val ref: String,
    val sha: String
)

@Serializable
data class GitHubMergeRequest(
    val base: String,
    val head: String,
    @SerialName("commit_message") val commitMessage: String? = null
)

@Serializable
data class GitHubMergeResult(
    val sha: String = "",
    val message: String = ""
)

@Serializable
data class GitHubPullRequest(
    val id: Long,
    val number: Int,
    val title: String,
    val state: String = "open",
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    val user: GitHubUser? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class CreatePullRequestRequest(
    val title: String,
    val head: String,
    val base: String,
    val body: String? = null
)

@Serializable
data class GitHubIssue(
    val id: Long,
    val number: Int,
    val title: String,
    val state: String = "open",
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    val user: GitHubUser? = null,
    @SerialName("created_at") val createdAt: String = "",
    val comments: Int = 0
)

@Serializable
data class CreateIssueRequest(
    val title: String,
    val body: String? = null
)

@Serializable
data class WorkflowDispatchRequest(
    val ref: String = "main",
    val inputs: Map<String, String> = emptyMap()
)

@Serializable
data class WorkflowRunsResponse(
    @SerialName("total_count") val totalCount: Int,
    @SerialName("workflow_runs") val workflowRuns: List<WorkflowRun> = emptyList()
)

@Serializable
data class WorkflowRun(
    val id: Long,
    val name: String = "",
    @SerialName("run_number") val runNumber: Int,
    val status: String,
    val conclusion: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class WorkflowJobsResponse(
    @SerialName("total_count") val totalCount: Int = 0,
    val jobs: List<WorkflowJob> = emptyList()
)

@Serializable
data class WorkflowJob(
    val id: Long,
    @SerialName("run_id") val runId: Long,
    val name: String,
    val status: String,
    val conclusion: String? = null,
    val steps: List<WorkflowJobStep> = emptyList()
)

@Serializable
data class WorkflowJobStep(
    val name: String,
    val status: String,
    val conclusion: String? = null,
    val number: Int
)

@Serializable
data class ArtifactsResponse(
    @SerialName("total_count") val totalCount: Int,
    val artifacts: List<ArtifactItem> = emptyList()
)

@Serializable
data class ArtifactItem(
    val id: Long,
    val name: String,
    val size_in_bytes: Long = 0,
    @SerialName("archive_download_url") val archiveDownloadUrl: String,
    val expired: Boolean = false
)

@Serializable
data class RepoPublicKeyResponse(
    @SerialName("key_id") val keyId: String,
    val key: String
)

@Serializable
data class SetSecretRequest(
    @SerialName("encrypted_value") val encryptedValue: String,
    @SerialName("key_id") val keyId: String
)

@Serializable
enum class SyncActionType {
    CREATE_OR_UPDATE,
    DELETE,
    RENAME
}

@Serializable
data class QueuedSyncAction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val projectId: String,
    val owner: String,
    val repo: String,
    val actionType: SyncActionType,
    val path: String,
    val content: String? = null,
    val oldPath: String? = null,
    val sha: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
