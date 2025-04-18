package com.example.handballconnect.data.repository

import android.net.Uri
import android.util.Log
import com.example.handballconnect.data.model.Comment
import com.example.handballconnect.data.model.Post
import com.example.handballconnect.data.storage.ImageStorageManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val userRepository: UserRepository,
    private val imageStorageManager: ImageStorageManager
) {
    private val fireStore = FirebaseFirestore.getInstance()
    private val postCollection = fireStore.collection("posts")
    private val likesCollection = fireStore.collection("likes")
    private val commentCollection = fireStore.collection("comments")

    // Create a new post with local image storage
    suspend fun createPost(text: String, imageUri: Uri? = null, isAnnouncement: Boolean = false): Result<Post> {
        val userId = userRepository.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        return try {
            // Get current user data
            var username = ""
            var userProfileImageUrl = ""

            // Use withTimeoutOrNull to prevent blocking indefinitely
            val currentUser = withTimeoutOrNull(5000) {
                userRepository.getCurrentUserData()
                    .first()
                    .getOrNull()
            }

            if (currentUser != null) {
                username = currentUser.username
                userProfileImageUrl = currentUser.profileImageUrl ?: ""
            } else {
                Log.w("PostRepository", "Could not retrieve current user data, using defaults")
                // Fallback in case we can't get the user data
                val firebaseUser = userRepository.getCurrentUser()
                username = firebaseUser?.displayName ?: "Unknown User"
            }

            // Generate a new post ID
            val postId = postCollection.document().id

            // Save image locally if provided
            var imageUrl: String? = null
            if (imageUri != null) {
                Log.d("PostRepository", "Processing image for post: $postId")
                val imageResult = imageStorageManager.savePostImage(postId, imageUri)
                imageResult.onSuccess { localReference ->
                    imageUrl = localReference
                    Log.d("PostRepository", "Image saved locally with reference: $localReference")
                }.onFailure { e ->
                    Log.e("PostRepository", "Failed to save image locally: ${e.message}")
                    // Continue without the image
                }
            }

            // Create the post object
            val post = Post(
                postId = postId,
                userId = userId,
                username = username,
                userProfileImageUrl = userProfileImageUrl,
                text = text,
                imageUrl = imageUrl,
                isAnnouncement = isAnnouncement,
                timestamp = System.currentTimeMillis()
            )

            // Save the post to Firestore
            Log.d("PostRepository", "Saving post to Firestore with ID: $postId")
            postCollection.document(postId).set(post).await()
            Log.d("PostRepository", "Post saved successfully")

            Result.success(post)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error creating post: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Delete a post - updated to delete local images
    suspend fun deletePost(postId: String): Result<Unit> {
        val userId = userRepository.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        return try {
            // Get current user data to check if admin
            val currentUserFlow = userRepository.getCurrentUserData()
            var isAdmin = false
            var isPostOwner = false

            // Collect user data from flow (just once)
            currentUserFlow.collect { result ->
                result.onSuccess { user ->
                    isAdmin = user.isAdmin
                }
                return@collect
            }

            // Check if user is the post owner
            val postDoc = postCollection.document(postId).get().await()
            val post = postDoc.toObject(Post::class.java)

            if (post != null) {
                isPostOwner = post.userId == userId
            }

            if (!isAdmin && !isPostOwner) {
                return Result.failure(Exception("You don't have permission to delete this post"))
            }

            // Delete post image if exists
            post?.imageUrl?.let { imageRef ->
                if (imageRef.startsWith(ImageStorageManager.LOCAL_URI_PREFIX)) {
                    val imageDeleted = imageStorageManager.deleteImage(imageRef)
                    Log.d("PostRepository", "Image deletion for post $postId: $imageDeleted")
                }
            }

            // Delete post
            postCollection.document(postId).delete().await()

            // Delete comments for this post
            val commentsQuery = commentCollection.whereEqualTo("postId", postId).get().await()
            commentsQuery.documents.forEach { doc ->
                commentCollection.document(doc.id).delete().await()
            }

            // Delete likes for this post
            val likesQuery = likesCollection.whereEqualTo("postId", postId).get().await()
            likesQuery.documents.forEach { doc ->
                likesCollection.document(doc.id).delete().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Like a post
    suspend fun likePost(postId: String): Result<Unit> {
        val userId = userRepository.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        return try {
            // Check if user already liked the post
            val likeDocRef = likesCollection.document("$postId-$userId")
            val likeDoc = likeDocRef.get().await()
            val alreadyLiked = likeDoc.exists()

            // Get the post document
            val postDoc = postCollection.document(postId)

            if (alreadyLiked) {
                // Unlike the post
                likeDocRef.delete().await()

                // Decrement like count
                fireStore.runTransaction { transaction ->
                    val snapshot = transaction.get(postDoc)
                    val currentLikes = snapshot.getLong("likeCount") ?: 0
                    transaction.update(postDoc, "likeCount", maxOf(0, currentLikes - 1))
                }.await()
            } else {
                // Like the post
                val likeData = hashMapOf(
                    "userId" to userId,
                    "postId" to postId,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                likeDocRef.set(likeData).await()

                // Increment like count
                fireStore.runTransaction { transaction ->
                    val snapshot = transaction.get(postDoc)
                    val currentLikes = snapshot.getLong("likeCount") ?: 0
                    transaction.update(postDoc, "likeCount", currentLikes + 1)
                }.await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Check if a post is liked by the current user
    fun isPostLikedByUser(postId: String): Flow<Result<Boolean>> = callbackFlow {
        val userId = userRepository.getCurrentUserId()
        if (userId == null) {
            trySend(Result.success(false))
            close()
            return@callbackFlow
        }

        val likeDocRef = likesCollection.document("$postId-$userId")
        val listenerRegistration = likeDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }

            val isLiked = snapshot != null && snapshot.exists()
            trySend(Result.success(isLiked))
        }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    // Add a comment to a post
    suspend fun addComment(postId: String, text: String): Result<Comment> {
        val userId = userRepository.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        return try {
            // Get current user data
            val currentUserFlow = userRepository.getCurrentUser()
            var username = ""
            var userProfileImageUrl = ""

            // Collect user data from flow (just once)
            currentUserFlow.let { result ->
                username = result?.email ?: ""
                // userProfileImageUrl can be retrieved in a similar way if needed
            }

            // Generate a new comment ID
            val commentId = commentCollection.document().id

            // Create the comment object
            val comment = Comment(
                commentId = commentId,
                postId = postId,
                userId = userId,
                username = username,
                userProfileImageUrl = userProfileImageUrl,
                text = text
            )

            // Save the comment to Firestore
            commentCollection.document(commentId).set(comment).await()

            // Increment comment count on the post
            val postRef = postCollection.document(postId)
            fireStore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val currentComments = snapshot.getLong("commentCount") ?: 0
                transaction.update(postRef, "commentCount", currentComments + 1)
            }.await()

            Result.success(comment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get comments for a post
    fun getCommentsForPost(postId: String): Flow<Result<List<Comment>>> = callbackFlow {
        val listenerRegistration = commentCollection
            .whereEqualTo("postId", postId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val comments = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Comment::class.java)
                    }
                    trySend(Result.success(comments))
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    fun getFeedPosts(): Flow<Result<List<Post>>> = callbackFlow {
        val listenerRegistration = postCollection
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if(error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if(snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)
                    }
                    trySend(Result.success(posts))
                }
            }
        awaitClose {
            listenerRegistration.remove()
        }
    }

    // Get a specific post
    fun getPostById(postId: String): Flow<Result<Post>> = callbackFlow {
        val postListener = postCollection.document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val post = snapshot.toObject(Post::class.java)
                    if (post != null) {
                        trySend(Result.success(post))
                    } else {
                        trySend(Result.failure(Exception("Post not found")))
                    }
                } else {
                    trySend(Result.failure(Exception("Post not found")))
                }
            }

        awaitClose {
            postListener.remove()
        }
    }
}