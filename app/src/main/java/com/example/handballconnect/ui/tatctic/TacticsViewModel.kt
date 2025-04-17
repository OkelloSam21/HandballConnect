package com.example.handballconnect.ui.tatctic

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.handballconnect.data.model.MovementArrow
import com.example.handballconnect.data.model.PlayerPosition
import com.example.handballconnect.data.model.Tactics
import com.example.handballconnect.data.repository.TacticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TacticsViewModel @Inject constructor(
    private val tacticsRepository: TacticsRepository
) : ViewModel() {
    
    // User tactics state
    private val _userTacticsState = MutableStateFlow<TacticsListState>(TacticsListState.Loading)
    val userTacticsState: StateFlow<TacticsListState> = _userTacticsState.asStateFlow()
    
    // Shared tactics state
    private val _sharedTacticsState = MutableStateFlow<TacticsListState>(TacticsListState.Loading)
    val sharedTacticsState: StateFlow<TacticsListState> = _sharedTacticsState.asStateFlow()
    
    // Currently selected tactics
    private val _selectedTactics = MutableStateFlow<Tactics?>(null)
    val selectedTactics: StateFlow<Tactics?> = _selectedTactics.asStateFlow()
    
    // Tactics save state
    private val _saveTacticsState = MutableStateFlow<SaveTacticsState>(SaveTacticsState.Initial)
    val saveTacticsState: StateFlow<SaveTacticsState> = _saveTacticsState.asStateFlow()
    
    // Current board state for editor
    private val _playerPositions = MutableStateFlow<List<PlayerPosition>>(emptyList())
    val playerPositions: StateFlow<List<PlayerPosition>> = _playerPositions.asStateFlow()
    
    private val _movementArrows = MutableStateFlow<List<MovementArrow>>(emptyList())
    val movementArrows: StateFlow<List<MovementArrow>> = _movementArrows.asStateFlow()
    
    // Initialize by loading tactics
    init {
        loadUserTactics()
        loadSharedTactics()
    }
    
    // Load user's tactics
    fun loadUserTactics() {
        _userTacticsState.value = TacticsListState.Loading
        
        viewModelScope.launch {
            tacticsRepository.getUserTactics().collect { result ->
                result.onSuccess { tacticsList ->
                    if (tacticsList.isEmpty()) {
                        _userTacticsState.value = TacticsListState.Empty
                    } else {
                        _userTacticsState.value = TacticsListState.Success(tacticsList)
                    }
                }.onFailure { exception ->
                    _userTacticsState.value = TacticsListState.Error(exception.message ?: "Failed to load tactics")
                }
            }
        }
    }
    
    // Load shared tactics
    fun loadSharedTactics() {
        _sharedTacticsState.value = TacticsListState.Loading
        
        viewModelScope.launch {
            tacticsRepository.getSharedTactics().collect { result ->
                result.onSuccess { tacticsList ->
                    if (tacticsList.isEmpty()) {
                        _sharedTacticsState.value = TacticsListState.Empty
                    } else {
                        _sharedTacticsState.value = TacticsListState.Success(tacticsList)
                    }
                }.onFailure { exception ->
                    _sharedTacticsState.value = TacticsListState.Error(exception.message ?: "Failed to load shared tactics")
                }
            }
        }
    }
    
    // Select tactics to view or edit
    fun selectTactics(tactics: Tactics) {
        _selectedTactics.value = tactics
        _playerPositions.value = tactics.players
        _movementArrows.value = tactics.movements
    }
    
    // Load tactics by ID
    fun loadTacticsById(tacticsId: String) {
        viewModelScope.launch {
            tacticsRepository.getTacticsById(tacticsId).collect { result ->
                result.onSuccess { tactics ->
                    selectTactics(tactics)
                }
            }
        }
    }
    
    // Create new tactics board
    fun createNewTactics() {
        // Start with default player positions
        val defaultOffensePlayers = tacticsRepository.getDefaultPlayerPositions(true)
        val defaultDefensePlayers = tacticsRepository.getDefaultPlayerPositions(false)
        
        _playerPositions.value = defaultOffensePlayers + defaultDefensePlayers
        _movementArrows.value = emptyList()
        _selectedTactics.value = null
    }
    
    // Save tactics
    fun saveTactics(
        title: String,
        description: String,
        boardImage: Bitmap?,
        isShared: Boolean
    ) {
        _saveTacticsState.value = SaveTacticsState.Saving
        
        val existingTacticsId = _selectedTactics.value?.tacticsId
        
        viewModelScope.launch {
            val result = tacticsRepository.saveTactics(
                title = title,
                description = description,
                players = _playerPositions.value,
                movements = _movementArrows.value,
                boardImage = boardImage,
                isShared = isShared,
                existingTacticsId = existingTacticsId
            )
            
            result.onSuccess { tactics ->
                _saveTacticsState.value = SaveTacticsState.Success(tactics)
                _selectedTactics.value = tactics
                
                // Reload tactics lists
                loadUserTactics()
                if (isShared) {
                    loadSharedTactics()
                }
            }.onFailure { exception ->
                _saveTacticsState.value = SaveTacticsState.Error(exception.message ?: "Failed to save tactics")
            }
        }
    }
    
    // Update player position
    fun updatePlayerPosition(player: PlayerPosition) {
        val currentPlayers = _playerPositions.value.toMutableList()
        val index = currentPlayers.indexOfFirst { it.id == player.id }
        
        if (index != -1) {
            currentPlayers[index] = player
            _playerPositions.value = currentPlayers
        } else {
            // Add new player
            currentPlayers.add(player)
            _playerPositions.value = currentPlayers
        }
    }
    
    // Remove player
    fun removePlayer(playerId: Int) {
        _playerPositions.value = _playerPositions.value.filter { it.id != playerId }
        
        // Also remove any movement arrows associated with this player
        _movementArrows.value = _movementArrows.value.filter { it.playerId != playerId }
    }
    
    // Add movement arrow
    fun addMovementArrow(arrow: MovementArrow) {
        val currentArrows = _movementArrows.value.toMutableList()
        currentArrows.add(arrow)
        _movementArrows.value = currentArrows
    }
    
    // Update movement arrow
    fun updateMovementArrow(arrow: MovementArrow) {
        val currentArrows = _movementArrows.value.toMutableList()
        val index = currentArrows.indexOfFirst { it.id == arrow.id }
        
        if (index != -1) {
            currentArrows[index] = arrow
            _movementArrows.value = currentArrows
        }
    }
    
    // Remove movement arrow
    fun removeMovementArrow(arrowId: Int) {
        _movementArrows.value = _movementArrows.value.filter { it.id != arrowId }
    }
    
    // Delete tactics
    fun deleteTactics(tacticsId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = tacticsRepository.deleteTactics(tacticsId)
            
            result.onSuccess {
                onSuccess()
                // Clear selected tactics if it's the one being deleted
                if (_selectedTactics.value?.tacticsId == tacticsId) {
                    _selectedTactics.value = null
                    _playerPositions.value = emptyList()
                    _movementArrows.value = emptyList()
                }
                
                // Reload tactics lists
                loadUserTactics()
                loadSharedTactics()
            }.onFailure { exception ->
                onError(exception.message ?: "Failed to delete tactics")
            }
        }
    }
    
    // Update sharing status
    fun updateSharingStatus(tacticsId: String, isShared: Boolean, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = tacticsRepository.updateSharingStatus(tacticsId, isShared)
            
            result.onSuccess {
                onSuccess()
                // Update selected tactics if it's the one being updated
                if (_selectedTactics.value?.tacticsId == tacticsId) {
                    _selectedTactics.value = _selectedTactics.value?.copy(isShared = isShared)
                }
                
                // Reload tactics lists
                loadUserTactics()
                loadSharedTactics()
            }.onFailure { exception ->
                onError(exception.message ?: "Failed to update sharing status")
            }
        }
    }
    
    // Reset save state
    fun resetSaveTacticsState() {
        _saveTacticsState.value = SaveTacticsState.Initial
    }
    
    // Apply template formation
    fun applyTemplate(templateName: String) {
        when (templateName) {
            "6-0" -> {
                // Standard 6-0 defense and matching offense
                val offensePlayers = listOf(
                    PlayerPosition(id = 1, x = 0.2f, y = 0.5f, number = 1, isOffense = true, label = "LW"),
                    PlayerPosition(id = 2, x = 0.3f, y = 0.3f, number = 2, isOffense = true, label = "LB"),
                    PlayerPosition(id = 3, x = 0.3f, y = 0.5f, number = 3, isOffense = true, label = "CB"),
                    PlayerPosition(id = 4, x = 0.3f, y = 0.7f, number = 4, isOffense = true, label = "RB"),
                    PlayerPosition(id = 5, x = 0.2f, y = 0.9f, number = 5, isOffense = true, label = "RW"),
                    PlayerPosition(id = 6, x = 0.15f, y = 0.5f, number = 6, isOffense = true, label = "P")
                )
                
                val defensePlayers = listOf(
                    PlayerPosition(id = 7, x = 0.8f, y = 0.3f, number = 1, isOffense = false, label = "LD"),
                    PlayerPosition(id = 8, x = 0.8f, y = 0.4f, number = 2, isOffense = false, label = "LHD"),
                    PlayerPosition(id = 9, x = 0.8f, y = 0.5f, number = 3, isOffense = false, label = "CHD"),
                    PlayerPosition(id = 10, x = 0.8f, y = 0.6f, number = 4, isOffense = false, label = "RHD"),
                    PlayerPosition(id = 11, x = 0.8f, y = 0.7f, number = 5, isOffense = false, label = "RD"),
                    PlayerPosition(id = 12, x = 0.9f, y = 0.5f, number = 12, isOffense = false, label = "GK")
                )
                
                _playerPositions.value = offensePlayers + defensePlayers
            }
            "5-1" -> {
                // 5-1 defense formation
                val offensePlayers = listOf(
                    PlayerPosition(id = 1, x = 0.2f, y = 0.5f, number = 1, isOffense = true, label = "LW"),
                    PlayerPosition(id = 2, x = 0.3f, y = 0.3f, number = 2, isOffense = true, label = "LB"),
                    PlayerPosition(id = 3, x = 0.3f, y = 0.5f, number = 3, isOffense = true, label = "CB"),
                    PlayerPosition(id = 4, x = 0.3f, y = 0.7f, number = 4, isOffense = true, label = "RB"),
                    PlayerPosition(id = 5, x = 0.2f, y = 0.9f, number = 5, isOffense = true, label = "RW"),
                    PlayerPosition(id = 6, x = 0.15f, y = 0.5f, number = 6, isOffense = true, label = "P")
                )
                
                val defensePlayers = listOf(
                    PlayerPosition(id = 7, x = 0.8f, y = 0.25f, number = 1, isOffense = false, label = "LD"),
                    PlayerPosition(id = 8, x = 0.8f, y = 0.4f, number = 2, isOffense = false, label = "LHD"),
                    PlayerPosition(id = 9, x = 0.8f, y = 0.5f, number = 3, isOffense = false, label = "CHD"),
                    PlayerPosition(id = 10, x = 0.8f, y = 0.6f, number = 4, isOffense = false, label = "RHD"),
                    PlayerPosition(id = 11, x = 0.8f, y = 0.75f, number = 5, isOffense = false, label = "RD"),
                    PlayerPosition(id = 12, x = 0.9f, y = 0.5f, number = 12, isOffense = false, label = "GK"),
                    // Advance defender in 5-1
                    PlayerPosition(id = 13, x = 0.7f, y = 0.5f, number = 6, isOffense = false, label = "AD")
                )
                
                _playerPositions.value = offensePlayers + defensePlayers
            }
            "3-2-1" -> {
                // 3-2-1 defense formation
                val offensePlayers = listOf(
                    PlayerPosition(id = 1, x = 0.2f, y = 0.5f, number = 1, isOffense = true, label = "LW"),
                    PlayerPosition(id = 2, x = 0.3f, y = 0.3f, number = 2, isOffense = true, label = "LB"),
                    PlayerPosition(id = 3, x = 0.3f, y = 0.5f, number = 3, isOffense = true, label = "CB"),
                    PlayerPosition(id = 4, x = 0.3f, y = 0.7f, number = 4, isOffense = true, label = "RB"),
                    PlayerPosition(id = 5, x = 0.2f, y = 0.9f, number = 5, isOffense = true, label = "RW"),
                    PlayerPosition(id = 6, x = 0.15f, y = 0.5f, number = 6, isOffense = true, label = "P")
                )
                
                val defensePlayers = listOf(
                    // Back row (3)
                    PlayerPosition(id = 7, x = 0.85f, y = 0.3f, number = 1, isOffense = false, label = "LD"),
                    PlayerPosition(id = 8, x = 0.85f, y = 0.5f, number = 2, isOffense = false, label = "CD"),
                    PlayerPosition(id = 9, x = 0.85f, y = 0.7f, number = 3, isOffense = false, label = "RD"),
                    // Middle row (2)
                    PlayerPosition(id = 10, x = 0.75f, y = 0.35f, number = 4, isOffense = false, label = "LM"),
                    PlayerPosition(id = 11, x = 0.75f, y = 0.65f, number = 5, isOffense = false, label = "RM"),
                    // Front (1)
                    PlayerPosition(id = 12, x = 0.65f, y = 0.5f, number = 6, isOffense = false, label = "FD"),
                    // Goalkeeper
                    PlayerPosition(id = 13, x = 0.9f, y = 0.5f, number = 12, isOffense = false, label = "GK")
                )
                
                _playerPositions.value = offensePlayers + defensePlayers
            }
            "fastbreak" -> {
                // Fast break scenario
                val offensePlayers = listOf(
                    PlayerPosition(id = 1, x = 0.5f, y = 0.2f, number = 1, isOffense = true, label = "LW"),
                    PlayerPosition(id = 2, x = 0.6f, y = 0.4f, number = 2, isOffense = true, label = "LB"),
                    PlayerPosition(id = 3, x = 0.7f, y = 0.5f, number = 3, isOffense = true, label = "CB"),
                    PlayerPosition(id = 4, x = 0.6f, y = 0.6f, number = 4, isOffense = true, label = "RB"),
                    PlayerPosition(id = 5, x = 0.5f, y = 0.8f, number = 5, isOffense = true, label = "RW"),
                    PlayerPosition(id = 6, x = 0.4f, y = 0.5f, number = 6, isOffense = true, label = "P")
                )
                
                val defensePlayers = listOf(
                    // Scattered defense during fastbreak
                    PlayerPosition(id = 7, x = 0.3f, y = 0.2f, number = 1, isOffense = false, label = "D1"),
                    PlayerPosition(id = 8, x = 0.2f, y = 0.4f, number = 2, isOffense = false, label = "D2"),
                    PlayerPosition(id = 9, x = 0.1f, y = 0.5f, number = 3, isOffense = false, label = "D3"),
                    PlayerPosition(id = 10, x = 0.2f, y = 0.6f, number = 4, isOffense = false, label = "D4"),
                    PlayerPosition(id = 11, x = 0.3f, y = 0.8f, number = 5, isOffense = false, label = "D5"),
                    PlayerPosition(id = 12, x = 0.9f, y = 0.5f, number = 12, isOffense = false, label = "GK")
                )
                
                _playerPositions.value = offensePlayers + defensePlayers
            }
            else -> {
                // Default to 6-0 if template not recognized
                val defaultOffensePlayers = tacticsRepository.getDefaultPlayerPositions(true)
                val defaultDefensePlayers = tacticsRepository.getDefaultPlayerPositions(false)
                _playerPositions.value = defaultOffensePlayers + defaultDefensePlayers
            }
        }
        
        // Clear movement arrows when applying a template
        _movementArrows.value = emptyList()
    }
    
    // Tactics list state sealed class
    sealed class TacticsListState {
        object Loading : TacticsListState()
        object Empty : TacticsListState()
        data class Success(val tacticsList: List<Tactics>) : TacticsListState()
        data class Error(val message: String) : TacticsListState()
    }
    
    // Save tactics state sealed class
    sealed class SaveTacticsState {
        object Initial : SaveTacticsState()
        object Saving : SaveTacticsState()
        data class Success(val tactics: Tactics) : SaveTacticsState()
        data class Error(val message: String) : SaveTacticsState()
    }
}