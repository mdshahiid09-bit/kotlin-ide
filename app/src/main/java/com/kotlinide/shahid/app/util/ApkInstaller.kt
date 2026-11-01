package com.kotlinide.shahid.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object ApkInstaller {

    fun extractApkFromZip(zipFile: File, outputDir: File): File? {
        outputDir.mkdirs()
        var extractedApk: File? = null
        try {
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                        val apkName = entry.name.substringAfterLast('/')
                        val outFile = File(outputDir, apkName)
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        extractedApk = outFile
                        break
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("ApkInstaller", "Failed to extract APK from ZIP", e)
        }
        return extractedApk
    }

    fun canInstallApks(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun installApk(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                return Result.failure(IllegalArgumentException("APK file is missing or empty."))
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AuditLogger.log(
                context = context,
                eventType = "APK_INSTALLED",
                details = "User launched package installer for: ${apkFile.name}"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("ApkInstaller", "Failed to launch package installer", e)
            Result.failure(e)
        }
    }
}
