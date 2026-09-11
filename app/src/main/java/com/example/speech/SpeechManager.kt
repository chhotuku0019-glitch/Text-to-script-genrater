package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
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

    fun setLanguage(language: String) {
        targetLanguage = when (language.lowercase()) {
            "hindi", "hi" -> "hi-IN"
            "hinglish" -> "hi-IN" // hi-IN or en-IN captures Hinglish best on Android
            "english", "en" -> "en-US"
            else -> Locale.getDefault().toLanguageTag()
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("SpeechManager", "Speech recognition not available on this device")
            return
        }

        stopListening(resetState = false)

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLanguage)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            isListening = true
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechManager", "Failed to start speech recognizer", e)
        }
    }

    fun pauseListening() {
        isListening = false
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error stopping speech recognizer", e)
        }
    }

    fun stopListening(resetState: Boolean = true) {
        isListening = false
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
                // If user is still actively in RECORDING status, restart automatically to achieve continuous dictation
                if (isListening && RecordingStateHolder.state.value.status == RecordingStatus.RECORDING) {
                    try {
                        startListening()
                    } catch (e: Exception) {
                        Log.e("SpeechManager", "Error restarting listener", e)
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val bestMatch = matches[0]
                    RecordingStateHolder.appendTranscript(bestMatch)
                }

                // Restart for continuous recording if still active
                if (isListening && RecordingStateHolder.state.value.status == RecordingStatus.RECORDING) {
                    startListening()
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
