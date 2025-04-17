package com.example.handballconnect.domain.model

import com.example.handballconnect.data.local.entity.UserEntity

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

// Mapper function
fun User.toEntity() = UserEntity(
    id = id,
    username = username,
    email = email,
    profileImageUrl = profileImageUrl,
    position = position,
    experience = experience,
    isAdmin = isAdmin,
    createdAt = createdAt
)




