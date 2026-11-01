package com.example.actions

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

data class DeviceActionResult(
    val handled: Boolean,
    val message: String
)

class DeviceControlHelper(private val context: Context) {
    private val TAG = "DeviceControlHelper"
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun parseAndHandle(query: String): DeviceActionResult? {
        val lower = query.lowercase().trim()

        // 1. Volume Controls
        if (lower.contains("volume") || lower.contains("आवाज") || lower.contains("sound")) {
            if (lower.contains("kam") || lower.contains("down") || lower.contains("ghatao") || lower.contains("कम")) {
                adjustVolume(AudioManager.ADJUST_LOWER)
                return DeviceActionResult(true, "वॉल्यूम कम कर दिया गया है।")
            }
            if (lower.contains("badhao") || lower.contains("up") || lower.contains("tez") || lower.contains("ज्यादा") || lower.contains("बढ़ाओ")) {
                adjustVolume(AudioManager.ADJUST_RAISE)
                return DeviceActionResult(true, "वॉल्यूम बढ़ा दिया गया है।")
            }
            if (lower.contains("mute") || lower.contains("band") || lower.contains("बंद")) {
                adjustVolume(AudioManager.ADJUST_MUTE)
                return DeviceActionResult(true, "वॉल्यूम म्यूट कर दिया गया है।")
            }
        }

        // 2. Wi-Fi Settings
        if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("वाईफाई")) {
            if (lower.contains("setting") || lower.contains("kholo") || lower.contains("on") || lower.contains("off")) {
                openSetting(Settings.ACTION_WIFI_SETTINGS)
                return DeviceActionResult(true, "Wi-Fi सेटिंग्स खोली जा रही हैं।")
            }
        }

        // 3. Bluetooth Settings
        if (lower.contains("bluetooth") || lower.contains("ब्लूटूथ")) {
            if (lower.contains("setting") || lower.contains("kholo") || lower.contains("on") || lower.contains("off")) {
                openSetting(Settings.ACTION_BLUETOOTH_SETTINGS)
                return DeviceActionResult(true, "ब्लूटूथ सेटिंग्स खोली जा रही हैं।")
            }
        }

        // 4. Brightness / Display Settings
        if (lower.contains("brightness") || lower.contains("display") || lower.contains("ब्राइटनेस") || lower.contains("स्क्रीन लाइट")) {
            openSetting(Settings.ACTION_DISPLAY_SETTINGS)
            return DeviceActionResult(true, "डिस्प्ले और ब्राइटनेस सेटिंग्स खोली जा रही हैं।")
        }

        // 5. Sound / Audio Settings
        if (lower.contains("sound setting") || lower.contains("रिंगटोन") || lower.contains("volume setting")) {
            openSetting(Settings.ACTION_SOUND_SETTINGS)
            return DeviceActionResult(true, "साउंड सेटिंग्स खोली जा रही हैं।")
        }

        return null
    }

    private fun adjustVolume(direction: Int) {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adjust volume", e)
        }
    }

    fun openSetting(action: String) {
        try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open setting $action", e)
        }
    }

    /**
     * Opens Battery Optimization / App Details for background wake-word execution on Xiaomi / Redmi devices.
     */
    fun openRedmiBatterySettings() {
        try {
            // First attempt: App details settings
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open application details", e)
            openSetting(Settings.ACTION_SETTINGS)
        }
    }

    fun openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                openSetting(Settings.ACTION_SETTINGS)
            }
        } else {
            openSetting(Settings.ACTION_SETTINGS)
        }
    }
}
