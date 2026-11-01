package com.kotlinide.shahid.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val features: List<String>
) {
    companion object {
        val ALL = listOf(
            ProjectTemplate(
                id = "empty_compose",
                name = "Empty Compose Activity",
                description = "Modern Jetpack Compose application with Material 3 Theme and preview support.",
                iconName = "crop_square",
                features = listOf("Jetpack Compose", "Material 3", "Kotlin 2.0")
            ),
            ProjectTemplate(
                id = "empty_views",
                name = "Empty Views Activity",
                description = "Classic Android Views layout with activity_main.xml, ConstraintLayout, and Material 3 theme.",
                iconName = "view_quilt",
                features = listOf("XML Layouts", "View Binding", "ConstraintLayout")
            )
        )
    }
}
