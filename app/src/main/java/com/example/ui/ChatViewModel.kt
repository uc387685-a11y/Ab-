package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.actions.AlarmTimerHelper
import com.example.actions.AppLauncherHelper
import com.example.actions.ContactCallerHelper
import com.example.actions.ContactItem
import com.example.actions.DeviceControlHelper
import com.example.actions.SocialShareHelper
import com.example.actions.WebSearchHelper
import com.example.ai.GeminiAssistant
import com.example.core.AssistantLanguage
import com.example.core.GreetingType
import com.example.core.NetworkMonitor
import com.example.core.PreferencesManager
import com.example.core.UserPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.voice.SpeechManager
import com.example.voice.TtsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

sealed interface AiChatStatus {
    data object Idle : AiChatStatus
    data object Listening : AiChatStatus
    data object Thinking : AiChatStatus
    data class Streaming(val partialText: String) : AiChatStatus
    data class Error(val message: String) : AiChatStatus
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ChatViewModel"
    private val context = application.applicationContext

    private val database = AppDatabase.getInstance(context)
    private val chatDao = database.chatDao()

    val networkMonitor = NetworkMonitor(context)
    val preferencesManager = PreferencesManager(context)

    val geminiAssistant = GeminiAssistant()
    val speechManager = SpeechManager(context)
    val ttsManager = TtsManager(context)

    val appLauncherHelper = AppLauncherHelper(context)
    val contactCallerHelper = ContactCallerHelper(context)
    val alarmTimerHelper = AlarmTimerHelper(context)
    val deviceControlHelper = DeviceControlHelper(context)
    val webSearchHelper = WebSearchHelper(context)
    val socialShareHelper = SocialShareHelper(context)

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val userPreferences: StateFlow<UserPreferences> = preferencesManager.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    val conversations: StateFlow<List<ConversationEntity>> = chatDao.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _chatStatus = MutableStateFlow<AiChatStatus>(AiChatStatus.Idle)
    val chatStatus: StateFlow<AiChatStatus> = _chatStatus.asStateFlow()

    // Confirmation dialogues state
    private val _pendingCallContact = MutableStateFlow<ContactItem?>(null)
    val pendingCallContact: StateFlow<ContactItem?> = _pendingCallContact.asStateFlow()

    private val _matchingContacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val matchingContacts: StateFlow<List<ContactItem>> = _matchingContacts.asStateFlow()

    private val _pendingCommentText = MutableStateFlow<String?>(null)
    val pendingCommentText: StateFlow<String?> = _pendingCommentText.asStateFlow()

    private val _lastAssistantSpeech = MutableStateFlow<String>("")
    val lastAssistantSpeech: StateFlow<String> = _lastAssistantSpeech.asStateFlow()

    init {
        setupVoiceListeners()
        initDefaultConversation()
    }

    private fun setupVoiceListeners() {
        speechManager.onSpeechResult = { recognizedText ->
            sendMessage(recognizedText)
        }

        speechManager.onWakeWordDetected = { wakePhrase, remainingCommand ->
            handleWakeWordActivation(wakePhrase, remainingCommand)
        }

        speechManager.onErrorOccurred = { errorMsg ->
            _chatStatus.value = AiChatStatus.Error(errorMsg)
        }
    }

    private fun initDefaultConversation() {
        viewModelScope.launch {
            val convos = chatDao.getAllConversations().firstOrNull() ?: emptyList()
            if (convos.isEmpty()) {
                val newId = UUID.randomUUID().toString()
                val initialConvo = ConversationEntity(
                    id = newId,
                    title = "Hello Umesh AI Chat"
                )
                chatDao.insertConversation(initialConvo)
                _currentConversationId.value = newId
                observeMessages(newId)

                // Add welcoming message
                val welcomeMessage = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = newId,
                    sender = "assistant",
                    text = "नमस्ते उमेश! मैं आपका पर्सनल AI असिस्टेंट हूँ। आप मुझसे बोलकर या लिखकर हिंदी, नेपाली या अंग्रेज़ी में बात कर सकते हैं।\n\nसुझाव:\n• 'YouTube kholo' या 'Settings kholo'\n• 'Rahul ko phone lagao'\n• '5 minute ka timer lagao'\n• 'Nepal ki aaj ki news batao'"
                )
                chatDao.insertMessage(welcomeMessage)
            } else {
                val firstId = convos.first().id
                _currentConversationId.value = firstId
                observeMessages(firstId)
            }
        }
    }

    private fun observeMessages(convoId: String) {
        viewModelScope.launch {
            chatDao.getMessagesForConversation(convoId).collect { msgList ->
                _messages.value = msgList
            }
        }
    }

    fun selectConversation(convoId: String) {
        _currentConversationId.value = convoId
        observeMessages(convoId)
    }

    fun startNewConversation(title: String = "नई बातचीत") {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val convo = ConversationEntity(id = newId, title = title)
            chatDao.insertConversation(convo)
            _currentConversationId.value = newId
            observeMessages(newId)
        }
    }

    fun deleteConversation(convo: ConversationEntity) {
        viewModelScope.launch {
            chatDao.deleteConversation(convo)
            val remaining = chatDao.getAllConversations().firstOrNull() ?: emptyList()
            if (remaining.isNotEmpty()) {
                selectConversation(remaining.first().id)
            } else {
                startNewConversation()
            }
        }
    }

    fun renameConversation(convo: ConversationEntity, newTitle: String) {
        viewModelScope.launch {
            chatDao.updateConversation(convo.copy(title = newTitle))
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            chatDao.clearAllConversations()
            startNewConversation()
        }
    }

    fun startListening() {
        val prefs = userPreferences.value
        val localeTag = prefs.language.localeTag
        ttsManager.stop()
        _chatStatus.value = AiChatStatus.Listening
        speechManager.startListening(localeTag)
    }

    fun stopListening() {
        speechManager.stopListening()
        _chatStatus.value = AiChatStatus.Idle
    }

    private fun handleWakeWordActivation(wakePhrase: String, remainingCommand: String) {
        if (remainingCommand.isNotBlank()) {
            // User said "Hello Maayra, YouTube kholo" directly
            sendMessage(remainingCommand)
            return
        }

        // User spoke the wake phrase alone: respond in Hindi "हाँ उमेश, बोलिए।"
        val greetingText = "हाँ उमेश, बोलिए।"
        _lastAssistantSpeech.value = greetingText

        val prefs = userPreferences.value
        ttsManager.setSpeed(prefs.ttsSpeed)
        ttsManager.setPitch(prefs.ttsPitch)
        ttsManager.setLanguage(Locale("hi", "IN"))
        ttsManager.speak(greetingText)

        // Hands-free: automatically listen for the user's subsequent command without pressing any button
        viewModelScope.launch {
            kotlinx.coroutines.delay(1100)
            speechManager.listenForCommand()
            _chatStatus.value = AiChatStatus.Listening
        }
    }

    fun sendMessage(inputText: String) {
        val text = inputText.trim()
        if (text.isEmpty()) return

        val convoId = _currentConversationId.value ?: return

        viewModelScope.launch {
            // Save User message to Room
            val userMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convoId,
                sender = "user",
                text = text
            )
            chatDao.insertMessage(userMsg)

            // Step 1: Check Local Device Actions & Offline Capabilities
            // 1. Time / Alarm / Timer
            val timeResult = alarmTimerHelper.parseAndHandle(text)
            if (timeResult != null && timeResult.handled) {
                postAssistantResponse(convoId, timeResult.message, "alarm_timer")
                return@launch
            }

            // 2. App Launch
            val launchResult = appLauncherHelper.tryParseAndLaunch(text)
            if (launchResult != null && launchResult.handled) {
                postAssistantResponse(convoId, launchResult.message, "app_launch")
                return@launch
            }

            // 3. Contact Call Search
            val contactQuery = contactCallerHelper.extractContactNameQuery(text)
            if (!contactQuery.isNullOrBlank()) {
                val matches = contactCallerHelper.searchContacts(contactQuery)
                if (matches.isEmpty()) {
                    postAssistantResponse(convoId, "'$contactQuery' नाम का कोई संपर्क नहीं मिला।", "contact_call")
                } else if (matches.size == 1) {
                    val contact = matches.first()
                    _pendingCallContact.value = contact
                    postAssistantResponse(
                        convoId,
                        "क्या आप ${contact.name} (${contact.phoneNumber}) को कॉल करना चाहते हैं?",
                        "contact_call"
                    )
                } else {
                    _matchingContacts.value = matches
                    postAssistantResponse(
                        convoId,
                        "कई संपर्क मिले। कृपया चुनें कि किसे कॉल करना है:",
                        "contact_call_multi"
                    )
                }
                return@launch
            }

            // 4. Device Controls (Volume, Settings)
            val deviceResult = deviceControlHelper.parseAndHandle(text)
            if (deviceResult != null && deviceResult.handled) {
                postAssistantResponse(convoId, deviceResult.message, "device_control")
                return@launch
            }

            // 5. Web / Social Search Intents
            val searchResult = webSearchHelper.parseAndHandle(text)
            if (searchResult != null && searchResult.handled) {
                postAssistantResponse(convoId, searchResult.message, "search_intent")
                return@launch
            }

            // 6. Social Comment Intent
            if (text.lowercase().contains("comment karo") || text.lowercase().contains("कमेंट करो")) {
                val commentText = text.substringAfter(":", "").ifBlank {
                    text.substringAfter("comment karo", "").trim()
                }
                if (commentText.isNotBlank()) {
                    _pendingCommentText.value = commentText
                    postAssistantResponse(
                        convoId,
                        "कमेंट तैयार है:\n\"$commentText\"\nक्या इसे पोस्ट/शेयर करना चाहते हैं?",
                        "comment_ready"
                    )
                    return@launch
                }
            }

            // Step 2: Cloud AI Inference (Gemini) or Offline Mode explanation
            if (!networkMonitor.isCurrentlyOnline()) {
                val offlineMsg = "ऑफ़लाइन मोड (Offline Mode):\nइंटरनेट कनेक्शन उपलब्ध नहीं है।\n\nआप ये ऑफ़लाइन काम कर सकते हैं:\n• ऐप्स खोलना (उदा. 'YouTube kholo', 'Settings kholo')\n• अलार्म या टाइमर लगाना (उदा. '5 minute ka timer')\n• वर्तमान समय ('Abhi kitna time hua hai?')\n• संपर्क खोजना व कॉल लगाना ('Rahul ko phone lagao')\n• वॉल्यूम घटाना या बढ़ाना।"
                postAssistantResponse(convoId, offlineMsg, "offline_mode")
                return@launch
            }

            // Call Gemini
            _chatStatus.value = AiChatStatus.Thinking
            val history = _messages.value.takeLast(10).map { Pair(it.sender, it.text) }

            try {
                val accumulated = StringBuilder()
                _chatStatus.value = AiChatStatus.Streaming("")

                geminiAssistant.generateStream(text, history).collect { chunk ->
                    accumulated.append(chunk)
                    _chatStatus.value = AiChatStatus.Streaming(accumulated.toString())
                }

                val fullResponse = accumulated.toString().trim()
                if (fullResponse.isNotBlank()) {
                    postAssistantResponse(convoId, fullResponse, null)
                } else {
                    _chatStatus.value = AiChatStatus.Error("उत्तर खाली प्राप्त हुआ। पुनः प्रयास करें।")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini chat failed", e)
                _chatStatus.value = AiChatStatus.Error(e.localizedMessage ?: "AI सेवा अनुपलब्ध है।")
            }
        }
    }

    private suspend fun postAssistantResponse(convoId: String, responseText: String, actionType: String?) {
        val assistantMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = convoId,
            sender = "assistant",
            text = responseText,
            actionType = actionType
        )
        chatDao.insertMessage(assistantMsg)
        _chatStatus.value = AiChatStatus.Idle
        _lastAssistantSpeech.value = responseText

        // Speak response if user enabled spoken responses
        if (userPreferences.value.voiceResponseEnabled) {
            val prefs = userPreferences.value
            ttsManager.setSpeed(prefs.ttsSpeed)
            ttsManager.setPitch(prefs.ttsPitch)
            val loc = when (prefs.language) {
                AssistantLanguage.NEPALI -> Locale("ne", "NP")
                AssistantLanguage.ENGLISH -> Locale("en", "IN")
                AssistantLanguage.HINDI -> Locale("hi", "IN")
            }
            ttsManager.setLanguage(loc)
            ttsManager.speak(responseText)
        }

        // Return to listening for "Hello Maayra" wake phrase in hands-free mode
        if (speechManager.isHandsFreeActive.value) {
            speechManager.restartHandsFreeListening()
        }
    }

    fun toggleHandsFreeMode() {
        val newState = !speechManager.isHandsFreeActive.value
        speechManager.setHandsFreeMode(newState)
    }

    fun confirmCall(contact: ContactItem) {
        _pendingCallContact.value = null
        _matchingContacts.value = emptyList()
        contactCallerHelper.makeCall(contact.phoneNumber)
    }

    fun cancelCall() {
        _pendingCallContact.value = null
        _matchingContacts.value = emptyList()
    }

    fun confirmCommentShare(comment: String, targetPackage: String? = null) {
        _pendingCommentText.value = null
        socialShareHelper.shareComment(comment, targetPackage)
    }

    fun cancelCommentShare() {
        _pendingCommentText.value = null
    }

    fun repeatLastResponse() {
        val speech = _lastAssistantSpeech.value
        if (speech.isNotBlank()) {
            ttsManager.speak(speech)
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    override fun onCleared() {
        speechManager.destroy()
        ttsManager.shutdown()
        super.onCleared()
    }
}
