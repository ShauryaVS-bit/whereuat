package com.example.whereuat.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.whereuat.bluetooth.BleScanner
import com.example.whereuat.data.AuthRepository
import com.example.whereuat.data.SessionRepository
import com.example.whereuat.model.AppUser
import com.example.whereuat.model.EventSession
import com.example.whereuat.model.FriendGroup
import com.example.whereuat.model.FriendNode
import com.example.whereuat.model.FriendSignal
import com.example.whereuat.model.ScanProfile
import com.example.whereuat.model.distanceLabel
import com.example.whereuat.model.estimateDistanceMeters
import com.example.whereuat.model.toProjection
import com.example.whereuat.utils.MotionMonitor
import com.example.whereuat.utils.OrientationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

sealed interface AppAction {
    data class UpdateDisplayName(val value: String) : AppAction
    data class UpdateSessionCode(val value: String) : AppAction
    data class CreateSession(val name: String) : AppAction
    object JoinSession : AppAction
    data class SelectTarget(val id: String) : AppAction
    object BackToRadar : AppAction
}

data class AppUiState(
    val loading: Boolean = false,
    val userId: String = "",
    val displayName: String = "Solo Dev",
    val sessionCodeInput: String = "",
    val activeSession: EventSession? = null,
    val nodes: List<FriendNode> = emptyList(),
    val groups: List<FriendGroup> = emptyList(),
    val selectedTarget: FriendNode? = null,
    val headingDeg: Float = 0f
)

class AppViewModel(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val scanner: BleScanner,
    private val orientationTracker: OrientationTracker,
    motionMonitor: MotionMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState(loading = true))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val scanProfile = motionMonitor.movingFlow().combine(_uiState) { moving, state ->
        if (state.activeSession == null) ScanProfile.BATTERY_SAVER
        else if (moving) ScanProfile.FAST
        else ScanProfile.BALANCED
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanProfile.BALANCED)

    private val signals = MutableStateFlow<Map<String, FriendSignal>>(emptyMap())

    init {
        bootstrap()
        observeOrientation()
        observeScan()
        projectNodes()
    }

    fun onAction(action: AppAction) {
        when (action) {
            is AppAction.UpdateDisplayName -> _uiState.value = _uiState.value.copy(displayName = action.value)
            is AppAction.UpdateSessionCode -> _uiState.value = _uiState.value.copy(sessionCodeInput = action.value)
            is AppAction.CreateSession -> createSession(action.name)
            AppAction.JoinSession -> joinSession()
            is AppAction.SelectTarget -> selectTarget(action.id)
            AppAction.BackToRadar -> _uiState.value = _uiState.value.copy(selectedTarget = null)
        }
    }

    private fun bootstrap() = viewModelScope.launch {
        val uid = authRepository.signInAnonymouslyIfNeeded()
        _uiState.value = _uiState.value.copy(loading = false, userId = uid)
    }

    private fun createSession(name: String) = viewModelScope.launch {
        val sessionId = sessionRepository.createSession(name = name, ownerId = _uiState.value.userId)
        _uiState.value = _uiState.value.copy(sessionCodeInput = sessionId)
        observeSession(sessionId)
    }

    private fun joinSession() = viewModelScope.launch {
        val sessionId = _uiState.value.sessionCodeInput.trim()
        if (sessionId.isBlank()) return@launch
        sessionRepository.joinSession(
            sessionId = sessionId,
            user = AppUser(id = _uiState.value.userId, displayName = _uiState.value.displayName)
        )
        observeSession(sessionId)
    }

    private fun observeSession(sessionId: String) = viewModelScope.launch {
        sessionRepository.observeSession(sessionId).collect { session ->
            _uiState.value = _uiState.value.copy(activeSession = session)
        }
    }

    private fun observeOrientation() = viewModelScope.launch {
        orientationTracker.azimuthFlow().collect { heading ->
            _uiState.value = _uiState.value.copy(headingDeg = heading)
        }
    }

    private fun observeScan() = viewModelScope.launch {
        scanner.scanFriends(scanProfile).collect { signal ->
            signals.value = signals.value.toMutableMap().apply {
                this[signal.friendId] = signal.copy(rssi = signal.rssi + scanner.wifiSignalBoost(signal.friendId))
            }
        }
    }

    private fun projectNodes() = viewModelScope.launch {
        signals.collect { signalMap ->
            val nodes = signalMap.values.map { signal ->
                val projection = signal.toProjection()
                val meters = estimateDistanceMeters(signal.rssi, -59)
                FriendNode(
                    id = signal.friendId,
                    displayName = signal.displayName,
                    radius = projection.radius,
                    angleDeg = projection.angle,
                    distanceLabel = distanceLabel(meters),
                    score = projection.distanceScore
                )
            }
            val grouped = cluster(nodes)
            _uiState.value = _uiState.value.copy(nodes = nodes, groups = grouped)
        }
    }

    private fun selectTarget(id: String) {
        _uiState.value = _uiState.value.copy(selectedTarget = _uiState.value.nodes.firstOrNull { it.id == id })
    }

    private fun cluster(nodes: List<FriendNode>): List<FriendGroup> {
        val pending = nodes.toMutableList()
        val groups = mutableListOf<FriendGroup>()
        var index = 1
        while (pending.isNotEmpty()) {
            val seed = pending.removeFirst()
            val members = mutableListOf(seed)
            val iterator = pending.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                if (abs(candidate.radius - seed.radius) < 0.1f && abs(candidate.angleDeg - seed.angleDeg) < 20f) {
                    members += candidate
                    iterator.remove()
                }
            }
            if (members.size > 1) {
                groups += FriendGroup(
                    id = "group-$index",
                    name = "Group $index",
                    members = members,
                    centerRadius = members.map { it.radius }.average().toFloat(),
                    centerAngle = members.map { it.angleDeg }.average().toFloat()
                )
                index++
            }
        }
        return groups
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AppViewModel(
                    authRepository = AuthRepository(),
                    sessionRepository = SessionRepository(),
                    scanner = BleScanner(context.applicationContext),
                    orientationTracker = OrientationTracker(context.applicationContext),
                    motionMonitor = MotionMonitor(context.applicationContext)
                ) as T
            }
        }
    }
}
