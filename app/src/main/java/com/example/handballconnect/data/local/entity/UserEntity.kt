package com.example.handballconnect.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.handballconnect.domain.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val profileImageUrl: String?,
    val position: String?,
    val experience: String?,
    val isAdmin: Boolean,
    val createdAt: Long
)

fun UserEntity.toDomain() = User(
    id = id,
    username = username,
    email = email,
    profileImageUrl = profileImageUrl,
    position = position,
    experience = experience,
    isAdmin = isAdmin,
    createdAt = createdAt
)