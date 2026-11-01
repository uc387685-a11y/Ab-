package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {
    private val TAG = "TtsManager"
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<VoiceInfo>> = _availableVoices.asStateFlow()

    private var currentSpeed = 1.0f
    private var currentPitch = 1.0f
    private var currentLocale: Locale = Locale("hi", "IN")
    private var currentVoiceName: String? = null

    data class VoiceInfo(
        val name: String,
        val locale: String,
        val isFemaleProbable: Boolean
    )

    init {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupProgressListener()
            setLanguage(currentLocale)
            updateVoiceList()
            applyParameters()
            Log.d(TAG, "TTS initialized successfully")
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
        }
    }

    private fun setupProgressListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                Log.w(TAG, "TTS utterance error: $errorCode")
            }
        })
    }

    fun setLanguage(locale: Locale) {
        currentLocale = locale
        if (!isInitialized) return
        val result = textToSpeech?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "TTS Language not supported: ${locale.language}. Falling back to default.")
            textToSpeech?.setLanguage(Locale.ENGLISH)
        }
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.5f, 2.0f)
        textToSpeech?.setSpeechRate(currentSpeed)
    }

    fun setPitch(pitch: Float) {
        currentPitch = pitch.coerceIn(0.5f, 2.0f)
        textToSpeech?.setPitch(currentPitch)
    }

    fun setVoiceByName(voiceName: String) {
        currentVoiceName = voiceName
        if (!isInitialized || voiceName.isEmpty()) return
        val voice = textToSpeech?.voices?.find { it.name == voiceName }
        if (voice != null) {
            textToSpeech?.voice = voice
        }
    }

    private fun applyParameters() {
        textToSpeech?.setSpeechRate(currentSpeed)
        textToSpeech?.setPitch(currentPitch)
        if (!currentVoiceName.isNullOrEmpty()) {
            setVoiceByName(currentVoiceName!!)
        }
    }

    fun updateVoiceList() {
        if (!isInitialized) return
        try {
            val voices = textToSpeech?.voices?.map { v ->
                VoiceInfo(
                    name = v.name,
                    locale = v.locale.displayName,
                    isFemaleProbable = v.name.lowercase().contains("female") || v.name.contains("f0")
                )
            } ?: emptyList()
            _availableVoices.value = voices
        } catch (e: Exception) {
            Log.w(TAG, "Could not query available TTS voices", e)
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isInitialized) {
            Log.w(TAG, "TTS speak called before initialization.")
            return
        }

        // Clean markdown symbols (e.g. asterisks, backticks) for smooth natural speech
        val cleanSpeech = text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`{1,3}.*?`{1,3}"), "")
            .replace("#", "")
            .trim()

        if (cleanSpeech.isBlank()) return

        stop()
        val utteranceId = "utterance_${System.currentTimeMillis()}"
        textToSpeech?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        try {
            if (textToSpeech?.isSpeaking == true) {
                textToSpeech?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        } finally {
            _isSpeaking.value = false
        }
    }

    fun shutdown() {
        try {
            stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
