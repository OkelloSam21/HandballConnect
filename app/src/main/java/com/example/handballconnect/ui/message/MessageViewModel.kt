package com.example.handballconnect.ui.message

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.handballconnect.data.model.Conversation
import com.example.handballconnect.data.model.Message
import com.example.handballconnect.data.repository.MessageRepository
import com.example.handballconnect.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // Conversations state
    private val _conversationsState = MutableStateFlow<ConversationsState>(ConversationsState.Loading)
    val conversationsState: StateFlow<ConversationsState> = _conversationsState.asStateFlow()

    // Currently selected conversation
    private val _selectedConversation = MutableStateFlow<Conversation?>(null)
    val selectedConversation: StateFlow<Conversation?> = _selectedConversation.asStateFlow()

    // Messages state
    private val _messagesState = MutableStateFlow<MessagesState>(MessagesState.Initial)
    val messagesState: StateFlow<MessagesState> = _messagesState.asStateFlow()

    // Message sending state
    private val _messageSendState = MutableStateFlow<ChatState>(ChatState.Initial)
    val messageSendState: StateFlow<ChatState> = _messageSendState.asStateFlow()

    // Users list for starting new conversations
    private val _usersState = MutableStateFlow<UsersState>(UsersState.Loading)
    val usersState: StateFlow<UsersState> = _usersState.asStateFlow()

    init {
        Log.d("MessageViewModel", "Initializing and loading conversations")
        loadConversations()
    }

    // Load user conversations
    fun loadConversations() {
        _conversationsState.value = ConversationsState.Loading

        viewModelScope.launch {
            try {
                Log.d("MessageViewModel", "Starting to load user conversations")
                messageRepository.getUserConversations().collect { result ->
                    result.onSuccess { conversations ->
                        Log.d("MessageViewModel", "Got ${conversations.size} conversations")
                        if (conversations.isEmpty()) {
                            _conversationsState.value = ConversationsState.Empty
                        } else {
                            _conversationsState.value = ConversationsState.Success(conversations)
                        }
                    }.onFailure { exception ->
                        Log.e(
                            "MessageViewModel",
                            "Failed to load conversations: ${exception.message}"
                        )
                        _conversationsState.value = ConversationsState.Error(
                            exception.message ?: "Failed to load conversations"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MessageViewModel", "Exception loading conversations: ${e.message}")
                _conversationsState.value =
                    ConversationsState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Select a conversation and load its messages
    fun selectConversation(conversation: Conversation) {
        _selectedConversation.value = conversation
        loadMessages(conversation.conversationId)
    }

    // Load messages for a conversation
    fun loadMessages(conversationId: String) {
        _messagesState.value = MessagesState.Loading

        viewModelScope.launch {
            try {
                // Load conversation data if not already loaded
                if (_selectedConversation.value == null || _selectedConversation.value?.conversationId != conversationId) {
                    val conversationResult = messageRepository.getConversationById(conversationId)
                    conversationResult.onSuccess { conversation ->
                        _selectedConversation.value = conversation
                    }
                }

                // Load messages
                messageRepository.getMessagesForConversation(conversationId).collect { result ->
                    result.onSuccess { messages ->
                        if (messages.isEmpty()) {
                            _messagesState.value = MessagesState.Empty
                        } else {
                            _messagesState.value = MessagesState.Success(messages)
                        }
                    }.onFailure { exception ->
                        _messagesState.value =
                            MessagesState.Error(exception.message ?: "Failed to load messages")
                    }
                }
            } catch (e: Exception) {
                _messagesState.value =
                    MessagesState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Get other participant's name in current conversation
    fun getOtherParticipantName(): String {
        val conversation = _selectedConversation.value ?: return ""
        val currentUserId = userRepository.getCurrentUserId() ?: return ""

        // Get the name of the other participant
        return conversation.participantNames[getOtherParticipantId(conversation, currentUserId)] ?: ""
    }

    // Helper function to get the other participant's ID
    private fun getOtherParticipantId(conversation: Conversation, currentUserId: String): String {
        return conversation.participantIds.firstOrNull { it != currentUserId } ?: ""
    }

    // Load users for starting new conversations
    fun loadUsers() {
        _usersState.value = UsersState.Loading

        viewModelScope.launch {
            userRepository.getAllUsers().collect { result ->
                result.onSuccess { users ->
                    // Filter out current user
                    val currentUserId = userRepository.getCurrentUserId() ?: ""
                    val filteredUsers = users.filter { it.userId != currentUserId }

                    if (filteredUsers.isEmpty()) {
                        _usersState.value = UsersState.Empty
                    } else {
                        _usersState.value = UsersState.Success(filteredUsers)
                    }
                }.onFailure { exception ->
                    _usersState.value =
                        UsersState.Error(exception.message ?: "Failed to load users")
                }
            }
        }
    }

    // Start or continue a conversation with a user
    fun startConversation(otherUserId: String) {
        viewModelScope.launch {
            try {
                Log.d("MessageViewModel", "Starting conversation with user: $otherUserId")
                val result = messageRepository.getOrCreateConversation(otherUserId)

                result.onSuccess { conversation ->
                    Log.d("MessageViewModel", "Conversation started successfully")
                    _selectedConversation.value = conversation
                    loadMessages(conversation.conversationId)
                }.onFailure { exception ->
                    Log.e("MessageViewModel", "Failed to start conversation: ${exception.message}")
                    throw exception // Re-throw to be caught by the UI
                }
            } catch (e: Exception) {
                Log.e("MessageViewModel", "Exception starting conversation: ${e.message}", e)
                throw e // Re-throw to be caught by the UI
            }
        }
    }

    // Send a text message
    fun sendTextMessage(text: String, conversationId: String) {
        _messageSendState.value = ChatState.Sending

        viewModelScope.launch {
            try {
                val result = messageRepository.sendTextMessage(conversationId, text)

                result.onSuccess {
                    _messageSendState.value = ChatState.Success
                }.onFailure { exception ->
                    _messageSendState.value =
                        ChatState.Error(exception.message ?: "Failed to send message")
                }
            } catch (e: Exception) {
                _messageSendState.value =
                    ChatState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Send an image message
    fun sendImageMessage(imageUri: Uri, conversationId: String) {
        _messageSendState.value = ChatState.Sending

        viewModelScope.launch {
            try {
                val result = messageRepository.sendImageMessage(conversationId, imageUri)

                result.onSuccess {
                    _messageSendState.value = ChatState.Success
                }.onFailure { exception ->
                    _messageSendState.value =
                        ChatState.Error(exception.message ?: "Failed to send image")
                }
            } catch (e: Exception) {
                _messageSendState.value =
                    ChatState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Reset message send state
    fun resetMessageSendState() {
        _messageSendState.value = ChatState.Initial
    }
}

// Conversations state sealed class
sealed class ConversationsState {
    object Loading : ConversationsState()
    object Empty : ConversationsState()
    data class Success(val conversations: List<Conversation>) : ConversationsState()
    data class Error(val message: String) : ConversationsState()
}

// Messages state sealed class
sealed class MessagesState {
    object Initial : MessagesState()
    object Loading : MessagesState()
    object Empty : MessagesState()
    data class Success(val messages: List<Message>) : MessagesState()
    data class Error(val message: String) : MessagesState()
}

// Message send state sealed class
sealed class ChatState {
    object Initial : ChatState()
    object Sending : ChatState()
    object Success : ChatState()
    data class Error(val message: String) : ChatState()
}