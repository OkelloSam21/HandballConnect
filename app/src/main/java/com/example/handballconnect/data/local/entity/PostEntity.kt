package com.example.handballconnect.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val username: String,
    val userProfileUrl: String?,
    val content: String,
    val imageUrl: String?,
    val likeCount: Int,
    val commentCount: Int,
    val isLikedByMe: Boolean,
    val isAnnouncement: Boolean,
    val isTacticsShare: Boolean,
    val tacticsId: String?,
    val createdAt: Long
)