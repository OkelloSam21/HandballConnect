package com.example.handballconnect.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages local image storage for the application
 */
@Singleton
class ImageStorageManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "ImageStorageManager"
        private const val PROFILE_IMAGE_PREFIX = "profile_"
        private const val POST_IMAGE_PREFIX = "post_"
        private const val DEFAULT_QUALITY = 80
        private const val PROFILE_IMAGE_SIZE = 300 // Size in pixels for profile images
        private const val POST_IMAGE_SIZE = 1080 // Size in pixels for post images

        // Used for storage paths
        private const val PROFILE_IMAGES_DIR = "profile_images"
        private const val POST_IMAGES_DIR = "post_images"

        // Used as prefix in local image references
        const val LOCAL_URI_PREFIX = "local:"
    }

    /**
     * Saves a profile image locally
     * @param userId User ID to associate with the image
     * @param imageUri Source URI of the image
     * @return Local reference URI string that can be stored in Firestore
     */
    suspend fun saveProfileImage(userId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving profile image for user: $userId")

            // Create directories if they don't exist
            val profileDir = File(context.filesDir, PROFILE_IMAGES_DIR)
            if (!profileDir.exists()) {
                profileDir.mkdirs()
            }

            // Generate unique filename
            val filename = "${PROFILE_IMAGE_PREFIX}${userId}_${UUID.randomUUID()}.jpg"
            val file = File(profileDir, filename)

            // Load and compress the image
            val bitmap = loadAndCompressBitmap(imageUri, PROFILE_IMAGE_SIZE)

            // Save compressed image to file
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, DEFAULT_QUALITY, out)
            }

            Log.d(TAG, "Profile image saved to: ${file.absolutePath}")

            // Create a local URI reference to store in Firestore
            val localReference = "$LOCAL_URI_PREFIX$PROFILE_IMAGES_DIR/$filename"
            return@withContext Result.success(localReference)

        } catch (e: Exception) {
            Log.e(TAG, "Error saving profile image: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Saves a post image locally
     * @param postId Post ID to associate with the image
     * @param imageUri Source URI of the image
     * @return Local reference URI string that can be stored in Firestore
     */
    suspend fun savePostImage(postId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving post image for post: $postId")

            // Create directories if they don't exist
            val postDir = File(context.filesDir, POST_IMAGES_DIR)
            if (!postDir.exists()) {
                postDir.mkdirs()
            }

            // Generate unique filename
            val filename = "${POST_IMAGE_PREFIX}${postId}_${UUID.randomUUID()}.jpg"
            val file = File(postDir, filename)

            // Load and compress the image
            val bitmap = loadAndCompressBitmap(imageUri, POST_IMAGE_SIZE)

            // Save compressed image to file
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, DEFAULT_QUALITY, out)
            }

            Log.d(TAG, "Post image saved to: ${file.absolutePath}")

            // Create a local URI reference to store in Firestore
            val localReference = "$LOCAL_URI_PREFIX$POST_IMAGES_DIR/$filename"
            return@withContext Result.success(localReference)

        } catch (e: Exception) {
            Log.e(TAG, "Error saving post image: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Deletes an image from local storage
     * @param localReference Local reference string of the image
     * @return True if deletion was successful, false otherwise
     */
    suspend fun deleteImage(localReference: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!localReference.startsWith(LOCAL_URI_PREFIX)) {
                Log.e(TAG, "Invalid local reference: $localReference")
                return@withContext false
            }

            // Extract the path from the reference
            val path = localReference.substring(LOCAL_URI_PREFIX.length)
            val file = File(context.filesDir, path)

            if (!file.exists()) {
                Log.e(TAG, "File does not exist: ${file.absolutePath}")
                return@withContext false
            }

            val result = file.delete()
            Log.d(TAG, "Deleted file: ${file.absolutePath}, result: $result")
            return@withContext result

        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Gets a usable URI for an image from its local reference
     * @param localReference Local reference string of the image
     * @return URI that can be used with Glide/Coil for image loading or null if invalid
     */
    fun getImageUri(localReference: String?): Uri? {
        if (localReference == null || !localReference.startsWith(LOCAL_URI_PREFIX)) {
            return null
        }

        try {
            // Extract the path from the reference
            val path = localReference.substring(LOCAL_URI_PREFIX.length)
            val file = File(context.filesDir, path)

            if (!file.exists()) {
                Log.e(TAG, "Image file does not exist: ${file.absolutePath}")
                return null
            }

            // Return a FileProvider URI that can be used by image loading libraries
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting image URI: ${e.message}", e)
            return null
        }
    }

    /**
     * Loads an image from Uri and compresses it to the specified max dimension
     */
    private fun loadAndCompressBitmap(imageUri: Uri, maxSize: Int): Bitmap {
        val contentResolver = context.contentResolver

        // Get original image dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        contentResolver.openInputStream(imageUri).use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight

        // Calculate sample size for loading
        var sampleSize = 1
        if (originalWidth > maxSize || originalHeight > maxSize) {
            val halfWidth = originalWidth / 2
            val halfHeight = originalHeight / 2

            while ((halfWidth / sampleSize) >= maxSize || (halfHeight / sampleSize) >= maxSize) {
                sampleSize *= 2
            }
        }

        // Load the bitmap with calculated sample size
        val loadOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        val bitmap = contentResolver.openInputStream(imageUri).use { input ->
            BitmapFactory.decodeStream(input, null, loadOptions)
        } ?: throw IOException("Failed to decode image from URI")

        // Calculate final dimensions maintaining aspect ratio
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap // Already small enough
        }

        val ratio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxSize
            newHeight = (newWidth / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (newHeight * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}