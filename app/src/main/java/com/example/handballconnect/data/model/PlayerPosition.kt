package com.example.handballconnect.data.model

import com.google.firebase.database.IgnoreExtraProperties
import java.util.Date

// Player position on the tactics board
data class PlayerPosition(
    val id: Int = 0,
    val x: Float = 0f,
    val y: Float = 0f,
    val number: Int = 0,
    val isOffense: Boolean = true,
    val label: String = ""
) {
    // Empty constructor needed for Firebase
    constructor() : this(0, 0f, 0f, 0, true, "")
    
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "x" to x,
            "y" to y,
            "number" to number,
            "isOffense" to isOffense,
            "label" to label
        )
    }
}

// Movement arrow on the tactics board
data class MovementArrow(
    val id: Int = 0,
    val startX: Float = 0f,
    val startY: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val playerId: Int? = null, // Optional reference to a specific player
    val isPass: Boolean = false // If false, it's a player movement
) {
    // Empty constructor needed for Firebase
    constructor() : this(0, 0f, 0f, 0f, 0f, null, false)
    
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "startX" to startX,
            "startY" to startY,
            "endX" to endX,
            "endY" to endY,
            "playerId" to playerId,
            "isPass" to isPass
        )
    }
}

@IgnoreExtraProperties
data class Tactics(
    val tacticsId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val players: List<PlayerPosition> = listOf(),
    val movements: List<MovementArrow> = listOf(),
    val imageUrl: String? = null,
    val isShared: Boolean = false,
    val timestamp: Long = Date().time
) {
    // Empty constructor needed for Firebase
    constructor() : this("", "", "", "", listOf(), listOf(), null, false, 0)
    
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "tacticsId" to tacticsId,
            "userId" to userId,
            "title" to title,
            "description" to description,
            "players" to players.map { it.toMap() },
            "movements" to movements.map { it.toMap() },
            "imageUrl" to imageUrl,
            "isShared" to isShared,
            "timestamp" to timestamp
        )
    }
}