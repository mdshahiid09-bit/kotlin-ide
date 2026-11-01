package com.kotlinide.shahid.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kotlinide.shahid.app.data.security.TokenStorage
import com.kotlinide.shahid.app.data.storage.AppPreferences
import com.kotlinide.shahid.app.data.storage.DraftStorage
import com.kotlinide.shahid.app.data.storage.ProjectStorage
import com.kotlinide.shahid.app.data.sync.SyncQueueManager
import com.kotlinide.shahid.app.ui.screens.build.BuildHistoryScreen
import com.kotlinide.shahid.app.ui.screens.editor.EditorScreen
import com.kotlinide.shahid.app.ui.screens.guide.FirstRunGuideScreen
import com.kotlinide.shahid.app.ui.screens.login.LoginScreen
import com.kotlinide.shahid.app.ui.screens.notes.NotesScreen
import com.kotlinide.shahid.app.ui.screens.projects.ProjectListScreen
import com.kotlinide.shahid.app.ui.screens.projects.wizard.NewProjectWizardScreen
import com.kotlinide.shahid.app.ui.screens.settings.CrashLogScreen
import com.kotlinide.shahid.app.ui.screens.settings.SettingsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    tokenStorage: TokenStorage,
    appPreferences: AppPreferences,
    projectStorage: ProjectStorage,
    draftStorage: DraftStorage,
    syncQueueManager: SyncQueueManager,
    onThemeToggle: (Boolean) -> Unit
) {
    val startDestination = when {
        !appPreferences.isFirstRunComplete -> Screen.FirstRunGuide.route
        !tokenStorage.hasToken() -> Screen.Login.route
        else -> Screen.ProjectList.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.FirstRunGuide.route) {
            FirstRunGuideScreen(
                tokenStorage = tokenStorage,
                appPreferences = appPreferences,
                onCompleteGuide = {
                    navController.navigate(Screen.NewProjectWizard.route) {
                        popUpTo(Screen.FirstRunGuide.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                tokenStorage = tokenStorage,
                appPreferences = appPreferences,
                onLoginSuccess = {
                    navController.navigate(Screen.ProjectList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ProjectList.route) {
            ProjectListScreen(
                projectStorage = projectStorage,
                tokenStorage = tokenStorage,
                onOpenProject = { projectId ->
                    navController.navigate(Screen.Editor.createRoute(projectId))
                },
                onCreateNewProject = {
                    navController.navigate(Screen.NewProjectWizard.route)
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onOpenNotes = { projectId ->
                    navController.navigate(Screen.Notes.createRoute(projectId))
                }
            )
        }

        composable(Screen.NewProjectWizard.route) {
            NewProjectWizardScreen(
                projectStorage = projectStorage,
                tokenStorage = tokenStorage,
                appPreferences = appPreferences,
                onBack = { navController.popBackStack() },
                onProjectCreated = { projectId ->
                    navController.navigate(Screen.Editor.createRoute(projectId)) {
                        popUpTo(Screen.NewProjectWizard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            EditorScreen(
                projectId = projectId,
                projectStorage = projectStorage,
                tokenStorage = tokenStorage,
                draftStorage = draftStorage,
                syncQueueManager = syncQueueManager,
                onBack = { navController.popBackStack() },
                onOpenBuildHistory = { pId ->
                    navController.navigate(Screen.BuildHistory.createRoute(pId))
                }
            )
        }

        composable(
            route = Screen.BuildHistory.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            BuildHistoryScreen(
                projectId = projectId,
                projectStorage = projectStorage,
                tokenStorage = tokenStorage,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Notes.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            NotesScreen(
                projectId = projectId,
                projectStorage = projectStorage,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                tokenStorage = tokenStorage,
                appPreferences = appPreferences,
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ProjectList.route) { inclusive = true }
                    }
                },
                onThemeToggle = onThemeToggle,
                onOpenCrashLog = {
                    navController.navigate(Screen.CrashLog.route)
                }
            )
        }

        composable(Screen.CrashLog.route) {
            CrashLogScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
