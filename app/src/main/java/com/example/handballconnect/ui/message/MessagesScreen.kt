package com.example.handballconnect.ui.message

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.handballconnect.data.model.Conversation
import com.example.handballconnect.data.model.User
import com.example.handballconnect.data.storage.ImageStorageManager
import com.example.handballconnect.ui.feed.formatTimestamp
import com.example.handballconnect.util.LocalAwareAsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    navigateToChat: (String) -> Unit,
    messageViewModel: MessageViewModel = hiltViewModel(),
    imageStorageManager: ImageStorageManager
) {
    val conversationsState by messageViewModel.conversationsState.collectAsState()
    val usersState by messageViewModel.usersState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showNewConversationDialog by remember { mutableStateOf(false) }

    // Load conversations when entering screen
    LaunchedEffect(Unit) {
        messageViewModel.loadConversations()
    }

    // Load users when showing new conversation dialog
    LaunchedEffect(showNewConversationDialog) {
        if (showNewConversationDialog) {
            messageViewModel.loadUsers()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Messages")
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewConversationDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Conversation")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Conversations list
            when (conversationsState) {
                is ConversationsState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ConversationsState.Success -> {
                    val conversations =
                        (conversationsState as ConversationsState.Success).conversations
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(conversations) { conversation ->
                            ConversationItem(
                                conversation = conversation,
                                onClick = {
                                    messageViewModel.selectConversation(conversation)
                                    navigateToChat(conversation.conversationId)
                                },
                                imageStorageManager = imageStorageManager
                            )
                            Divider(
                                modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        }
                    }
                }

                is ConversationsState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "No conversations yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tap the + button to start a conversation",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is ConversationsState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (conversationsState as ConversationsState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
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
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                        is UsersState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (usersState as UsersState.Error).message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        is UsersState.Success -> {
                            val users = (usersState as UsersState.Success).users
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
                                            scope.launch {
                                                try {
                                                    messageViewModel.startConversation(user.userId)
                                                    val conversation =
                                                        messageViewModel.selectedConversation.value
                                                    if (conversation != null) {
                                                        showNewConversationDialog = false
                                                        navigateToChat(conversation.conversationId)
                                                    }
                                                } catch (e: Exception) {
                                                    snackbarHostState.showSnackbar("Failed to start conversation: ${e.message}")
                                                }
                                            }
                                        },
                                        imageStorageManager = imageStorageManager
                                    )
                                }
                            }
                        }

                        is UsersState.Empty -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No users found")
                            }
                        }

                        is UsersState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (usersState as UsersState.Error).message,
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

        @Composable
        fun ConversationItem(
            conversation: Conversation,
            onClick: () -> Unit,
            imageStorageManager: ImageStorageManager
        ) {
            val currentUserId = conversation.participantIds.getOrNull(0) ?: ""
            val otherUserId = conversation.participantIds.firstOrNull { it != currentUserId } ?: ""

            val otherUserName = conversation.participantNames[otherUserId] ?: "Unknown User"
            val otherUserImage = conversation.participantImages[otherUserId] ?: ""
            val unreadCount = conversation.unreadCount[currentUserId] ?: 0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile image
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (otherUserImage.isNotEmpty()) {
                        LocalAwareAsyncImage(
                            imageReference = otherUserImage,
                            imageStorageManager = imageStorageManager,
                            contentDescription = otherUserName,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentScale = ContentScale.Crop,
                            fallbackImageUrl = null
                        )
                    } else {
                        Text(
                            text = otherUserName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
                            text = otherUserName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
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
                        if (unreadCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(unreadCount.toString())
                                    }
                                }
                            ) {}
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.profileImageUrl.isNotEmpty()) {
                        LocalAwareAsyncImage(
                            imageReference = user.profileImageUrl,
                            imageStorageManager = imageStorageManager,
                            contentDescription = user.username,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            fallbackImageUrl = null
                        )
                    } else {
                        Text(
                            text = user.username.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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