package com.example.handballconnect.ui.message

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.handballconnect.data.model.Conversation
import com.example.handballconnect.data.model.Message
import com.example.handballconnect.data.model.User
import com.example.handballconnect.data.storage.ImageStorageManager
import com.example.handballconnect.util.LocalAwareAsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    messageViewModel: MessageViewModel,
    imageStorageManager: ImageStorageManager
) {
    val conversationsState by messageViewModel.conversationsState.collectAsState()
    val selectedConversation by messageViewModel.selectedConversation.collectAsState()
    val messagesState by messageViewModel.messagesState.collectAsState()
    val messageSendState by messageViewModel.messageSendState.collectAsState()
    val usersState by messageViewModel.usersState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showChatView by remember { mutableStateOf(false) }
    var showNewConversationDialog by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            messageViewModel.sendImageMessage(uri)
        }
    }

    // Load users when showing new conversation dialog
    LaunchedEffect(showNewConversationDialog) {
        if (showNewConversationDialog) {
            messageViewModel.loadUsers()
        }
    }

    // Handle message send state changes
    LaunchedEffect(messageSendState) {
        when (messageSendState) {
            is MessageViewModel.MessageSendState.Success -> {
                messageText = ""
                selectedImageUri = null
                messageViewModel.resetMessageSendState()
            }

            is MessageViewModel.MessageSendState.Error -> {
                val errorMessage =
                    (messageSendState as MessageViewModel.MessageSendState.Error).message
                scope.launch {
                    snackbarHostState.showSnackbar(errorMessage)
                }
                messageViewModel.resetMessageSendState()
            }

            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showChatView && selectedConversation != null) {
                        Text(messageViewModel.getOtherParticipantName())
                    } else {
                        Text("Messages")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showChatView = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showChatView) {
                FloatingActionButton(onClick = { showNewConversationDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New Conversation")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showChatView && selectedConversation != null) {
                // Chat view
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Messages list
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (messagesState) {
                            is MessageViewModel.MessagesState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            is MessageViewModel.MessagesState.Success -> {
                                val messages =
                                    (messagesState as MessageViewModel.MessagesState.Success).messages
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    reverseLayout = false
                                ) {
                                    items(messages) { message ->
                                        MessageItem(message = message)
                                    }
                                }
                            }

                            is MessageViewModel.MessagesState.Empty -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No messages yet. Start the conversation!")
                                }
                            }

                            is MessageViewModel.MessagesState.Error -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (messagesState as MessageViewModel.MessagesState.Error).message,
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
                                Icon(Icons.Default.Image, contentDescription = "Add Image")
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
                                        messageViewModel.sendTextMessage(messageText)
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            } else {
                // Conversations list
                when (conversationsState) {
                    is MessageViewModel.ConversationsState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is MessageViewModel.ConversationsState.Success -> {
                        val conversations =
                            (conversationsState as MessageViewModel.ConversationsState.Success).conversations
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = paddingValues
                        ) {
                            items(conversations) { conversation ->
                                ConversationItem(
                                    conversation = conversation,
                                    onClick = {
                                        messageViewModel.selectConversation(conversation)
                                        showChatView = true
                                    }
                                )
                            }
                        }
                    }

                    is MessageViewModel.ConversationsState.Empty -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No conversations yet. Start a new conversation!")
                        }
                    }

                    is MessageViewModel.ConversationsState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (conversationsState as MessageViewModel.ConversationsState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    // New Conversation Dialog
    if (showNewConversationDialog) {
        Dialog(onDismissRequest = { showNewConversationDialog = false }) {
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
                    Text(
                        text = "New Conversation",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when (usersState) {
                        is MessageViewModel.UsersState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is MessageViewModel.UsersState.Success -> {
                            val users = (usersState as MessageViewModel.UsersState.Success).users
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(users) { user ->
                                    UserItem(
                                        user = user,
                                        onClick = {
                                            messageViewModel.startConversation(
                                                otherUserId = user.userId,
//                                                userId = user.userId,
//                                                onSuccess = { conversation ->
//                                                    messageViewModel.selectConversation(conversation)
//                                                    showNewConversationDialog = false
//                                                    showChatView = true
//                                                },
//                                                onError = { error ->
//                                                    scope.launch {
//                                                        snackbarHostState.showSnackbar(error)
//                                                    }
//                                                }
                                            )
                                        },
                                        imageStorageManager = imageStorageManager
                                    )
                                }
                            }
                        }

                        is MessageViewModel.UsersState.Empty -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No users found")
                            }
                        }

                        is MessageViewModel.UsersState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (usersState as MessageViewModel.UsersState.Error).message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Other user's profile image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(
                        conversation.participantImages.values.firstOrNull()
                            ?: "https://via.placeholder.com/40"
                    )
                    .crossfade(true)
                    .build(),
                contentDescription = "User profile",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.participantNames.values.firstOrNull() ?: "Unknown User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = formatTimestamp(conversation.lastMessageTimestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage.takeIf { it.isNotEmpty() }
                            ?: "No messages yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Unread message badge
                    conversation.unreadCount.values.firstOrNull()?.let { unreadCount ->
                        if (unreadCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge { Text(unreadCount.toString()) }
                                }
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserItem(
    user: User,
    onClick: () -> Unit,
    imageStorageManager: ImageStorageManager
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User profile image
        LocalAwareAsyncImage(
            imageReference = user.profileImageUrl,
            imageStorageManager = imageStorageManager,
            contentDescription = user.username,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentScale = ContentScale.Crop,
            fallbackImageUrl = "https://via.placeholder.com/40"
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = user.username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (user.position.isNotEmpty()) {
                Text(
                    text = user.position,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Message bubble
        Surface(
            color = if (message.senderId == "currentUserId")
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(
                    if (message.senderId == "currentUserId") Alignment.End else Alignment.Start
                )
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Image if present
                message.imageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Message image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(8.dp))
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
                    text = formatTimeOnly(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.senderId == "currentUserId")
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

// Helper function for timestamp formatting (date and time)
fun formatTimestamp(timestamp: Long): String {
    val now = Date()
    val messageDate = Date(timestamp)
    val diffInMillis = now.time - messageDate.time
    val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

    return when {
        diffInDays < 1 -> {
            // Today - show time only
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            formatter.format(messageDate)
        }

        diffInDays < 2 -> {
            // Yesterday
            "Yesterday"
        }

        diffInDays < 7 -> {
            // Within a week - show day name
            val formatter = SimpleDateFormat("EEEE", Locale.getDefault())
            formatter.format(messageDate)
        }

        else -> {
            // Older - show date
            val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
            formatter.format(messageDate)
        }
    }
}

// Helper function for time-only formatting
fun formatTimeOnly(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}