package com.example.actions

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TimeActionResult(
    val handled: Boolean,
    val message: String,
    val spokenResponse: String
)

class AlarmTimerHelper(private val context: Context) {
    private val TAG = "AlarmTimerHelper"

    fun getCurrentTimeString(language: String = "hi"): String {
        val now = Date()
        val formatTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
        val formatDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(now)

        return when (language) {
            "ne" -> "अहिले $formatTime भएको छ। आज $formatDate हो।"
            "en" -> "The current time is $formatTime on $formatDate."
            else -> "अभी समय $formatTime हुआ है। आज $formatDate है।"
        }
    }

    fun parseAndHandle(query: String): TimeActionResult? {
        val lower = query.lowercase().trim()

        // 1. Current Time check
        if (lower.contains("time hua") || lower.contains("time kya") || lower.contains("kitna time") ||
            lower.contains("samay kya") || lower.contains("what time") || lower.contains("कति बज्यो") || lower.contains("समय")
        ) {
            val speech = getCurrentTimeString()
            return TimeActionResult(true, speech, speech)
        }

        // 2. Timer check e.g. "5 minute ka timer"
        if (lower.contains("timer") || lower.contains("टाइमर")) {
            val seconds = extractDurationInSeconds(lower)
            if (seconds > 0) {
                val success = setTimer(seconds, "Hello Umesh Timer")
                val minutes = seconds / 60
                val msg = if (success) {
                    if (minutes > 0) "$minutes मिनट का टाइमर लगा दिया गया है।" else "$seconds सेकंड का टाइमर शुरू हो गया है।"
                } else {
                    "टाइमर सेट करने में त्रुटि हुई।"
                }
                return TimeActionResult(true, msg, msg)
            }
        }

        // 3. Alarm check e.g. "6 baje alarm", "alarm lagao"
        if (lower.contains("alarm") || lower.contains("अलार्म")) {
            val time = extractAlarmTime(lower)
            if (time != null) {
                val (hour, minute) = time
                val success = setAlarm(hour, minute, "Hello Umesh Alarm")
                val msg = if (success) {
                    val amPm = if (hour < 12) "सुबह" else "शाम"
                    val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
                    "$amPm $displayHour बजकर ${String.format("%02d", minute)} मिनट का अलार्म लगा दिया गया है।"
                } else {
                    "अलार्म सेट करने में समस्या आई।"
                }
                return TimeActionResult(true, msg, msg)
            }
        }

        return null
    }

    private fun extractDurationInSeconds(text: String): Int {
        val hindiNumbers = mapOf(
            "ek" to 1, "do" to 2, "teen" to 3, "chaar" to 4, "paanch" to 5,
            "chhe" to 6, "saat" to 7, "aath" to 8, "nau" to 9, "das" to 10,
            "pandrah" to 15, "bees" to 20, "pachees" to 25, "tees" to 30,
            "ek" to 1, "एक" to 1, "दो" to 2, "तीन" to 3, "चार" to 4, "पाँच" to 5,
            "छह" to 6, "सात" to 7, "आठ" to 8, "नौ" to 9, "दस" to 10
        )

        val digitMatch = Regex("(\\d+)\\s*(minute|min|minat|सेकंड|second|sec|मिनट)").find(text)
        if (digitMatch != null) {
            val number = digitMatch.groupValues[1].toIntOrNull() ?: 1
            val unit = digitMatch.groupValues[2]
            return if (unit.startsWith("s") || unit.contains("सेकंड")) number else number * 60
        }

        for ((word, num) in hindiNumbers) {
            if (text.contains("$word minute") || text.contains("$word मिनट")) {
                return num * 60
            }
        }

        return 0
    }

    private fun extractAlarmTime(text: String): Pair<Int, Int>? {
        val match = Regex("(\\d{1,2})[:.]?(\\d{2})?\\s*(baje|बजे|am|pm)?").find(text)
        if (match != null) {
            var hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: 0

            val isEvening = text.contains("shaam") || text.contains("shyam") || text.contains("raat") ||
                    text.contains("pm") || text.contains("शाम") || text.contains("रात")
            val isMorning = text.contains("subah") || text.contains("morning") || text.contains("am") ||
                    text.contains("सुबह")

            if (isEvening && hour < 12) hour += 12
            if (isMorning && hour == 12) hour = 0

            return Pair(hour, minute)
        }
        return null
    }

    fun setTimer(seconds: Int, message: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set timer intent", e)
            false
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set alarm intent", e)
            false
        }
    }
}
