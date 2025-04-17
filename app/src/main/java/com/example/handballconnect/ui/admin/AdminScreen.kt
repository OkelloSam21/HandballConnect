package com.example.handballconnect.ui.admin

import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.handballconnect.data.model.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    adminViewModel: AdminViewModel
) {
    val usersState by adminViewModel.usersState.collectAsState()
    val adminOperationState by adminViewModel.adminOperationState.collectAsState()
    val announcementState by adminViewModel.announcementState.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var announcementText by remember { mutableStateOf("") }
    
    // Handle admin operation state changes
    LaunchedEffect(adminOperationState) {
        when (adminOperationState) {
            is AdminViewModel.AdminOperationState.Success -> {
                snackbarHostState.showSnackbar(
                    (adminOperationState as AdminViewModel.AdminOperationState.Success).message
                )
                adminViewModel.resetAdminOperationState()
            }
            is AdminViewModel.AdminOperationState.Error -> {
                snackbarHostState.showSnackbar(
                    (adminOperationState as AdminViewModel.AdminOperationState.Error).message
                )
                adminViewModel.resetAdminOperationState()
            }
            else -> {}
        }
    }
    
    // Handle announcement state changes
    LaunchedEffect(announcementState) {
        when (announcementState) {
            is AdminViewModel.AnnouncementState.Success -> {
                showAnnouncementDialog = false
                announcementText = ""
                snackbarHostState.showSnackbar(
                    (announcementState as AdminViewModel.AnnouncementState.Success).message
                )
                adminViewModel.resetAnnouncementState()
            }
            is AdminViewModel.AnnouncementState.Error -> {
                snackbarHostState.showSnackbar(
                    (announcementState as AdminViewModel.AnnouncementState.Error).message
                )
                adminViewModel.resetAnnouncementState()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    IconButton(onClick = { showAnnouncementDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Announcement")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("User Management") }
                    )
                    
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Statistics") }
                    )
                }
                
                when (selectedTabIndex) {
                    0 -> {
                        // User management tab
                        when (usersState) {
                            is AdminViewModel.UsersState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is AdminViewModel.UsersState.Success -> {
                                val users = (usersState as AdminViewModel.UsersState.Success).users
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = paddingValues
                                ) {
                                    items(users) { user ->
                                        UserManagementItem(
                                            user = user,
                                            onToggleAdmin = { isAdmin ->
                                                adminViewModel.updateUserAdminStatus(user.userId, isAdmin)
                                            }
                                        )
                                    }
                                }
                            }
                            is AdminViewModel.UsersState.Empty -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No users found")
                                }
                            }
                            is AdminViewModel.UsersState.Error -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (usersState as AdminViewModel.UsersState.Error).message,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Statistics tab (stub for future implementation)
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Statistics will be available in future updates")
                        }
                    }
                }
            }
        }
    }
    
    // Create announcement dialog
    if (showAnnouncementDialog) {
        Dialog(onDismissRequest = { showAnnouncementDialog = false }) {
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
                            text = "Create Announcement",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        IconButton(onClick = { showAnnouncementDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = announcementText,
                        onValueChange = { announcementText = it },
                        label = { Text("Announcement Text") },
                        placeholder = { Text("Enter announcement text...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if (announcementText.isNotBlank()) {
                                adminViewModel.createAnnouncement(announcementText)
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Announcement text cannot be empty")
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        enabled = announcementState !is AdminViewModel.AnnouncementState.Loading
                    ) {
                        if (announcementState is AdminViewModel.AnnouncementState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Post Announcement")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserManagementItem(
    user: User,
    onToggleAdmin: (Boolean) -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User profile image
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.profileImageUrl.takeIf { it.isNotEmpty() } ?: "https://via.placeholder.com/50")
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
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (user.position.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Position: ${user.position}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // Admin toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin",
                        tint = if (user.isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Switch(
                        checked = user.isAdmin,
                        onCheckedChange = { isAdmin ->
                            onToggleAdmin(isAdmin)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Additional user info
            if (user.experience.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Experience Level:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = user.experience,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Admin status badge
            if (user.isAdmin) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Admin",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}