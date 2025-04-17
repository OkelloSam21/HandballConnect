package com.example.handballconnect.ui.tatctic

import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.handballconnect.data.model.MovementArrow
import com.example.handballconnect.data.model.PlayerPosition
import com.example.handballconnect.data.model.Tactics
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TacticsScreen(
    tacticsViewModel: TacticsViewModel
) {
    val userTacticsState by tacticsViewModel.userTacticsState.collectAsState()
    val sharedTacticsState by tacticsViewModel.sharedTacticsState.collectAsState()
    val selectedTactics by tacticsViewModel.selectedTactics.collectAsState()
    val saveTacticsState by tacticsViewModel.saveTacticsState.collectAsState()
    val playerPositions by tacticsViewModel.playerPositions.collectAsState()
    val movementArrows by tacticsViewModel.movementArrows.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showTacticsEditor by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var tacticsTitle by remember { mutableStateOf("") }
    var tacticsDescription by remember { mutableStateOf("") }
    var isShared by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }

    // For creating new movement arrows
    var isDrawingArrow by remember { mutableStateOf(false) }
    var arrowStart by remember { mutableStateOf(Offset.Zero) }
    var arrowEnd by remember { mutableStateOf(Offset.Zero) }
    var currentlyDraggedPlayerId by remember { mutableIntStateOf(-1) }

    // Reset tactics editor state when opening it
    LaunchedEffect(showTacticsEditor) {
        if (showTacticsEditor) {
            if (selectedTactics == null) {
                tacticsViewModel.createNewTactics()
                tacticsTitle = ""
                tacticsDescription = ""
                isShared = false
            } else {
                tacticsTitle = selectedTactics!!.title
                tacticsDescription = selectedTactics!!.description
                isShared = selectedTactics!!.isShared
            }
        }
    }

    // Handle save tactics state changes
    LaunchedEffect(saveTacticsState) {
        when (saveTacticsState) {
            is TacticsViewModel.SaveTacticsState.Success -> {
                showSaveDialog = false
                showTacticsEditor = false
                snackbarHostState.showSnackbar("Tactics saved successfully")
                tacticsViewModel.resetSaveTacticsState()
            }
            is TacticsViewModel.SaveTacticsState.Error -> {
                val errorMessage = (saveTacticsState as TacticsViewModel.SaveTacticsState.Error).message
                snackbarHostState.showSnackbar(errorMessage)
                tacticsViewModel.resetSaveTacticsState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar =  {
            TopAppBar(
                title = {
                    if (showTacticsEditor) {
                        Text("Tactics Board Editor")
                    } else {
                        Text("Tactics Board")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showTacticsEditor = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (showTacticsEditor) {
                        // Show template button and save button in editor
                        IconButton(onClick = { showTemplateDialog = true }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Apply Template")
                        }

                        IconButton(onClick = { showSaveDialog = true }) {
                            Icon(Icons.Default.Save, contentDescription = "Save Tactics")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showTacticsEditor) {
                ExtendedFloatingActionButton(
                    onClick = {
                        tacticsViewModel.createNewTactics()
                        showTacticsEditor = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "New Tactics") },
                    text = { Text("New Tactics") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showTacticsEditor) {
                // Tactics editor view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Tactics board canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(16.dp)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .pointerInteropFilter { event ->
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        // Start drawing an arrow or check if we're clicking on a player
                                        val touchPoint = Offset(event.x, event.y)
                                        val clickedPlayer = playerPositions.firstOrNull { player ->
                                            val playerOffset = Offset(
                                                player.x * event.x,
                                                player.y * event.y
                                            )
                                            val distance = (playerOffset - touchPoint).getDistance()
                                            distance < 40f // Approximate player circle radius
                                        }

                                        if (clickedPlayer != null) {
                                            // We clicked on a player
                                            currentlyDraggedPlayerId = clickedPlayer.id
                                        } else {
                                            // Start drawing an arrow
                                            isDrawingArrow = true
                                            arrowStart = Offset(event.x, event.y)
                                            arrowEnd = arrowStart
                                        }
                                        true
                                    }

                                    MotionEvent.ACTION_MOVE -> {
                                        if (isDrawingArrow) {
                                            // Update arrow end position
                                            arrowEnd = Offset(event.x, event.y)
                                        } else if (currentlyDraggedPlayerId != -1) {
                                            // Move the player
                                            val player =
                                                playerPositions.firstOrNull { it.id == currentlyDraggedPlayerId }
                                            player?.let {
                                                val canvasWidth = event.x
                                                val canvasHeight = event.y
                                                val updatedPlayer = it.copy(
                                                    x = event.x / canvasWidth,
                                                    y = event.y / canvasHeight
                                                )
                                                tacticsViewModel.updatePlayerPosition(updatedPlayer)
                                            }
                                        }
                                        true
                                    }

                                    MotionEvent.ACTION_UP -> {
                                        if (isDrawingArrow && arrowStart != arrowEnd) {
                                            // Create a new arrow
                                            val newArrow = MovementArrow(
                                                id = movementArrows.size + 1,
                                                startX = arrowStart.x,
                                                startY = arrowStart.y,
                                                endX = arrowEnd.x,
                                                endY = arrowEnd.y,
                                                playerId = currentlyDraggedPlayerId.takeIf { it != -1 },
                                                isPass = false
                                            )
                                            tacticsViewModel.addMovementArrow(newArrow)
                                        }

                                        // Reset states
                                        isDrawingArrow = false
                                        currentlyDraggedPlayerId = -1
                                        true
                                    }

                                    else -> false
                                }
                            }
                    ) {
                        TacticsBoard(
                            playerPositions = playerPositions,
                            movementArrows = movementArrows,
                            currentArrow = if (isDrawingArrow) Pair(arrowStart, arrowEnd) else null,
                            onDeletePlayer = { playerId ->
                                tacticsViewModel.removePlayer(playerId)
                            },
                            onDeleteArrow = { arrowId ->
                                tacticsViewModel.removeMovementArrow(arrowId)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Player controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Add offense player
                        Button(
                            onClick = {
                                val newPlayerId = (playerPositions.maxOfOrNull { it.id } ?: 0) + 1
                                val newPlayer = PlayerPosition(
                                    id = newPlayerId,
                                    x = 0.3f,
                                    y = 0.5f,
                                    number = newPlayerId,
                                    isOffense = true,
                                    label = "O${newPlayerId}"
                                )
                                tacticsViewModel.updatePlayerPosition(newPlayer)
                            }
                        ) {
                            Text("Add Offense")
                        }

                        // Add defense player
                        Button(
                            onClick = {
                                val newPlayerId = (playerPositions.maxOfOrNull { it.id } ?: 0) + 1
                                val newPlayer = PlayerPosition(
                                    id = newPlayerId,
                                    x = 0.7f,
                                    y = 0.5f,
                                    number = newPlayerId,
                                    isOffense = false,
                                    label = "D${newPlayerId}"
                                )
                                tacticsViewModel.updatePlayerPosition(newPlayer)
                            }
                        ) {
                            Text("Add Defense")
                        }

                        // Clear arrows
                        Button(
                            onClick = {
                                // Remove all arrows
                                movementArrows.forEach { arrow ->
                                    tacticsViewModel.removeMovementArrow(arrow.id)
                                }
                            }
                        ) {
                            Text("Clear Arrows")
                        }
                    }
                }
            } else {
                // Tactics list view
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("My Tactics") }
                        )

                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("Shared Tactics") }
                        )
                    }

                    when (selectedTabIndex) {
                        0 -> {
                            // My tactics tab
                            when (userTacticsState) {
                                is TacticsViewModel.TacticsListState.Loading -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                                is TacticsViewModel.TacticsListState.Success -> {
                                    val tacticsList = (userTacticsState as TacticsViewModel.TacticsListState.Success).tacticsList
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = paddingValues
                                    ) {
                                        items(tacticsList) { tactics ->
                                            TacticsItem(
                                                tactics = tactics,
                                                onEditClick = {
                                                    tacticsViewModel.selectTactics(tactics)
                                                    showTacticsEditor = true
                                                },
                                                onShareClick = {
                                                    tacticsViewModel.updateSharingStatus(
                                                        tacticsId = tactics.tacticsId,
                                                        isShared = !tactics.isShared,
                                                        onSuccess = {
                                                            scope.launch {
                                                                val message = if (tactics.isShared)
                                                                    "Tactics no longer shared"
                                                                else
                                                                    "Tactics shared successfully"
                                                                snackbarHostState.showSnackbar(message)
                                                            }
                                                        },
                                                        onError = { error ->
                                                            scope.launch {
                                                                snackbarHostState.showSnackbar(error)
                                                            }
                                                        }
                                                    )
                                                },
                                                onDeleteClick = {
                                                    tacticsViewModel.deleteTactics(
                                                        tacticsId = tactics.tacticsId,
                                                        onSuccess = {
                                                            scope.launch {
                                                                snackbarHostState.showSnackbar("Tactics deleted successfully")
                                                            }
                                                        },
                                                        onError = { error ->
                                                            scope.launch {
                                                                snackbarHostState.showSnackbar(error)
                                                            }
                                                        }
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                                is TacticsViewModel.TacticsListState.Empty -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("You haven't created any tactics yet")
                                    }
                                }
                                is TacticsViewModel.TacticsListState.Error -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (userTacticsState as TacticsViewModel.TacticsListState.Error).message,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            // Shared tactics tab
                            when (sharedTacticsState) {
                                is TacticsViewModel.TacticsListState.Loading -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                                is TacticsViewModel.TacticsListState.Success -> {
                                    val tacticsList = (sharedTacticsState as TacticsViewModel.TacticsListState.Success).tacticsList
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = paddingValues
                                    ) {
                                        items(tacticsList) { tactics ->
                                            TacticsItem(
                                                tactics = tactics,
                                                onEditClick = {
                                                    tacticsViewModel.selectTactics(tactics)
                                                    showTacticsEditor = true
                                                },
                                                isViewer = true
                                            )
                                        }
                                    }
                                }
                                is TacticsViewModel.TacticsListState.Empty -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No shared tactics available")
                                    }
                                }
                                is TacticsViewModel.TacticsListState.Error -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (sharedTacticsState as TacticsViewModel.TacticsListState.Error).message,
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
    }

    // Save tactics dialog
    if (showSaveDialog) {
        Dialog(onDismissRequest = { showSaveDialog = false }) {
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
                        text = "Save Tactics Board",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = tacticsTitle,
                        onValueChange = { tacticsTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = tacticsDescription,
                        onValueChange = { tacticsDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Share with others")
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { isShared = !isShared }) {
                            Icon(
                                imageVector = if (isShared) Icons.Default.CheckCircle else Icons.Default.Share,
                                contentDescription = "Share tactics",
                                tint = if (isShared) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showSaveDialog = false },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (tacticsTitle.isNotBlank()) {
                                    // Create a bitmap from the tactics board
                                    // In a real app, you'd capture the actual board view
                                    val boardImage: Bitmap? = null

                                    tacticsViewModel.saveTactics(
                                        title = tacticsTitle,
                                        description = tacticsDescription,
                                        boardImage = boardImage,
                                        isShared = isShared
                                    )
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please enter a title")
                                    }
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // Template dialog
    if (showTemplateDialog) {
        Dialog(onDismissRequest = { showTemplateDialog = false }) {
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
                        text = "Apply Template",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            TemplateItem(
                                name = "6-0",
                                description = "Standard 6-0 defense formation",
                                onClick = {
                                    tacticsViewModel.applyTemplate("6-0")
                                    showTemplateDialog = false
                                }
                            )
                        }

                        item {
                            TemplateItem(
                                name = "5-1",
                                description = "5-1 defense formation",
                                onClick = {
                                    tacticsViewModel.applyTemplate("5-1")
                                    showTemplateDialog = false
                                }
                            )
                        }

                        item {
                            TemplateItem(
                                name = "3-2-1",
                                description = "3-2-1 defense formation",
                                onClick = {
                                    tacticsViewModel.applyTemplate("3-2-1")
                                    showTemplateDialog = false
                                }
                            )
                        }

                        item {
                            TemplateItem(
                                name = "Fast Break",
                                description = "Fast break offensive setup",
                                onClick = {
                                    tacticsViewModel.applyTemplate("fastbreak")
                                    showTemplateDialog = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showTemplateDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun TacticsBoard(
    playerPositions: List<PlayerPosition>,
    movementArrows: List<MovementArrow>,
    currentArrow: Pair<Offset, Offset>? = null,
    onDeletePlayer: (Int) -> Unit,
    onDeleteArrow: (Int) -> Unit
) {
    // Court colors
    val courtColor = Color(0xFF1A78C2)
    val courtLineColor = Color.White
    val goalAreaColor = Color(0xFFD32F2F).copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height

            // Draw court background
            drawRect(
                color = courtColor,
                size = size
            )

            // Draw centerline
            drawLine(
                color = courtLineColor,
                start = Offset(width / 2, 0f),
                end = Offset(width / 2, height),
                strokeWidth = 5f
            )

            // Draw goal areas (simplified)
            // Left goal area
            drawRect(
                color = goalAreaColor,
                topLeft = Offset(0f, height * 0.25f),
                size = size.copy(width = width * 0.15f, height = height * 0.5f)
            )

            // Right goal area
            drawRect(
                color = goalAreaColor,
                topLeft = Offset(width * 0.85f, height * 0.25f),
                size = size.copy(width = width * 0.15f, height = height * 0.5f)
            )

            // Draw movement arrows
            movementArrows.forEach { arrow ->
                drawArrow(
                    start = Offset(arrow.startX, arrow.startY),
                    end = Offset(arrow.endX, arrow.endY),
                    color = if (arrow.isPass) Color.Yellow else Color.Red,
                    strokeWidth = 5f
                )
            }

            // Draw current arrow being created
            currentArrow?.let { (start, end) ->
                if (start != end) {
                    drawArrow(
                        start = start,
                        end = end,
                        color = Color.Green,
                        strokeWidth = 5f
                    )
                }
            }
        }

        // Draw players on top of the court
        playerPositions.forEach { player ->
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    // Position the player according to relative coordinates
                    .padding(
                        start = (player.x * (LocalContext.current.resources.displayMetrics.widthPixels * 0.8f)).dp,
                        top = (player.y * (LocalContext.current.resources.displayMetrics.heightPixels * 0.5f)).dp
                    )
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (player.isOffense)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = player.number.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                // Delete button for player
                IconButton(
                    onClick = { onDeletePlayer(player.id) },
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete player",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Player label
                if (player.label.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(top = 52.dp)
                    ) {
                        Text(
                            text = player.label,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TacticsItem(
    tactics: Tactics,
    onEditClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    isViewer: Boolean = false
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tactics.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (tactics.isShared) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Shared",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tactics.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Created: ${formatDate(tactics.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Display thumbnail if available
            tactics.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Tactics thumbnail",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isViewer) {
                    // Share button
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = if (tactics.isShared) "Unshare" else "Share",
                            tint = if (tactics.isShared) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Delete button
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }

                // Edit/View button
                Button(onClick = onEditClick) {
                    Icon(
                        imageVector = if (isViewer) Icons.Default.ArrowForward else Icons.Default.Edit,
                        contentDescription = if (isViewer) "View" else "Edit"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isViewer) "View" else "Edit")
                }
            }
        }
    }
}

@Composable
fun TemplateItem(
    name: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = name,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Helper function to draw an arrow on canvas
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    // Draw line
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // Calculate arrowhead
    val dx = end.x - start.x
    val dy = end.y - start.y
    val angle = atan2(dy, dx)

    val arrowLength = 20f
    val arrowAngle = Math.PI / 6 // 30 degrees

    val x1 = end.x - arrowLength * cos(angle - arrowAngle).toFloat()
    val y1 = end.y - arrowLength * sin(angle - arrowAngle).toFloat()
    val x2 = end.x - arrowLength * cos(angle + arrowAngle).toFloat()
    val y2 = end.y - arrowLength * sin(angle + arrowAngle).toFloat()

    // Draw arrowhead
    val arrowPath = Path().apply {
        moveTo(end.x, end.y)
        lineTo(x1, y1)
        lineTo(x2, y2)
        close()
    }

    drawPath(
        path = arrowPath,
        color = color
    )
}

// Helper function to format date
fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return format.format(date)
}