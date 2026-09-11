package com.example.data.repository

import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import com.example.data.local.ScriptVersionEntity
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {

    fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    fun getRecentDrafts(): Flow<List<ProjectEntity>> = projectDao.getRecentDrafts()

    fun getProjectsByStatus(status: String): Flow<List<ProjectEntity>> = projectDao.getProjectsByStatus(status)

    fun getProjectFlowById(id: Long): Flow<ProjectEntity?> = projectDao.getProjectFlowById(id)

    fun searchProjects(query: String): Flow<List<ProjectEntity>> = projectDao.searchProjects(query)

    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getProjectById(id)

    suspend fun createProject(
        title: String,
        videoType: String,
        language: String,
        targetDuration: String,
        tone: String,
        targetAudience: String,
        tags: String = "AI, YouTube",
        ideaText: String = ""
    ): Long {
        val newProject = ProjectEntity(
            title = title.ifBlank { "Untitled Video Idea" },
            videoType = videoType,
            language = language,
            targetDuration = targetDuration,
            tone = tone,
            targetAudience = targetAudience,
            tags = tags,
            status = "Draft",
            ideaText = ideaText,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = projectDao.insertProject(newProject)
        // Create initial version
        projectDao.insertVersion(
            ScriptVersionEntity(
                projectId = id,
                versionName = "Draft v1 (Initial)",
                scriptContent = "",
                note = "Initial project creation",
                wordCount = 0
            )
        )
        return id
    }

    suspend fun updateProject(project: ProjectEntity) {
        val wordCount = calculateWordCount(project.currentScript)
        val durationSec = (wordCount / 2.5).toInt() // default 150 wpm = 2.5 words/sec
        val updated = project.copy(
            updatedAt = System.currentTimeMillis(),
            wordCount = wordCount,
            estimatedDurationSeconds = durationSec
        )
        projectDao.updateProject(updated)
    }

    suspend fun duplicateProject(projectId: Long): Long {
        val original = projectDao.getProjectById(projectId) ?: return -1L
        val copy = original.copy(
            id = 0,
            title = "${original.title} (Copy)",
            status = "Draft",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newId = projectDao.insertProject(copy)
        projectDao.insertVersion(
            ScriptVersionEntity(
                projectId = newId,
                versionName = "Duplicated from v${original.id}",
                scriptContent = copy.currentScript,
                note = "Duplicated from '${original.title}'",
                wordCount = copy.wordCount
            )
        )
        return newId
    }

    suspend fun deleteProject(id: Long) {
        projectDao.deleteVersionsForProject(id)
        projectDao.deleteProjectById(id)
    }

    suspend fun setStatus(projectId: Long, status: String) {
        val project = projectDao.getProjectById(projectId) ?: return
        updateProject(project.copy(status = status))
    }

    // Version Control
    fun getVersionsForProject(projectId: Long): Flow<List<ScriptVersionEntity>> =
        projectDao.getVersionsForProject(projectId)

    suspend fun saveVersion(projectId: Long, versionName: String, note: String = ""): Long {
        val project = projectDao.getProjectById(projectId) ?: return -1L
        val wordCount = calculateWordCount(project.currentScript)
        return projectDao.insertVersion(
            ScriptVersionEntity(
                projectId = projectId,
                versionName = versionName,
                scriptContent = project.currentScript,
                note = note,
                wordCount = wordCount
            )
        )
    }

    suspend fun restoreVersion(projectId: Long, version: ScriptVersionEntity) {
        val project = projectDao.getProjectById(projectId) ?: return
        // Save current as a safety backup before restoring
        saveVersion(projectId, "Auto-Backup before restoring '${version.versionName}'", "Safety backup")
        updateProject(project.copy(currentScript = version.scriptContent))
    }

    suspend fun deleteVersion(versionId: Long) {
        projectDao.deleteVersionById(versionId)
    }

    companion object {
        fun calculateWordCount(text: String): Int {
            if (text.isBlank()) return 0
            return text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
        }
    }
}
