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
                id = "empty_activity",
                name = "Empty Activity",
                description = "Minimal Jetpack Compose screen with Material 3 Theme and simple greeting button.",
                iconName = "crop_square",
                features = listOf("Single Screen", "Material 3", "Clean Architecture")
            ),
            ProjectTemplate(
                id = "bottom_navigation",
                name = "Bottom Navigation",
                description = "Modern 3-tab layout (Home, Dashboard, Settings) with Scaffold and NavigationBar.",
                iconName = "view_quilt",
                features = listOf("3 Tabs", "Scaffold & TopBar", "Navigation Compose")
            ),
            ProjectTemplate(
                id = "login_home",
                name = "Login + Home",
                description = "Full authentication flow with Email/Password validation and interactive Dashboard.",
                iconName = "lock",
                features = listOf("Form Validation", "Animated Transitions", "Dashboard UI")
            )
        )
    }
}
