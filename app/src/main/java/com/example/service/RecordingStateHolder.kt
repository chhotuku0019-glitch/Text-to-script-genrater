package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RecordingStatus {
    IDLE,
    RECORDING,
    PAUSED,
    PROCESSING
}

data class RecordingState(
    val status: RecordingStatus = RecordingStatus.IDLE,
    val durationSeconds: Long = 0L,
    val currentRmsDb: Float = 0f,
    val interimText: String = "",
    val fullTranscript: String = "",
    val projectId: Long? = null,
    val isFloatingVisible: Boolean = false,
    val isBackgroundMode: Boolean = true
)

object RecordingStateHolder {
    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    fun updateStatus(status: RecordingStatus) {
        _state.value = _state.value.copy(status = status)
    }

    fun updateDuration(seconds: Long) {
        _state.value = _state.value.copy(durationSeconds = seconds)
    }

    fun updateRms(rmsDb: Float) {
        _state.value = _state.value.copy(currentRmsDb = rmsDb)
    }

    fun updateInterimText(text: String) {
        _state.value = _state.value.copy(interimText = text)
    }

    fun appendTranscript(chunk: String) {
        val trimmed = chunk.trim()
        if (trimmed.isEmpty()) return
        val current = _state.value.fullTranscript.trim()
        val newTranscript = if (current.isEmpty()) trimmed else "$current $trimmed"
        _state.value = _state.value.copy(
            fullTranscript = newTranscript,
            interimText = ""
        )
    }

    fun setFullTranscript(text: String) {
        _state.value = _state.value.copy(fullTranscript = text)
    }

    fun setProjectId(id: Long?) {
        _state.value = _state.value.copy(projectId = id)
    }

    fun setFloatingVisible(visible: Boolean) {
        _state.value = _state.value.copy(isFloatingVisible = visible)
    }

    fun setBackgroundMode(bgMode: Boolean) {
        _state.value = _state.value.copy(isBackgroundMode = bgMode)
    }

    fun reset() {
        _state.value = RecordingState()
    }
}
