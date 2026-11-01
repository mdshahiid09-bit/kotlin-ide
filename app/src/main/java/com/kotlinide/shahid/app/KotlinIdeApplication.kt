package com.kotlinide.shahid.app

import android.app.Application
import com.kotlinide.shahid.app.data.security.TokenStorage
import com.kotlinide.shahid.app.data.storage.AppPreferences
import com.kotlinide.shahid.app.data.storage.ProjectStorage
import com.kotlinide.shahid.app.util.CrashReportingHelper
import com.kotlinide.shahid.app.util.NotificationHelper

class KotlinIdeApplication : Application() {

    lateinit var tokenStorage: TokenStorage
        private set

    lateinit var appPreferences: AppPreferences
        private set

    lateinit var projectStorage: ProjectStorage
        private set

    override fun onCreate() {
        super.onCreate()

        tokenStorage = TokenStorage(this)
        appPreferences = AppPreferences(this)
        projectStorage = ProjectStorage(this)

        NotificationHelper.initNotificationChannel(this)
        CrashReportingHelper.init(this, appPreferences)
    }
}
