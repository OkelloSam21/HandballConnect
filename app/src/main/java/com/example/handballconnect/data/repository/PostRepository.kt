package com.example.handballconnect.data.repository

import android.net.Uri
import com.example.handballconnect.data.model.Comment
import com.example.handballconnect.data.model.Post
import com.example.handballconnect.data.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Query
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val userRepository: UserRepository
) {
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    private val fireStore = FirebaseFirestore.getInstance()
    private val postCollection = fireStore.collection("posts")
    private val likesCollection = fireStore.collection("likes")
    private val commentCollection = fireStore.collection("comments")

    // Create a new post
    suspend fun createPost(text: String, imageUri: Uri? = null, isAnnouncement: Boolean = false): Result<Post> {
        val userId = userRepository.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        return try {
            // Get current user data
            val currentUserFlow = userRepository.getCurrentUserData()
            var username = ""
            var userProfileImageUrl = ""

            // Collect user data from flow (just once)
            currentUserFlow.collect { result ->
                result.onSuccess { user ->
                    username = user.username
                    userProfileImageUrl = user.profileImageUrl
                }
                return@collect
            }

            // Generate a new post ID

            val postId = postCollection.document().id

//            val postId = database.child("posts").push().key ?:
//            return Result.failure(Exception("Failed to generate post ID"))

            // Upload image if available
            var imageUrl: String? = null
            if (imageUri != null) {
                val filename = UUID.randomUUID().toString()
                val ref = storage.child("post_images/$postId/$filename")

                ref.putFile(imageUri).await()
                imageUrl = ref.downloadUrl.await().toString()
            }


            // Create the post object
            val post = Post(
                postId = postId,
                userId = userId,
                username = username,
                userProfileImageUrl = userProfileImageUrl,
                text = text,
                imageUrl = imageUrl,
                isAnnouncement = isAnnouncement
            )

            // Save the post to Firebase
//            database.child("posts").child(postId).setValue(post.toMap()).await()

            //save the post to Firestore
            postCollection.document().set(post).await()

            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get feed posts
    fun getFeedPosts(): Flow<Result<List<Post>>> = callbackFlow {
//
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
//        val postListener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val post = snapshot.getValue(Post::class.java)
//                if (post != null) {
//                    trySend(Result.success(post))
//                } else {
//                    trySend(Result.failure(Exception("Post not found")))
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                trySend(Result.failure(error.toException()))
//            }
//        }
//
//        val postRef = database.child("posts").child(postId)
//        postRef.addValueEventListener(postListener)
//
//        awaitClose {
//            postRef.removeEventListener(postListener)
//        }
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

    // Like a post
    suspend fun likePost(postId: String): Result<Unit> {
//        val userId = userRepository.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
//
//        return try {
//            // Check if user already liked the post
//            val likeSnapshot = database.child("likes").child(postId).child(userId).get().await()
//            val alreadyLiked = likeSnapshot.exists()
//
//            if (alreadyLiked) {
//                // Unlike the post
//                database.child("likes").child(postId).child(userId).removeValue().await()
//
//                // Decrement like count
//                val postRef = database.child("posts").child(postId)
//
//                // Fix: Use correct Transaction.Handler implementation
//                postRef.runTransaction(object : Transaction.Handler {
//
//                    override fun doTransaction(currentData: MutableData): Transaction.Result {
//                        val post = currentData.getValue(Post::class.java)
//                        if (post != null) {
//                            val updatedPost = post.copy(likeCount = post.likeCount - 1)
//                            currentData.value = updatedPost
//                        }
//                        return Transaction.success(currentData)
//                    }
//
//
//                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
//                        // Transaction completed
//                    }
//                })
//            } else {
//                // Like the post
//                database.child("likes").child(postId).child(userId).setValue(true).await()
//
//                // Increment like count
//                val postRef = database.child("posts").child(postId)
//
//                // Fix: Use correct Transaction.Handler implementation
//                postRef.runTransaction(object : Transaction.Handler {
//
//
//                    override fun doTransaction(currentData: MutableData): Transaction.Result {
//                        val post = currentData.getValue(Post::class.java)
//                        if (post != null) {
//                            val updatedPost = post.copy(likeCount = post.likeCount + 1)
//                            currentData.value = updatedPost
//                        }
//                        return Transaction.success(currentData)
//                    }
//
//                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
//                        // Transaction completed
//                    }
//                })
//            }
//
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }

        val userId = userRepository.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        return try {
            // Check if user already liked the post
            val likeSnapshot = likesCollection.document(postId).get().await()
            val alreadyLiked = likeSnapshot.exists()

            if (alreadyLiked) {
                // Unlike the post
                likesCollection.document(postId).delete().await()
            } else {
                // Like the post
                likesCollection.document(postId).set(mapOf(userId to true)).await()
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

        val likeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isLiked = snapshot.exists()
                trySend(Result.success(isLiked))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }

        val likeRef = database.child("likes").child(postId).child(userId)
        likeRef.addValueEventListener(likeListener)

        awaitClose {
            likeRef.removeEventListener(likeListener)
        }
    }

    // Add a comment to a post
    suspend fun addComment(postId: String, text: String): Result<Comment> {
        val userId = userRepository.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        return try {
            // Get current user data
            val currentUserFlow = userRepository.getCurrentUserData()
            var username = ""
            var userProfileImageUrl = ""

            // Collect user data from flow (just once)
            currentUserFlow.collect { result ->
                result.onSuccess { user ->
                    username = user.username
                    userProfileImageUrl = user.profileImageUrl
                }
                return@collect
            }

            // Generate a new comment ID
            val commentId = database.child("comments").child(postId).push().key ?:
            return Result.failure(Exception("Failed to generate comment ID"))

            // Create the comment object
            val comment = Comment(
                commentId = commentId,
                postId = postId,
                userId = userId,
                username = username,
                userProfileImageUrl = userProfileImageUrl,
                text = text
            )

            // Save the comment to Firebase
            database.child("comments").child(postId).child(commentId).setValue(comment.toMap()).await()

            // Increment comment count on the post
            val postRef = database.child("posts").child(postId)

            // Fix: Use correct Transaction.Handler implementation
            postRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val post = currentData.getValue(Post::class.java)
                    if (post != null) {
                        val updatedPost = post.copy(commentCount = post.commentCount + 1)
                        currentData.value = updatedPost
                    }
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    // Transaction completed
                }
            })

            Result.success(comment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get comments for a post
    fun getCommentsForPost(postId: String): Flow<Result<List<Comment>>> = callbackFlow {
        val commentsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val commentsList = mutableListOf<Comment>()
                for (commentSnapshot in snapshot.children) {
                    commentSnapshot.getValue(Comment::class.java)?.let {
                        commentsList.add(it)
                    }
                }

                // Sort by timestamp (newest first)
                commentsList.sortByDescending { it.timestamp }

                trySend(Result.success(commentsList))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }

        val commentsRef = database.child("comments").child(postId)
        commentsRef.addValueEventListener(commentsListener)

        awaitClose {
            commentsRef.removeEventListener(commentsListener)
        }
    }

    // Delete a post (for post owner or admin)
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
            val postSnapshot = database.child("posts").child(postId).get().await()
            val post = postSnapshot.getValue(Post::class.java)

            if (post != null) {
                isPostOwner = post.userId == userId
            }

            if (!isAdmin && !isPostOwner) {
                return Result.failure(Exception("You don't have permission to delete this post"))
            }

            // Delete post, comments, and likes
            database.child("posts").child(postId).removeValue().await()
            database.child("comments").child(postId).removeValue().await()
            database.child("likes").child(postId).removeValue().await()

            // Delete post image if exists
            post?.imageUrl?.let {
                if (it.isNotEmpty()) {
                    storage.child("post_images/$postId").delete().await()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}