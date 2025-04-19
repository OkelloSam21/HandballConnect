package com.example.handballconnect.ui.feed

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.handballconnect.data.model.Comment
import com.example.handballconnect.data.model.Post
import com.example.handballconnect.data.storage.ImageStorageManager
import com.example.handballconnect.util.LocalAwareAsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    feedViewModel: FeedViewModel,
    imageStorageManager: ImageStorageManager
) {
    val feedState by feedViewModel.feedState.collectAsState()
    val selectedPost by feedViewModel.selectedPost.collectAsState()
    val commentsState by feedViewModel.commentsState.collectAsState()
    val postCreationState by feedViewModel.postCreationState.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val refreshState = rememberPullToRefreshState()

    // State for new post creation
    var showNewPostDialog by remember { mutableStateOf(false) }
    var postText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // State for comments bottom sheet
    var showCommentsSheet by remember { mutableStateOf(false) }
    val commentSheetState = rememberModalBottomSheetState()
    var commentText by remember { mutableStateOf("") }

    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            Log.d("FeedScreen", "Image selected: $uri")
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri)
                Log.d("FeedScreen", "Image mime type: $mimeType")

                // Store the uri for later use
                selectedImageUri = uri
            } catch (e: Exception) {
                Log.e("FeedScreen", "Error handling selected image: ${e.message}", e)
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to process the selected image")
                }
            }
        } else {
            Log.d("FeedScreen", "No image selected")
        }
    }

    // Handle post creation state changes
    LaunchedEffect(postCreationState) {
        when (postCreationState) {
            is FeedViewModel.PostCreationState.Success -> {
                snackbarHostState.showSnackbar("Post created successfully")
                feedViewModel.resetPostCreationState()
                postText = ""
                selectedImageUri = null
                showNewPostDialog = false
            }

            is FeedViewModel.PostCreationState.Error -> {
                snackbarHostState.showSnackbar(
                    (postCreationState as FeedViewModel.PostCreationState.Error).message
                )
                feedViewModel.resetPostCreationState()
            }

            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Handball Feed") }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewPostDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add Post") },
                text = { Text("New Post") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = feedState is FeedViewModel.FeedState.Loading,
            onRefresh = {
                feedViewModel.loadFeedPosts()
            },
            indicator = {
                Indicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = feedState is FeedViewModel.FeedState.Loading,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    state = refreshState
                )
            },
            state = refreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (feedState) {
                is FeedViewModel.FeedState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is FeedViewModel.FeedState.Success -> {
                    val posts = (feedState as FeedViewModel.FeedState.Success).posts
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = paddingValues
                    ) {
                        items(posts) { post ->
                            PostItem(
                                post = post,
                                onLikeClick = { feedViewModel.toggleLike(post.postId) },
                                onCommentClick = {
                                    feedViewModel.selectPostForComments(post)
                                    showCommentsSheet = true
                                },
                                onDeleteClick = {
                                    feedViewModel.deletePost(
                                        post.postId,
                                        onSuccess = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Post deleted")
                                            }
                                        },
                                        onError = { error ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(error)
                                            }
                                        }
                                    )
                                },
                                imageStorageManager = imageStorageManager
                            )
                        }
                    }
                }

                is FeedViewModel.FeedState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No posts yet. Be the first to post!")
                    }
                }

                is FeedViewModel.FeedState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (feedState as FeedViewModel.FeedState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // New Post Dialog
    if (showNewPostDialog) {
        Dialog(
            onDismissRequest = { showNewPostDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Create Post",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = { showNewPostDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = postText,
                        onValueChange = { postText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("What's on your mind?") },
                        minLines = 3,
                        maxLines = 6
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Image preview
                    selectedImageUri?.let { uri ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove image",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { imagePicker.launch("image/*") }
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Add Image")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Image")
                        }

                        Button(
                            onClick = {
                                if (postText.isNotBlank()) {
                                    feedViewModel.createPost(postText, selectedImageUri)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Post text cannot be empty")
                                    }
                                }
                            },
                            enabled = postCreationState !is FeedViewModel.PostCreationState.Loading
                        ) {
                            if (postCreationState is FeedViewModel.PostCreationState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Post")
                            }
                        }
                    }
                }
            }
        }
    }

    // Comments Bottom Sheet
    if (showCommentsSheet && selectedPost != null) {
        ModalBottomSheet(
            onDismissRequest = { showCommentsSheet = false },
            sheetState = commentSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Comments",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (commentsState) {
                    is FeedViewModel.CommentsState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is FeedViewModel.CommentsState.Success -> {
                        val comments =
                            (commentsState as FeedViewModel.CommentsState.Success).comments
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(comments) { comment ->
                                CommentItem(comment = comment)
                            }
                        }
                    }

                    is FeedViewModel.CommentsState.Empty -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No comments yet. Be the first to comment!")
                        }
                    }

                    is FeedViewModel.CommentsState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (commentsState as FeedViewModel.CommentsState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    else -> {}
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add a comment...") },
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank() && selectedPost != null) {
                                feedViewModel.addComment(
                                    selectedPost!!.postId, commentText,
                                    onSuccess = {
                                        commentText = ""
                                    },
                                    onError = { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(error)
                                        }
                                    }
                                )
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Comment")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onDeleteClick: () -> Unit,
    imageStorageManager: ImageStorageManager
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header - User info and menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // User profile image
                    LocalAwareAsyncImage(
                        imageReference = post.userProfileImageUrl,
                        imageStorageManager = imageStorageManager,
                        contentDescription = "User profile",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop,
                        fallbackImageUrl = "https://via.placeholder.com/40"
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = post.username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = formatTimestamp(post.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete"
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            // Post announcement tag if applicable
            if (post.isAnnouncement) {
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "Announcement",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post content
            Text(
                text = post.text,
                style = MaterialTheme.typography.bodyLarge
            )

            // Post image if available
            if (!post.imageUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                LocalAwareAsyncImage(
                    imageReference = post.imageUrl,
                    imageStorageManager = imageStorageManager,
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Like and comment counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${post.likeCount} likes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${post.commentCount} comments",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Like button
                Row(
                    modifier = Modifier
                        .clickable { onLikeClick() }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = "Like",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Like",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Comment button
                Row(
                    modifier = Modifier
                        .clickable { onCommentClick() }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = "Comment",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Comment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // User profile image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(comment.userProfileImageUrl.takeIf { it.isNotEmpty() }
                    ?: "https://via.placeholder.com/32")
                .crossfade(true)
                .build(),
            contentDescription = "User profile",
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatTimestamp(comment.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Helper function to format timestamps
fun formatTimestamp(timestamp: Long): String {
    val currentTime = Date().time
    val timeDifference = currentTime - timestamp

    return when {
        timeDifference < 1000 * 60 -> "Just now"
        timeDifference < 1000 * 60 * 60 -> "${timeDifference / (1000 * 60)} minutes ago"
        timeDifference < 1000 * 60 * 60 * 24 -> "${timeDifference / (1000 * 60 * 60)} hours ago"
        timeDifference < 1000 * 60 * 60 * 24 * 7 -> "${timeDifference / (1000 * 60 * 60 * 24)} days ago"
        else -> {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}