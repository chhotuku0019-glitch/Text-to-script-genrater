package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("scriptforge_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val KEY_MODEL = "selected_gemini_model"
        private const val KEY_TEMPERATURE = "ai_temperature"
        private const val KEY_SPEAKING_WPM = "speaking_speed_wpm"
        private const val KEY_RECORDING_MODE = "default_recording_mode"
        private const val KEY_AUTO_CLEAN = "auto_clean_transcript"
        private const val KEY_AUTO_CONVERT = "auto_convert_script"
        private const val KEY_TELEPROMPTER_SPEED = "teleprompter_speed_px"
        private const val KEY_TELEPROMPTER_FONT_SIZE = "teleprompter_font_size"
        private const val KEY_SCREEN_RECORD_MODE = "screen_recording_mode"
        private const val KEY_DARK_THEME = "dark_theme_enabled"
    }

    var customApiKey: String
        get() = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_KEY, value.trim()).apply()

    var selectedModel: String
        get() = prefs.getString(KEY_MODEL, "gemini-3.5-flash") ?: "gemini-3.5-flash"
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, 0.7f)
        set(value) = prefs.edit().putFloat(KEY_TEMPERATURE, value).apply()

    var speakingWpm: Int
        get() = prefs.getInt(KEY_SPEAKING_WPM, 150)
        set(value) = prefs.edit().putInt(KEY_SPEAKING_WPM, value).apply()

    var defaultRecordingMode: String // "floating", "background", "standard"
        get() = prefs.getString(KEY_RECORDING_MODE, "background") ?: "background"
        set(value) = prefs.edit().putString(KEY_RECORDING_MODE, value).apply()

    var autoCleanTranscript: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CLEAN, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CLEAN, value).apply()

    var autoConvertScript: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONVERT, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONVERT, value).apply()

    var teleprompterSpeed: Int // 1 (Slow), 2 (Medium), 3 (Fast), 4 (Very fast)
        get() = prefs.getInt(KEY_TELEPROMPTER_SPEED, 2)
        set(value) = prefs.edit().putInt(KEY_TELEPROMPTER_SPEED, value).apply()

    var teleprompterFontSize: Int // 24sp, 32sp, 40sp, etc.
        get() = prefs.getInt(KEY_TELEPROMPTER_FONT_SIZE, 32)
        set(value) = prefs.edit().putInt(KEY_TELEPROMPTER_FONT_SIZE, value).apply()

    var isScreenRecordingMode: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_RECORD_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_RECORD_MODE, value).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()
}
