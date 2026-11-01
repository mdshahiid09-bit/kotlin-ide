package com.kotlinide.shahid.app.ui.navigation

sealed class Screen(val route: String) {
    object FirstRunGuide : Screen("first_run_guide")
    object Login : Screen("login")
    object ProjectList : Screen("project_list")
    object NewProjectWizard : Screen("new_project_wizard")
    object Editor : Screen("editor/{projectId}") {
        fun createRoute(projectId: String) = "editor/$projectId"
    }
    object BuildHistory : Screen("build_history/{projectId}") {
        fun createRoute(projectId: String) = "build_history/$projectId"
    }
    object Notes : Screen("notes/{projectId}") {
        fun createRoute(projectId: String) = "notes/$projectId"
    }
    object Settings : Screen("settings")
    object CrashLog : Screen("crash_log")
}
