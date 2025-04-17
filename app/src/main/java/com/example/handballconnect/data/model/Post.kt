package com.example.handballconnect.data.model

import com.google.firebase.database.IgnoreExtraProperties
import java.util.Date

@IgnoreExtraProperties
data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImageUrl: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = Date().time,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isAnnouncement: Boolean = false
) {
    // Empty constructor needed for Firebase
    constructor() : this("", "", "", "", "", null, 0, 0, 0, false)
    
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "postId" to postId,
            "userId" to userId,
            "username" to username,
            "userProfileImageUrl" to userProfileImageUrl,
            "text" to text,
            "imageUrl" to imageUrl,
            "timestamp" to timestamp,
            "likeCount" to likeCount,
            "commentCount" to commentCount,
            "isAnnouncement" to isAnnouncement
        )
    }
}