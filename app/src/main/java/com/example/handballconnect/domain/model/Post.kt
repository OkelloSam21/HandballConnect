package com.example.handballconnect.domain.model

data class Post(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileUrl: String? = null,
    val content: String = "",
    val imageUrl: String? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val isAnnouncement: Boolean = false,
    val isTacticsShare: Boolean = false,
    val tacticsId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)