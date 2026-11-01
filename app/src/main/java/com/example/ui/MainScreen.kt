package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.GeminiAppBuilder
import com.example.ui.screens.AppBuilderScreen
import com.example.ui.screens.ChatHistorySheet
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.PhotoGeneratorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoGeneratorScreen
import com.example.ui.screens.VoiceChangerScreen
import com.example.ui.theme.AssistantAccentGlow
import com.example.ui.theme.UmeshPrimary

enum class AppDestination(val route: String, val labelHi: String, val labelEn: String, val icon: ImageVector) {
    CHAT("chat", "असिस्टेंट", "Chat", Icons.Default.Chat),
    PHOTO("photo", "फ़ोटो AI", "Photo", Icons.Default.AutoAwesome),
    VIDEO("video", "वीडियो AI", "Video", Icons.Default.Videocam),
    BUILDER("builder", "ऐप बिल्डर", "Builder", Icons.Default.Build),
    VOICE("voice", "वॉइस", "Voice", Icons.Default.RecordVoiceOver),
    SETTINGS("settings", "सेटिंग्स", "Settings", Icons.Default.Settings)
}

@Composable
fun MainScreen(
    chatViewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(AppDestination.CHAT) }
    var showHistorySheet by remember { mutableStateOf(false) }

    val userPrefs by chatViewModel.userPreferences.collectAsState()
    val isNepali = userPrefs.language.code == "ne"
    val isEnglish = userPrefs.language.code == "en"

    val appBuilder = remember { GeminiAppBuilder() }

    // Android back button handling
    if (currentDestination != AppDestination.CHAT) {
        BackHandler {
            currentDestination = AppDestination.CHAT
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                AppDestination.entries.forEach { destination ->
                    val isSelected = currentDestination == destination
                    val label = when {
                        isEnglish -> destination.labelEn
                        isNepali -> destination.labelHi
                        else -> destination.labelHi
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = label
                            )
                        },
                        label = { Text(label, fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.outline,
                            unselectedTextColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.testTag("nav_${destination.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                AppDestination.CHAT -> {
                    ChatScreen(
                        viewModel = chatViewModel,
                        onOpenHistory = { showHistorySheet = true }
                    )
                }
                AppDestination.PHOTO -> {
                    PhotoGeneratorScreen(
                        geminiAssistant = chatViewModel.geminiAssistant
                    )
                }
                AppDestination.VIDEO -> {
                    VideoGeneratorScreen(
                        geminiAssistant = chatViewModel.geminiAssistant
                    )
                }
                AppDestination.BUILDER -> {
                    AppBuilderScreen(
                        appBuilder = appBuilder
                    )
                }
                AppDestination.VOICE -> {
                    VoiceChangerScreen(
                        ttsManager = chatViewModel.ttsManager,
                        preferencesManager = chatViewModel.preferencesManager
                    )
                }
                AppDestination.SETTINGS -> {
                    SettingsScreen(
                        preferencesManager = chatViewModel.preferencesManager
                    )
                }
            }

            if (showHistorySheet) {
                ChatHistorySheet(
                    viewModel = chatViewModel,
                    onDismiss = { showHistorySheet = false }
                )
            }
        }
    }
}
