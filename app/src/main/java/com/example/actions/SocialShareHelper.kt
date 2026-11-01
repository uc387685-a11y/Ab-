package com.example.actions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class SocialShareHelper(private val context: Context) {
    private val TAG = "SocialShareHelper"

    /**
     * Prepares and copies the spoken comment to clipboard and opens the target app or system share chooser.
     */
    fun shareComment(commentText: String, targetPackage: String? = null): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Hello Umesh Comment", commentText)
            clipboard.setPrimaryClip(clip)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, commentText)
                if (!targetPackage.isNullOrEmpty()) {
                    setPackage(targetPackage)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "कमेंट शेयर करें").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share comment", e)
            false
        }
    }

    /**
     * Shares a selected photo to Instagram, Facebook, TikTok, WhatsApp or system share.
     */
    fun sharePhoto(imageUri: Uri, targetPackage: String? = null, caption: String = ""): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                if (caption.isNotBlank()) {
                    putExtra(Intent.EXTRA_TEXT, caption)
                }
                if (!targetPackage.isNullOrEmpty()) {
                    setPackage(targetPackage)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "फोटो अपलोड या शेयर करें").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share photo", e)
            false
        }
    }
}
