package com.example.handballconnect.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.util.Date

@IgnoreExtraProperties
data class User(
    @PropertyName("userId")
    val userId: String = "",

    @PropertyName("username")
    val username: String = "",

    @PropertyName("email")
    val email: String = "",

    @PropertyName("profileImageUrl")
    val profileImageUrl: String = "",

    @PropertyName("position")
    val position: String = "",

    @PropertyName("experience")
    val experience: String = "",

    // Map the Firestore "admin" field to our isAdmin property
    @get:PropertyName("admin")
//    @set:PropertyName("admin")
    @PropertyName("admin")
    val isAdmin: Boolean = false,

    var isDisabled: Boolean = false,

    @PropertyName("createdAt")
    val createdAt: Long = Date().time
) {
    // Empty constructor needed for Firebase
    constructor() : this("", "", "", "", "", "", false, false,0)

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "username" to username,
            "email" to email,
            "profileImageUrl" to profileImageUrl,
            "position" to position,
            "experience" to experience,
            "admin" to isAdmin,
            "isDisabled" to isDisabled,
            "createdAt" to createdAt
        )
    }
}