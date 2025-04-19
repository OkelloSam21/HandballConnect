package com.example.handballconnect.data.repository

import android.net.Uri
import android.util.Log
import com.example.handballconnect.data.model.User
import com.example.handballconnect.data.storage.ImageStorageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val imageStorageManager: ImageStorageManager
) {
    private val auth = FirebaseAuth.getInstance()
    private val fireStore = FirebaseFirestore.getInstance()
    private val userCollection = fireStore.collection("users")

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

                userCollection.document(userId).set(newUser).await()
                Result.success(newUser)
            } else {
                Result.failure(Exception("User registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get current user data
    fun getCurrentUserData(): Flow<Result<User>> = callbackFlow {
        val userId = getCurrentUserId() ?: run {
            Log.e("UserRepository", "User not logged in")
            trySend(Result.failure(Exception("User not logged in")))
            close()
            return@callbackFlow
        }

        Log.d("UserRepository", "Fetching user data for ID: $userId")

        val listenerRegistration = userCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "Error fetching user data: ${error.message}")
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        // Log the raw document data for debugging
                        Log.d("UserRepository", "Raw document data: ${snapshot.data}")

                        // Check specifically for the admin field
                        val adminValue = snapshot.getBoolean("admin")
                        Log.d("UserRepository", "Admin field from direct get: $adminValue")

                        // Create a User object with correct field mapping
                        val user = User(
                            userId = snapshot.id,
                            username = snapshot.getString("username") ?: "",
                            email = snapshot.getString("email") ?: "",
                            profileImageUrl = snapshot.getString("profileImageUrl") ?: "",
                            position = snapshot.getString("position") ?: "",
                            experience = snapshot.getString("experience") ?: "",
                            // Important: Use the admin field from Firestore
                            isAdmin = adminValue ?: false,
                            createdAt = snapshot.getLong("createdAt") ?: 0
                        )

                        Log.d("UserRepository", "Mapped to User object: $user")
                        Log.d("UserRepository", "User is admin: ${user.isAdmin}")

                        trySend(Result.success(user))
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Error mapping document to User object: ${e.message}", e)

                        // Fallback: Try to use Firestore's toObject method
                        try {
                            val fallbackUser = snapshot.toObject(User::class.java)
                            if (fallbackUser != null) {
                                Log.d("UserRepository", "Fallback User via toObject: $fallbackUser, isAdmin: ${fallbackUser.isAdmin}")
                                trySend(Result.success(fallbackUser))
                            } else {
                                trySend(Result.failure(Exception("Failed to map user data")))
                            }
                        } catch (fallbackError: Exception) {
                            Log.e("UserRepository", "Fallback mapping also failed: ${fallbackError.message}", fallbackError)
                            trySend(Result.failure(fallbackError))
                        }
                    }
                } else {
                    Log.e("UserRepository", "User document doesn't exist")
                    trySend(Result.failure(Exception("User not found")))
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    // Get user by ID
    fun getUserById(userId: String): Flow<Result<User>> = callbackFlow {
        val listenerRegistration = userCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    if (user != null) {
                        trySend(Result.success(user))
                    } else {
                        trySend(Result.failure(Exception("User data is null")))
                    }
                } else {
                    trySend(Result.failure(Exception("User not found")))
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
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
                userCollection.document(userId).update(updates).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Upload profile image with local storage
    suspend fun uploadProfileImage(imageUri: Uri): Result<String> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        return try {
            Log.d("UserRepository", "Starting profile image upload for user: $userId")

            // Save image locally
            val imageResult = imageStorageManager.saveProfileImage(userId, imageUri)

            imageResult.onSuccess { localReference ->
                Log.d("UserRepository", "Profile image saved locally: $localReference")

                // Delete old image if exists
                val userDoc = userCollection.document(userId).get().await()
                val user = userDoc.toObject(User::class.java)

                user?.profileImageUrl?.let { oldImageRef ->
                    if (oldImageRef.startsWith(ImageStorageManager.LOCAL_URI_PREFIX)) {
                        val imageDeleted = imageStorageManager.deleteImage(oldImageRef)
                        Log.d("UserRepository", "Old profile image deletion: $imageDeleted")
                    }
                }

                // Update user profile with the new local reference
                userCollection.document(userId).update("profileImageUrl", localReference).await()
                Log.d("UserRepository", "User profile updated with new image reference")

                // Also update the Auth user's display name to trigger UI refreshes
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val currentDisplayName = currentUser.displayName ?: ""
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(currentDisplayName)
                        .build()

                    currentUser.updateProfile(profileUpdates).await()
                    Log.d("UserRepository", "Firebase Auth user profile refreshed")
                }

                return Result.success(localReference)
            }

            return imageResult
        } catch (e: Exception) {
            Log.e("UserRepository", "Error uploading profile image: ${e.message}", e)
            return Result.failure(e)
        }
    }

    // Get all users
    fun getAllUsers(): Flow<Result<List<User>>> = callbackFlow {
        val listenerRegistration = userCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)
                    }
                    trySend(Result.success(users))
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    // Update user admin status
    suspend fun updateUserAdminStatus(userId: String, isAdmin: Boolean): Result<Unit> {
        return try {
            userCollection.document(userId).update("isAdmin", isAdmin).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks and fixes the admin status for a user
     * This is helpful when there might be a field name mismatch between "admin" and "isAdmin"
     */
    suspend fun checkAndFixAdminStatus(userId: String? = null): Result<Boolean> {
        // Use the provided userId or the current user's ID
        val targetUserId = userId ?: getCurrentUserId() ?:
        return Result.failure(Exception("User not logged in"))

        try {
            Log.d("UserRepository", "Checking admin status for user: $targetUserId")

            // Get the user document
            val userDoc = userCollection.document(targetUserId).get().await()

            if (!userDoc.exists()) {
                Log.e("UserRepository", "User document doesn't exist")
                return Result.failure(Exception("User not found"))
            }

            // Log all fields for debugging
            Log.d("UserRepository", "User document data: ${userDoc.data}")

            // Check for "admin" field
            val adminValue = userDoc.getBoolean("admin")
            Log.d("UserRepository", "Admin field value: $adminValue")

            // Check for "isAdmin" field
            val isAdminValue = userDoc.getBoolean("isAdmin")
            Log.d("UserRepository", "isAdmin field value: $isAdminValue")

            // Determine the actual admin status
            val shouldBeAdmin = adminValue ?: isAdminValue ?: false

            // If we have conflicting values or a missing field, fix it
            if (adminValue != isAdminValue || adminValue == null || isAdminValue == null) {
                Log.d("UserRepository", "Fixing admin status to be consistent: $shouldBeAdmin")

                val updates = hashMapOf<String, Any>(
                    "admin" to shouldBeAdmin,
                    "isAdmin" to shouldBeAdmin
                )

                userCollection.document(targetUserId).update(updates).await()
                Log.d("UserRepository", "Updated admin fields: $updates")
            }

            return Result.success(shouldBeAdmin)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error checking/fixing admin status: ${e.message}", e)
            return Result.failure(e)
        }
    }
}