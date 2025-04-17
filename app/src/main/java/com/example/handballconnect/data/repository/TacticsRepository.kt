package com.example.handballconnect.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.example.handballconnect.data.model.MovementArrow
import com.example.handballconnect.data.model.PlayerPosition
import com.example.handballconnect.data.model.Tactics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TacticsRepository @Inject constructor(
    private val userRepository: UserRepository
) {
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference
    
    // Save tactics
    suspend fun saveTactics(
        title: String,
        description: String,
        players: List<PlayerPosition>,
        movements: List<MovementArrow>,
        boardImage: Bitmap?,
        isShared: Boolean,
        existingTacticsId: String? = null
    ): Result<Tactics> {
        val userId = userRepository.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            // Use existing ID or generate a new one
            val tacticsId = existingTacticsId ?: database.child("tactics").push().key ?: 
                return Result.failure(Exception("Failed to generate tactics ID"))
            
            // Upload board image if provided
            var imageUrl: String? = null
            boardImage?.let {
                val baos = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val data = baos.toByteArray()
                
                val ref = storage.child("tactics_images/$tacticsId.png")
                ref.putBytes(data).await()
                imageUrl = ref.downloadUrl.await().toString()
            }
            
            // Create tactics object
            val tactics = Tactics(
                tacticsId = tacticsId,
                userId = userId,
                title = title,
                description = description,
                players = players,
                movements = movements,
                imageUrl = imageUrl,
                isShared = isShared
            )
            
            // Save to Firebase
            database.child("tactics").child(tacticsId).setValue(tactics.toMap()).await()
            
            Result.success(tactics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get user's tactics
    fun getUserTactics(): Flow<Result<List<Tactics>>> = callbackFlow {
        val userId = userRepository.getCurrentUserId()
        if (userId == null) {
            trySend(Result.failure(Exception("User not logged in")))
            close()
            return@callbackFlow
        }
        
        val tacticsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tacticsList = mutableListOf<Tactics>()
                
                for (tacticsSnapshot in snapshot.children) {
                    val tactics = tacticsSnapshot.getValue(Tactics::class.java)
                    if (tactics != null && tactics.userId == userId) {
                        tacticsList.add(tactics)
                    }
                }
                
                // Sort by timestamp (newest first)
                tacticsList.sortByDescending { it.timestamp }
                
                trySend(Result.success(tacticsList))
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        
        val tacticsRef = database.child("tactics")
        tacticsRef.addValueEventListener(tacticsListener)
        
        awaitClose {
            tacticsRef.removeEventListener(tacticsListener)
        }
    }
    
    // Get shared tactics
    fun getSharedTactics(): Flow<Result<List<Tactics>>> = callbackFlow {
        val tacticsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tacticsList = mutableListOf<Tactics>()
                
                for (tacticsSnapshot in snapshot.children) {
                    val tactics = tacticsSnapshot.getValue(Tactics::class.java)
                    if (tactics != null && tactics.isShared) {
                        tacticsList.add(tactics)
                    }
                }
                
                // Sort by timestamp (newest first)
                tacticsList.sortByDescending { it.timestamp }
                
                trySend(Result.success(tacticsList))
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        
        val tacticsRef = database.child("tactics")
        tacticsRef.addValueEventListener(tacticsListener)
        
        awaitClose {
            tacticsRef.removeEventListener(tacticsListener)
        }
    }
    
    // Get specific tactics
    fun getTacticsById(tacticsId: String): Flow<Result<Tactics>> = callbackFlow {
        val tacticsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tactics = snapshot.getValue(Tactics::class.java)
                if (tactics != null) {
                    trySend(Result.success(tactics))
                } else {
                    trySend(Result.failure(Exception("Tactics not found")))
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        
        val tacticsRef = database.child("tactics").child(tacticsId)
        tacticsRef.addValueEventListener(tacticsListener)
        
        awaitClose {
            tacticsRef.removeEventListener(tacticsListener)
        }
    }
    
    // Delete tactics
    suspend fun deleteTactics(tacticsId: String): Result<Unit> {
        val userId = userRepository.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            // Get tactics to check ownership
            val tacticsSnapshot = database.child("tactics").child(tacticsId).get().await()
            val tactics = tacticsSnapshot.getValue(Tactics::class.java)
                ?: return Result.failure(Exception("Tactics not found"))
            
            // Check if user owns the tactics
            if (tactics.userId != userId) {
                // Check if user is admin
                val currentUserFlow = userRepository.getCurrentUserData()
                var isAdmin = false
                
                currentUserFlow.collect { result ->
                    result.onSuccess { user ->
                        isAdmin = user.isAdmin
                    }
                    return@collect
                }
                
                if (!isAdmin) {
                    return Result.failure(Exception("You don't have permission to delete these tactics"))
                }
            }
            
            // Delete tactics image if exists
            tactics.imageUrl?.let {
                storage.child("tactics_images/$tacticsId.png").delete().await()
            }
            
            // Delete tactics
            database.child("tactics").child(tacticsId).removeValue().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update sharing status
    suspend fun updateSharingStatus(tacticsId: String, isShared: Boolean): Result<Unit> {
        val userId = userRepository.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            // Get tactics to check ownership
            val tacticsSnapshot = database.child("tactics").child(tacticsId).get().await()
            val tactics = tacticsSnapshot.getValue(Tactics::class.java)
                ?: return Result.failure(Exception("Tactics not found"))
            
            // Check if user owns the tactics
            if (tactics.userId != userId) {
                return Result.failure(Exception("You don't have permission to update these tactics"))
            }
            
            // Update sharing status
            database.child("tactics").child(tacticsId).child("isShared").setValue(isShared).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get default player positions (for templates)
    fun getDefaultPlayerPositions(isOffense: Boolean): List<PlayerPosition> {
        return if (isOffense) {
            // Default offense formation (6-0)
            listOf(
                PlayerPosition(id = 1, x = 0.2f, y = 0.5f, number = 1, isOffense = true, label = "LW"),
                PlayerPosition(id = 2, x = 0.3f, y = 0.3f, number = 2, isOffense = true, label = "LB"),
                PlayerPosition(id = 3, x = 0.3f, y = 0.5f, number = 3, isOffense = true, label = "CB"),
                PlayerPosition(id = 4, x = 0.3f, y = 0.7f, number = 4, isOffense = true, label = "RB"),
                PlayerPosition(id = 5, x = 0.2f, y = 0.9f, number = 5, isOffense = true, label = "RW"),
                PlayerPosition(id = 6, x = 0.15f, y = 0.5f, number = 6, isOffense = true, label = "P")
            )
        } else {
            // Default defense formation (6-0)
            listOf(
                PlayerPosition(id = 7, x = 0.8f, y = 0.3f, number = 1, isOffense = false, label = "LD"),
                PlayerPosition(id = 8, x = 0.8f, y = 0.4f, number = 2, isOffense = false, label = "LHD"),
                PlayerPosition(id = 9, x = 0.8f, y = 0.5f, number = 3, isOffense = false, label = "CHD"),
                PlayerPosition(id = 10, x = 0.8f, y = 0.6f, number = 4, isOffense = false, label = "RHD"),
                PlayerPosition(id = 11, x = 0.8f, y = 0.7f, number = 5, isOffense = false, label = "RD"),
                PlayerPosition(id = 12, x = 0.9f, y = 0.5f, number = 12, isOffense = false, label = "GK")
            )
        }
    }
}