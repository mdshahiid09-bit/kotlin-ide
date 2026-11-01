package com.kotlinide.shahid.app.util

import android.content.Context
import com.kotlinide.shahid.app.data.db.AuditLogEntry
import com.kotlinide.shahid.app.data.db.KotlinIdeDatabaseHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AuditLogger {

    fun log(context: Context, eventType: String, details: String, projectId: String? = null, repoName: String? = null) {
        try {
            KotlinIdeDatabaseHelper.getInstance(context).insertAuditLog(
                eventType = eventType,
                details = details,
                projectId = projectId,
                repoName = repoName
            )
        } catch (e: Exception) {
            // Non-fatal logging failure
        }
    }

    fun getLogs(context: Context, limit: Int = 100): List<AuditLogEntry> {
        return try {
            KotlinIdeDatabaseHelper.getInstance(context).getAuditLogs(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun exportAsCsv(context: Context): File? {
        return try {
            val logs = getLogs(context, 500)
            val dir = File(context.getExternalFilesDir(null), "AuditLogs").apply { mkdirs() }
            val file = File(dir, "audit_log_${System.currentTimeMillis()}.csv")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val builder = StringBuilder("id,timestamp_iso,event_type,details,project_id,repo_name\n")
            for (item in logs) {
                val dateStr = dateFormat.format(Date(item.timestamp))
                val safeDetails = item.details.replace("\"", "\"\"")
                builder.append("\"${item.id}\",\"$dateStr\",\"${item.eventType}\",\"$safeDetails\",\"${item.projectId.orEmpty()}\",\"${item.repoName.orEmpty()}\"\n")
            }
            file.writeText(builder.toString())
            file
        } catch (e: Exception) {
            null
        }
    }
}
