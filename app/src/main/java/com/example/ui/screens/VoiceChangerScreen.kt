package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.PreferencesManager
import com.example.ui.theme.AssistantAccentGlow
import com.example.ui.theme.MicActiveRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.UmeshPrimary
import com.example.voice.AudioRecordManager
import com.example.voice.TtsManager
import com.example.voice.VoicePreset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChangerScreen(
    ttsManager: TtsManager,
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    val audioRecordManager = remember { AudioRecordManager(context) }
    DisposableEffect(Unit) {
        onDispose { audioRecordManager.cleanup() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Voice Studio & Changer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "असिस्टेंट वॉइस व ऑडियो इफ़ेक्ट्स",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("A. असिस्टेंट आवाज़ (TTS)", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("B. यूज़र वॉइस कनवर्टर", fontSize = 13.sp) }
                )
            }

            if (selectedTab == 0) {
                AssistantVoiceTab(ttsManager, preferencesManager)
            } else {
                UserVoiceConverterTab(audioRecordManager)
            }
        }
    }
}

@Composable
private fun AssistantVoiceTab(
    ttsManager: TtsManager,
    preferencesManager: PreferencesManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by preferencesManager.preferencesFlow.collectAsState(initial = null)
    val availableVoices by ttsManager.availableVoices.collectAsState()

    var speed by remember { mutableFloatStateOf(prefs?.ttsSpeed ?: 1.0f) }
    var pitch by remember { mutableFloatStateOf(prefs?.ttsPitch ?: 1.0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "असिस्टेंट आवाज़ नियंत्रण (Assistant Voice Tuning)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = UmeshPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Speech Rate Slider
                Text(
                    text = "बोलने की गति (Speech Speed): ${String.format("%.1fx", speed)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = speed,
                    onValueChange = {
                        speed = it
                        ttsManager.setSpeed(it)
                    },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Speech Pitch Slider
                Text(
                    text = "आवाज़ की पिच (Pitch Tone): ${String.format("%.1fx", pitch)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = pitch,
                    onValueChange = {
                        pitch = it
                        ttsManager.setPitch(it)
                    },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            ttsManager.setSpeed(speed)
                            ttsManager.setPitch(pitch)
                            ttsManager.speak("नमस्ते उमेश! मैं आपकी नई आवाज़ की सेटिंग का परीक्षण कर रहा हूँ।")
                        },
                        modifier = Modifier.testTag("test_voice_button")
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("आवाज़ टेस्ट करें")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                preferencesManager.setTtsSettings(speed, pitch)
                                Toast.makeText(context, "सेटिंग्स सुरक्षित की गईं!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("save_voice_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("सहेजें (Save)")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        speed = 1.0f
                        pitch = 1.0f
                        ttsManager.setSpeed(1.0f)
                        ttsManager.setPitch(1.0f)
                        scope.launch { preferencesManager.resetVoiceSettings() }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("डिफ़ॉल्ट सेटिंग्स रीसेट करें")
                }
            }
        }
    }
}

@Composable
private fun UserVoiceConverterTab(audioRecordManager: AudioRecordManager) {
    val context = LocalContext.current
    val isRecording by audioRecordManager.isRecording.collectAsState()
    val isPlaying by audioRecordManager.isPlaying.collectAsState()
    val hasRecording by audioRecordManager.hasRecording.collectAsState()
    val currentPreset by audioRecordManager.currentPreset.collectAsState()

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            audioRecordManager.startRecording()
        } else {
            Toast.makeText(context, "माइक अनुमति आवश्यक है", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Recording Control Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "अपनी आवाज़ रिकॉर्ड करें (Record Audio)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isRecording) "रिकॉर्डिंग जारी है... बोलिए..." else "बटन दबाकर बोलना शुरू करें",
                    fontSize = 13.sp,
                    color = if (isRecording) MicActiveRed else MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) MicActiveRed else UmeshPrimary)
                        .clickable {
                            if (isRecording) {
                                audioRecordManager.stopRecording()
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    audioRecordManager.startRecording()
                                } else {
                                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Voice Conversion Presets Card
        if (hasRecording) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "आवाज़ बदलने के प्रीसेट (Voice Presets):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = UmeshPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    VoicePreset.entries.forEach { preset ->
                        val isSelected = currentPreset == preset && isPlaying
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                .clickable {
                                    audioRecordManager.playWithPreset(preset)
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = preset.titleHi,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Pitch: ${preset.pitch}x • Speed: ${preset.speed}x",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play with preset",
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(onClick = { audioRecordManager.deleteRecording() }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("रिकॉर्डिंग मिटाएँ")
                        }

                        Button(onClick = {
                            Toast.makeText(context, "परिवर्तित ऑडियो सुरक्षित किया गया!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ऑडियो सहेजें")
                        }
                    }
                }
            }
        }
    }
}
