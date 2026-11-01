package com.kotlinide.shahid.app.data.storage

import android.content.Context
import com.kotlinide.shahid.app.data.db.KotlinIdeDatabaseHelper
import com.kotlinide.shahid.app.data.model.BuildInfo
import com.kotlinide.shahid.app.data.model.Note
import com.kotlinide.shahid.app.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProjectStorage(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val dbHelper = KotlinIdeDatabaseHelper.getInstance(context)

    private val projectsFile = File(context.filesDir, "projects.json")
    private val buildsFile = File(context.filesDir, "builds.json")
    private val notesFile = File(context.filesDir, "notes.json")

    private val projectsDir = File(context.filesDir, "projects_data").apply { mkdirs() }

    init {
        migrateLegacyJsonToDb()
    }

    private fun migrateLegacyJsonToDb() {
        try {
            if (projectsFile.exists()) {
                val pList = json.decodeFromString<List<Project>>(projectsFile.readText())
                pList.forEach { dbHelper.saveProject(it) }
            }
            if (buildsFile.exists()) {
                val bList = json.decodeFromString<List<BuildInfo>>(buildsFile.readText())
                bList.forEach { dbHelper.saveBuild(it) }
            }
            if (notesFile.exists()) {
                val nList = json.decodeFromString<List<Note>>(notesFile.readText())
                nList.forEach { dbHelper.saveNote(it) }
            }
        } catch (e: Exception) {
            // Ignore migration error
        }
    }

    suspend fun getProjects(): List<Project> = withContext(Dispatchers.IO) {
        val dbProjects = dbHelper.getProjects()
        if (dbProjects.isNotEmpty()) {
            return@withContext dbProjects
        }
        if (!projectsFile.exists()) return@withContext emptyList()
        try {
            val content = projectsFile.readText()
            val list = json.decodeFromString<List<Project>>(content)
            list.forEach { dbHelper.saveProject(it) }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        dbHelper.saveProject(project)
        val current = dbHelper.getProjects()
        try {
            projectsFile.writeText(json.encodeToString(current))
        } catch (e: Exception) {}
    }

    suspend fun getProject(projectId: String): Project? = withContext(Dispatchers.IO) {
        dbHelper.getProject(projectId)
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        dbHelper.deleteProject(projectId)
        val current = dbHelper.getProjects()
        try {
            projectsFile.writeText(json.encodeToString(current))
        } catch (e: Exception) {}

        val dir = getProjectDirectory(projectId)
        if (dir.exists()) {
            dir.deleteRecursively()
        }

        deleteBuildsForProject(projectId)
        deleteNotesForProject(projectId)
    }

    fun getProjectDirectory(projectId: String): File {
        return File(projectsDir, projectId).apply { mkdirs() }
    }

    suspend fun getBuilds(projectId: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        cleanOldBuildLogs()
        dbHelper.getBuilds(projectId)
    }

    suspend fun saveBuild(build: BuildInfo) = withContext(Dispatchers.IO) {
        dbHelper.saveBuild(build)
        try {
            val allBuilds = dbHelper.getBuilds(build.projectId)
            buildsFile.writeText(json.encodeToString(allBuilds))
        } catch (e: Exception) {}
    }

    private suspend fun deleteBuildsForProject(projectId: String) = withContext(Dispatchers.IO) {
        dbHelper.deleteBuildsForProject(projectId)
    }

    private suspend fun cleanOldBuildLogs() = withContext(Dispatchers.IO) {
        dbHelper.cleanOldBuildLogs()
    }

    suspend fun getNotes(projectId: String): List<Note> = withContext(Dispatchers.IO) {
        dbHelper.getNotes(projectId)
    }

    suspend fun saveNote(note: Note) = withContext(Dispatchers.IO) {
        dbHelper.saveNote(note)
    }

    suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        dbHelper.deleteNote(noteId)
    }

    private suspend fun deleteNotesForProject(projectId: String) = withContext(Dispatchers.IO) {
        dbHelper.deleteNotesForProject(projectId)
    }

    suspend fun exportProjectToZip(projectId: String, targetZipFile: File): Boolean = withContext(Dispatchers.IO) {
        val sourceDir = getProjectDirectory(projectId)
        if (!sourceDir.exists()) return@withContext false

        try {
            ZipOutputStream(FileOutputStream(targetZipFile)).use { zos ->
                sourceDir.walkTopDown().forEach { file ->
                    val relativePath = file.relativeTo(sourceDir).path
                    if (file.isDirectory) {
                        if (relativePath.isNotEmpty()) {
                            val entry = ZipEntry("$relativePath/")
                            zos.putNextEntry(entry)
                            zos.closeEntry()
                        }
                    } else {
                        val entry = ZipEntry(relativePath)
                        zos.putNextEntry(entry)
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
