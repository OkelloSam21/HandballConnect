package com.example.handballconnect.ui.message

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.handballconnect.data.model.Message
import com.example.handballconnect.data.storage.ImageStorageManager
import com.example.handballconnect.ui.feed.formatTimestamp
import com.example.handballconnect.util.LocalAwareAsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    navigateBack: () -> Unit,
    imageStorageManager: ImageStorageManager,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val messagesState by chatViewModel.messagesState.collectAsState()
    val chatState by chatViewModel.chatState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val listState = rememberLazyListState()

    // Load messages when entering the screen
    LaunchedEffect(conversationId) {
        chatViewModel.loadMessages(conversationId)
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messagesState) {
        if (messagesState is MessagesState.Success) {
            val messages = (messagesState as MessagesState.Success).messages
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // Handle chat state changes
    LaunchedEffect(chatState) {
        when (chatState) {
            is ChatState.Success -> {
                messageText = ""
                selectedImageUri = null
            }
            is ChatState.Error -> {
                val errorMessage = (chatState as ChatState.Error).message
                scope.launch {
                    snackbarHostState.showSnackbar(errorMessage)
                }
            }
            else -> {}
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            chatViewModel.sendImageMessage(uri, conversationId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Use profile pic or default icon
                            Icon(Icons.Filled.Person, contentDescription = "Contact")
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = chatViewModel.getOtherParticipantName(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Messages list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (messagesState) {
                    is MessagesState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is MessagesState.Success -> {
                        val messages = (messagesState as MessagesState.Success).messages
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(
                                top = 8.dp,
                                bottom = 8.dp
                            )
                        ) {
                            items(messages) { message ->
                                MessageItem(
                                    message = message,
                                    currentUserId = chatViewModel.getCurrentUserId() ?: "",
                                    imageStorageManager = imageStorageManager
                                )
                            }
                        }
                    }

                    is MessagesState.Empty -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No messages yet. Start the conversation!")
                        }
                    }

                    is MessagesState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (messagesState as MessagesState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    else -> {}
                }
            }

            // Message input
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Add Image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                chatViewModel.sendTextMessage(messageText, conversationId)
                            }
                        },
                        enabled = messageText.isNotBlank() && chatState !is ChatState.Sending
                    ) {
                        if (chatState is ChatState.Sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    currentUserId: String,
    imageStorageManager: ImageStorageManager
) {
    val isCurrentUser = message.senderId == currentUserId
    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Message bubble
        Card(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isCurrentUser) 12.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 12.dp
            ),
            modifier = Modifier
                .align(alignment)
                .padding(horizontal = 8.dp)
                .padding(bottom = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Image if present
                message.imageUrl?.let { imageUrl ->
                    if (imageUrl.isNotEmpty()) {
                        LocalAwareAsyncImage(
                            imageReference = imageUrl,
                            imageStorageManager = imageStorageManager,
                            contentDescription = "Message image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            fallbackImageUrl = null
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Text content
                if (message.text.isNotEmpty() && message.text != "[Image]") {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

//// Helper function for time-only formatting
//fun formatTimeOnly(timestamp: Long): String {
//    val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
//    return formatter.format(java.util.Date(timestamp))
//}