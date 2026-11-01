package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.actions.ContactItem
import com.example.data.local.MessageEntity
import com.example.ui.AiChatStatus
import com.example.ui.ChatViewModel
import com.example.ui.theme.AssistantAccentGlow
import com.example.ui.theme.MicActiveRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.UmeshPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOnline by viewModel.isOnline.collectAsState()
    val userPrefs by viewModel.userPreferences.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val chatStatus by viewModel.chatStatus.collectAsState()
    val isListening by viewModel.speechManager.isListening.collectAsState()
    val isHandsFree by viewModel.speechManager.isHandsFreeActive.collectAsState()
    val rmsDb by viewModel.speechManager.rmsDb.collectAsState()
    val isSpeaking by viewModel.ttsManager.isSpeaking.collectAsState()
    val pendingCallContact by viewModel.pendingCallContact.collectAsState()
    val matchingContacts by viewModel.matchingContacts.collectAsState()
    val pendingCommentText by viewModel.pendingCommentText.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Permission launchers
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            Toast.makeText(context, "माइक्रोफ़ोन अनुमति आवश्यक है (Microphone permission required)", Toast.LENGTH_SHORT).show()
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        pendingCallContact?.let { contact ->
            viewModel.confirmCall(contact)
        }
    }

    LaunchedEffect(messages.size, chatStatus) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Hello Umesh AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Status indicator pill
                            val statusColor = if (isOnline) SuccessGreen else MaterialTheme.colorScheme.error
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOnline) "Online" else "Offline",
                                fontSize = 12.sp,
                                color = statusColor
                            )
                        }
                        Text(
                            text = when (userPrefs.language.code) {
                                "ne" -> "तपाईंको निजी AI सहायक"
                                "en" -> "Your Personal AI Assistant"
                                else -> "आपका निजी AI सहायक"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Stop speaking button if active
                    if (isSpeaking) {
                        IconButton(
                            onClick = { viewModel.stopSpeaking() },
                            modifier = Modifier.testTag("stop_speaking_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "आवाज़ बंद करें (Stop speaking)",
                                tint = MicActiveRed
                            )
                        }
                    }

                    // Replay last speech
                    IconButton(
                        onClick = { viewModel.repeatLastResponse() },
                        modifier = Modifier.testTag("repeat_response_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "पुनः सुनें (Repeat response)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Voice response toggle
                    IconButton(
                        onClick = {
                            scope.launch {
                                viewModel.preferencesManager.setVoiceResponseEnabled(!userPrefs.voiceResponseEnabled)
                            }
                        },
                        modifier = Modifier.testTag("toggle_voice_response")
                    ) {
                        Icon(
                            imageVector = if (userPrefs.voiceResponseEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "वॉइस उत्तर टॉगल (Toggle voice response)",
                            tint = if (userPrefs.voiceResponseEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    // New Chat
                    IconButton(
                        onClick = { viewModel.startNewConversation() },
                        modifier = Modifier.testTag("new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "नई बातचीत (New conversation)",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // History
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.testTag("chat_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "इतिहास (Chat history)",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Offline Warning Banner
            if (!isOnline) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Offline indicator",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ऑफ़लाइन मोड सक्रिय है। ऐप्स खोलना, टाइमर, अलार्म और कॉल स्थानीय रूप से समर्थित हैं।",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatMessageItem(
                        message = message,
                        onRepeat = { viewModel.ttsManager.speak(message.text) }
                    )
                }

                // Streaming / Thinking Indicator
                if (chatStatus is AiChatStatus.Streaming) {
                    item {
                        val streamText = (chatStatus as AiChatStatus.Streaming).partialText
                        StreamingBubble(text = streamText)
                    }
                } else if (chatStatus is AiChatStatus.Thinking) {
                    item {
                        ThinkingBubble()
                    }
                }

                // Error Card
                if (chatStatus is AiChatStatus.Error) {
                    item {
                        val errorMsg = (chatStatus as AiChatStatus.Error).message
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "त्रुटि",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "त्रुटि (Error): $errorMsg",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.sendMessage("पुनः प्रयास करें") },
                                    modifier = Modifier.testTag("retry_button")
                                ) {
                                    Text("पुनः प्रयास करें (Retry)")
                                }
                            }
                        }
                    }
                }

                // Confirmation Card for Contact Call
                if (pendingCallContact != null) {
                    item {
                        val contact = pendingCallContact!!
                        ContactCallCard(
                            contact = contact,
                            onConfirm = {
                                val hasCallPermission = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.CALL_PHONE
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasCallPermission) {
                                    viewModel.confirmCall(contact)
                                } else {
                                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                }
                            },
                            onCancel = { viewModel.cancelCall() }
                        )
                    }
                }

                // Multiple Contacts Selection Card
                if (matchingContacts.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "कृपया संपर्क चुनें:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                matchingContacts.forEach { contact ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.confirmCall(contact) }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(contact.phoneNumber, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Icon(Icons.Default.Call, contentDescription = "Call", tint = SuccessGreen)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { viewModel.cancelCall() },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("रद्द करें")
                                }
                            }
                        }
                    }
                }

                // Confirmation Card for Social Comment
                if (pendingCommentText != null) {
                    item {
                        CommentConfirmCard(
                            comment = pendingCommentText!!,
                            onShare = { pkg -> viewModel.confirmCommentShare(pendingCommentText!!, pkg) },
                            onCancel = { viewModel.cancelCommentShare() }
                        )
                    }
                }
            }

            // Waveform Audio Visualizer when Listening
            AnimatedVisibility(visible = isListening, enter = fadeIn(), exit = fadeOut()) {
                AudioWaveVisualizer(rmsDb = rmsDb)
            }

            // Hands-Free "Hello Maayra" Activation Mode Bar
            Surface(
                color = if (isHandsFree) AssistantAccentGlow.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val hasMic = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasMic) {
                            viewModel.toggleHandsFreeMode()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isHandsFree) AssistantAccentGlow else MaterialTheme.colorScheme.outline)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHandsFree) "Hands-Free सक्रिय: 'Hello Maayra' बोलें" else "Hands-Free चालू करें (बिना बटन दबाए 'Hello Maayra' बोलें)",
                            fontSize = 12.sp,
                            fontWeight = if (isHandsFree) FontWeight.Bold else FontWeight.Medium,
                            color = if (isHandsFree) AssistantAccentGlow else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (isHandsFree) "ON" else "OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHandsFree) SuccessGreen else MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Bottom Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Microphone Button
                    val micColor by animateColorAsState(
                        targetValue = if (isListening) MicActiveRed else UmeshPrimary,
                        label = "mic_color"
                    )
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isListening) 1.25f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    IconButton(
                        onClick = {
                            if (isListening) {
                                viewModel.stopListening()
                            } else {
                                val hasMicPermission = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasMicPermission) {
                                    viewModel.startListening()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(micColor)
                            .testTag("mic_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                            contentDescription = if (isListening) "सुनना बंद करें" else "बोलकर पूछें",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Text Input
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = when (userPrefs.language.code) {
                                    "ne" -> "केही सोध्नुहोस् वा आज्ञा दिनुहोस्..."
                                    "en" -> "Ask Hello Umesh or command..."
                                    else -> "कुछ पूछें या कमांड दें..."
                                },
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "भेजें (Send)",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: MessageEntity,
    onRepeat: () -> Unit
) {
    val isUser = message.sender == "user"
    val context = LocalContext.current

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val timeString = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AssistantAccentGlow)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hello Umesh AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AssistantAccentGlow
                        )
                    }
                }

                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeString,
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )

                    if (!isUser) {
                        Row {
                            IconButton(
                                onClick = onRepeat,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "आवाज़ सुनें",
                                    tint = textColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Hello Umesh AI", message.text))
                                    Toast.makeText(context, "कॉपी किया गया (Copied)", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "कॉपी करें",
                                    tint = textColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingBubble(text: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.widthIn(max = 320.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Hello Umesh AI लिख रहा है...",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = text.ifEmpty { "..." },
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.widthIn(max = 240.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "सोच रहा हूँ... (Thinking)",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AudioWaveVisualizer(rmsDb: Float) {
    val barCount = 18
    val heightMultiplier = (rmsDb * 4f).coerceIn(6f, 32f)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "सुन रहा हूँ... बोलिए (Listening...)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MicActiveRed,
                modifier = Modifier.padding(end = 12.dp)
            )
            for (i in 0 until barCount) {
                val variableHeight = (heightMultiplier * (0.4f + (i % 4) * 0.25f)).dp
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .width(4.dp)
                        .height(variableHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(AssistantAccentGlow, UmeshPrimary)
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun ContactCallCard(
    contact: ContactItem,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SuccessGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "फोन कॉल की पुष्टि (Confirm Call)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${contact.name} • ${contact.phoneNumber}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("cancel_call_button")
                ) {
                    Text("रद्द करें")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.testTag("confirm_call_button")
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("कॉल करें (Call Now)")
                }
            }
        }
    }
}

@Composable
private fun CommentConfirmCard(
    comment: String,
    onShare: (String?) -> Unit,
    onCancel: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "सोशल मीडिया कमेंट (Social Comment Ready)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "\"$comment\"",
                    modifier = Modifier.padding(10.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text("रद्द करें")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onShare(null) }) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("शेयर या पोस्ट करें")
                }
            }
        }
    }
}
