package com.example.handballconnect.data.model

import com.google.firebase.database.IgnoreExtraProperties
import java.util.Date

@IgnoreExtraProperties
data class Message(
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = Date().time,
    val isRead: Boolean = false
) {
    // Empty constructor needed for Firebase
    constructor() : this("", "", "", "", null, 0, false)
    
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "messageId" to messageId,
            "conversationId" to conversationId,
            "senderId" to senderId,
            "text" to text,
            "imageUrl" to imageUrl,
            "timestamp" to timestamp,
            "isRead" to isRead
        )
    }
}