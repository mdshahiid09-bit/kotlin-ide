package com.kotlinide.shahid.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BuildInfo(
    val id: Long,
    val projectId: String,
    val runNumber: Int,
    val versionCode: Int,
    val versionName: String,
    val status: String,
    val conclusion: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0L,
    val apkDownloadUrl: String? = null,
    val aabDownloadUrl: String? = null,
    val apkLocalPath: String? = null,
    val aabLocalPath: String? = null,
    val rawLog: String = "",
    val simpleErrorMessage: String? = null,
    val errors: List<BuildErrorLine> = emptyList()
)

@Serializable
data class BuildErrorLine(
    val filePath: String,
    val fileName: String,
    val lineNumber: Int,
    val columnNumber: Int = 1,
    val message: String
)
