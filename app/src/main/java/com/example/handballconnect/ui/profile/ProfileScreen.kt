package com.example.handballconnect.ui.profile

import android.net.Uri
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.handballconnect.data.storage.ImageStorageManager
import com.example.handballconnect.ui.auth.AuthViewModel
import com.example.handballconnect.util.LocalAwareAsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    imageStorageManager: ImageStorageManager // Inject this
) {
    val userData by authViewModel.userData.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isEditMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Position dropdown
    var isPositionMenuExpanded by remember { mutableStateOf(false) }
    val positions = listOf(
        "Goalkeeper",
        "Left Wing",
        "Left Back",
        "Center Back",
        "Right Back",
        "Right Wing",
        "Pivot",
        "Coach",
        "Other"
    )

    // Experience dropdown
    var isExperienceMenuExpanded by remember { mutableStateOf(false) }
    val experiences = listOf(
        "Beginner",
        "Intermediate",
        "Advanced",
        "Professional",
        "Coach"
    )

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("ProfileScreen", "Profile image selected: $uri")
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri)
                Log.d("ProfileScreen", "Image mime type: $mimeType")

                selectedImageUri = uri
                isLoading = true

                authViewModel.uploadProfileImage(
                    imageUri = uri,
                    onSuccess = { imageUrl ->
                        Log.d("ProfileScreen", "Profile image uploaded successfully: $imageUrl")
                        isLoading = false
                        // Show a success message
                        scope.launch {
                            snackbarHostState.showSnackbar("Profile image updated successfully")
                        }
                    },
                    onError = { error ->
                        Log.e("ProfileScreen", "Failed to upload profile image: $error")
                        isLoading = false
                        // Show error to user
                        scope.launch {
                            snackbarHostState.showSnackbar("Failed to update profile image: $error")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Error handling selected profile image: ${e.message}", e)
                isLoading = false
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to process the selected image")
                }
            }
        }
    }

    // Initialize form values from user data
    LaunchedEffect(userData, isEditMode) {
        userData?.let {
            username = it.username
            position = it.position
            experience = it.experience
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    if (isEditMode) {
                        IconButton(
                            onClick = {
                                authViewModel.updateProfile(username, position, experience)
                                isEditMode = false
                            }
                        ) {
                            Icon(Icons.Default.Done, contentDescription = "Save")
                        }
                    } else {
                        IconButton(
                            onClick = { isEditMode = true }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }

                    IconButton(
                        onClick = { showLogoutDialog = true }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Use LocalAwareAsyncImage for the profile image
                    LocalAwareAsyncImage(
                        imageReference = userData?.profileImageUrl,
                        imageStorageManager = imageStorageManager,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop,
                        fallbackImageUrl = "https://via.placeholder.com/150"
                    )

                    if (isEditMode) {
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.BottomEnd)
                                .clickable { imagePicker.launch("image/*") },
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Camera,
                                    contentDescription = "Change picture",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                // Rest of the profile screen content...
                // (User info card, edit fields, etc.)

                Spacer(modifier = Modifier.height(24.dp))

                // User info card
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (isEditMode) {
                            // Edit mode - show form fields
                            Text(
                                text = "Edit Profile",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Username") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Position dropdown
                            ExposedDropdownMenuBox(
                                expanded = isPositionMenuExpanded,
                                onExpandedChange = { isPositionMenuExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = position,
                                    onValueChange = { position = it },
                                    label = { Text("Position") },
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPositionMenuExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )

                                ExposedDropdownMenu(
                                    expanded = isPositionMenuExpanded,
                                    onDismissRequest = { isPositionMenuExpanded = false }
                                ) {
                                    positions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                position = option
                                                isPositionMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Experience dropdown
                            ExposedDropdownMenuBox(
                                expanded = isExperienceMenuExpanded,
                                onExpandedChange = { isExperienceMenuExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = experience,
                                    onValueChange = { experience = it },
                                    label = { Text("Experience Level") },
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExperienceMenuExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )

                                ExposedDropdownMenu(
                                    expanded = isExperienceMenuExpanded,
                                    onDismissRequest = { isExperienceMenuExpanded = false }
                                ) {
                                    experiences.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                experience = option
                                                isExperienceMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // Display mode - show user info
                            Text(
                                text = userData?.username ?: "",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = userData?.email ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            // Display user details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Position:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = userData?.position?.takeIf { it.isNotEmpty() } ?: "Not specified",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Experience Level:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = userData?.experience?.takeIf { it.isNotEmpty() } ?: "Not specified",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            // Show admin badge if user is admin
                            if (userData?.isAdmin == true) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Admin",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stats/activity card (for future implementation)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Activity Summary",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "0",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Posts",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "0",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Comments",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "0",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Tactics",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Logout confirmation dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout") },
                text = { Text("Are you sure you want to logout?") },
                confirmButton = {
                    Button(
                        onClick = {
                            authViewModel.logoutUser()
                            navController.navigate("login") {
                                popUpTo("main") { inclusive = true }
                            }
                        }
                    ) {
                        Text("Logout")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showLogoutDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
