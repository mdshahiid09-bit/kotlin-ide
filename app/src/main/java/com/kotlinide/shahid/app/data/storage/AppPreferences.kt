package com.kotlinide.shahid.app.data.storage

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    var isFirstRunComplete: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_RUN_COMPLETE, value).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()

    var isCrashlyticsOptIn: Boolean
        get() = prefs.getBoolean(KEY_CRASHLYTICS_OPT_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_CRASHLYTICS_OPT_IN, value).apply()

    var crashlyticsDialogAnswered: Boolean
        get() = prefs.getBoolean(KEY_CRASH_DIALOG_ANSWERED, false)
        set(value) = prefs.edit().putBoolean(KEY_CRASH_DIALOG_ANSWERED, value).apply()

    var githubUsername: String
        get() = prefs.getString(KEY_GITHUB_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GITHUB_USERNAME, value).apply()

    companion object {
        private const val KEY_FIRST_RUN_COMPLETE = "first_run_complete"
        private const val KEY_DARK_THEME = "dark_theme_enabled"
        private const val KEY_CRASHLYTICS_OPT_IN = "crashlytics_opt_in"
        private const val KEY_CRASH_DIALOG_ANSWERED = "crash_dialog_answered"
        private const val KEY_GITHUB_USERNAME = "github_username"
    }
}
