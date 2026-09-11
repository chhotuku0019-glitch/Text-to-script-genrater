package com.example.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScriptForgeApp
import com.example.data.gemini.DescriptionData
import com.example.data.gemini.HookItem
import com.example.data.gemini.SceneSuggestion
import com.example.data.gemini.ScriptAnalysis
import com.example.data.gemini.ThumbnailConcept
import com.example.data.gemini.TitleItem
import com.example.data.local.ProjectEntity
import com.example.data.local.ScriptVersionEntity
import com.example.service.RecordingState
import com.example.service.RecordingStateHolder
import com.example.service.RecordingStatus
import com.example.service.SpeechRecordingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

sealed class Screen {
    object Home : Screen()
    object Projects : Screen()
    object NewProject : Screen()
    object TextToScript : Screen()
    data class SpeechRecord(val projectId: Long? = null) : Screen()
    data class ProjectWorkspace(val projectId: Long, val initialTab: Int = 0) : Screen()
    data class Teleprompter(val projectId: Long) : Screen()
    object Settings : Screen()
}

class MainViewModel : ViewModel() {

    private val app = ScriptForgeApp.instance
    private val repository = app.projectRepository
    private val geminiService = app.geminiService
    val preferences = app.preferencesManager

    // Navigation State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val screenBackStack = mutableListOf<Screen>()

    // Search and Filter State for Projects List
    val searchQuery = MutableStateFlow("")
    val selectedStatusFilter = MutableStateFlow("All") // "All", "Draft", "Final", "Archived"
    val selectedTagFilter = MutableStateFlow<String?>(null)

    // Projects Flow
    val allProjects: StateFlow<List<ProjectEntity>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDrafts: StateFlow<List<ProjectEntity>> = repository.getRecentDrafts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Project in Workspace
    private val _activeProjectId = MutableStateFlow<Long?>(null)
    val activeProjectId: StateFlow<Long?> = _activeProjectId.asStateFlow()

    val activeProject: StateFlow<ProjectEntity?> = _activeProjectId.flatMapLatest { id ->
        if (id != null && id > 0) repository.getProjectFlowById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeProjectVersions: StateFlow<List<ScriptVersionEntity>> = _activeProjectId.flatMapLatest { id ->
        if (id != null && id > 0) repository.getVersionsForProject(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recording State
    val recordingState: StateFlow<RecordingState> = RecordingStateHolder.state

    // AI Processing State
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiStatusMessage = MutableStateFlow("")
    val aiStatusMessage: StateFlow<String> = _aiStatusMessage.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Autosave Status: "Saved ✓", "Saving...", ""
    private val _saveStatus = MutableStateFlow("Saved ✓")
    val saveStatus: StateFlow<String> = _saveStatus.asStateFlow()

    private var autoSaveJob: Job? = null

    // Teleprompter Control
    val isTeleprompterPlaying = MutableStateFlow(false)
    val teleprompterSpeed = MutableStateFlow(preferences.teleprompterSpeed)
    val teleprompterFontSize = MutableStateFlow(preferences.teleprompterFontSize)
    val teleprompterMirrorMode = MutableStateFlow(false)

    // Undo/Redo State for Script Editor
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenBackStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
        if (screen is Screen.ProjectWorkspace) {
            _activeProjectId.value = screen.projectId
        } else if (screen is Screen.Teleprompter) {
            _activeProjectId.value = screen.projectId
        }
    }

    fun navigateBack(): Boolean {
        if (screenBackStack.isNotEmpty()) {
            val previous = screenBackStack.removeAt(screenBackStack.size - 1)
            _currentScreen.value = previous
            if (previous is Screen.ProjectWorkspace) {
                _activeProjectId.value = previous.projectId
            }
            return true
        }
        if (_currentScreen.value != Screen.Home) {
            _currentScreen.value = Screen.Home
            return true
        }
        return false
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    // --- Project Operations ---

    fun createNewProject(
        title: String,
        videoType: String,
        language: String,
        targetDuration: String,
        tone: String,
        targetAudience: String,
        tags: String,
        ideaText: String,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = repository.createProject(
                title = title,
                videoType = videoType,
                language = language,
                targetDuration = targetDuration,
                tone = tone,
                targetAudience = targetAudience,
                tags = tags,
                ideaText = ideaText
            )
            _activeProjectId.value = id
            onCreated(id)
        }
    }

    fun updateActiveProject(updated: ProjectEntity) {
        viewModelScope.launch {
            _saveStatus.value = "Saving..."
            repository.updateProject(updated)
            delay(300)
            _saveStatus.value = "Saved ✓"
        }
    }

    fun updateScriptContent(newScript: String) {
        val current = activeProject.value ?: return
        if (current.currentScript == newScript) return

        // Push to undo stack
        if (undoStack.isEmpty() || undoStack.last() != current.currentScript) {
            undoStack.add(current.currentScript)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
        }

        autoSaveJob?.cancel()
        _saveStatus.value = "Saving..."
        autoSaveJob = viewModelScope.launch {
            delay(600) // Debounce auto-save
            repository.updateProject(current.copy(currentScript = newScript))
            _saveStatus.value = "Saved ✓"
        }
    }

    fun undoScript() {
        val current = activeProject.value ?: return
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(current.currentScript)
            viewModelScope.launch {
                repository.updateProject(current.copy(currentScript = previous))
            }
        }
    }

    fun redoScript() {
        val current = activeProject.value ?: return
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(current.currentScript)
            viewModelScope.launch {
                repository.updateProject(current.copy(currentScript = next))
            }
        }
    }

    fun duplicateProject(projectId: Long) {
        viewModelScope.launch {
            val newId = repository.duplicateProject(projectId)
            if (newId > 0) {
                showMessage("Project duplicated successfully!")
            }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            if (_activeProjectId.value == projectId) {
                _activeProjectId.value = null
                navigateTo(Screen.Home)
            }
            showMessage("Project deleted.")
        }
    }

    fun setProjectStatus(projectId: Long, status: String) {
        viewModelScope.launch {
            repository.setStatus(projectId, status)
            showMessage("Project status updated to $status.")
        }
    }

    fun saveNewVersion(projectId: Long, versionName: String, note: String = "") {
        viewModelScope.launch {
            repository.saveVersion(projectId, versionName, note)
            showMessage("Version '$versionName' saved.")
        }
    }

    fun restoreVersion(projectId: Long, version: ScriptVersionEntity) {
        viewModelScope.launch {
            repository.restoreVersion(projectId, version)
            showMessage("Restored '${version.versionName}'.")
        }
    }

    fun deleteVersion(versionId: Long) {
        viewModelScope.launch {
            repository.deleteVersion(versionId)
            showMessage("Version removed.")
        }
    }

    // --- Speech Recording Operations ---

    fun startRecording(context: Context, projectId: Long? = null, language: String = "Hinglish") {
        SpeechRecordingService.startRecording(context, projectId, language)
    }

    fun pauseRecording(context: Context) {
        SpeechRecordingService.pauseRecording(context)
    }

    fun resumeRecording(context: Context) {
        SpeechRecordingService.resumeRecording(context)
    }

    fun stopRecording(context: Context) {
        SpeechRecordingService.stopRecording(context)
    }

    fun saveRecordedTranscriptToProject(projectId: Long, rawTranscript: String) {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId) ?: return@launch
            val updated = project.copy(
                rawTranscript = rawTranscript,
                cleanTranscript = if (project.cleanTranscript.isEmpty()) rawTranscript else project.cleanTranscript
            )
            repository.updateProject(updated)
            showMessage("Voice transcript saved to project!")
        }
    }

    // --- Gemini AI Operations ---

    fun cleanTranscriptAi(projectId: Long, rawTranscript: String, language: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiStatusMessage.value = "AI is cleaning transcript and removing filler words..."
            val result = geminiService.cleanTranscript(rawTranscript, language)
            _isAiLoading.value = false
            result.onSuccess { cleanText ->
                val project = repository.getProjectById(projectId) ?: return@onSuccess
                repository.updateProject(project.copy(cleanTranscript = cleanText))
                showMessage("Transcript cleaned successfully!")
            }.onFailure { err ->
                showMessage("Cleaning failed: ${err.localizedMessage ?: "Unknown error"}. Your raw transcript is safe.")
            }
        }
    }

    fun generateScriptFromTextAi(
        title: String,
        ideaText: String,
        videoType: String,
        language: String,
        targetDuration: String,
        tone: String,
        targetAudience: String,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiStatusMessage.value = "Generating structured YouTube script with Hook, Sections & CTA..."
            val result = geminiService.generateScriptFromText(
                ideaText = ideaText,
                videoType = videoType,
                language = language,
                targetDuration = targetDuration,
                tone = tone,
                targetAudience = targetAudience
            )
            _isAiLoading.value = false
            result.onSuccess { script ->
                val id = repository.createProject(
                    title = title.ifBlank { "YouTube: $videoType ($targetDuration)" },
                    videoType = videoType,
                    language = language,
                    targetDuration = targetDuration,
                    tone = tone,
                    targetAudience = targetAudience,
                    ideaText = ideaText
                )
                val project = repository.getProjectById(id)
                if (project != null) {
                    repository.updateProject(project.copy(currentScript = script))
                    repository.saveVersion(id, "Initial Generated Script", "Generated from Text idea")
                }
                _activeProjectId.value = id
                showMessage("Master YouTube Script generated!")
                onSuccess(id)
            }.onFailure { err ->
                showMessage("Generation failed: ${err.localizedMessage ?: "Please verify Gemini API key"}")
            }
        }
    }

    fun convertTranscriptToScriptAi(projectId: Long) {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId) ?: return@launch
            val transcript = project.cleanTranscript.ifEmpty { project.rawTranscript }
            if (transcript.isBlank()) {
                showMessage("Please record or write a transcript first.")
                return@launch
            }

            _isAiLoading.value = true
            _aiStatusMessage.value = "Structuring speech into a professional YouTube script..."
            val result = geminiService.generateScriptFromTranscript(
                cleanTranscript = transcript,
                videoType = project.videoType,
                language = project.language,
                targetDuration = project.targetDuration,
                tone = project.tone,
                targetAudience = project.targetAudience
            )
            _isAiLoading.value = false
            result.onSuccess { script ->
                // Save old version if currentScript exists
                if (project.currentScript.isNotBlank()) {
                    repository.saveVersion(projectId, "Previous Script (Pre-Speech Convert)", "Auto backup")
                }
                repository.updateProject(project.copy(currentScript = script))
                repository.saveVersion(projectId, "Generated from Voice Transcript", "Speech-to-Script")
                showMessage("Speech converted to YouTube Script!")
            }.onFailure { err ->
                showMessage("Conversion failed: ${err.localizedMessage ?: "Please try again"}")
            }
        }
    }

    fun applyScriptToolAi(projectId: Long, toolName: String, selectedText: String = "") {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId) ?: return@launch
            if (project.currentScript.isBlank()) {
                showMessage("Script is empty. Generate or write a script first.")
                return@launch
            }

            _isAiLoading.value = true
            _aiStatusMessage.value = "Applying AI tool '$toolName'..."
            val result = geminiService.applyScriptTool(
                toolName = toolName,
                currentScript = project.currentScript,
                language = project.language,
                selectedText = selectedText
            )
            _isAiLoading.value = false
            result.onSuccess { modifiedScript ->
                repository.saveVersion(projectId, "Before $toolName", "Pre-tool modification")
                val finalScript = if (selectedText.isNotEmpty() && project.currentScript.contains(selectedText)) {
                    project.currentScript.replace(selectedText, modifiedScript)
                } else {
                    modifiedScript
                }
                repository.updateProject(project.copy(currentScript = finalScript))
                showMessage("AI Tool '$toolName' applied successfully!")
            }.onFailure { err ->
                showMessage("AI Tool failed: ${err.localizedMessage ?: "Error"}")
            }
        }
    }

    fun generateHooksAi(projectId: Long) {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId) ?: return@launch
            val content = project.currentScript.ifEmpty { project.cleanTranscript.ifEmpty { project.ideaText } }
            if (content.isBlank()) {
                showMessage("Provide some script content or idea first.")
                return@launch
            }

            _isAiLoading.value = true
            _aiStatusMessage.value = "Crafting 8 high-retention YouTube hooks..."
            val result = geminiService.generateHooks(content, project.videoType, project.language)
            _isAiLoading.value = false
            result.onSuccess { hooksList ->
                val jsonArr = JSONArray()
                hooksList.forEach { h ->
                    val obj = JSONObject()
                    obj.put("type", h.type)
                    obj.put("text", h.text)
                    obj.put("explanation", h.explanation)
                    jsonArr.put(obj)
                }
                repository.updateProject(project.copy(hooksJson = jsonArr.toString()))
                showMessage("8 Hook variations generated!")
            }.onFailure { err ->
                showMessage("Hook generation failed: ${err.localizedMessage ?: "Error"}")
            }
        }
    }

    fun generateTitlesAi(projectId: Long) {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId) ?: return@launch
            val content = project.currentScript.ifEmpty { project.cleanTranscript.ifEmpty { project.ideaText } }
            if (content.isBlank()) {
                showMessage("Provide some script content or idea first.")
                return@launch
            }

            _isAiLoading.value = true
            _aiStatusMessage.value = "Generating 10 click-worthy YouTube title ideas..."
            val result = geminiService.generateTitles(content, project.videoType, project.language)
            _isAiLoading.value = false
            result.onSuccess { titlesList ->
                val jsonArr = JSONArray()
                titlesList.forEach { t ->
                    val obj = JSONObject()
                    obj.put("category", t.category)
                    obj.put("title", t.title)
                    obj.put("scoreHint", t.scoreHint)
                    jsonArr.put(obj)
                }
                repository.updateProject(project.copy(titlesJson = jsonArr.toString()))
                showMessage("10 Title ideas generated!")
            }.onFailure { err ->
                showMessage("Title generation failed: ${err.localizedMessage ?: "Error"}")
            }
        }
    }

    fun generateDescriptionAndChaptersAi(projectId: Long) {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId) ?: return@launch
            if (project.currentScript.isBlank()) {
                showMessage("Create a script first to generate descriptions and chapters.")
                return@launch
            }

            _isAiLoading.value = true
            _aiStatusMessage.value = "Optimizing SEO description and calculating chapters..."
            val result = geminiService.generateDescriptionAndChapters(project.currentScript, project.videoType, project.language)
            _isAiLoading.value = false
            result.onSuccess { descData ->
                val rootObj = JSONObject()
                rootObj.put("shortDescription", descData.shortDescription)
                rootObj.put("seoDescription", descData.seoDescription)
                rootObj.put("detailedDescription", descData.detailedDescription)
                val chapArr = JSONArray()
                descData.chapters.forEach { c ->
                    val cObj = JSONObject()
                    cObj.put("timestamp", c.timestamp)
                    cObj.put("title", c.title)
                    chapArr.put(cObj)
                }
                rootObj.put("chapters", chapArr)
                repository.updateProject(project.copy(descriptionJson = rootObj.toString()))
                showMessage("Description and chapters ready!")
            }.onFailure { err ->
                showMessage("Description generation failed: ${err.localizedMessage ?: "Error"}")
            }
        }
    }

    fun generateThumbnailConceptsAi(projectId: Long) {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId) ?: return@launch
            val content = project.currentScript.ifEmpty { project.cleanTranscript.ifEmpty { project.ideaText } }
            if (content.isBlank()) {
                showMessage("Provide some script content or idea first.")
                return@launch
            }

            _isAiLoading.value = true
            _aiStatusMessage.value = "Generating thumbnail concepts and text overlays..."
            val result = geminiService.generateThumbnailConcepts(content, project.videoType, project.language)
            _isAiLoading.value = false
            result.onSuccess { conceptsList ->
                val jsonArr = JSONArray()
                conceptsList.forEach { c ->
                    val obj = JSONObject()
                    obj.put("mainText", c.mainText)
                    obj.put("visualIdea", c.visualIdea)
                    obj.put("subjectPlacement", c.subjectPlacement)
                    obj.put("emotion", c.emotion)
                    obj.put("composition", c.composition)
                    jsonArr.put(obj)
                }
                repository.updateProject(project.copy(thumbnailsJson = jsonArr.toString()))
                showMessage("5 Thumbnail concepts generated!")
            }.onFailure { err ->
                showMessage("Thumbnail generation failed: ${err.localizedMessage ?: "Error"}")
            }
        }
    }

    fun generateSceneSuggestionsAi(projectId: Long) {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId) ?: return@launch
            if (project.currentScript.isBlank()) {
                showMessage("Create a script first to generate scene suggestions.")
                return@launch
            }

            _isAiLoading.value = true
            _aiStatusMessage.value = "Creating visual scene breakdown, B-roll & screen recording ideas..."
            val result = geminiService.generateSceneSuggestions(project.currentScript, project.videoType, project.language)
            _isAiLoading.value = false
            result.onSuccess { scenesList ->
                val jsonArr = JSONArray()
                scenesList.forEach { s ->
                    val obj = JSONObject()
                    obj.put("scriptSection", s.scriptSection)
                    obj.put("sceneIdea", s.sceneIdea)
                    obj.put("bRollIdea", s.bRollIdea)
                    obj.put("screenRecordingIdea", s.screenRecordingIdea)
                    obj.put("animationIdea", s.animationIdea)
                    obj.put("aiImagePrompt", s.aiImagePrompt)
                    jsonArr.put(obj)
                }
                repository.updateProject(project.copy(scenesJson = jsonArr.toString()))
                showMessage("Scene and B-roll suggestions generated!")
            }.onFailure { err ->
                showMessage("Scene generation failed: ${err.localizedMessage ?: "Error"}")
            }
        }
    }

    fun analyzeScriptAi(projectId: Long) {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId) ?: return@launch
            if (project.currentScript.isBlank()) {
                showMessage("Create a script first to analyze retention and quality.")
                return@launch
            }

            _isAiLoading.value = true
            _aiStatusMessage.value = "Analyzing hook, retention pacing, clarity, and structure..."
            val result = geminiService.analyzeScript(
                project.currentScript,
                project.videoType,
                project.language,
                project.targetDuration
            )
            _isAiLoading.value = false
            result.onSuccess { analysis ->
                val obj = JSONObject()
                obj.put("hookScore", analysis.hookScore)
                obj.put("clarityScore", analysis.clarityScore)
                obj.put("structureScore", analysis.structureScore)
                obj.put("engagementScore", analysis.engagementScore)
                obj.put("pacingScore", analysis.pacingScore)
                obj.put("valueScore", analysis.valueScore)
                obj.put("repetitionScore", analysis.repetitionScore)
                obj.put("ctaScore", analysis.ctaScore)
                obj.put("overallScore", analysis.overallScore)
                obj.put("summary", analysis.summary)

                val strArr = JSONArray()
                analysis.strengths.forEach { strArr.put(it) }
                obj.put("strengths", strArr)

                val impArr = JSONArray()
                analysis.improvements.forEach { impArr.put(it) }
                obj.put("improvements", impArr)

                repository.updateProject(project.copy(analysisJson = obj.toString()))
                showMessage("Script Analysis complete! Overall Score: ${analysis.overallScore}/100")
            }.onFailure { err ->
                showMessage("Analysis failed: ${err.localizedMessage ?: "Error"}")
            }
        }
    }

    // Export Helpers
    fun exportScriptAsMarkdown(project: ProjectEntity): String {
        return buildString {
            appendLine("# ${project.title}")
            appendLine("**Video Type:** ${project.videoType} | **Language:** ${project.language} | **Duration:** ${project.targetDuration}")
            appendLine("**Tone:** ${project.tone} | **Audience:** ${project.targetAudience.ifEmpty { "General" }}")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## YouTube Script")
            appendLine(project.currentScript)
            appendLine()
            appendLine("---")
            appendLine("*Generated and edited with ScriptForge AI*")
        }
    }

    fun shareScript(context: Context, project: ProjectEntity) {
        val content = exportScriptAsMarkdown(project)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_TITLE, project.title)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export Script")
        context.startActivity(shareIntent)
    }
}
