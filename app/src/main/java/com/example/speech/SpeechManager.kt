package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.service.RecordingStateHolder
import com.example.service.RecordingStatus
import java.util.Locale

class SpeechManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var targetLanguage: String = "en-IN" // default hinglish/indian english
    private val mainHandler = Handler(Looper.getMainLooper())
    private var restartRunnable: Runnable? = null
    private var lastRestartTime = 0L

    fun setLanguage(language: String) {
        targetLanguage = when (language.lowercase()) {
            "hindi", "hi" -> "hi-IN"
            "hinglish" -> "hi-IN" // hi-IN or en-IN captures Hinglish best on Android
            "english", "en" -> "en-US"
            else -> Locale.getDefault().toLanguageTag()
        }
    }

    private fun buildRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLanguage)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Lengthen silence thresholds so it does not disconnect every few seconds
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 4000L)
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("SpeechManager", "Speech recognition not available on this device")
            return
        }

        cancelPendingRestart()
        isListening = true

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            }

            speechRecognizer?.startListening(buildRecognizerIntent())
            lastRestartTime = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Failed to start speech recognizer", e)
        }
    }

    private fun safeRestart(delayMillis: Long = 400L) {
        if (!isListening || RecordingStateHolder.state.value.status != RecordingStatus.RECORDING) {
            return
        }

        cancelPendingRestart()
        restartRunnable = Runnable {
            if (!isListening || RecordingStateHolder.state.value.status != RecordingStatus.RECORDING) return@Runnable

            val now = System.currentTimeMillis()
            if (now - lastRestartTime < 300) {
                // Throttle rapid loops
                return@Runnable
            }
            lastRestartTime = now

            try {
                speechRecognizer?.cancel()
                speechRecognizer?.startListening(buildRecognizerIntent())
            } catch (e: Exception) {
                Log.e("SpeechManager", "Error in safeRestart, recreating recognizer", e)
                try {
                    speechRecognizer?.destroy()
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createListener())
                        startListening(buildRecognizerIntent())
                    }
                } catch (ex: Exception) {
                    Log.e("SpeechManager", "Failed to recreate speech recognizer", ex)
                }
            }
        }
        mainHandler.postDelayed(restartRunnable!!, delayMillis)
    }

    private fun cancelPendingRestart() {
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        restartRunnable = null
    }

    fun pauseListening() {
        isListening = false
        cancelPendingRestart()
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error stopping speech recognizer", e)
        }
    }

    fun stopListening(resetState: Boolean = true) {
        isListening = false
        cancelPendingRestart()
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error destroying speech recognizer", e)
        }
        if (resetState) {
            RecordingStateHolder.updateInterimText("")
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechManager", "onReadyForSpeech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("SpeechManager", "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Normalize rmsdB for wave visualizer (usually -2 to 10 dB)
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                RecordingStateHolder.updateRms(normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("SpeechManager", "onEndOfSpeech")
            }

            override fun onError(error: Int) {
                Log.w("SpeechManager", "Speech recognition error: $error")
                // Non-critical errors like speech timeout (6), no match (7), or busy (8) are restarted gracefully with delay
                if (isListening && RecordingStateHolder.state.value.status == RecordingStatus.RECORDING) {
                    val delay = when (error) {
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_NO_MATCH -> 400L
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 600L
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> 1200L
                        else -> 500L
                    }
                    safeRestart(delay)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val bestMatch = matches[0]
                    RecordingStateHolder.appendTranscript(bestMatch)
                }
                RecordingStateHolder.updateInterimText("")

                // Seamlessly continue recording without rapid start/stop beeps
                if (isListening && RecordingStateHolder.state.value.status == RecordingStatus.RECORDING) {
                    safeRestart(300L)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    RecordingStateHolder.updateInterimText(matches[0])
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
