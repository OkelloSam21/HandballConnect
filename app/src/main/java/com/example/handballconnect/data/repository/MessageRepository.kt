package com.example.handballconnect.data.repository

import android.net.Uri
import com.example.handballconnect.data.model.Conversation
import com.example.handballconnect.data.model.Message
import com.example.handballconnect.data.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val userRepository: UserRepository
) {
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference
    
    // Get conversations for current user
    fun getUserConversations(): Flow<Result<List<Conversation>>> = callbackFlow {
        val userId = userRepository.getCurrentUserId()
        if (userId == null) {
            trySend(Result.failure(Exception("User not logged in")))
            close()
            return@callbackFlow
        }
        
        val conversationsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversationsList = mutableListOf<Conversation>()
                
                for (conversationSnapshot in snapshot.children) {
                    val conversation = conversationSnapshot.getValue(Conversation::class.java)
                    if (conversation != null && conversation.participantIds.contains(userId)) {
                        conversationsList.add(conversation)
                    }
                }
                
                // Sort by last message timestamp (newest first)
                conversationsList.sortByDescending { it.lastMessageTimestamp }
                
                trySend(Result.success(conversationsList))
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        
        val conversationsRef = database.child("conversations")
        conversationsRef.addValueEventListener(conversationsListener)
        
        awaitClose {
            conversationsRef.removeEventListener(conversationsListener)
        }
    }
    
    // Get or create conversation with user
    suspend fun getOrCreateConversation(otherUserId: String): Result<Conversation> {
        val currentUserId = userRepository.getCurrentUserId() ?: 
            return Result.failure(Exception("User not logged in"))
        
        // Check if conversation already exists
        val existingConversation = findExistingConversation(currentUserId, otherUserId)
        
        if (existingConversation != null) {
            return Result.success(existingConversation)
        }
        
        // Create a new conversation
        return try {
            // Get current user data
            val currentUserResult = userRepository.getUserById(currentUserId).collect { result ->
                return@collect
            }
            
            // Get other user data
            val otherUserResult = userRepository.getUserById(otherUserId).collect { result ->
                return@collect
            }
            
            var currentUser: User? = null
            var otherUser: User? = null

            userRepository.getUserById(currentUserId).collect { result ->
                result.onSuccess { user ->
                    currentUser = user
                }.onFailure {
                    return@collect
                }
            }

            userRepository.getUserById(otherUserId).collect { result ->
                result.onSuccess { user ->
                    otherUser = user
                }.onFailure {
                    return@collect
                }
            }
            
            if (currentUser == null || otherUser == null) {
                return Result.failure(Exception("Failed to retrieve user data"))
            }
            
            // Generate conversation ID
            val conversationId = database.child("conversations").push().key ?: 
                return Result.failure(Exception("Failed to generate conversation ID"))
            
            // Create maps for participant names and images
            val participantIds = listOf(currentUserId, otherUserId)
            val participantNames = mapOf(
                currentUserId to currentUser!!.username,
                otherUserId to otherUser!!.username
            )
            val participantImages = mapOf(
                currentUserId to currentUser!!.profileImageUrl,
                otherUserId to otherUser!!.profileImageUrl
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
                unreadCount = unreadCount
            )
            
            // Save to Firebase
            database.child("conversations").child(conversationId).setValue(conversation.toMap()).await()
            
            Result.success(conversation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Find existing conversation between two users
    private suspend fun findExistingConversation(userId1: String, userId2: String): Conversation? {
        return try {
            val snapshot = database.child("conversations").get().await()
            
            for (conversationSnapshot in snapshot.children) {
                val conversation = conversationSnapshot.getValue(Conversation::class.java)
                
                if (conversation != null && 
                    conversation.participantIds.contains(userId1) && 
                    conversation.participantIds.contains(userId2)) {
                    return conversation
                }
            }
            
            null
        } catch (e: Exception) {
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
        
        val messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messagesList = mutableListOf<Message>()
                
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null) {
                        messagesList.add(message)
                    }
                }
                
                // Sort by timestamp
                messagesList.sortBy { it.timestamp }
                
                trySend(Result.success(messagesList))
                
                // Mark messages as read
                markMessagesAsRead(conversationId, userId)
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        
        val messagesRef = database.child("messages").child(conversationId)
        messagesRef.addValueEventListener(messagesListener)
        
        awaitClose {
            messagesRef.removeEventListener(messagesListener)
        }
    }
    
    // Send a text message
    suspend fun sendTextMessage(conversationId: String, text: String): Result<Message> {
        val senderId = userRepository.getCurrentUserId() ?: 
            return Result.failure(Exception("User not logged in"))
        
        return try {
            // Get conversation to update last message
            val conversationSnapshot = database.child("conversations").child(conversationId).get().await()
            val conversation = conversationSnapshot.getValue(Conversation::class.java)
                ?: return Result.failure(Exception("Conversation not found"))
            
            // Generate message ID
            val messageId = database.child("messages").child(conversationId).push().key ?: 
                return Result.failure(Exception("Failed to generate message ID"))
            
            // Create the message
            val message = Message(
                messageId = messageId,
                conversationId = conversationId,
                senderId = senderId,
                text = text
            )
            
            // Save the message
            database.child("messages").child(conversationId).child(messageId).setValue(message.toMap()).await()
            
            // Update conversation with last message details
            val otherUserId = conversation.getOtherParticipantId(senderId) ?: ""
            
            val conversationUpdates = mapOf(
                "lastMessage" to text,
                "lastMessageTimestamp" to message.timestamp,
                "lastMessageSenderId" to senderId,
                "unreadCount/${otherUserId}" to (conversation.unreadCount[otherUserId] ?: 0) + 1
            )
            
            database.child("conversations").child(conversationId).updateChildren(conversationUpdates).await()
            
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
            val conversationSnapshot = database.child("conversations").child(conversationId).get().await()
            val conversation = conversationSnapshot.getValue(Conversation::class.java)
                ?: return Result.failure(Exception("Conversation not found"))
            
            // Generate message ID
            val messageId = database.child("messages").child(conversationId).push().key ?: 
                return Result.failure(Exception("Failed to generate message ID"))
            
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
                imageUrl = imageUrl
            )
            
            // Save the message
            database.child("messages").child(conversationId).child(messageId).setValue(message.toMap()).await()
            
            // Update conversation with last message details
            val otherUserId = conversation.getOtherParticipantId(senderId) ?: ""
            
            val conversationUpdates = mapOf(
                "lastMessage" to "[Image]",
                "lastMessageTimestamp" to message.timestamp,
                "lastMessageSenderId" to senderId,
                "unreadCount/${otherUserId}" to (conversation.unreadCount[otherUserId] ?: 0) + 1
            )
            
            database.child("conversations").child(conversationId).updateChildren(conversationUpdates).await()
            
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Mark messages as read
    private fun markMessagesAsRead(conversationId: String, userId: String) {
        try {
            // Update unread count to zero for current user
            database.child("conversations").child(conversationId)
                .child("unreadCount").child(userId).setValue(0)
        } catch (e: Exception) {
            // Handle error silently
        }
    }
}