package com.example.handballconnect.data.model

import com.google.firebase.database.IgnoreExtraProperties
import java.util.Date

@IgnoreExtraProperties
data class Conversation(
    val conversationId: String = "",
    val participantIds: List<String> = listOf(),
    val participantNames: Map<String, String> = mapOf(),
    val participantImages: Map<String, String> = mapOf(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = Date().time,
    val lastMessageSenderId: String = "",
    val unreadCount: Map<String, Int> = mapOf()
) {
    // Empty constructor needed for Firebase
    constructor() : this("", listOf(), mapOf(), mapOf(), "", 0, "", mapOf())
    
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "conversationId" to conversationId,
            "participantIds" to participantIds,
            "participantNames" to participantNames,
            "participantImages" to participantImages,
            "lastMessage" to lastMessage,
            "lastMessageTimestamp" to lastMessageTimestamp,
            "lastMessageSenderId" to lastMessageSenderId,
            "unreadCount" to unreadCount
        )
    }
    
    // Helper function to get other participant's info (for 1-on-1 chats)
    fun getOtherParticipantId(currentUserId: String): String? {
        return participantIds.find { it != currentUserId }
    }
    
    fun getOtherParticipantName(currentUserId: String): String {
        val otherId = getOtherParticipantId(currentUserId) ?: return ""
        return participantNames[otherId] ?: ""
    }
    
    fun getOtherParticipantImage(currentUserId: String): String {
        val otherId = getOtherParticipantId(currentUserId) ?: return ""
        return participantImages[otherId] ?: ""
    }
    
    fun getCurrentUserUnreadCount(currentUserId: String): Int {
        return unreadCount[currentUserId] ?: 0
    }
}