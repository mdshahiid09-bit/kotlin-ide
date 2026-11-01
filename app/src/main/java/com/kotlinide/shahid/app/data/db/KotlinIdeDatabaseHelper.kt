package com.kotlinide.shahid.app.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.kotlinide.shahid.app.data.api.GitHubRepoResponse
import com.kotlinide.shahid.app.data.api.GitHubUser
import com.kotlinide.shahid.app.data.model.BuildInfo
import com.kotlinide.shahid.app.data.model.Note
import com.kotlinide.shahid.app.data.model.Project
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class AuditLogEntry(
    val id: String,
    val timestamp: Long,
    val eventType: String,
    val details: String,
    val projectId: String? = null,
    val repoName: String? = null
)

class KotlinIdeDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private const val DATABASE_NAME = "kotlin_ide_local.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: KotlinIdeDatabaseHelper? = null

        fun getInstance(context: Context): KotlinIdeDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KotlinIdeDatabaseHelper(context).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE projects (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                package_name TEXT NOT NULL,
                version_name TEXT NOT NULL,
                version_code INTEGER NOT NULL,
                template_id TEXT NOT NULL,
                min_sdk INTEGER NOT NULL,
                target_sdk INTEGER NOT NULL,
                repo_name TEXT NOT NULL,
                repo_owner TEXT NOT NULL,
                repo_url TEXT NOT NULL,
                is_private INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                last_build_status TEXT NOT NULL,
                last_build_id INTEGER NOT NULL,
                has_keystore INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE build_records (
                id INTEGER PRIMARY KEY,
                project_id TEXT NOT NULL,
                run_number INTEGER NOT NULL,
                version_code INTEGER NOT NULL,
                version_name TEXT NOT NULL,
                status TEXT NOT NULL,
                conclusion TEXT,
                created_at INTEGER NOT NULL,
                duration_seconds INTEGER NOT NULL,
                apk_download_url TEXT,
                aab_download_url TEXT,
                apk_local_path TEXT,
                aab_local_path TEXT,
                raw_log TEXT,
                simple_error_message TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE notes (
                id TEXT PRIMARY KEY,
                project_id TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE audit_logs (
                id TEXT PRIMARY KEY,
                timestamp INTEGER NOT NULL,
                event_type TEXT NOT NULL,
                details TEXT NOT NULL,
                project_id TEXT,
                repo_name TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE cached_repos (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                full_name TEXT NOT NULL,
                is_private INTEGER NOT NULL,
                html_url TEXT NOT NULL,
                clone_url TEXT NOT NULL,
                default_branch TEXT NOT NULL,
                owner_login TEXT NOT NULL,
                owner_avatar TEXT NOT NULL,
                last_synced INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS projects")
        db.execSQL("DROP TABLE IF EXISTS build_records")
        db.execSQL("DROP TABLE IF EXISTS notes")
        db.execSQL("DROP TABLE IF EXISTS audit_logs")
        db.execSQL("DROP TABLE IF EXISTS cached_repos")
        onCreate(db)
    }

    // Projects CRUD
    @Synchronized
    fun getProjects(): List<Project> {
        val list = mutableListOf<Project>()
        val db = readableDatabase
        val cursor = db.query(
            "projects",
            null,
            null,
            null,
            null,
            null,
            "updated_at DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToProject(it))
            }
        }
        return list
    }

    @Synchronized
    fun getProject(id: String): Project? {
        val db = readableDatabase
        val cursor = db.query(
            "projects",
            null,
            "id = ?",
            arrayOf(id),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) cursorToProject(it) else null
        }
    }

    @Synchronized
    fun saveProject(project: Project) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", project.id)
            put("name", project.name)
            put("package_name", project.packageName)
            put("version_name", project.versionName)
            put("version_code", project.versionCode)
            put("template_id", project.templateId)
            put("min_sdk", project.minSdk)
            put("target_sdk", project.targetSdk)
            put("repo_name", project.repoName)
            put("repo_owner", project.repoOwner)
            put("repo_url", project.repoUrl)
            put("is_private", if (project.isPrivate) 1 else 0)
            put("created_at", project.createdAt)
            put("updated_at", project.updatedAt)
            put("last_build_status", project.lastBuildStatus)
            put("last_build_id", project.lastBuildId)
            put("has_keystore", if (project.hasKeystore) 1 else 0)
        }
        db.insertWithOnConflict("projects", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized
    fun deleteProject(id: String) {
        val db = writableDatabase
        db.delete("projects", "id = ?", arrayOf(id))
        deleteBuildsForProject(id)
        deleteNotesForProject(id)
    }

    private fun cursorToProject(cursor: Cursor): Project {
        return Project(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            packageName = cursor.getString(cursor.getColumnIndexOrThrow("package_name")),
            versionName = cursor.getString(cursor.getColumnIndexOrThrow("version_name")),
            versionCode = cursor.getInt(cursor.getColumnIndexOrThrow("version_code")),
            templateId = cursor.getString(cursor.getColumnIndexOrThrow("template_id")),
            minSdk = cursor.getInt(cursor.getColumnIndexOrThrow("min_sdk")),
            targetSdk = cursor.getInt(cursor.getColumnIndexOrThrow("target_sdk")),
            repoName = cursor.getString(cursor.getColumnIndexOrThrow("repo_name")),
            repoOwner = cursor.getString(cursor.getColumnIndexOrThrow("repo_owner")),
            repoUrl = cursor.getString(cursor.getColumnIndexOrThrow("repo_url")),
            isPrivate = cursor.getInt(cursor.getColumnIndexOrThrow("is_private")) == 1,
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
            lastBuildStatus = cursor.getString(cursor.getColumnIndexOrThrow("last_build_status")),
            lastBuildId = cursor.getLong(cursor.getColumnIndexOrThrow("last_build_id")),
            hasKeystore = cursor.getInt(cursor.getColumnIndexOrThrow("has_keystore")) == 1
        )
    }

    // Build Records CRUD
    @Synchronized
    fun getBuilds(projectId: String): List<BuildInfo> {
        val list = mutableListOf<BuildInfo>()
        val db = readableDatabase
        val cursor = db.query(
            "build_records",
            null,
            "project_id = ?",
            arrayOf(projectId),
            null,
            null,
            "created_at DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToBuild(it))
            }
        }
        return list
    }

    @Synchronized
    fun saveBuild(build: BuildInfo) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", build.id)
            put("project_id", build.projectId)
            put("run_number", build.runNumber)
            put("version_code", build.versionCode)
            put("version_name", build.versionName)
            put("status", build.status)
            put("conclusion", build.conclusion)
            put("created_at", build.createdAt)
            put("duration_seconds", build.durationSeconds)
            put("apk_download_url", build.apkDownloadUrl)
            put("aab_download_url", build.aabDownloadUrl)
            put("apk_local_path", build.apkLocalPath)
            put("aab_local_path", build.aabLocalPath)
            put("raw_log", build.rawLog)
            put("simple_error_message", build.simpleErrorMessage)
        }
        db.insertWithOnConflict("build_records", null, values, SQLiteDatabase.CONFLICT_REPLACE)

        // Keep maximum 10 builds per project to conserve phone storage
        val allProjectBuilds = getBuilds(build.projectId)
        if (allProjectBuilds.size > 10) {
            val toDelete = allProjectBuilds.drop(10)
            for (b in toDelete) {
                db.delete("build_records", "id = ?", arrayOf(b.id.toString()))
            }
        }
    }

    @Synchronized
    fun deleteBuildsForProject(projectId: String) {
        val db = writableDatabase
        db.delete("build_records", "project_id = ?", arrayOf(projectId))
    }

    @Synchronized
    fun cleanOldBuildLogs() {
        val db = writableDatabase
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
        val values = ContentValues().apply {
            put("raw_log", "")
        }
        db.update("build_records", values, "created_at < ?", arrayOf(thirtyDaysAgo.toString()))
    }

    private fun cursorToBuild(cursor: Cursor): BuildInfo {
        return BuildInfo(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            projectId = cursor.getString(cursor.getColumnIndexOrThrow("project_id")),
            runNumber = cursor.getInt(cursor.getColumnIndexOrThrow("run_number")),
            versionCode = cursor.getInt(cursor.getColumnIndexOrThrow("version_code")),
            versionName = cursor.getString(cursor.getColumnIndexOrThrow("version_name")),
            status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
            conclusion = cursor.getString(cursor.getColumnIndexOrThrow("conclusion")),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
            durationSeconds = cursor.getLong(cursor.getColumnIndexOrThrow("duration_seconds")),
            apkDownloadUrl = cursor.getString(cursor.getColumnIndexOrThrow("apk_download_url")),
            aabDownloadUrl = cursor.getString(cursor.getColumnIndexOrThrow("aab_download_url")),
            apkLocalPath = cursor.getString(cursor.getColumnIndexOrThrow("apk_local_path")),
            aabLocalPath = cursor.getString(cursor.getColumnIndexOrThrow("aab_local_path")),
            rawLog = cursor.getString(cursor.getColumnIndexOrThrow("raw_log")).orEmpty(),
            simpleErrorMessage = cursor.getString(cursor.getColumnIndexOrThrow("simple_error_message"))
        )
    }

    // Notes CRUD
    @Synchronized
    fun getNotes(projectId: String): List<Note> {
        val list = mutableListOf<Note>()
        val db = readableDatabase
        val cursor = db.query(
            "notes",
            null,
            "project_id = ?",
            arrayOf(projectId),
            null,
            null,
            "updated_at DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Note(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        projectId = it.getString(it.getColumnIndexOrThrow("project_id")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        content = it.getString(it.getColumnIndexOrThrow("content")),
                        updatedAt = it.getLong(it.getColumnIndexOrThrow("updated_at"))
                    )
                )
            }
        }
        return list
    }

    @Synchronized
    fun saveNote(note: Note) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", note.id)
            put("project_id", note.projectId)
            put("title", note.title)
            put("content", note.content)
            put("updated_at", note.updatedAt)
        }
        db.insertWithOnConflict("notes", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized
    fun deleteNote(noteId: String) {
        val db = writableDatabase
        db.delete("notes", "id = ?", arrayOf(noteId))
    }

    @Synchronized
    fun deleteNotesForProject(projectId: String) {
        val db = writableDatabase
        db.delete("notes", "project_id = ?", arrayOf(projectId))
    }

    // Audit Logging
    @Synchronized
    fun insertAuditLog(eventType: String, details: String, projectId: String? = null, repoName: String? = null) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", java.util.UUID.randomUUID().toString())
            put("timestamp", System.currentTimeMillis())
            put("event_type", eventType)
            put("details", details)
            put("project_id", projectId)
            put("repo_name", repoName)
        }
        db.insert("audit_logs", null, values)

        // Retain last 500 audit entries
        db.execSQL(
            """
            DELETE FROM audit_logs WHERE id NOT IN (
                SELECT id FROM audit_logs ORDER BY timestamp DESC LIMIT 500
            )
            """.trimIndent()
        )
    }

    @Synchronized
    fun getAuditLogs(limit: Int = 100): List<AuditLogEntry> {
        val list = mutableListOf<AuditLogEntry>()
        val db = readableDatabase
        val cursor = db.query(
            "audit_logs",
            null,
            null,
            null,
            null,
            null,
            "timestamp DESC",
            limit.toString()
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    AuditLogEntry(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        eventType = it.getString(it.getColumnIndexOrThrow("event_type")),
                        details = it.getString(it.getColumnIndexOrThrow("details")),
                        projectId = it.getString(it.getColumnIndexOrThrow("project_id")),
                        repoName = it.getString(it.getColumnIndexOrThrow("repo_name"))
                    )
                )
            }
        }
        return list
    }

    // Repository Cache
    @Synchronized
    fun cacheRepositories(repos: List<GitHubRepoResponse>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("cached_repos", null, null)
            val now = System.currentTimeMillis()
            for (repo in repos) {
                val values = ContentValues().apply {
                    put("id", repo.id)
                    put("name", repo.name)
                    put("full_name", repo.fullName)
                    put("is_private", if (repo.private) 1 else 0)
                    put("html_url", repo.htmlUrl)
                    put("clone_url", repo.cloneUrl)
                    put("default_branch", repo.defaultBranch)
                    put("owner_login", repo.owner.login)
                    put("owner_avatar", repo.owner.avatarUrl)
                    put("last_synced", now)
                }
                db.insertWithOnConflict("cached_repos", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun getCachedRepositories(): List<GitHubRepoResponse> {
        val list = mutableListOf<GitHubRepoResponse>()
        val db = readableDatabase
        val cursor = db.query("cached_repos", null, null, null, null, null, "name ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    GitHubRepoResponse(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        fullName = it.getString(it.getColumnIndexOrThrow("full_name")),
                        private = it.getInt(it.getColumnIndexOrThrow("is_private")) == 1,
                        htmlUrl = it.getString(it.getColumnIndexOrThrow("html_url")),
                        cloneUrl = it.getString(it.getColumnIndexOrThrow("clone_url")),
                        defaultBranch = it.getString(it.getColumnIndexOrThrow("default_branch")),
                        owner = GitHubUser(
                            login = it.getString(it.getColumnIndexOrThrow("owner_login")),
                            id = 0L,
                            avatarUrl = it.getString(it.getColumnIndexOrThrow("owner_avatar"))
                        )
                    )
                )
            }
        }
        return list
    }
}
