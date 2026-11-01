package com.kotlinide.shahid.app.util

import android.content.Context
import com.kotlinide.shahid.app.data.storage.AppPreferences
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

object CrashReportingHelper {

    fun init(context: Context, appPreferences: AppPreferences) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (appPreferences.isCrashlyticsOptIn) {
                try {
                    val sanitized = sanitizeCrashReport(throwable)
                    val crashFile = File(context.filesDir, "last_crash.log")
                    crashFile.writeText(sanitized)
                } catch (_: Exception) {}
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun sanitizeCrashReport(throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        var rawTrace = sw.toString()

        rawTrace = rawTrace.replace(Regex("ghp_[a-zA-Z0-9]{30,}"), "[REDACTED_GITHUB_TOKEN]")
        rawTrace = rawTrace.replace(Regex("github_pat_[a-zA-Z0-9_]{30,}"), "[REDACTED_GITHUB_TOKEN]")
        rawTrace = rawTrace.replace(Regex("(?i)password[=:]\\s*['\"]?[^\\s'\"]+"), "password=[REDACTED]")
        rawTrace = rawTrace.replace(Regex("(?i)keystore[=:]\\s*['\"]?[^\\s'\"]+"), "keystore=[REDACTED]")

        return "Timestamp: ${System.currentTimeMillis()}\nThread: ${Thread.currentThread().name}\nTrace:\n$rawTrace"
    }

    fun getLastCrashReport(context: Context): String? {
        val crashFile = File(context.filesDir, "last_crash.log")
        return if (crashFile.exists()) {
            val text = crashFile.readText()
            crashFile.delete()
            text
        } else null
    }
}
