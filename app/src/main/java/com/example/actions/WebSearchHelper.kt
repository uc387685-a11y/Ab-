package com.example.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.net.URLEncoder

data class SearchIntentResult(
    val handled: Boolean,
    val query: String,
    val platform: String,
    val message: String
)

class WebSearchHelper(private val context: Context) {
    private val TAG = "WebSearchHelper"

    fun parseAndHandle(text: String): SearchIntentResult? {
        val lower = text.lowercase().trim()

        // 1. YouTube search e.g. "YouTube par PUBG headshot video khojo"
        if (lower.contains("youtube") || lower.contains("यूट्यूब")) {
            if (lower.contains("search") || lower.contains("khojo") || lower.contains("video") || lower.contains("ढूंढो") || lower.contains("खोजो")) {
                val cleanedQuery = cleanQuery(lower, listOf("youtube", "par", "video", "search", "khojo", "karo", "dikhaye", "kholo", "dekho", "यूट्यूब", "पर", "खोजो"))
                if (cleanedQuery.isNotBlank()) {
                    openYouTubeSearch(cleanedQuery)
                    return SearchIntentResult(true, cleanedQuery, "YouTube", "YouTube पर '$cleanedQuery' खोजा जा रहा है।")
                }
            }
        }

        // 2. Facebook search e.g. "Facebook par ye page search karo"
        if (lower.contains("facebook") || lower.contains("फेसबुक")) {
            if (lower.contains("search") || lower.contains("khojo") || lower.contains("page") || lower.contains("खोजो")) {
                val cleanedQuery = cleanQuery(lower, listOf("facebook", "par", "page", "search", "khojo", "karo", "account", "फेसबुक", "पर", "खोजो"))
                if (cleanedQuery.isNotBlank()) {
                    openFacebookSearch(cleanedQuery)
                    return SearchIntentResult(true, cleanedQuery, "Facebook", "Facebook पर '$cleanedQuery' खोजा जा रहा है।")
                }
            }
        }

        // 3. Instagram search e.g. "Instagram par ye account search karo"
        if (lower.contains("instagram") || lower.contains("insta") || lower.contains("इंस्टाग्राम")) {
            if (lower.contains("search") || lower.contains("khojo") || lower.contains("account") || lower.contains("profile") || lower.contains("खोजो")) {
                val cleanedQuery = cleanQuery(lower, listOf("instagram", "insta", "par", "account", "profile", "search", "khojo", "karo", "इंस्टाग्राम", "पर", "खोजो"))
                if (cleanedQuery.isNotBlank()) {
                    openInstagramSearch(cleanedQuery)
                    return SearchIntentResult(true, cleanedQuery, "Instagram", "Instagram पर '$cleanedQuery' खोजा जा रहा है।")
                }
            }
        }

        // 4. General Google Search query
        if (lower.startsWith("search ") || lower.contains("google search") || lower.contains("गूगल पर सर्च")) {
            val query = cleanQuery(lower, listOf("google search", "google par search", "search", "khojo", "karo", "kripya", "गूगल पर सर्च"))
            if (query.isNotBlank()) {
                openGoogleSearch(query)
                return SearchIntentResult(true, query, "Google", "Google पर '$query' सर्च किया जा रहा है।")
            }
        }

        return null
    }

    private fun cleanQuery(text: String, stopWords: List<String>): String {
        var result = text.replace("hello umesh", "")
            .replace("hey umesh", "")
            .replace("हेलो उमेश", "")
            .replace("हे उमेश", "")
        for (w in stopWords) {
            result = result.replace(w, "")
        }
        return result.replace(":", "").trim()
    }

    fun openGoogleSearch(query: String) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val uri = Uri.parse("https://www.google.com/search?q=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch web search", e)
        }
    }

    fun openYouTubeSearch(query: String) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val appUri = Uri.parse("vnd.youtube://results?search_query=$encoded")
        val webUri = Uri.parse("https://www.youtube.com/results?search_query=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, appUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun openFacebookSearch(query: String) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val uri = Uri.parse("https://www.facebook.com/search/top/?q=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Facebook search", e)
        }
    }

    fun openInstagramSearch(query: String) {
        val cleanTagOrName = query.replace("@", "").trim()
        val uri = Uri.parse("https://www.instagram.com/explore/tags/$cleanTagOrName/")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Instagram search", e)
        }
    }
}
