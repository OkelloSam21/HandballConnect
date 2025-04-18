package com.example.handballconnect.data.repository

import android.net.Uri
import android.util.Log
import com.example.handballconnect.data.model.Conversation
import com.example.handballconnect.data.model.Message
import com.example.handballconnect.data.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val userRepository: UserRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    private val conversationsCollection = firestore.collection("conversations")
    private val messagesCollection = firestore.collection("messages")

    // Get conversations for current user
    fun getUserConversations(): Flow<Result<List<Conversation>>> = callbackFlow {
        val userId = userRepository.getCurrentUserId()
        if (userId == null) {
            trySend(Result.failure(Exception("User not logged in")))
            close()
            return@callbackFlow
        }

        val listenerRegistration = conversationsCollection
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val conversationsList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Conversation::class.java)
                    }
                    trySend(Result.success(conversationsList))
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    // Get or create conversation with user
    suspend fun getOrCreateConversation(otherUserId: String): Result<Conversation> {
        Log.d("MessageRepository", "Starting getOrCreateConversation with user: $otherUserId")

        val currentUserId = userRepository.getCurrentUserId() ?:
        return Result.failure(Exception("User not logged in"))

        // Check if conversation already exists
        try {
            Log.d("MessageRepository", "Checking for existing conversation")
            val existingConversation = findExistingConversation(currentUserId, otherUserId)

            if (existingConversation != null) {
                Log.d("MessageRepository", "Found existing conversation: ${existingConversation.conversationId}")
                return Result.success(existingConversation)
            }

            Log.d("MessageRepository", "No existing conversation found, creating new one")

            // Create a new conversation
            // Get current user data
            var currentUserName = "Unknown User"
            var currentUserImage = ""
            var otherUserName = "Unknown User"
            var otherUserImage = ""

            // Try to get current user data
            try {
                val currentUserDoc = firestore.collection("users").document(currentUserId).get().await()
                if (currentUserDoc.exists()) {
                    val userData = currentUserDoc.data
                    if (userData != null) {
                        currentUserName = userData["username"] as? String ?: "Unknown User"
                        currentUserImage = userData["profileImageUrl"] as? String ?: ""
                        Log.d("MessageRepository", "Got current user data: $currentUserName")
                    }
                }
            } catch (e: Exception) {
                Log.e("MessageRepository", "Error fetching current user data: ${e.message}")
            }

            // Try to get other user data
            try {
                val otherUserDoc = firestore.collection("users").document(otherUserId).get().await()
                if (otherUserDoc.exists()) {
                    val userData = otherUserDoc.data
                    if (userData != null) {
                        otherUserName = userData["username"] as? String ?: "Unknown User"
                        otherUserImage = userData["profileImageUrl"] as? String ?: ""
                        Log.d("MessageRepository", "Got other user data: $otherUserName")
                    }
                } else {
                    Log.e("MessageRepository", "Other user document doesn't exist")
                    return Result.failure(Exception("User not found"))
                }
            } catch (e: Exception) {
                Log.e("MessageRepository", "Error fetching other user data: ${e.message}")
                return Result.failure(Exception("Failed to load user data: ${e.message}"))
            }

            // Generate conversation ID
            val conversationId = conversationsCollection.document().id
            Log.d("MessageRepository", "Generated conversation ID: $conversationId")

            // Create maps for participant names and images
            val participantIds = listOf(currentUserId, otherUserId)
            val participantNames = mapOf(
                currentUserId to currentUserName,
                otherUserId to otherUserName
            )
            val participantImages = mapOf(
                currentUserId to currentUserImage,
                otherUserId to otherUserImage
            )
            val unreadCount = mapOf(
                currentUserId to 0,
                otherUserId to 0
            )

            // Create the conversation
            val conversation = Conversation(
                conversationId = conversationId,
                participantIds = participantIds,
                participantNames = participantNames,
                participantImages = participantImages,
                lastMessage = "",
                lastMessageTimestamp = System.currentTimeMillis(),
                lastMessageSenderId = "",
                unreadCount = unreadCount
            )

            // Save to Firestore
            Log.d("MessageRepository", "Saving new conversation to Firestore")
            conversationsCollection.document(conversationId).set(conversation).await()
            Log.d("MessageRepository", "Conversation saved successfully")

            return Result.success(conversation)
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error in getOrCreateConversation: ${e.message}", e)
            return Result.failure(e)
        }
    }

    // Find existing conversation between two users
    private suspend fun findExistingConversation(userId1: String, userId2: String): Conversation? {
        return try {
            Log.d("MessageRepository", "Searching for conversation between $userId1 and $userId2")

            val query = conversationsCollection
                .whereArrayContains("participantIds", userId1)
                .get()
                .await()

            val existingConversation = query.documents
                .mapNotNull { doc -> doc.toObject(Conversation::class.java) }
                .firstOrNull { conversation -> conversation.participantIds.contains(userId2) }

            if (existingConversation != null) {
                Log.d("MessageRepository", "Found existing conversation: ${existingConversation.conversationId}")
            } else {
                Log.d("MessageRepository", "No existing conversation found")
            }

            existingConversation
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error finding conversation: ${e.message}", e)
            null
        }
    }

    // Get messages for a conversation
    fun getMessagesForConversation(conversationId: String): Flow<Result<List<Message>>> = callbackFlow {
        val userId = userRepository.getCurrentUserId()
        if (userId == null) {
            trySend(Result.failure(Exception("User not logged in")))
            close()
            return@callbackFlow
        }

        val listenerRegistration = messagesCollection
            .whereEqualTo("conversationId", conversationId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messagesList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Message::class.java)
                    }
                    trySend(Result.success(messagesList))

                    // Mark messages as read
                    markMessagesAsRead(conversationId, userId)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    // Send a text message
    suspend fun sendTextMessage(conversationId: String, text: String): Result<Message> {
        val senderId = userRepository.getCurrentUserId() ?:
        return Result.failure(Exception("User not logged in"))

        return try {
            // Get conversation to update last message
            val conversationDoc = conversationsCollection.document(conversationId).get().await()
            val conversation = conversationDoc.toObject(Conversation::class.java)
                ?: return Result.failure(Exception("Conversation not found"))

            // Generate message ID
            val messageId = messagesCollection.document().id

            // Create the message
            val message = Message(
                messageId = messageId,
                conversationId = conversationId,
                senderId = senderId,
                text = text,
                timestamp = System.currentTimeMillis()
            )

            // Save the message
            messagesCollection.document(messageId).set(message).await()

            // Update conversation with last message details
            val otherUserId = conversation.participantIds.firstOrNull { it != senderId } ?: ""

            val conversationUpdates = mapOf(
                "lastMessage" to text,
                "lastMessageTimestamp" to message.timestamp,
                "lastMessageSenderId" to senderId,
                "unreadCount.$otherUserId" to (conversation.unreadCount[otherUserId] ?: 0) + 1
            )

            conversationsCollection.document(conversationId).update(conversationUpdates).await()

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Send an image message
    suspend fun sendImageMessage(conversationId: String, imageUri: Uri): Result<Message> {
        val senderId = userRepository.getCurrentUserId() ?:
        return Result.failure(Exception("User not logged in"))

        return try {
            // Get conversation to update last message
            val conversationDoc = conversationsCollection.document(conversationId).get().await()
            val conversation = conversationDoc.toObject(Conversation::class.java)
                ?: return Result.failure(Exception("Conversation not found"))

            // Generate message ID
            val messageId = messagesCollection.document().id

            // Upload image
            val filename = UUID.randomUUID().toString()
            val ref = storage.child("message_images/$conversationId/$messageId/$filename")

            ref.putFile(imageUri).await()
            val imageUrl = ref.downloadUrl.await().toString()

            // Create the message
            val message = Message(
                messageId = messageId,
                conversationId = conversationId,
                senderId = senderId,
                text = "[Image]",
                imageUrl = imageUrl,
                timestamp = System.currentTimeMillis()
            )

            // Save the message
            messagesCollection.document(messageId).set(message).await()

            // Update conversation with last message details
            val otherUserId = conversation.participantIds.firstOrNull { it != senderId } ?: ""

            val conversationUpdates = mapOf(
                "lastMessage" to "[Image]",
                "lastMessageTimestamp" to message.timestamp,
                "lastMessageSenderId" to senderId,
                "unreadCount.$otherUserId" to (conversation.unreadCount[otherUserId] ?: 0) + 1
            )

            conversationsCollection.document(conversationId).update(conversationUpdates).await()

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mark messages as read
    private fun markMessagesAsRead(conversationId: String, userId: String) {
        try {
            // Update unread count to zero for current user
            conversationsCollection.document(conversationId)
                .update("unreadCount.$userId", 0)
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error marking messages as read: ${e.message}")
        }
    }
}