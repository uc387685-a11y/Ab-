package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

data class GeneratedAppProject(
    val appTitle: String,
    val description: String,
    val mainActivityCode: String,
    val gradleDependencies: String,
    val manifestCode: String,
    val buildAndRunInstructions: String
)

class GeminiAppBuilder {
    private val TAG = "GeminiAppBuilder"

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are the Hello Umesh AI App Builder Engine.
                    Your role is to help users build complete Android applications with Kotlin and Jetpack Compose.
                    Users will prompt you in Hindi (e.g. 'Ek simple calculator Android app banao'), Nepali, or English.
                    
                    When asked to create an app, provide a comprehensive production-ready response containing:
                    1. App Title & Summary in Hindi and English.
                    2. Complete MainActivity.kt code using Jetpack Compose, Material 3, clean architecture, and modern state management.
                    3. Required dependencies for app/build.gradle.kts.
                    4. AndroidManifest.xml permissions and tags.
                    5. Step-by-step instructions on how to run or export to Android Studio.
                    
                    Write pristine, complete Kotlin code with all required imports.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun generateApp(userPrompt: String): Result<GeneratedAppProject> {
        return try {
            val responseText = withTimeout(60_000L) {
                val prompt = "Create a complete native Android application in Kotlin with Jetpack Compose based on this requirement: $userPrompt"
                val res = model.generateContent(prompt)
                res.text ?: throw IllegalStateException("Model returned empty text.")
            }

            // Extract file blocks from the response
            val parsed = parseProjectFromResponse(userPrompt, responseText)
            Result.success(parsed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "App builder generation error", e)
            Result.failure(e)
        }
    }

    private fun parseProjectFromResponse(originalPrompt: String, fullResponse: String): GeneratedAppProject {
        // Look for Kotlin code block
        val kotlinRegex = Regex("```kotlin([\\s\\S]*?)```")
        val gradleRegex = Regex("```(?:kotlin|gradle|groovy)([\\s\\S]*?)```")
        val xmlRegex = Regex("```xml([\\s\\S]*?)```")

        val kotlinMatches = kotlinRegex.findAll(fullResponse).toList()
        val mainActivity = kotlinMatches.firstOrNull()?.groupValues?.get(1)?.trim()
            ?: "// Jetpack Compose Main Activity\n$fullResponse"

        val gradleCode = if (kotlinMatches.size > 1) {
            kotlinMatches[1].groupValues[1].trim()
        } else {
            gradleRegex.findAll(fullResponse).firstOrNull()?.groupValues?.get(1)?.trim()
                ?: """
                dependencies {
                    implementation(platform(libs.androidx.compose.bom))
                    implementation(libs.androidx.activity.compose)
                    implementation(libs.androidx.compose.material3)
                    implementation(libs.androidx.lifecycle.viewmodel.compose)
                }
                """.trimIndent()
        }

        val manifestCode = xmlRegex.find(fullResponse)?.groupValues?.get(1)?.trim()
            ?: """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application
                    android:allowBackup="true"
                    android:label="@string/app_name"
                    android:theme="@style/Theme.Material3.DayNight">
                    <activity
                        android:name=".MainActivity"
                        android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
            """.trimIndent()

        val title = if (originalPrompt.length > 30) originalPrompt.take(28) + "..." else originalPrompt

        return GeneratedAppProject(
            appTitle = title,
            description = fullResponse.take(250) + "...",
            mainActivityCode = mainActivity,
            gradleDependencies = gradleCode,
            manifestCode = manifestCode,
            buildAndRunInstructions = "Open Android Studio -> New Project -> Empty Compose Activity. Replace MainActivity.kt and build.gradle.kts with the code above. Click 'Run' (Shift + F10)."
        )
    }
}
