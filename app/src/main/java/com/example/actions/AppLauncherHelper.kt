package com.example.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log

data class AppLaunchResult(
    val handled: Boolean,
    val appName: String,
    val success: Boolean,
    val message: String
)

class AppLauncherHelper(private val context: Context) {
    private val TAG = "AppLauncherHelper"

    private val supportedApps = listOf(
        AppTarget("YouTube", listOf("youtube", "yt", "यूट्यूब"), "com.google.android.youtube"),
        AppTarget("Facebook", listOf("facebook", "fb", "फेसबुक"), "com.facebook.katana"),
        AppTarget("Instagram", listOf("instagram", "insta", "इंस्टाग्राम"), "com.instagram.android"),
        AppTarget("WhatsApp", listOf("whatsapp", "व्हाट्सएप"), "com.whatsapp"),
        AppTarget("TikTok", listOf("tiktok", "टिकटॉक"), "com.zhiliaoapp.musically"),
        AppTarget("Google", listOf("google", "chrome", "गूगल", "क्रोम", "browser"), "com.android.chrome"),
        AppTarget("Settings", listOf("settings", "phone settings", "सेटिंग्स", "फोन सेटिंग्स"), "com.android.settings", isSettings = true),
        AppTarget("Camera", listOf("camera", "कैमरा"), "android.media.action.IMAGE_CAPTURE", isIntentAction = true),
        AppTarget("Clock", listOf("clock", "ghadi", "घड़ी"), "com.android.deskclock")
    )

    data class AppTarget(
        val displayName: String,
        val triggers: List<String>,
        val packageOrAction: String,
        val isSettings: Boolean = false,
        val isIntentAction: Boolean = false
    )

    fun tryParseAndLaunch(query: String): AppLaunchResult? {
        val lower = query.lowercase().trim()
        val isLaunchCommand = lower.contains("kholo") ||
                lower.contains("khol do") ||
                lower.contains("kholna") ||
                lower.contains("open") ||
                lower.contains("chalao") ||
                lower.contains("start") ||
                lower.contains("खोलो") ||
                lower.contains("चलाओ")

        if (!isLaunchCommand) return null

        for (app in supportedApps) {
            val matchedTrigger = app.triggers.any { trigger -> lower.contains(trigger) }
            if (matchedTrigger) {
                return launchApp(app)
            }
        }

        return null
    }

    fun launchApp(target: AppTarget): AppLaunchResult {
        return try {
            if (target.isSettings) {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                AppLaunchResult(true, target.displayName, true, "${target.displayName} खोल दिया गया है।")
            } else if (target.isIntentAction) {
                val intent = Intent(target.packageOrAction).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                AppLaunchResult(true, target.displayName, true, "${target.displayName} शुरू हो गया है।")
            } else {
                val packageManager = context.packageManager
                val launchIntent = packageManager.getLaunchIntentForPackage(target.packageOrAction)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    AppLaunchResult(true, target.displayName, true, "${target.displayName} खोल दिया गया है।")
                } else {
                    // Try web fallback for known apps
                    val fallbackUrl = when (target.displayName) {
                        "YouTube" -> "https://www.youtube.com"
                        "Facebook" -> "https://www.facebook.com"
                        "Instagram" -> "https://www.instagram.com"
                        "TikTok" -> "https://www.tiktok.com"
                        else -> null
                    }
                    if (fallbackUrl != null) {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(webIntent)
                        AppLaunchResult(true, target.displayName, true, "${target.displayName} ऐप इंस्टॉल नहीं है, ब्राउज़र में खोला जा रहा है।")
                    } else {
                        AppLaunchResult(
                            true,
                            target.displayName,
                            false,
                            "${target.displayName} ऐप आपके डिवाइस पर इंस्टॉल नहीं है। Play Store से डाउनलोड करें।"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch ${target.displayName}", e)
            AppLaunchResult(true, target.displayName, false, "ऐप खोलने में समस्या आई: ${e.localizedMessage}")
        }
    }
}
