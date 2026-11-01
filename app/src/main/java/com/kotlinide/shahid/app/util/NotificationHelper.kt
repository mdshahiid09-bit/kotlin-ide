package com.kotlinide.shahid.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kotlinide.shahid.app.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "build_complete_channel"
    private const val CHANNEL_NAME = "Build Notifications"
    private const val CHANNEL_DESC = "Notifications for completed GitHub Actions builds"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showBuildNotification(
        context: Context,
        projectName: String,
        isSuccess: Boolean,
        buildId: Long
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_BUILD_ID", buildId)
            putExtra("EXTRA_PROJECT_NAME", projectName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            buildId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isSuccess) "Build Succeeded: $projectName" else "Build Failed: $projectName"
        val message = if (isSuccess) {
            "APK and AAB artifacts are ready for download."
        } else {
            "Build failed on GitHub Actions. Tap to view Gradle error log."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(buildId.toInt(), builder.build())
        } catch (_: SecurityException) {
        }
    }
}
