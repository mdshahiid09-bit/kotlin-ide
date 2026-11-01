package com.kotlinide.shahid.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String,
    val name: String,
    val packageName: String,
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val templateId: String = "empty_activity",
    val minSdk: Int = 26,
    val targetSdk: Int = 35,
    val repoName: String,
    val repoOwner: String = "",
    val repoUrl: String = "",
    val isPrivate: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastBuildStatus: String = "NONE",
    val lastBuildId: Long = 0L,
    val hasKeystore: Boolean = false
)
