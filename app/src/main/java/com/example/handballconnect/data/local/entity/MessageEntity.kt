package com.example.handballconnect.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val imageUrl: String?,
    val timestamp: Long,
    val isRead: Boolean,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis()
)