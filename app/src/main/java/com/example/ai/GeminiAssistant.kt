package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiAssistant"

class GeminiAssistant {
    private val systemPrompt = """
        You are Hello Umesh AI (नमस्ते, मैं उमेश का पर्सनल AI असिस्टेंट हूँ।).
        You are an intelligent, friendly, and respectful personal assistant for Android smartphones (including Redmi devices).
        Languages:
        1. Hindi (हिंदी) - Primary conversational language.
        2. Nepali (नेपाली) - Fully supported and natural.
        3. English - Fluent and precise.

        Personality & Guidelines:
        - Conversational, warm, and helpful.
        - Answer in the language the user addresses you in (Hindi, Nepali, or English).
        - When requested, explain complicated concepts in simple Hindi or Nepali.
        - When asked about current events, world news, Nepal or India news, technology, or gaming, provide factual, objective summaries without inventing breaking facts.
        - Keep answers concise and readable for mobile screens. Format with clear paragraphs, bullet points, or bold headers where helpful.
    """.trimIndent()

    // Lazy initialization as mandated by Firebase AI Logic skill
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(systemPrompt) }
        )
    }

    /**
     * Unary response generation with 60-second timeout window.
     */
    suspend fun generateResponse(
        prompt: String,
        history: List<Pair<String, String>> = emptyList()
    ): Result<String> {
        return try {
            val responseText = withTimeout(60_000L) {
                if (history.isEmpty()) {
                    val res = model.generateContent(prompt)
                    res.text ?: "मुझे कोई उत्तर प्राप्त नहीं हुआ।"
                } else {
                    val chatHistory = history.map { (sender, text) ->
                        val role = if (sender == "user") "user" else "model"
                        content(role) { text(text) }
                    }
                    val chat = model.startChat(chatHistory)
                    val res = chat.sendMessage(prompt)
                    res.text ?: "मुझे कोई उत्तर प्राप्त नहीं हुआ।"
                }
            }
            Result.success(responseText)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini generation timed out", e)
            Result.failure(Exception("AI उत्तर में समय अधिक लगा (Timeout after 60s). कृपया पुनः प्रयास करें।"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generation error", e)
            Result.failure(e)
        }
    }

    /**
     * Streaming response generation to minimize Time-to-First-Token (TTFT).
     */
    fun generateStream(
        prompt: String,
        history: List<Pair<String, String>> = emptyList()
    ): Flow<String> = flow {
        withTimeout(60_000L) {
            if (history.isEmpty()) {
                model.generateContentStream(prompt).collect { chunk ->
                    val text = chunk.text
                    if (!text.isNullOrEmpty()) {
                        emit(text)
                    }
                }
            } else {
                val chatHistory = history.map { (sender, text) ->
                    val role = if (sender == "user") "user" else "model"
                    content(role) { text(text) }
                }
                val chat = model.startChat(chatHistory)
                chat.sendMessageStream(prompt).collect { chunk ->
                    val text = chunk.text
                    if (!text.isNullOrEmpty()) {
                        emit(text)
                    }
                }
            }
        }
    }

    /**
     * Multimodal generation for image analysis or photo editing requests.
     */
    suspend fun analyzeImage(bitmap: Bitmap, prompt: String): Result<String> {
        return try {
            val responseText = withTimeout(60_000L) {
                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }
                val res = model.generateContent(inputContent)
                res.text ?: "छवि का विश्लेषण पूर्ण हुआ।"
            }
            Result.success(responseText)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Multimodal analysis failed", e)
            Result.failure(e)
        }
    }
}
