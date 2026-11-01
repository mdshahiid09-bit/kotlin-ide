package com.kotlinide.shahid.app.data.storage

import android.content.Context
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

    private val projectsFile = File(context.filesDir, "projects.json")
    private val buildsFile = File(context.filesDir, "builds.json")
    private val notesFile = File(context.filesDir, "notes.json")

    private val projectsDir = File(context.filesDir, "projects_data").apply { mkdirs() }

    suspend fun getProjects(): List<Project> = withContext(Dispatchers.IO) {
        if (!projectsFile.exists()) return@withContext emptyList()
        try {
            val content = projectsFile.readText()
            json.decodeFromString<List<Project>>(content)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        val current = getProjects().toMutableList()
        val index = current.indexOfFirst { it.id == project.id }
        if (index >= 0) {
            current[index] = project
        } else {
            current.add(0, project)
        }
        projectsFile.writeText(json.encodeToString(current))
    }

    suspend fun getProject(projectId: String): Project? = withContext(Dispatchers.IO) {
        getProjects().find { it.id == projectId }
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        val current = getProjects().toMutableList()
        current.removeAll { it.id == projectId }
        projectsFile.writeText(json.encodeToString(current))

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
        if (!buildsFile.exists()) return@withContext emptyList()
        try {
            val allBuilds = json.decodeFromString<List<BuildInfo>>(buildsFile.readText())
            allBuilds.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveBuild(build: BuildInfo) = withContext(Dispatchers.IO) {
        val allBuilds = try {
            if (buildsFile.exists()) json.decodeFromString<List<BuildInfo>>(buildsFile.readText()).toMutableList()
            else mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }

        val index = allBuilds.indexOfFirst { it.id == build.id && it.projectId == build.projectId }
        if (index >= 0) {
            allBuilds[index] = build
        } else {
            allBuilds.add(0, build)
        }

        val grouped = allBuilds.groupBy { it.projectId }
        val trimmedList = mutableListOf<BuildInfo>()
        for ((_, projectBuilds) in grouped) {
            val top10 = projectBuilds.sortedByDescending { it.createdAt }.take(10)
            trimmedList.addAll(top10)
        }

        buildsFile.writeText(json.encodeToString(trimmedList))
    }

    private suspend fun deleteBuildsForProject(projectId: String) = withContext(Dispatchers.IO) {
        if (!buildsFile.exists()) return@withContext
        try {
            val allBuilds = json.decodeFromString<List<BuildInfo>>(buildsFile.readText()).toMutableList()
            allBuilds.removeAll { it.projectId == projectId }
            buildsFile.writeText(json.encodeToString(allBuilds))
        } catch (_: Exception) {}
    }

    private suspend fun cleanOldBuildLogs() = withContext(Dispatchers.IO) {
        if (!buildsFile.exists()) return@withContext
        try {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
            val allBuilds = json.decodeFromString<List<BuildInfo>>(buildsFile.readText()).toMutableList()
            var modified = false
            for (i in allBuilds.indices) {
                val b = allBuilds[i]
                if (b.createdAt < thirtyDaysAgo && b.rawLog.isNotBlank()) {
                    allBuilds[i] = b.copy(rawLog = "")
                    modified = true
                }
            }
            if (modified) {
                buildsFile.writeText(json.encodeToString(allBuilds))
            }
        } catch (_: Exception) {}
    }

    suspend fun getNotes(projectId: String): List<Note> = withContext(Dispatchers.IO) {
        if (!notesFile.exists()) return@withContext emptyList()
        try {
            val allNotes = json.decodeFromString<List<Note>>(notesFile.readText())
            allNotes.filter { it.projectId == projectId }.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveNote(note: Note) = withContext(Dispatchers.IO) {
        val allNotes = try {
            if (notesFile.exists()) json.decodeFromString<List<Note>>(notesFile.readText()).toMutableList()
            else mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }

        val index = allNotes.indexOfFirst { it.id == note.id }
        if (index >= 0) {
            allNotes[index] = note
        } else {
            allNotes.add(0, note)
        }
        notesFile.writeText(json.encodeToString(allNotes))
    }

    suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        if (!notesFile.exists()) return@withContext
        try {
            val allNotes = json.decodeFromString<List<Note>>(notesFile.readText()).toMutableList()
            allNotes.removeAll { it.id == noteId }
            notesFile.writeText(json.encodeToString(allNotes))
        } catch (_: Exception) {}
    }

    private suspend fun deleteNotesForProject(projectId: String) = withContext(Dispatchers.IO) {
        if (!notesFile.exists()) return@withContext
        try {
            val allNotes = json.decodeFromString<List<Note>>(notesFile.readText()).toMutableList()
            allNotes.removeAll { it.projectId == projectId }
            notesFile.writeText(json.encodeToString(allNotes))
        } catch (_: Exception) {}
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
