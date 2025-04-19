package com.example.handballconnect.util

import android.util.Log
import com.example.handballconnect.data.model.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Helper class for debugging admin-related issues
 */
object AdminDebugHelper {
    private const val TAG = "AdminDebugHelper"
    
    /**
     * Logs detailed information about the user's admin status
     */
    suspend fun debugAdminStatus(userId: String): Boolean {
        Log.d(TAG, "Debugging admin status for user: $userId")
        
        try {
            val firestore = FirebaseFirestore.getInstance()
            val userDoc = firestore.collection("users").document(userId).get().await()
            
            if (!userDoc.exists()) {
                Log.e(TAG, "User document does not exist for ID: $userId")
                return false
            }
            
            // Log the raw document data
            Log.d(TAG, "Raw Firestore document: ${userDoc.data}")
            
            // Check if the 'admin' field exists
            val adminField = userDoc.getBoolean("admin")
            Log.d(TAG, "Admin field value from direct get: $adminField")
            
            // Try alternative ways to access the field
            val adminViaData = userDoc.data?.get("admin") as? Boolean
            Log.d(TAG, "Admin field via data map: $adminViaData")
            
            // Check if there's a potential field name mismatch
            val isAdminField = userDoc.getBoolean("isAdmin")
            Log.d(TAG, "isAdmin field direct get: $isAdminField")
            
            val isAdminViaData = userDoc.data?.get("isAdmin") as? Boolean
            Log.d(TAG, "isAdmin field via data map: $isAdminViaData")
            
            // Convert to User object and check
            val user = userDoc.toObject(User::class.java)
            Log.d(TAG, "User object after conversion: $user")
            Log.d(TAG, "isAdmin value from User object: ${user?.isAdmin}")
            
            // If admin field exists and has a value, return it
            return adminField ?: adminViaData ?: false
            
        } catch (e: Exception) {
            Log.e(TAG, "Error debugging admin status: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Attempts to fix admin status by ensuring the right field is present
     */
    suspend fun fixAdminStatus(userId: String, shouldBeAdmin: Boolean): Boolean {
        Log.d(TAG, "Attempting to fix admin status for user: $userId, setting to: $shouldBeAdmin")
        
        try {
            val firestore = FirebaseFirestore.getInstance()
            val userRef = firestore.collection("users").document(userId)
            
            // Update both possible field names to be safe
            val updates = hashMapOf<String, Any>(
                "admin" to shouldBeAdmin,
                "isAdmin" to shouldBeAdmin
            )
            
            userRef.update(updates).await()
            Log.d(TAG, "Admin status fix attempted, updated fields: $updates")
            
            // Verify the fix
            val updatedDoc = userRef.get().await()
            val adminAfterFix = updatedDoc.getBoolean("admin")
            val isAdminAfterFix = updatedDoc.getBoolean("isAdmin")
            
            Log.d(TAG, "After fix - admin: $adminAfterFix, isAdmin: $isAdminAfterFix")
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fix admin status: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Utility to log all fields in a document
     */
    fun logAllFields(document: DocumentSnapshot) {
        val data = document.data ?: return
        
        Log.d(TAG, "All fields in document ${document.id}:")
        data.entries.forEach { (key, value) ->
            Log.d(TAG, "  $key: $value (${value?.javaClass?.simpleName})")
        }
    }
}