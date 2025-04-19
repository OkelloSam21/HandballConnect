package com.example.handballconnect.domain.model

// Domain model
data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val position: String? = null,
    val experience: String? = null,
    val isAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)




