package com.example.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.model.CalmNote
import com.example.model.CalmTask
import com.example.model.CortisolEngine
import com.example.model.CortisolLog
import com.example.model.CortisolMood
import com.example.model.FocusSession
import com.example.model.GamificationCatalog
import com.example.model.NoteTag
import com.example.model.RewardItem
import com.example.model.TaskCategory
import com.example.model.TaskPriority
import com.example.model.UserProfile
import com.example.notifications.TaskReminderScheduler
import com.example.widget.CalmFlowWidget
import com.example.widget.WidgetSnapshot
import com.example.widget.WidgetSnapshotStore
import com.example.widget.WidgetTaskPreview
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CompletionOutcome(
    val xpGained: Long = 0L,
    val coinsGained: Long = 0L,
    val leveledUpTo: Long? = null,
    val newlyUnlockedBadges: List<String> = emptyList()
)

class CalmFlowRepository(
    private val db: FirebaseFirestore,
    private val appContext: Context,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireMatchedUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user access." }
        return uid
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireMatchedUserId(userId)
        val docRef = userDoc(uid)
        emit(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, docRef.path, uid)
                    throw error
                }
        )
    }.let { outer ->
        flow {
            outer.collect { innerFlow ->
                innerFlow.collect { emit(it) }
            }
        }
    }

    fun observeTasks(userId: String): Flow<List<CalmTask>> = flow {
        val uid = requireMatchedUserId(userId)
        val colRef = userDoc(uid).collection("tasks")
        colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(CalmTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, colRef.path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeNotes(userId: String): Flow<List<CalmNote>> = flow {
        val uid = requireMatchedUserId(userId)
        val colRef = userDoc(uid).collection("notes")
        colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(CalmNote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, colRef.path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeCortisolLogs(userId: String): Flow<List<CortisolLog>> = flow {
        val uid = requireMatchedUserId(userId)
        val colRef = userDoc(uid).collection("cortisolLogs")
        colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(CortisolLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.loggedAtMillis }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, colRef.path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeFocusSessions(userId: String): Flow<List<FocusSession>> = flow {
        val uid = requireMatchedUserId(userId)
        val colRef = userDoc(uid).collection("focusSessions")
        colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(FocusSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.startedAtMillis }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, colRef.path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeRewards(userId: String): Flow<List<RewardItem>> = flow {
        val uid = requireMatchedUserId(userId)
        val colRef = userDoc(uid).collection("rewards")
        colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(RewardItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.costCoins }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, colRef.path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    private fun logStreamError(error: Throwable, op: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception) ?: return
        if (auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    suspend fun ensureProfileAndStarterRewards(userId: String, displayName: String?): Result<Unit> {
        var path = "users/$userId"
        return try {
            val uid = requireMatchedUserId(userId)
            val docRef = userDoc(uid)
            path = docRef.path
            val existingSnap = docRef.get().await()
            val today = todayDateKey()

            if (!existingSnap.exists()) {
                val cleanName = displayName?.trim()?.take(80)?.takeIf { it.isNotEmpty() } ?: "Flow Seeker"
                val profilePayload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "xp" to 0L,
                    "level" to 1L,
                    "calmCoins" to 60L,
                    "streakDays" to 1L,
                    "totalFocusMinutes" to 0L,
                    "totalTasksCompleted" to 0L,
                    "cortisolScore" to 32L,
                    "unlockedBadges" to emptyList<String>(),
                    "lastActiveDate" to today,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(profilePayload).await()

                // Seed starter self-care reward store items if rewards collection is empty
                val existingRewards = docRef.collection("rewards").limit(1).get().await()
                if (existingRewards.isEmpty) {
                    val starterRewards = listOf(
                        Triple("Mindful Espresso Ritual", 40L, "coffee"),
                        Triple("20m Sunlit Walk", 55L, "walk"),
                        Triple("Guilt-Free Music / Vinyl Break", 75L, "music"),
                        Triple("Early Log-Off Pass", 160L, "rest")
                    )
                    starterRewards.forEach { (title, cost, icon) ->
                        val rewardRef = docRef.collection("rewards").document()
                        rewardRef.set(
                            mapOf(
                                "userId" to uid,
                                "title" to title,
                                "costCoins" to cost,
                                "iconKey" to icon,
                                "redeemedCount" to 0L,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            } else {
                val profile = existingSnap.toObject(UserProfile::class.java)
                if (profile != null && profile.lastActiveDate != today) {
                    val nextStreak = if (isYesterday(profile.lastActiveDate, today)) {
                        (profile.streakDays + 1L).coerceAtMost(36500L)
                    } else if (profile.lastActiveDate.isBlank()) {
                        1L
                    } else {
                        1L
                    }
                    docRef.update(
                        mapOf(
                            "streakDays" to nextStreak,
                            "lastActiveDate" to today,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveTask(
        userId: String,
        existingTaskId: String?,
        title: String,
        description: String,
        priority: TaskPriority,
        cognitiveLoad: Int,
        category: TaskCategory,
        dueAtMillis: Long,
        reminderAtMillis: Long,
        estimatedMinutes: Int
    ): Result<Unit> {
        var path = "users/$userId/tasks"
        return try {
            val uid = requireMatchedUserId(userId)
            val cleanTitle = title.trim().take(140)
            require(cleanTitle.isNotEmpty()) { "Task title cannot be blank." }
            val cleanDesc = description.trim().take(600)
            val clampedLoad = cognitiveLoad.coerceIn(1, 5).toLong()
            val clampedMinutes = estimatedMinutes.coerceIn(5, 480).toLong()
            val xpReward = (priority.xpBase + (clampedLoad * 10L) + (clampedMinutes / 10L) * 5L).coerceIn(10L, 500L)

            val colRef = userDoc(uid).collection("tasks")
            val docRef = if (!existingTaskId.isNullOrBlank()) colRef.document(existingTaskId) else colRef.document()
            path = docRef.path

            if (existingTaskId.isNullOrBlank()) {
                val payload = mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "description" to cleanDesc,
                    "priority" to priority.name,
                    "cognitiveLoad" to clampedLoad,
                    "category" to category.name,
                    "completed" to false,
                    "dueAtMillis" to dueAtMillis.coerceAtLeast(0L),
                    "reminderAtMillis" to reminderAtMillis.coerceAtLeast(0L),
                    "estimatedMinutes" to clampedMinutes,
                    "xpReward" to xpReward,
                    "completedAtMillis" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "description" to cleanDesc,
                        "priority" to priority.name,
                        "cognitiveLoad" to clampedLoad,
                        "category" to category.name,
                        "dueAtMillis" to dueAtMillis.coerceAtLeast(0L),
                        "reminderAtMillis" to reminderAtMillis.coerceAtLeast(0L),
                        "estimatedMinutes" to clampedMinutes,
                        "xpReward" to xpReward,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (reminderAtMillis > System.currentTimeMillis()) {
                TaskReminderScheduler.scheduleReminder(
                    context = appContext,
                    taskId = docRef.id,
                    title = cleanTitle,
                    body = "${priority.label} priority • ${clampedMinutes}m focus estimate",
                    triggerAtMillis = reminderAtMillis
                )
            } else {
                TaskReminderScheduler.cancelReminder(appContext, docRef.id)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompletion(
        userId: String,
        task: CalmTask,
        profile: UserProfile?
    ): Result<CompletionOutcome> {
        var path = "users/$userId/tasks/${task.id}"
        return try {
            val uid = requireMatchedUserId(userId)
            require(task.id.isNotBlank()) { "Task ID is missing." }
            val nextCompleted = !task.completed
            val completedAt = if (nextCompleted) System.currentTimeMillis() else 0L
            val taskRef = userDoc(uid).collection("tasks").document(task.id)
            path = taskRef.path

            taskRef.update(
                mapOf(
                    "completed" to nextCompleted,
                    "completedAtMillis" to completedAt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (nextCompleted) {
                TaskReminderScheduler.cancelReminder(appContext, task.id)
            } else if (task.reminderAtMillis > System.currentTimeMillis()) {
                TaskReminderScheduler.scheduleReminder(
                    context = appContext,
                    taskId = task.id,
                    title = task.title,
                    body = "${task.priorityEnum.label} priority reminder",
                    triggerAtMillis = task.reminderAtMillis
                )
            }

            var outcome = CompletionOutcome()
            if (nextCompleted && profile != null) {
                val xpGain = task.xpReward.coerceIn(10L, 500L)
                val coinGain = (xpGain / 3L).coerceAtLeast(8L)
                val newXp = (profile.xp + xpGain).coerceAtMost(10_000_000L)
                val newLevel = computeLevel(newXp)
                val newCoins = (profile.calmCoins + coinGain).coerceAtMost(1_000_000L)
                val newTasksCompleted = (profile.totalTasksCompleted + 1L).coerceAtMost(1_000_000L)
                val newCortisol = (profile.cortisolScore - 3L).coerceIn(8L, 100L)

                val existingBadges = profile.unlockedBadges.toMutableSet()
                val newBadges = mutableListOf<String>()
                if (newTasksCompleted >= 1L && existingBadges.add("FIRST_TASK")) newBadges += "FIRST_TASK"
                if (newTasksCompleted >= 5L && existingBadges.add("FIVE_DONE")) newBadges += "FIVE_DONE"

                userDoc(uid).update(
                    mapOf(
                        "xp" to newXp,
                        "level" to newLevel,
                        "calmCoins" to newCoins,
                        "totalTasksCompleted" to newTasksCompleted,
                        "cortisolScore" to newCortisol,
                        "unlockedBadges" to existingBadges.take(30),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                outcome = CompletionOutcome(
                    xpGained = xpGain,
                    coinsGained = coinGain,
                    leveledUpTo = if (newLevel > profile.level) newLevel else null,
                    newlyUnlockedBadges = newBadges
                )
            }
            Result.success(outcome)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(userId: String, taskId: String): Result<Unit> {
        var path = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireMatchedUserId(userId)
            require(taskId.isNotBlank()) { "Task ID is missing." }
            val ref = userDoc(uid).collection("tasks").document(taskId)
            path = ref.path
            ref.delete().await()
            TaskReminderScheduler.cancelReminder(appContext, taskId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveNote(
        userId: String,
        existingNoteId: String?,
        title: String,
        content: String,
        tag: NoteTag,
        pinned: Boolean,
        linkedTaskTitle: String
    ): Result<Unit> {
        var path = "users/$userId/notes"
        return try {
            val uid = requireMatchedUserId(userId)
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Note title cannot be blank." }
            val cleanContent = content.trim().take(4000)
            val cleanLinked = linkedTaskTitle.trim().take(140)

            val colRef = userDoc(uid).collection("notes")
            val docRef = if (!existingNoteId.isNullOrBlank()) colRef.document(existingNoteId) else colRef.document()
            path = docRef.path

            if (existingNoteId.isNullOrBlank()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to cleanTitle,
                        "content" to cleanContent,
                        "tag" to tag.name,
                        "pinned" to pinned,
                        "linkedTaskTitle" to cleanLinked,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "content" to cleanContent,
                        "tag" to tag.name,
                        "pinned" to pinned,
                        "linkedTaskTitle" to cleanLinked,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleNotePin(userId: String, note: CalmNote): Result<Unit> {
        var path = "users/$userId/notes/${note.id}"
        return try {
            val uid = requireMatchedUserId(userId)
            val docRef = userDoc(uid).collection("notes").document(note.id)
            path = docRef.path
            docRef.update(
                mapOf(
                    "pinned" to !note.pinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(userId: String, noteId: String): Result<Unit> {
        var path = "users/$userId/notes/$noteId"
        return try {
            val uid = requireMatchedUserId(userId)
            val docRef = userDoc(uid).collection("notes").document(noteId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun logCortisolCheckIn(
        userId: String,
        level: Int,
        mood: CortisolMood,
        trigger: String,
        note: String,
        profile: UserProfile?,
        isBreathingRecovery: Boolean = false
    ): Result<CompletionOutcome> {
        var path = "users/$userId/cortisolLogs"
        return try {
            val uid = requireMatchedUserId(userId)
            val clampedLevel = level.coerceIn(0, 100).toLong()
            val cleanTrigger = trigger.trim().take(80).ifEmpty { "Manual Check-in" }
            val cleanNote = note.trim().take(300)
            val docRef = userDoc(uid).collection("cortisolLogs").document()
            path = docRef.path

            docRef.set(
                mapOf(
                    "userId" to uid,
                    "level" to clampedLevel,
                    "trigger" to cleanTrigger,
                    "mood" to mood.name,
                    "note" to cleanNote,
                    "loggedAtMillis" to System.currentTimeMillis().coerceAtLeast(1L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            var outcome = CompletionOutcome()
            if (profile != null) {
                val xpGain = if (isBreathingRecovery) 35L else 15L
                val coinGain = if (isBreathingRecovery) 15L else 5L
                val newXp = (profile.xp + xpGain).coerceAtMost(10_000_000L)
                val newLevel = computeLevel(newXp)
                val newCoins = (profile.calmCoins + coinGain).coerceAtMost(1_000_000L)

                val existingBadges = profile.unlockedBadges.toMutableSet()
                val newBadges = mutableListOf<String>()
                if ((isBreathingRecovery || clampedLevel <= 40L) && existingBadges.add("CORTISOL_TAMER")) {
                    newBadges += "CORTISOL_TAMER"
                }

                userDoc(uid).update(
                    mapOf(
                        "cortisolScore" to clampedLevel,
                        "xp" to newXp,
                        "level" to newLevel,
                        "calmCoins" to newCoins,
                        "unlockedBadges" to existingBadges.take(30),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                outcome = CompletionOutcome(
                    xpGained = xpGain,
                    coinsGained = coinGain,
                    leveledUpTo = if (newLevel > profile.level) newLevel else null,
                    newlyUnlockedBadges = newBadges
                )
            }
            Result.success(outcome)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordFocusSession(
        userId: String,
        taskTitle: String,
        durationMinutes: Int,
        completed: Boolean,
        breathingEnabled: Boolean,
        profile: UserProfile?
    ): Result<CompletionOutcome> {
        var path = "users/$userId/focusSessions"
        return try {
            val uid = requireMatchedUserId(userId)
            val clampedMinutes = durationMinutes.coerceIn(1, 240).toLong()
            val xpGain = ((clampedMinutes * 3L) + if (completed) 25L else 5L).coerceIn(0L, 1000L)
            val coinGain = (xpGain / 3L).coerceAtLeast(5L)
            val cortisolDelta = when {
                breathingEnabled && completed -> -14L
                completed -> -8L
                else -> -3L
            }.coerceIn(-40L, 40L)

            val docRef = userDoc(uid).collection("focusSessions").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "taskTitle" to taskTitle.trim().take(140).ifEmpty { "Deep Work Flow" },
                    "durationMinutes" to clampedMinutes,
                    "completed" to completed,
                    "cortisolDelta" to cortisolDelta,
                    "xpEarned" to xpGain,
                    "startedAtMillis" to System.currentTimeMillis().coerceAtLeast(1L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            var outcome = CompletionOutcome(xpGained = xpGain, coinsGained = coinGain)
            if (profile != null) {
                val newXp = (profile.xp + xpGain).coerceAtMost(10_000_000L)
                val newLevel = computeLevel(newXp)
                val newCoins = (profile.calmCoins + coinGain).coerceAtMost(1_000_000L)
                val newFocusTotal = (profile.totalFocusMinutes + clampedMinutes).coerceAtMost(10_000_000L)
                val newCortisol = (profile.cortisolScore + cortisolDelta).coerceIn(8L, 100L)

                val existingBadges = profile.unlockedBadges.toMutableSet()
                val newBadges = mutableListOf<String>()
                if (completed && existingBadges.add("DEEP_DIVER")) newBadges += "DEEP_DIVER"
                if (newFocusTotal >= 60L && existingBadges.add("HOUR_FLOW")) newBadges += "HOUR_FLOW"

                userDoc(uid).update(
                    mapOf(
                        "xp" to newXp,
                        "level" to newLevel,
                        "calmCoins" to newCoins,
                        "totalFocusMinutes" to newFocusTotal,
                        "cortisolScore" to newCortisol,
                        "unlockedBadges" to existingBadges.take(30),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                outcome = CompletionOutcome(
                    xpGained = xpGain,
                    coinsGained = coinGain,
                    leveledUpTo = if (newLevel > profile.level) newLevel else null,
                    newlyUnlockedBadges = newBadges
                )
            }
            Result.success(outcome)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addCustomReward(
        userId: String,
        title: String,
        costCoins: Long,
        iconKey: String
    ): Result<Unit> {
        var path = "users/$userId/rewards"
        return try {
            val uid = requireMatchedUserId(userId)
            val cleanTitle = title.trim().take(80)
            require(cleanTitle.isNotEmpty()) { "Reward title cannot be blank." }
            val clampedCost = costCoins.coerceIn(10L, 10000L)
            val cleanIcon = iconKey.trim().take(30).ifEmpty { "coffee" }

            val docRef = userDoc(uid).collection("rewards").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "costCoins" to clampedCost,
                    "iconKey" to cleanIcon,
                    "redeemedCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun redeemReward(
        userId: String,
        reward: RewardItem,
        profile: UserProfile
    ): Result<CompletionOutcome> {
        var path = "users/$userId/rewards/${reward.id}"
        return try {
            val uid = requireMatchedUserId(userId)
            require(profile.calmCoins >= reward.costCoins) { "Not enough Calm Coins yet." }

            val rewardRef = userDoc(uid).collection("rewards").document(reward.id)
            path = rewardRef.path
            rewardRef.update(
                mapOf(
                    "redeemedCount" to (reward.redeemedCount + 1L).coerceAtMost(10000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val existingBadges = profile.unlockedBadges.toMutableSet()
            val newBadges = mutableListOf<String>()
            if (existingBadges.add("REWARD_CLAIMER")) newBadges += "REWARD_CLAIMER"

            val nextCoins = (profile.calmCoins - reward.costCoins).coerceAtLeast(0L)
            val nextCortisol = (profile.cortisolScore - 4L).coerceIn(8L, 100L)

            userDoc(uid).update(
                mapOf(
                    "calmCoins" to nextCoins,
                    "cortisolScore" to nextCortisol,
                    "unlockedBadges" to existingBadges.take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(CompletionOutcome(newlyUnlockedBadges = newBadges))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteReward(userId: String, rewardId: String): Result<Unit> {
        var path = "users/$userId/rewards/$rewardId"
        return try {
            val uid = requireMatchedUserId(userId)
            val ref = userDoc(uid).collection("rewards").document(rewardId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun syncWidgetSnapshot(
        tasks: List<CalmTask>,
        profile: UserProfile?,
        logs: List<CortisolLog>,
        sessions: List<FocusSession>
    ) {
        try {
            val insight = CortisolEngine.analyze(tasks, logs.firstOrNull(), sessions)
            val pending = tasks
                .filter { !it.completed }
                .sortedWith(
                    compareByDescending<CalmTask> { it.priorityEnum.weight }
                        .thenBy { if (it.dueAtMillis == 0L) Long.MAX_VALUE else it.dueAtMillis }
                )
            val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
            val previews = pending.take(3).map { task ->
                val dueLabel = if (task.dueAtMillis > 0L) formatter.format(Date(task.dueAtMillis)) else task.priorityEnum.label
                WidgetTaskPreview(
                    title = task.title,
                    priorityLabel = task.priorityEnum.label,
                    dueLabel = dueLabel
                )
            }
            WidgetSnapshotStore.saveSnapshot(
                appContext,
                WidgetSnapshot(
                    cortisolScore = insight.score,
                    cortisolZone = insight.zoneLabel,
                    streakDays = profile?.streakDays ?: 1L,
                    xp = profile?.xp ?: 0L,
                    level = profile?.level ?: 1L,
                    pendingCount = pending.size,
                    topTasks = previews,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
            CalmFlowWidget().updateAll(appContext)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Widget update should never crash main app
        }
    }

    private fun computeLevel(xp: Long): Long {
        var level = 1L
        var remaining = xp
        while (remaining >= GamificationCatalog.xpForNextLevel(level) && level < 1000L) {
            remaining -= GamificationCatalog.xpForNextLevel(level)
            level++
        }
        return level
    }

    private fun todayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun isYesterday(lastDate: String, today: String): Boolean {
        if (lastDate.isBlank()) return false
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d1 = fmt.parse(lastDate)?.time ?: return false
            val d2 = fmt.parse(today)?.time ?: return false
            val diffDays = (d2 - d1) / 86_400_000L
            diffDays == 1L
        } catch (_: Exception) {
            false
        }
    }
}
