package com.example

import android.app.Application
import com.example.data.gemini.GeminiService
import com.example.data.local.PreferencesManager
import com.example.data.local.ScriptDatabase
import com.example.data.repository.ProjectRepository

class ScriptForgeApp : Application() {

    lateinit var database: ScriptDatabase
        private set

    lateinit var projectRepository: ProjectRepository
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var geminiService: GeminiService
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = ScriptDatabase.getInstance(this)
        projectRepository = ProjectRepository(database.projectDao())
        preferencesManager = PreferencesManager(this)

        geminiService = GeminiService(
            getApiKey = { preferencesManager.customApiKey },
            getModel = { preferencesManager.selectedModel },
            getTemperature = { preferencesManager.temperature }
        )
    }

    companion object {
        lateinit var instance: ScriptForgeApp
            private set
    }
}
