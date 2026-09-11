package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val videoType: String = "Tutorial",
    val language: String = "Hinglish",
    val targetDuration: String = "8 min",
    val tone: String = "Energetic",
    val targetAudience: String = "",
    val tags: String = "AI, Tutorial",
    val status: String = "Draft", // "Draft", "Final", "Archived"
    val ideaText: String = "",
    val rawTranscript: String = "",
    val cleanTranscript: String = "",
    val currentScript: String = "",
    val notes: String = "",
    val titlesJson: String = "[]",
    val descriptionJson: String = "{}",
    val hooksJson: String = "[]",
    val thumbnailsJson: String = "[]",
    val scenesJson: String = "[]",
    val analysisJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val wordCount: Int = 0,
    val estimatedDurationSeconds: Int = 0
)

@Entity(tableName = "script_versions")
data class ScriptVersionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val versionName: String, // e.g. "Draft 1", "After Hook Fix", "Final v1"
    val scriptContent: String,
    val note: String = "",
    val wordCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
