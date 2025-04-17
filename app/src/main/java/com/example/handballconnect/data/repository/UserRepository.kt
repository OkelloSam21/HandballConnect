package com.example.handballconnect.data.repository

import android.net.Uri
import com.example.handballconnect.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    //firestore
    private val fireStore = FirebaseFirestore.getInstance()
    val userCollection = fireStore.collection("users")
    
    // Get current user ID
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    // Get current Firebase user
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    // Check if user is logged in
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    // Register new user
    suspend fun registerUser(email: String, password: String, username: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                // Update profile with username
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build()
                
                firebaseUser.updateProfile(profileUpdates).await()
                
                // Create user in database
                val userId = firebaseUser.uid
                val newUser = User(
                    userId = userId,
                    username = username,
                    email = email
                )

                userCollection.document().set(newUser)

//                database.child("users").child(userId).setValue(newUser.toMap()).await()
                Result.success(newUser)
            } else {
                Result.failure(Exception("User registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Login user
    suspend fun loginUser(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Logout user
    fun logoutUser() {
        auth.signOut()
    }
    
    // Reset password
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get user by ID
    fun getUserById(userId: String): Flow<Result<User>> = callbackFlow {
        val userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    trySend(Result.success(user))
                } else {
                    trySend(Result.failure(Exception("User not found")))
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        
        val userRef = database.child("users").child(userId)
        userRef.addValueEventListener(userListener)
        
        // Remove the listener when the flow is cancelled
        awaitClose {
            userRef.removeEventListener(userListener)
        }
    }
    
    // Get current user data
    fun getCurrentUserData(): Flow<Result<User>> {
        val userId = getCurrentUserId() ?: return callbackFlow {
            trySend(Result.failure(Exception("User not logged in")))
            awaitClose { }
        }
        
        return getUserById(userId)
    }
    
    // Update user profile
    suspend fun updateUserProfile(
        username: String? = null,
        position: String? = null,
        experience: String? = null
    ): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val updates = mutableMapOf<String, Any>()
            
            username?.let { updates["username"] = it }
            position?.let { updates["position"] = it }
            experience?.let { updates["experience"] = it }
            
            if (updates.isNotEmpty()) {
                database.child("users").child(userId).updateChildren(updates).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Upload profile image
    suspend fun uploadProfileImage(imageUri: Uri): Result<String> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val filename = UUID.randomUUID().toString()
            val ref = storage.child("profile_images/$userId/$filename")
            
            val uploadTask = ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            
            // Update user profile with the new image URL
            database.child("users").child(userId).child("profileImageUrl").setValue(downloadUrl).await()
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get all users (for admin)
    fun getAllUsers(): Flow<Result<List<User>>> = callbackFlow {
        val usersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usersList = mutableListOf<User>()
                for (userSnapshot in snapshot.children) {
                    userSnapshot.getValue(User::class.java)?.let {
                        usersList.add(it)
                    }
                }
                trySend(Result.success(usersList))
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        
        val usersRef = database.child("users")
        usersRef.addValueEventListener(usersListener)
        
        awaitClose {
            usersRef.removeEventListener(usersListener)
        }
    }
    
    // Update user admin status (for admin)
    suspend fun updateUserAdminStatus(userId: String, isAdmin: Boolean): Result<Unit> {
        return try {
            database.child("users").child(userId).child("isAdmin").setValue(isAdmin).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}