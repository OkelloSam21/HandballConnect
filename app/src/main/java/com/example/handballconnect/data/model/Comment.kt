package com.example.handballconnect.data.model

import com.google.firebase.database.IgnoreExtraProperties
import java.util.Date

@IgnoreExtraProperties
data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImageUrl: String = "",
    val text: String = "",
    val timestamp: Long = Date().time
) {
    // Empty constructor needed for Firebase
    constructor() : this("", "", "", "", "", "", 0)
    
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "commentId" to commentId,
            "postId" to postId,
            "userId" to userId,
            "username" to username,
            "userProfileImageUrl" to userProfileImageUrl,
            "text" to text,
            "timestamp" to timestamp
        )
    }
}