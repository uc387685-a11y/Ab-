package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.core.configureAppCheck
import com.example.service.WakeWordService
import com.example.ui.ChatViewModel
import com.example.ui.MainScreen
import com.example.ui.theme.HelloUmeshTheme

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MANDATORY: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        // Handle wake phrase intent if launched by background WakeWordService
        handleIncomingWakeIntent(intent)

        setContent {
            val userPrefs by chatViewModel.userPreferences.collectAsState()
            val isDark = when (userPrefs.themeMode) {
                "LIGHT" -> false
                "SYSTEM" -> isSystemInDarkTheme()
                else -> true // Default to professional dark theme
            }

            HelloUmeshTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(chatViewModel = chatViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingWakeIntent(intent)
    }

    private fun handleIncomingWakeIntent(intent: Intent?) {
        val wakePhrase = intent?.getStringExtra(WakeWordService.EXTRA_WAKE_PHRASE)
        val command = intent?.getStringExtra("extra_command")
        if (!command.isNullOrBlank()) {
            chatViewModel.sendMessage(command)
        } else if (!wakePhrase.isNullOrBlank()) {
            chatViewModel.startListening()
        }
    }
}
