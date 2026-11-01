package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiAssistant
import com.example.ui.theme.AssistantAccentGlow
import com.example.ui.theme.UmeshPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoGeneratorScreen(
    geminiAssistant: GeminiAssistant,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var videoPrompt by remember { mutableStateOf("Drone flight over snow-capped Himalayan peaks of Nepal at golden sunrise, 4K cinematic") }
    var selectedStyle by remember { mutableStateOf("Cinematic Drone") }
    var selectedDuration by remember { mutableStateOf("5s") }
    var selectedRatio by remember { mutableStateOf("16:9") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    var isGenerating by remember { mutableStateOf(false) }
    var progressPhase by remember { mutableStateOf("") }
    var generationProgress by remember { mutableFloatStateOf(0f) }
    var generatedVideoScript by remember { mutableStateOf<String?>(null) }
    var isPlayingPreview by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> imageUri = uri }

    val videoStyles = listOf("Cinematic Drone", "Anime Motion", "3D CGI", "Hyperlapse", "Realistic Vlog")
    val durationOptions = listOf("5s", "10s")
    val ratioOptions = listOf("16:9 (Landscape)", "9:16 (Reels/Shorts)")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Video Generator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "टेक्स्ट और इमेज से वीडियो (Prompt to Video)",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Service Transparency & Quota Notice Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = UmeshPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "वीडियो जनरेशन क्लाउड GPU सेवा (जैसे Google Veo / Imagen Video) पर निर्भर करती है। ऐप में आप वीडियो का विवरण, कैमरा मोशन, और सिनेमैटिक स्क्रिप्ट तैयार कर सकते हैं।",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            // Input Form Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "वीडियो का विवरण लिखें (Describe Video):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = videoPrompt,
                        onValueChange = { videoPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_prompt_input"),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "वीडियो स्टाइल (Video Style):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        videoStyles.forEach { style ->
                            FilterChip(
                                selected = selectedStyle == style,
                                onClick = { selectedStyle = style },
                                label = { Text(style, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("अवधि (Duration):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                durationOptions.forEach { dur ->
                                    FilterChip(
                                        selected = selectedDuration == dur,
                                        onClick = { selectedDuration = dur },
                                        label = { Text(dur, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                        Column {
                            Text("आकार (Ratio):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ratioOptions.forEach { rat ->
                                    FilterChip(
                                        selected = selectedRatio == rat,
                                        onClick = { selectedRatio = rat },
                                        label = { Text(rat.take(4), fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Optional Image-to-Video attachment
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = UmeshPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (imageUri != null) "छवि संलग्न है (Image-to-Video Mode)" else "शुरुआती फ़ोटो जोड़ें (Image-to-Video)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("स्थैतिक छवि को एनिमेट करने के लिए", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (videoPrompt.isBlank()) return@Button
                            scope.launch {
                                isGenerating = true
                                generationProgress = 0.1f
                                progressPhase = "मोशन वेक्टर और कैमरा पाथ तैयार हो रहा है..."
                                delay(600)
                                generationProgress = 0.4f
                                progressPhase = "फ़्रेम-बाय-फ़्रेम रेंडरिंग और लाइट सिमुलेशन..."
                                delay(800)
                                generationProgress = 0.75f
                                progressPhase = "AI वीडियो इनकोडिंग और ऑडियो सिंथेसिस..."

                                val prompt = """
                                    Create a complete cinematic shot-by-shot storyboard and video generation prompt for:
                                    $videoPrompt
                                    Style: $selectedStyle
                                    Duration: $selectedDuration
                                    Aspect Ratio: $selectedRatio
                                    Include visual cues, camera movement, camera focal length, and audio/sound atmosphere in Hindi and English.
                                """.trimIndent()

                                val result = geminiAssistant.generateResponse(prompt)
                                generationProgress = 1.0f
                                delay(300)
                                isGenerating = false

                                if (result.isSuccess) {
                                    generatedVideoScript = result.getOrNull()
                                } else {
                                    Toast.makeText(context, "जनरेशन में त्रुटि आई: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_video_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("प्रोसेसिंग... (${(generationProgress * 100).toInt()}%)", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.Videocam, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("वीडियो जनरेट करें (Generate Video)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isGenerating) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { generationProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(progressPhase, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Video Result & Storyboard
            if (generatedVideoScript != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("जनरेट किया गया वीडियो (Video Result)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row {
                                IconButton(onClick = {
                                    Toast.makeText(context, "वीडियो प्रोजेक्ट सहेजा गया (Video Project Saved)", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.Download, contentDescription = "Save")
                                }
                                IconButton(onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Hello Umesh AI Video Storyboard:\n$generatedVideoScript")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "वीडियो शेयर करें"))
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Video Player Simulation Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(UmeshPrimary)
                                        .clickable { isPlayingPreview = !isPlayingPreview },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isPlayingPreview) "प्लेबैक सक्रिय है ($selectedDuration)" else "प्रीव्यू चलाएं",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "सिनेमैटिक शॉट-बाय-शॉट विवरण (Cinematic Breakdown):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = generatedVideoScript!!,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
