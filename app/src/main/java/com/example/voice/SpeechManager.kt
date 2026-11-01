package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechManager(private val context: Context) {
    private val TAG = "SpeechManager"
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isHandsFreeActive = MutableStateFlow(false)
    val isHandsFreeActive: StateFlow<Boolean> = _isHandsFreeActive.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    var onSpeechResult: ((String) -> Unit)? = null
    var onWakeWordDetected: ((phrase: String, remainingCommand: String) -> Unit)? = null
    var onErrorOccurred: ((String) -> Unit)? = null

    private var currentLocaleTag: String = "hi-IN"
    private var isExpectingCommandAfterWake: Boolean = false

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            _partialText.value = ""
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            _rmsDb.value = rmsdB.coerceIn(0f, 10f)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
            _rmsDb.value = 0f
        }

        override fun onError(error: Int) {
            _isListening.value = false
            _rmsDb.value = 0f

            val isTimeoutOrNoMatch = error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY

            // If in hands-free mode and just waiting for wake word, quietly restart listening
            if (_isHandsFreeActive.value && !isExpectingCommandAfterWake && isTimeoutOrNoMatch) {
                mainHandler.postDelayed({
                    if (_isHandsFreeActive.value && !_isListening.value) {
                        startListening(currentLocaleTag)
                    }
                }, 350)
                return
            }

            isExpectingCommandAfterWake = false
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error (ऑडियो रिकॉर्डिंग त्रुटि)"
                SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission needed (माइक अनुमति चाहिए)"
                SpeechRecognizer.ERROR_NETWORK -> "Network required for speech (इंटरनेट आवश्यक है)"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking clearly. (आवाज़ पहचान में नहीं आई)"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Voice server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout (कोई आवाज़ नहीं मिली)"
                else -> "Speech recognition error ($error)"
            }
            Log.w(TAG, "SpeechRecognizer error: $message")
            if (!isTimeoutOrNoMatch) {
                onErrorOccurred?.invoke(message)
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _rmsDb.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim() ?: ""
            if (recognizedText.isBlank()) {
                if (_isHandsFreeActive.value && !isExpectingCommandAfterWake) {
                    restartHandsFreeListening()
                }
                return
            }

            if (isExpectingCommandAfterWake) {
                isExpectingCommandAfterWake = false
                onSpeechResult?.invoke(recognizedText)
            } else {
                val wakeAnalysis = analyzeWakeWord(recognizedText)
                if (wakeAnalysis.isWakeWord) {
                    onWakeWordDetected?.invoke(wakeAnalysis.wakePhrase, wakeAnalysis.remainingCommand)
                } else if (!_isHandsFreeActive.value) {
                    // Normal button-triggered speech result
                    onSpeechResult?.invoke(recognizedText)
                } else {
                    // Hands-free mode heard non-wake background sound, re-listen for wake phrase
                    restartHandsFreeListening()
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull() ?: ""
            _partialText.value = partial

            if (!isExpectingCommandAfterWake) {
                val wakeAnalysis = analyzeWakeWord(partial)
                if (wakeAnalysis.isWakeWord) {
                    stopListening()
                    onWakeWordDetected?.invoke(wakeAnalysis.wakePhrase, wakeAnalysis.remainingCommand)
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun setHandsFreeMode(enabled: Boolean) {
        _isHandsFreeActive.value = enabled
        if (enabled) {
            startListening(currentLocaleTag)
        } else {
            stopListening()
        }
    }

    fun listenForCommand() {
        isExpectingCommandAfterWake = true
        startListening(currentLocaleTag)
    }

    fun restartHandsFreeListening() {
        isExpectingCommandAfterWake = false
        mainHandler.postDelayed({
            if (_isHandsFreeActive.value && !_isListening.value) {
                startListening(currentLocaleTag)
            }
        }, 300)
    }

    fun startListening(localeTag: String = "hi-IN") {
        currentLocaleTag = localeTag
        if (speechRecognizer == null) {
            initRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            _isListening.value = false
            onErrorOccurred?.invoke("Speech recognition start failed: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping listener", e)
        } finally {
            _isListening.value = false
            _rmsDb.value = 0f
        }
    }

    fun destroy() {
        try {
            _isHandsFreeActive.value = false
            mainHandler.removeCallbacksAndMessages(null)
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speechRecognizer", e)
        }
    }

    data class WakeWordAnalysis(
        val isWakeWord: Boolean,
        val wakePhrase: String,
        val remainingCommand: String
    )

    fun analyzeWakeWord(text: String): WakeWordAnalysis {
        val lower = text.lowercase(Locale.ROOT)
        val wakeKeywords = listOf(
            "hello maayra", "hey maayra", "hello mayra", "hey mayra",
            "हेलो मायरा", "हे मायरा", "मायरा", "maayra", "mayra",
            "hello umesh", "hey umesh", "हे उमेश", "हेलो उमेश",
            "हेल्लो उमेश", "नमस्ते उमेश"
        )

        for (keyword in wakeKeywords) {
            if (lower.contains(keyword)) {
                val index = lower.indexOf(keyword)
                val remaining = text.substring(index + keyword.length)
                    .replace(",", "")
                    .replace(":", "")
                    .trim()
                return WakeWordAnalysis(
                    isWakeWord = true,
                    wakePhrase = keyword,
                    remainingCommand = remaining
                )
            }
        }
        return WakeWordAnalysis(isWakeWord = false, wakePhrase = "", remainingCommand = "")
    }
}
