package com.example.voice

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class VoicePreset(val titleHi: String, val titleEn: String, val pitch: Float, val speed: Float) {
    ORIGINAL("सामान्य (Normal)", "Original Normal", 1.0f, 1.0f),
    CHIPMUNK("हीलियम / चूहिया (Helium)", "Chipmunk Helium", 1.75f, 1.2f),
    DEEP_TITAN("गहरा टाइटन (Deep Titan)", "Deep Voice Titan", 0.60f, 0.9f),
    ROBOT("रोबोट AI (Robot)", "Robot AI Effect", 0.70f, 0.85f),
    ALIEN("एलियन (Alien)", "Cosmic Alien", 1.45f, 1.15f),
    WALKIE_TALKIE("वॉकी-टॉकी (Radio)", "Walkie Talkie Radio", 1.10f, 1.05f)
}

class AudioRecordManager(private val context: Context) {
    private val TAG = "AudioRecordManager"

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _hasRecording = MutableStateFlow(false)
    val hasRecording: StateFlow<Boolean> = _hasRecording.asStateFlow()

    private val _currentPreset = MutableStateFlow(VoicePreset.ORIGINAL)
    val currentPreset: StateFlow<VoicePreset> = _currentPreset.asStateFlow()

    fun startRecording(): Boolean {
        stopPlayback()
        return try {
            val file = File(context.cacheDir, "umesh_voice_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = file

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            _isRecording.value = true
            _hasRecording.value = false
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            _isRecording.value = false
            false
        }
    }

    fun stopRecording(): File? {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isRecording.value = false
            _hasRecording.value = currentRecordingFile?.exists() == true
            return currentRecordingFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            _isRecording.value = false
            return null
        }
    }

    fun playWithPreset(preset: VoicePreset) {
        val file = currentRecordingFile ?: return
        if (!file.exists()) return

        stopPlayback()
        _currentPreset.value = preset

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val params = PlaybackParams().apply {
                        pitch = preset.pitch
                        speed = preset.speed
                    }
                    playbackParams = params
                }

                setOnCompletionListener {
                    _isPlaying.value = false
                }
                start()
            }
            _isPlaying.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio with preset", e)
            _isPlaying.value = false
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback", e)
        } finally {
            _isPlaying.value = false
        }
    }

    fun deleteRecording() {
        stopPlayback()
        currentRecordingFile?.delete()
        currentRecordingFile = null
        _hasRecording.value = false
    }

    fun cleanup() {
        try {
            if (_isRecording.value) stopRecording()
            stopPlayback()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error", e)
        }
    }
}
