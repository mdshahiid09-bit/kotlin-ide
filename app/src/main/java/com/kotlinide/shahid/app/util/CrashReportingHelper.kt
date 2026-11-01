package com.kotlinide.shahid.app.util

import android.content.Context
import com.kotlinide.shahid.app.data.storage.AppPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class CrashLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val tag: String = "CRASH",
    val message: String,
    val stackTrace: String,
    val isFatal: Boolean = false
)

object CrashReportingHelper {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private const val MAX_LOGS = 50
    private const val CRASH_LOG_FILE = "crash_logs_history.json"
    private var appContext: Context? = null
    private var preferences: AppPreferences? = null

    fun init(context: Context, appPreferences: AppPreferences? = null) {
        appContext = context.applicationContext
        preferences = appPreferences

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordCrash(
                    tag = "UNCAUGHT:${thread.name}",
                    message = throwable.message ?: "Uncaught fatal exception",
                    throwable = throwable,
                    isFatal = true
                )
            } catch (t: Throwable) {
                // Protect against recursive crash
            }

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    @Synchronized
    fun logHandledException(tag: String, message: String, throwable: Throwable? = null) {
        recordCrash(tag = tag, message = message, throwable = throwable, isFatal = false)
    }

    @Synchronized
    fun recordCrash(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        isFatal: Boolean = false
    ) {
        val ctx = appContext ?: return
        try {
            val sw = StringWriter()
            throwable?.printStackTrace(PrintWriter(sw))
            val rawTrace = sw.toString().ifBlank { message }
            val sanitizedTrace = sanitize(rawTrace)

            val entry = CrashLogEntry(
                tag = tag,
                message = sanitize(message),
                stackTrace = sanitizedTrace,
                isFatal = isFatal
            )

            val file = File(ctx.filesDir, CRASH_LOG_FILE)
            val currentLogs = if (file.exists()) {
                try {
                    json.decodeFromString<List<CrashLogEntry>>(file.readText()).toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }

            currentLogs.add(0, entry)
            val trimmed = currentLogs.take(MAX_LOGS)
            file.writeText(json.encodeToString(trimmed))
        } catch (t: Throwable) {
            // Never crash inside crash reporting
        }
    }

    @Synchronized
    fun getCrashLogs(context: Context): List<CrashLogEntry> {
        val file = File(context.filesDir, CRASH_LOG_FILE)
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString<List<CrashLogEntry>>(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun clearCrashLogs(context: Context): Boolean {
        val file = File(context.filesDir, CRASH_LOG_FILE)
        return try {
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            false
        }
    }

    fun sanitize(input: String): String {
        var clean = input
        // Redact Authorization headers and Bearer tokens first
        clean = clean.replace(Regex("(?i)authorization:\\s*Bearer\\s+[^\\s]+"), "Authorization: Bearer [REDACTED]")
        clean = clean.replace(Regex("(?i)authorization:\\s*token\\s+[^\\s]+"), "Authorization: token [REDACTED]")
        clean = clean.replace(Regex("(?i)\\bBearer\\s+[^\\s]+"), "Bearer [REDACTED]")
        // Redact standalone GitHub Classic PAT
        clean = clean.replace(Regex("ghp_[a-zA-Z0-9]{20,}"), "[REDACTED_PAT_TOKEN]")
        // Redact standalone GitHub Fine-grained PAT
        clean = clean.replace(Regex("github_pat_[a-zA-Z0-9_]{20,}"), "[REDACTED_PAT_TOKEN]")
        // Redact passwords and keystore credentials
        clean = clean.replace(Regex("(?i)password[=:]\\s*['\"]?[^\\s'\"]+"), "password=[REDACTED]")
        clean = clean.replace(Regex("(?i)keypass[=:]\\s*['\"]?[^\\s'\"]+"), "keypass=[REDACTED]")
        clean = clean.replace(Regex("(?i)storepass[=:]\\s*['\"]?[^\\s'\"]+"), "storepass=[REDACTED]")
        clean = clean.replace(Regex("(?i)keystore[=:]\\s*['\"]?[^\\s'\"]+"), "keystore=[REDACTED]")
        clean = clean.replace(Regex("(?i)alias[=:]\\s*['\"]?[^\\s'\"]+"), "alias=[REDACTED]")
        // Redact URL parameters containing sensitive tokens
        clean = clean.replace(Regex("(?i)(access_token|client_secret|token)=[^&\\s]+"), "$1=[REDACTED]")
        return clean
    }
}
