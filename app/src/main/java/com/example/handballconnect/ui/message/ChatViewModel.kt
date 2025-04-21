package com.example.handballconnect.ui.message

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.handballconnect.data.model.Conversation
import com.example.handballconnect.data.model.User
import com.example.handballconnect.data.repository.MessageRepository
import com.example.handballconnect.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // Messages state
    private val _messagesState = MutableStateFlow<MessagesState>(MessagesState.Initial)
    val messagesState: StateFlow<MessagesState> = _messagesState.asStateFlow()

    // Message sending state
    private val _chatState = MutableStateFlow<ChatState>(ChatState.Initial)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    // Selected conversation
    private val _selectedConversation = MutableStateFlow<Conversation?>(null)
    val selectedConversation: StateFlow<Conversation?> = _selectedConversation.asStateFlow()

    // Send a text message with improved error handling
    fun sendTextMessage(text: String, conversationId: String) {
        _chatState.value = ChatState.Sending

        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Sending message in conversation: $conversationId")
                val result = messageRepository.sendTextMessage(conversationId, text)

                result.onSuccess {
                    Log.d("ChatViewModel", "Message sent successfully")
                    _chatState.value = ChatState.Success
                }.onFailure { exception ->
                    Log.e("ChatViewModel", "Failed to send message: ${exception.message}")
                    _chatState.value =
                        ChatState.Error(exception.message ?: "Failed to send message")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Exception sending message: ${e.message}")
                _chatState.value =
                    ChatState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Send an image message
    fun sendImageMessage(imageUri: Uri, conversationId: String) {
        _chatState.value = ChatState.Sending

        viewModelScope.launch {
            try {
                val result = messageRepository.sendImageMessage(conversationId, imageUri)

                result.onSuccess {
                    _chatState.value = ChatState.Success
                }.onFailure { exception ->
                    _chatState.value =
                        ChatState.Error(exception.message ?: "Failed to send image")
                }
            } catch (e: Exception) {
                _chatState.value =
                    ChatState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Set the currently selected conversation
    fun selectConversation(conversation: Conversation) {
        _selectedConversation.value = conversation
        loadMessages(conversation.conversationId)
    }

    // Get the current user ID
    fun getCurrentUserId(): String? {
        return userRepository.getCurrentUserId()
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

    // Reset chat state
    fun resetChatState() {
        _chatState.value = ChatState.Initial
    }
}


// Users state sealed class
sealed class UsersState {
    object Loading : UsersState()
    object Empty : UsersState()
    data class Success(val users: List<User>) : UsersState()
    data class Error(val message: String) : UsersState()
}