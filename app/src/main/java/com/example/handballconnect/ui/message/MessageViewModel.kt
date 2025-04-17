package com.example.handballconnect.ui.message

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.handballconnect.data.model.Conversation
import com.example.handballconnect.data.model.Message
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
    private val _messageSendState = MutableStateFlow<MessageSendState>(MessageSendState.Initial)
    val messageSendState: StateFlow<MessageSendState> = _messageSendState.asStateFlow()
    
    // Users list for starting new conversations
    private val _usersState = MutableStateFlow<UsersState>(UsersState.Loading)
    val usersState: StateFlow<UsersState> = _usersState.asStateFlow()
    
    // Initialize by loading conversations
    init {
        loadConversations()
    }
    
    // Load user conversations
    fun loadConversations() {
        _conversationsState.value = ConversationsState.Loading
        
        viewModelScope.launch {
            messageRepository.getUserConversations().collect { result ->
                result.onSuccess { conversations ->
                    if (conversations.isEmpty()) {
                        _conversationsState.value = ConversationsState.Empty
                    } else {
                        _conversationsState.value = ConversationsState.Success(conversations)
                    }
                }.onFailure { exception ->
                    _conversationsState.value = ConversationsState.Error(exception.message ?: "Failed to load conversations")
                }
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
            messageRepository.getMessagesForConversation(conversationId).collect { result ->
                result.onSuccess { messages ->
                    if (messages.isEmpty()) {
                        _messagesState.value = MessagesState.Empty
                    } else {
                        _messagesState.value = MessagesState.Success(messages)
                    }
                }.onFailure { exception ->
                    _messagesState.value = MessagesState.Error(exception.message ?: "Failed to load messages")
                }
            }
        }
    }
    
    // Send a text message
    fun sendTextMessage(text: String) {
        val conversationId = _selectedConversation.value?.conversationId ?: return
        
        _messageSendState.value = MessageSendState.Sending
        
        viewModelScope.launch {
            val result = messageRepository.sendTextMessage(conversationId, text)
            
            result.onSuccess {
                _messageSendState.value = MessageSendState.Success
            }.onFailure { exception ->
                _messageSendState.value = MessageSendState.Error(exception.message ?: "Failed to send message")
            }
        }
    }
    
    // Send an image message
    fun sendImageMessage(imageUri: Uri) {
        val conversationId = _selectedConversation.value?.conversationId ?: return
        
        _messageSendState.value = MessageSendState.Sending
        
        viewModelScope.launch {
            val result = messageRepository.sendImageMessage(conversationId, imageUri)
            
            result.onSuccess {
                _messageSendState.value = MessageSendState.Success
            }.onFailure { exception ->
                _messageSendState.value = MessageSendState.Error(exception.message ?: "Failed to send image")
            }
        }
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
                    _usersState.value = UsersState.Error(exception.message ?: "Failed to load users")
                }
            }
        }
    }
    
    // Start or continue a conversation with a user
    fun startConversation(userId: String, onSuccess: (Conversation) -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = messageRepository.getOrCreateConversation(userId)
            
            result.onSuccess { conversation ->
                selectConversation(conversation)
                onSuccess(conversation)
            }.onFailure { exception ->
                onError(exception.message ?: "Failed to start conversation")
            }
        }
    }
    
    // Reset message send state
    fun resetMessageSendState() {
        _messageSendState.value = MessageSendState.Initial
    }
    
    // Get other participant's name in current conversation
    fun getOtherParticipantName(): String {
        val conversation = _selectedConversation.value ?: return ""
        val currentUserId = userRepository.getCurrentUserId() ?: return ""
        return conversation.getOtherParticipantName(currentUserId)
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
    sealed class MessageSendState {
        object Initial : MessageSendState()
        object Sending : MessageSendState()
        object Success : MessageSendState()
        data class Error(val message: String) : MessageSendState()
    }
    
    // Users state sealed class
    sealed class UsersState {
        object Loading : UsersState()
        object Empty : UsersState()
        data class Success(val users: List<User>) : UsersState()
        data class Error(val message: String) : UsersState()
    }
}