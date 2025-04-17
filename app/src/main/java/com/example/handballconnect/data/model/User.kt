package com.example.handballconnect.data.model

import com.google.firebase.database.IgnoreExtraProperties
import java.util.Date

@IgnoreExtraProperties
data class User(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val position: String = "",
    val experience: String = "",
    val isAdmin: Boolean = false,
    val createdAt: Long = Date().time
) {
    // Empty constructor needed for Firebase
    constructor() : this("", "", "", "", "", "", false, 0)
    
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "username" to username,
            "email" to email,
            "profileImageUrl" to profileImageUrl,
            "position" to position,
            "experience" to experience,
            "isAdmin" to isAdmin,
            "createdAt" to createdAt
        )
    }
}