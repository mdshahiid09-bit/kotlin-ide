package com.kotlinide.shahid.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val projectId: String,
    val title: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis()
)
