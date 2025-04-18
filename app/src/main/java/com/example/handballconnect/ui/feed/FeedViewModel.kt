package com.example.handballconnect.ui.feed

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.handballconnect.data.model.Comment
import com.example.handballconnect.data.model.Post
import com.example.handballconnect.data.repository.PostRepository
import com.example.handballconnect.data.repository.UserRepository
import com.example.handballconnect.ui.auth.LoginScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // Feed state
    private val _feedState = MutableStateFlow<FeedState>(FeedState.Loading)
    val feedState: StateFlow<FeedState> = _feedState.asStateFlow()

    // Selected post for comments
    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost.asStateFlow()

    // Comments state
    private val _commentsState = MutableStateFlow<CommentsState>(CommentsState.Initial)
    val commentsState: StateFlow<CommentsState> = _commentsState.asStateFlow()

    // Initialize by loading feed posts
    init {
        loadFeedPosts()
    }

    // Post creation state
    private val _postCreationState = MutableStateFlow<PostCreationState>(PostCreationState.Initial)
    val postCreationState: StateFlow<PostCreationState> = _postCreationState.asStateFlow()

    // Reset post creation state
//    fun resetPostCreationState() {
//        _postCreationState.value = PostCreationState.Initial
//    }
    
    // Load feed posts
    fun loadFeedPosts() {
        _feedState.value = FeedState.Loading
        
        viewModelScope.launch {
            postRepository.getFeedPosts().collect { result ->
                try {
                    result.onSuccess { posts ->
                        if (posts.isEmpty()) {
                            _feedState.value = FeedState.Empty
                        } else {
                            _feedState.value = FeedState.Success(posts)
                        }
                    }.onFailure { exception ->
                        _feedState.value = FeedState.Error(exception.message ?: "Failed to load posts")
                        Log.d("FeedViewModel", "Error loading post")
                    }
                } catch (e: Exception) {
                    Log.e("FeedViewModel", "Exception in loadingPosts: ${e.message}")
                    _feedState.value = FeedState.Error(e.message ?: "An unexpected error occurred")
                }

            }
        }
    }

    // Create a new post with improved error handling
    fun createPost(text: String, imageUri: Uri? = null, isAnnouncement: Boolean = false) {
        _postCreationState.value = PostCreationState.Loading

        viewModelScope.launch {
            try {
                val result = postRepository.createPost(text, imageUri, isAnnouncement)

                result.onSuccess {
                    _postCreationState.value = PostCreationState.Success
                    // Reload feed posts to include the new post
                    loadFeedPosts()
                }.onFailure { exception ->
                    Log.e("FeedViewModel", "Post creation failed: ${exception.message}")
                    _postCreationState.value = PostCreationState.Error(exception.message ?: "Failed to create post")
                }
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Exception during post creation: ${e.message}")
                _postCreationState.value = PostCreationState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Like or unlike a post
    fun toggleLike(postId: String) {
        viewModelScope.launch {
            postRepository.likePost(postId)
        }
    }
    
    // Delete a post
    fun deletePost(postId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = postRepository.deletePost(postId)
            
            result.onSuccess {
                onSuccess()
                // Reload feed posts to reflect the deletion
                loadFeedPosts()
            }.onFailure { exception ->
                onError(exception.message ?: "Failed to delete post")
            }
        }
    }
    
    // Set selected post for comments view
    fun selectPostForComments(post: Post) {
        _selectedPost.value = post
        loadComments(post.postId)
    }
    
    // Load comments for a post
    private fun loadComments(postId: String) {
        _commentsState.value = CommentsState.Loading
        
        viewModelScope.launch {
            postRepository.getCommentsForPost(postId).collect { result ->
                result.onSuccess { comments ->
                    if (comments.isEmpty()) {
                        _commentsState.value = CommentsState.Empty
                    } else {
                        _commentsState.value = CommentsState.Success(comments)
                    }
                }.onFailure { exception ->
                    _commentsState.value = CommentsState.Error(exception.message ?: "Failed to load comments")
                }
            }
        }
    }
    
    // Add a comment to a post
    fun addComment(postId: String, text: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = postRepository.addComment(postId, text)
            
            result.onSuccess {
                onSuccess()
                // Reload comments to include the new one
                loadComments(postId)
            }.onFailure { exception ->
                onError(exception.message ?: "Failed to add comment")
            }
        }
    }
    
    // Clear post creation state
    fun resetPostCreationState() {
        _postCreationState.value = PostCreationState.Initial
    }
    
    // Check if current user is admin
    fun isCurrentUserAdmin(): Boolean {
        var isAdmin = false
        viewModelScope.launch {
            userRepository.getCurrentUserData().collect { result ->
                isAdmin = result.getOrNull()?.isAdmin ?: false
            }
        }
        return isAdmin
    }
    
    // Feed state sealed class
    sealed class FeedState {
        object Loading : FeedState()
        object Empty : FeedState()
        data class Success(val posts: List<Post>) : FeedState()
        data class Error(val message: String) : FeedState()
    }
    
    // Comments state sealed class
    sealed class CommentsState {
        object Initial : CommentsState()
        object Loading : CommentsState()
        object Empty : CommentsState()
        data class Success(val comments: List<Comment>) : CommentsState()
        data class Error(val message: String) : CommentsState()
    }
    
    // Post creation state sealed class
    sealed class PostCreationState {
        object Initial : PostCreationState()
        object Loading : PostCreationState()
        object Success : PostCreationState()
        data class Error(val message: String) : PostCreationState()
    }
}