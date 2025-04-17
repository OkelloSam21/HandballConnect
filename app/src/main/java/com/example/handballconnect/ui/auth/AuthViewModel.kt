package com.example.handballconnect.ui.auth

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.handballconnect.data.model.User
import com.example.handballconnect.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    // Auth state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // Current user data
    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData.asStateFlow()
    
    // Initialize by checking current auth state
    init {
        checkAuthState()
    }
    
    // Check current authentication state
    private fun checkAuthState() {
        if (userRepository.isUserLoggedIn()) {
            _authState.value = AuthState.Authenticated
            fetchCurrentUserData()
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    // Register a new user
    fun registerUser(email: String, password: String, username: String) {
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            val result = userRepository.registerUser(email, password, username)
            
            result.onSuccess {
                _authState.value = AuthState.Authenticated
                fetchCurrentUserData()
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Registration failed")
            }
        }
    }
    
    // Login user
    fun loginUser(email: String, password: String) {
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            val result = userRepository.loginUser(email, password)
            
            result.onSuccess {
                _authState.value = AuthState.Authenticated
                fetchCurrentUserData()
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Login failed")
            }
        }
    }
    
    // Logout user
    fun logoutUser() {
        userRepository.logoutUser()
        _authState.value = AuthState.Unauthenticated
        _userData.value = null
    }
    
    // Reset password
    fun resetPassword(email: String) {
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            val result = userRepository.resetPassword(email)
            
            result.onSuccess {
                _authState.value = AuthState.PasswordResetSent
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Password reset failed")
            }
        }
    }
    
    // Fetch current user data
    fun fetchCurrentUserData() {
        viewModelScope.launch {
            userRepository.getCurrentUserData().collect { result ->
                result.onSuccess { user ->
                    _userData.value = user
                }.onFailure {
                    // User data couldn't be fetched, but user might still be authenticated
                    _userData.value = null
                }
            }
        }
    }
    
    // Update user profile
    fun updateProfile(username: String? = null, position: String? = null, experience: String? = null) {
        viewModelScope.launch {
            val result = userRepository.updateUserProfile(username, position, experience)
            
            result.onSuccess {
                // Refresh user data after update
                fetchCurrentUserData()
            }
        }
    }
    
    // Upload profile image
    fun uploadProfileImage(imageUri: Uri, onSuccess: (String) -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = userRepository.uploadProfileImage(imageUri)
            
            result.onSuccess { imageUrl ->
                // Refresh user data after update
                fetchCurrentUserData()
                onSuccess(imageUrl)
            }.onFailure { exception ->
                onError(exception.message ?: "Failed to upload profile image")
            }
        }
    }
    
    // Auth state sealed class
    sealed class AuthState {
        object Initial : AuthState()
        object Loading : AuthState()
        object Authenticated : AuthState()
        object Unauthenticated : AuthState()
        object PasswordResetSent : AuthState()
        data class Error(val message: String) : AuthState()
    }
}