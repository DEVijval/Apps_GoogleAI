package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CalmFlowRepository
import com.example.data.CompletionOutcome
import com.example.model.CalmNote
import com.example.model.CalmTask
import com.example.model.CortisolEngine
import com.example.model.CortisolInsight
import com.example.model.CortisolLog
import com.example.model.CortisolMood
import com.example.model.FocusSession
import com.example.model.GamificationCatalog
import com.example.model.NoteTag
import com.example.model.RewardItem
import com.example.model.TaskCategory
import com.example.model.TaskPriority
import com.example.model.UiState
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TaskFilter(val label: String) {
    ALL("All Active"),
    TODAY("Due Today"),
    OVERDUE("Overdue"),
    HIGH_PRIORITY("High Focus"),
    COMPLETED("Completed")
}

enum class TaskSort(val label: String) {
    STRESS_SMART("Stress-Smart"),
    DEADLINE("Deadline"),
    COGNITIVE_LOAD("Cognitive Load")
}

data class FocusTimerState(
    val selectedTaskId: String? = null,
    val selectedTaskTitle: String = "Deep Work Flow",
    val durationMinutes: Int = 25,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val breathingMode: Boolean = false
)

sealed interface CalmFlowEvent {
    data class Banner(val message: String, val isCelebration: Boolean = false) : CalmFlowEvent
    data class Error(val message: String) : CalmFlowEvent
}

data class DashboardBundle(
    val profile: UserProfile?,
    val tasks: List<CalmTask>,
    val notes: List<CalmNote>,
    val cortisolLogs: List<CortisolLog>,
    val focusSessions: List<FocusSession>,
    val rewards: List<RewardItem>,
    val cortisolInsight: CortisolInsight
)

class CalmFlowViewModel(
    private val repository: CalmFlowRepository,
    private val currentUserId: String,
    private val initialDisplayName: String?
) : ViewModel() {

    private val _events = MutableSharedFlow<CalmFlowEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<CalmFlowEvent> = _events.asSharedFlow()

    private val _taskFilter = MutableStateFlow(TaskFilter.ALL)
    val taskFilter: StateFlow<TaskFilter> = _taskFilter.asStateFlow()

    private val _taskSort = MutableStateFlow(TaskSort.STRESS_SMART)
    val taskSort: StateFlow<TaskSort> = _taskSort.asStateFlow()

    private val _noteQuery = MutableStateFlow("")
    val noteQuery: StateFlow<String> = _noteQuery.asStateFlow()

    private val _noteTagFilter = MutableStateFlow<NoteTag?>(null)
    val noteTagFilter: StateFlow<NoteTag?> = _noteTagFilter.asStateFlow()

    private val _timerState = MutableStateFlow(FocusTimerState())
    val timerState: StateFlow<FocusTimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeProfile(currentUserId).toUiState("profile")

    val tasksState: StateFlow<UiState<List<CalmTask>>> =
        repository.observeTasks(currentUserId).toUiState("tasks")

    val notesState: StateFlow<UiState<List<CalmNote>>> =
        repository.observeNotes(currentUserId).toUiState("notes")

    val cortisolLogsState: StateFlow<UiState<List<CortisolLog>>> =
        repository.observeCortisolLogs(currentUserId).toUiState("cortisolLogs")

    val focusSessionsState: StateFlow<UiState<List<FocusSession>>> =
        repository.observeFocusSessions(currentUserId).toUiState("focusSessions")

    val rewardsState: StateFlow<UiState<List<RewardItem>>> =
        repository.observeRewards(currentUserId).toUiState("rewards")

    val dashboardState: StateFlow<UiState<DashboardBundle>> = combine(
        profileState,
        tasksState,
        notesState,
        cortisolLogsState,
        focusSessionsState,
        rewardsState
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val pState = arr[0] as UiState<UserProfile?>
        @Suppress("UNCHECKED_CAST")
        val tState = arr[1] as UiState<List<CalmTask>>
        @Suppress("UNCHECKED_CAST")
        val nState = arr[2] as UiState<List<CalmNote>>
        @Suppress("UNCHECKED_CAST")
        val cState = arr[3] as UiState<List<CortisolLog>>
        @Suppress("UNCHECKED_CAST")
        val fState = arr[4] as UiState<List<FocusSession>>
        @Suppress("UNCHECKED_CAST")
        val rState = arr[5] as UiState<List<RewardItem>>

        val firstError = listOf(pState, tState, nState, cState, fState, rState)
            .filterIsInstance<UiState.Error>()
            .firstOrNull()

        when {
            firstError != null -> UiState.Error(firstError.message)
            pState is UiState.Success &&
                tState is UiState.Success &&
                nState is UiState.Success &&
                cState is UiState.Success &&
                fState is UiState.Success &&
                rState is UiState.Success -> {
                val insight = CortisolEngine.analyze(
                    tasks = tState.data,
                    latestLog = cState.data.firstOrNull(),
                    recentSessions = fState.data
                )
                UiState.Success(
                    DashboardBundle(
                        profile = pState.data,
                        tasks = tState.data,
                        notes = nState.data,
                        cortisolLogs = cState.data,
                        focusSessions = fState.data,
                        rewards = rState.data,
                        cortisolInsight = insight
                    )
                )
            }
            else -> UiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    init {
        viewModelScope.launch {
            repository.ensureProfileAndStarterRewards(currentUserId, initialDisplayName)
                .onFailure { err ->
                    _events.tryEmit(CalmFlowEvent.Error(err.localizedMessage ?: "Failed to initialize profile"))
                }
        }
        viewModelScope.launch {
            dashboardState.collect { state ->
                if (state is UiState.Success) {
                    repository.syncWidgetSnapshot(
                        tasks = state.data.tasks,
                        profile = state.data.profile,
                        logs = state.data.cortisolLogs,
                        sessions = state.data.focusSessions
                    )
                }
            }
        }
    }

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    fun setTaskFilter(filter: TaskFilter) {
        _taskFilter.value = filter
    }

    fun setTaskSort(sort: TaskSort) {
        _taskSort.value = sort
    }

    fun setNoteQuery(query: String) {
        _noteQuery.value = query
    }

    fun setNoteTagFilter(tag: NoteTag?) {
        _noteTagFilter.value = tag
    }

    fun saveTask(
        existingTaskId: String?,
        title: String,
        description: String,
        priority: TaskPriority,
        cognitiveLoad: Int,
        category: TaskCategory,
        dueAtMillis: Long,
        reminderAtMillis: Long,
        estimatedMinutes: Int
    ) {
        viewModelScope.launch {
            repository.saveTask(
                userId = currentUserId,
                existingTaskId = existingTaskId,
                title = title,
                description = description,
                priority = priority,
                cognitiveLoad = cognitiveLoad,
                category = category,
                dueAtMillis = dueAtMillis,
                reminderAtMillis = reminderAtMillis,
                estimatedMinutes = estimatedMinutes
            ).onSuccess {
                _events.tryEmit(
                    CalmFlowEvent.Banner(
                        if (existingTaskId == null) "Task scheduled" else "Task updated"
                    )
                )
            }.onFailure { err ->
                _events.tryEmit(CalmFlowEvent.Error(err.localizedMessage ?: "Failed to save task"))
            }
        }
    }

    fun toggleTaskCompletion(task: CalmTask, profile: UserProfile?) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(currentUserId, task, profile)
                .onSuccess { outcome ->
                    emitOutcomeCelebration("Task completed", outcome)
                }
                .onFailure { err ->
                    _events.tryEmit(CalmFlowEvent.Error(err.localizedMessage ?: "Failed to update task"))
                }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(currentUserId, taskId)
                .onSuccess { _events.tryEmit(CalmFlowEvent.Banner("Task removed")) }
                .onFailure { err ->
                    _events.tryEmit(CalmFlowEvent.Error(err.localizedMessage ?: "Failed to delete task"))
                }
        }
    }

    fun saveNote(
        existingNoteId: String?,
        title: String,
        content: String,
        tag: NoteTag,
        pinned: Boolean,
        linkedTaskTitle: String
    ) {
        viewModelScope.launch {
            repository.saveNote(
                userId = currentUserId,
                existingNoteId = existingNoteId,
                title = title,
                content = content,
                tag = tag,
                pinned = pinned,
                linkedTaskTitle = linkedTaskTitle
            ).onSuccess {
                _events.tryEmit(
                    CalmFlowEvent.Banner(
                        if (existingNoteId == null) "Note saved to vault" else "Note updated"
                    )
                )
            }.onFailure { err ->
                _events.tryEmit(CalmFlowEvent.Error(err.localizedMessage ?: "Failed to save note"))
            }
        }
    }

    fun toggleNotePin(note: CalmNote) {
        viewModelScope.launch {
            repository.toggleNotePin(currentUserId, note)
                .onFailure { err ->
                    _events.tryEmit(CalmFlowEvent.Error(err.localizedMessage ?: "Failed to update note"))
                }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(currentUserId, noteId)
                .onSuccess { _events.tryEmit(CalmFlowEvent.Banner("Note deleted")) }
                .onFailure { err ->
                    _events.tryEmit(CalmFlowEvent.Error(err.localizedMessage ?: "Failed to delete note"))
                }
        }
    }

    fun logCortisolCheckIn(
        level: Int,
        mood: CortisolMood,
        trigger: String,
        note: String,
        profile: UserProfile?,
        isBreathingRecovery: Boolean = false
    ) {
        viewModelScope.launch {
            repository.logCortisolCheckIn(
                userId = currentUserId,
                level = level,
                mood = mood,
                trigger = trigger,
                note = note,
                profile = profile,
                isBreathingRecovery = isBreathingRecovery
            ).onSuccess { outcome ->
                val prefix = if (isBreathingRecovery) "Cortisol reset logged" else "Check-in saved"
                emitOutcomeCelebration(prefix, outcome)
            }.onFailure { err ->
                _events.tryEmit(CalmFlowEvent.Error(err.localizedMessage ?: "Failed to save check-in"))
            }
        }
    }

    fun addCustomReward(title: String, costCoins: Long, iconKey: String) {
        viewModelScope.launch {
            repository.addCustomReward(currentUserId, title, costCoins, iconKey)
                .onSuccess { _events.tryEmit(CalmFlowEvent.Banner("Custom reward added")) }
                .onFailure { err ->
                    _events.tryEmit(CalmFlowEvent.Error(err.localizedMessage ?: "Failed to add reward"))
                }
        }
    }

    fun redeemReward(reward: RewardItem, profile: UserProfile) {
        if (profile.calmCoins < reward.costCoins) {
            _events.tryEmit(
                CalmFlowEvent.Error("Need ${reward.costCoins - profile.calmCoins} more Calm Coins")
            )
            return
        }
        viewModelScope.launch {
            repository.redeemReward(currentUserId, reward, profile)
                .onSuccess { outcome ->
                    val badgeNote = outcome.newlyUnlockedBadges.firstOrNull()?.let { id ->
                        val title = GamificationCatalog.badges.firstOrNull { it.id == id }?.title ?: id
                        " • Unlocked $title!"
                    }.orEmpty()
                    _events.tryEmit(
                        CalmFlowEvent.Banner(
                            "Redeemed \"${reward.title}\"! Enjoy your break$badgeNote",
                            isCelebration = true
                        )
                    )
                }
                .onFailure { err ->
                    _events.tryEmit(CalmFlowEvent.Error(err.localizedMessage ?: "Failed to redeem reward"))
                }
        }
    }

    fun deleteReward(rewardId: String) {
        viewModelScope.launch {
            repository.deleteReward(currentUserId, rewardId)
                .onFailure { err ->
                    _events.tryEmit(CalmFlowEvent.Error(err.localizedMessage ?: "Failed to delete reward"))
                }
        }
    }

    // --- Focus Timer Engine ---

    fun selectFocusTask(task: CalmTask?) {
        _timerState.update { current ->
            if (current.isRunning) return@update current
            val mins = task?.estimatedMinutes?.toInt()?.coerceIn(5, 120) ?: current.durationMinutes
            current.copy(
                selectedTaskId = task?.id,
                selectedTaskTitle = task?.title ?: "Deep Work Flow",
                durationMinutes = mins,
                remainingSeconds = mins * 60
            )
        }
    }

    fun setTimerPreset(minutes: Int) {
        val clamped = minutes.coerceIn(5, 120)
        _timerState.update { current ->
            if (current.isRunning) return@update current
            current.copy(
                durationMinutes = clamped,
                remainingSeconds = clamped * 60
            )
        }
    }

    fun toggleBreathingMode(enabled: Boolean) {
        _timerState.update { it.copy(breathingMode = enabled) }
    }

    fun toggleTimerRunning(profile: UserProfile?) {
        val current = _timerState.value
        if (current.isRunning) {
            pauseTimer()
        } else {
            startTimer(profile)
        }
    }

    private fun startTimer(profile: UserProfile?) {
        timerJob?.cancel()
        _timerState.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (_timerState.value.isRunning && _timerState.value.remainingSeconds > 0) {
                delay(1000L)
                _timerState.update { state ->
                    if (!state.isRunning) state
                    else state.copy(remainingSeconds = (state.remainingSeconds - 1).coerceAtLeast(0))
                }
            }
            if (_timerState.value.isRunning && _timerState.value.remainingSeconds == 0) {
                val finished = _timerState.value
                _timerState.update {
                    it.copy(isRunning = false, remainingSeconds = it.durationMinutes * 60)
                }
                persistFocusSession(finished, completed = true, elapsedMinutes = finished.durationMinutes, profile = profile)
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _timerState.update { it.copy(isRunning = false) }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerState.update {
            it.copy(isRunning = false, remainingSeconds = it.durationMinutes * 60)
        }
    }

    fun finishFocusEarly(profile: UserProfile?) {
        val current = _timerState.value
        timerJob?.cancel()
        val elapsedSec = (current.durationMinutes * 60) - current.remainingSeconds
        val elapsedMins = (elapsedSec / 60).coerceAtLeast(1)
        _timerState.update {
            it.copy(isRunning = false, remainingSeconds = it.durationMinutes * 60)
        }
        if (elapsedSec >= 30) {
            persistFocusSession(current, completed = false, elapsedMinutes = elapsedMins, profile = profile)
        } else {
            _events.tryEmit(CalmFlowEvent.Banner("Focus timer reset (run at least 30s to log)"))
        }
    }

    private fun persistFocusSession(
        snapshot: FocusTimerState,
        completed: Boolean,
        elapsedMinutes: Int,
        profile: UserProfile?
    ) {
        viewModelScope.launch {
            repository.recordFocusSession(
                userId = currentUserId,
                taskTitle = snapshot.selectedTaskTitle,
                durationMinutes = elapsedMinutes,
                completed = completed,
                breathingEnabled = snapshot.breathingMode,
                profile = profile
            ).onSuccess { outcome ->
                val label = if (completed) "Deep Work complete (${elapsedMinutes}m)" else "Focus logged (${elapsedMinutes}m)"
                emitOutcomeCelebration(label, outcome)
            }.onFailure { err ->
                _events.tryEmit(CalmFlowEvent.Error(err.localizedMessage ?: "Failed to record focus session"))
            }
        }
    }

    private fun emitOutcomeCelebration(prefix: String, outcome: CompletionOutcome) {
        if (outcome.xpGained == 0L && outcome.coinsGained == 0L && outcome.newlyUnlockedBadges.isEmpty()) {
            _events.tryEmit(CalmFlowEvent.Banner(prefix))
            return
        }
        val parts = mutableListOf(prefix)
        if (outcome.xpGained > 0L) parts += "+${outcome.xpGained} XP"
        if (outcome.coinsGained > 0L) parts += "+${outcome.coinsGained} Coins"
        outcome.leveledUpTo?.let { parts += "Level $it!" }
        outcome.newlyUnlockedBadges.firstOrNull()?.let { badgeId ->
            val badgeTitle = GamificationCatalog.badges.firstOrNull { it.id == badgeId }?.title ?: badgeId
            parts += "Badge: $badgeTitle"
        }
        _events.tryEmit(CalmFlowEvent.Banner(parts.joinToString(" • "), isCelebration = true))
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    private companion object {
        const val TAG = "CalmFlowVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
