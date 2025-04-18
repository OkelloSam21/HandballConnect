package com.example.handballconnect.util

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.example.handballconnect.data.storage.ImageStorageManager
import javax.inject.Inject
import androidx.core.net.toUri

/**
 * Utility class for handling image loading with local references
 */
class ImageLoadingUtils @Inject constructor(
    private val imageStorageManager: ImageStorageManager
) {
    /**
     * Resolves an image reference to a proper URI
     * @param imageReference Can be a local reference or a regular HTTP URL
     * @return The URI to use for loading, or null if the image can't be resolved
     */
    fun resolveImageUri(imageReference: String?): Uri? {
        if (imageReference == null || imageReference.isEmpty()) {
            return null
        }
        
        return if (imageReference.startsWith(ImageStorageManager.LOCAL_URI_PREFIX)) {
            // It's a local image, get the proper URI
            imageStorageManager.getImageUri(imageReference)
        } else if (imageReference.startsWith("http")) {
            // It's a remote URL
            imageReference.toUri()
        } else {
            // Unknown format
            null
        }
    }
    
    /**
     * Gets a fallback URL for when an image can't be loaded
     */
    fun getFallbackImageUrl(size: Int = 100): String {
        return "https://via.placeholder.com/${size}x${size}"
    }
}

/**
 * Composable for loading images with local reference support
 */
@Composable
fun LocalAwareAsyncImage(
    imageReference: String?,
    imageStorageManager: ImageStorageManager,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showLoading: Boolean = true,
    fallbackImageUrl: String? = null
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    
    val imageUri = remember(imageReference) {
        if (imageReference?.startsWith(ImageStorageManager.LOCAL_URI_PREFIX) == true) {
            // It's a local image
            imageStorageManager.getImageUri(imageReference)
        } else if (imageReference?.startsWith("http") == true) {
            // It's a remote URL
            imageReference.toUri()
        } else {
            // Use fallback or null
            fallbackImageUrl?.toUri()
        }
    }

    Box(modifier = Modifier) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUri)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            onState = { state ->
                isLoading = state is AsyncImagePainter.State.Loading
            }
        )

        if (isLoading && showLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ){
                CircularProgressIndicator()
            }

        }
    }
}

