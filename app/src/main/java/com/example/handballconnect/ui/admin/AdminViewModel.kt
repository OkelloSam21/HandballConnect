package com.example.handballconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.handballconnect.data.model.User
import com.example.handballconnect.data.repository.PostRepository
import com.example.handballconnect.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) : ViewModel() {
    
    // Users state
    private val _usersState = MutableStateFlow<UsersState>(UsersState.Loading)
    val usersState: StateFlow<UsersState> = _usersState.asStateFlow()
    
    // Admin operation state
    private val _adminOperationState = MutableStateFlow<AdminOperationState>(AdminOperationState.Initial)
    val adminOperationState: StateFlow<AdminOperationState> = _adminOperationState.asStateFlow()
    
    // Announcement creation state
    private val _announcementState = MutableStateFlow<AnnouncementState>(AnnouncementState.Initial)
    val announcementState: StateFlow<AnnouncementState> = _announcementState.asStateFlow()
    
    // Initialize by loading users
    init {
        loadUsers()
    }
    
    // Load all users
    fun loadUsers() {
        _usersState.value = UsersState.Loading
        
        viewModelScope.launch {
            userRepository.getAllUsers().collect { result ->
                result.onSuccess { users ->
                    if (users.isEmpty()) {
                        _usersState.value = UsersState.Empty
                    } else {
                        _usersState.value = UsersState.Success(users)
                    }
                }.onFailure { exception ->
                    _usersState.value = UsersState.Error(exception.message ?: "Failed to load users")
                }
            }
        }
    }
    
    // Update user admin status
    fun updateUserAdminStatus(userId: String, isAdmin: Boolean) {
        _adminOperationState.value = AdminOperationState.Loading
        
        viewModelScope.launch {
            val result = userRepository.updateUserAdminStatus(userId, isAdmin)
            
            result.onSuccess {
                _adminOperationState.value = AdminOperationState.Success("Admin status updated successfully")
                // Reload users to reflect changes
                loadUsers()
            }.onFailure { exception ->
                _adminOperationState.value = AdminOperationState.Error(exception.message ?: "Failed to update admin status")
            }
        }
    }
    
    // Create announcement
    fun createAnnouncement(text: String) {
        _announcementState.value = AnnouncementState.Loading
        
        viewModelScope.launch {
            val result = postRepository.createPost(text, isAnnouncement = true)
            
            result.onSuccess {
                _announcementState.value = AnnouncementState.Success("Announcement created successfully")
            }.onFailure { exception ->
                _announcementState.value = AnnouncementState.Error(exception.message ?: "Failed to create announcement")
            }
        }
    }
    
    // Get current user admin status
    fun isCurrentUserAdmin(): Boolean {
        var isAdmin = false
        viewModelScope.launch {
            userRepository.getCurrentUserData().collect { result ->
                isAdmin = result.getOrNull()?.isAdmin ?: false
            }
        }
        return isAdmin
    }
    
    // Reset admin operation state
    fun resetAdminOperationState() {
        _adminOperationState.value = AdminOperationState.Initial
    }
    
    // Reset announcement state
    fun resetAnnouncementState() {
        _announcementState.value = AnnouncementState.Initial
    }
    
    // Users state sealed class
    sealed class UsersState {
        object Loading : UsersState()
        object Empty : UsersState()
        data class Success(val users: List<User>) : UsersState()
        data class Error(val message: String) : UsersState()
    }
    
    // Admin operation state sealed class
    sealed class AdminOperationState {
        object Initial : AdminOperationState()
        object Loading : AdminOperationState()
        data class Success(val message: String) : AdminOperationState()
        data class Error(val message: String) : AdminOperationState()
    }
    
    // Announcement state sealed class
    sealed class AnnouncementState {
        object Initial : AnnouncementState()
        object Loading : AnnouncementState()
        data class Success(val message: String) : AnnouncementState()
        data class Error(val message: String) : AnnouncementState()
    }
}