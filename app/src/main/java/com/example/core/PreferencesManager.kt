package com.example.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "umesh_ai_preferences")

enum class GreetingType(val key: String, val labelHi: String, val labelEn: String, val defaultSpeechHi: String) {
    WELLBEING(
        "wellbeing",
        "हालचाल पहले (Wellbeing first)",
        "Ask about wellbeing first",
        "हाँ उमेश! कैसे हो? सब ठीक है ना? बताओ, क्या मदद चाहिए?"
    ),
    DIRECT(
        "direct",
        "सीधे कमांड (Direct to command)",
        "Go directly to command",
        "हाँ उमेश, बोलो, मैं सुन रहा हूँ।"
    ),
    SHORT(
        "short",
        "छोटा अभिवादन (Short greeting)",
        "Short greeting",
        "हाँ उमेश!"
    ),
    DISABLED(
        "disabled",
        "अभिवादन बंद (Disabled)",
        "Disabled",
        ""
    )
}

enum class AssistantLanguage(val code: String, val displayName: String, val localeTag: String) {
    HINDI("hi", "हिंदी (Hindi)", "hi-IN"),
    NEPALI("ne", "नेपाली (Nepali)", "ne-NP"),
    ENGLISH("en", "English", "en-IN")
}

data class UserPreferences(
    val language: AssistantLanguage = AssistantLanguage.HINDI,
    val greetingType: GreetingType = GreetingType.WELLBEING,
    val themeMode: String = "DARK", // DARK, LIGHT, SYSTEM
    val wakeWordEnabled: Boolean = true,
    val voiceResponseEnabled: Boolean = true,
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val selectedVoiceName: String = "",
    val redmiGuideDismissed: Boolean = false
)

class PreferencesManager(private val context: Context) {
    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_GREETING_TYPE = stringPreferencesKey("greeting_type")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        private val KEY_VOICE_RESPONSE_ENABLED = booleanPreferencesKey("voice_response_enabled")
        private val KEY_TTS_SPEED = floatPreferencesKey("tts_speed")
        private val KEY_TTS_PITCH = floatPreferencesKey("tts_pitch")
        private val KEY_SELECTED_VOICE = stringPreferencesKey("selected_voice_name")
        private val KEY_REDMI_GUIDE = booleanPreferencesKey("redmi_guide_dismissed")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val langCode = prefs[KEY_LANGUAGE] ?: "hi"
        val lang = AssistantLanguage.entries.find { it.code == langCode } ?: AssistantLanguage.HINDI

        val greetingKey = prefs[KEY_GREETING_TYPE] ?: "wellbeing"
        val greeting = GreetingType.entries.find { it.key == greetingKey } ?: GreetingType.WELLBEING

        UserPreferences(
            language = lang,
            greetingType = greeting,
            themeMode = prefs[KEY_THEME_MODE] ?: "DARK",
            wakeWordEnabled = prefs[KEY_WAKE_WORD_ENABLED] ?: true,
            voiceResponseEnabled = prefs[KEY_VOICE_RESPONSE_ENABLED] ?: true,
            ttsSpeed = prefs[KEY_TTS_SPEED] ?: 1.0f,
            ttsPitch = prefs[KEY_TTS_PITCH] ?: 1.0f,
            selectedVoiceName = prefs[KEY_SELECTED_VOICE] ?: "",
            redmiGuideDismissed = prefs[KEY_REDMI_GUIDE] ?: false
        )
    }

    suspend fun setLanguage(language: AssistantLanguage) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language.code }
    }

    suspend fun setGreetingType(greetingType: GreetingType) {
        context.dataStore.edit { it[KEY_GREETING_TYPE] = greetingType.key }
    }

    suspend fun setThemeMode(themeMode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = themeMode }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun setVoiceResponseEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VOICE_RESPONSE_ENABLED] = enabled }
    }

    suspend fun setTtsSettings(speed: Float, pitch: Float, voiceName: String = "") {
        context.dataStore.edit {
            it[KEY_TTS_SPEED] = speed
            it[KEY_TTS_PITCH] = pitch
            if (voiceName.isNotEmpty()) {
                it[KEY_SELECTED_VOICE] = voiceName
            }
        }
    }

    suspend fun setRedmiGuideDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[KEY_REDMI_GUIDE] = dismissed }
    }

    suspend fun resetVoiceSettings() {
        context.dataStore.edit {
            it[KEY_TTS_SPEED] = 1.0f
            it[KEY_TTS_PITCH] = 1.0f
            it[KEY_SELECTED_VOICE] = ""
        }
    }
}
