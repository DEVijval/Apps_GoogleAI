package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.roundToInt

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class TaskPriority(val label: String, val weight: Int, val xpBase: Long) {
    CRITICAL("Critical", 4, 100L),
    HIGH("High", 3, 70L),
    BALANCED("Balanced", 2, 45L),
    LOW("Low", 1, 25L);

    companion object {
        fun fromRaw(raw: String): TaskPriority =
            entries.firstOrNull { it.name == raw } ?: BALANCED
    }
}

enum class TaskCategory(val label: String) {
    DEEP_WORK("Deep Work"),
    ADMIN("Admin"),
    WELLNESS("Wellness"),
    CREATIVE("Creative"),
    PERSONAL("Personal");

    companion object {
        fun fromRaw(raw: String): TaskCategory =
            entries.firstOrNull { it.name == raw } ?: DEEP_WORK
    }
}

enum class NoteTag(val label: String) {
    BRAIN_DUMP("Brain Dump"),
    IDEA("Idea"),
    REFLECTION("Reflection"),
    MEETING("Meeting"),
    GRATITUDE("Gratitude");

    companion object {
        fun fromRaw(raw: String): NoteTag =
            entries.firstOrNull { it.name == raw } ?: BRAIN_DUMP
    }
}

enum class CortisolMood(val label: String, val baselineLevel: Int) {
    SERENE("Serene", 22),
    FOCUSED("Focused", 36),
    RECOVERING("Recovering", 42),
    TENSE("Tense", 68),
    OVERWHELMED("Overwhelmed", 86);

    companion object {
        fun fromRaw(raw: String): CortisolMood =
            entries.firstOrNull { it.name == raw } ?: FOCUSED
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Flow Seeker",
    val xp: Long = 0L,
    val level: Long = 1L,
    val calmCoins: Long = 50L,
    val streakDays: Long = 1L,
    val totalFocusMinutes: Long = 0L,
    val totalTasksCompleted: Long = 0L,
    val cortisolScore: Long = 35L,
    val unlockedBadges: List<String> = emptyList(),
    val lastActiveDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CalmTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val priority: String = TaskPriority.BALANCED.name,
    val cognitiveLoad: Long = 2L,
    val category: String = TaskCategory.DEEP_WORK.name,
    val completed: Boolean = false,
    val dueAtMillis: Long = 0L,
    val reminderAtMillis: Long = 0L,
    val estimatedMinutes: Long = 25L,
    val xpReward: Long = 50L,
    val completedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val priorityEnum: TaskPriority get() = TaskPriority.fromRaw(priority)
    val categoryEnum: TaskCategory get() = TaskCategory.fromRaw(category)
}

@Keep
@IgnoreExtraProperties
data class CalmNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val tag: String = NoteTag.BRAIN_DUMP.name,
    val pinned: Boolean = false,
    val linkedTaskTitle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val tagEnum: NoteTag get() = NoteTag.fromRaw(tag)
}

@Keep
@IgnoreExtraProperties
data class CortisolLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val level: Long = 35L,
    val trigger: String = "",
    val mood: String = CortisolMood.FOCUSED.name,
    val note: String = "",
    val loggedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val moodEnum: CortisolMood get() = CortisolMood.fromRaw(mood)
}

@Keep
@IgnoreExtraProperties
data class FocusSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val taskTitle: String = "",
    val durationMinutes: Long = 25L,
    val completed: Boolean = true,
    val cortisolDelta: Long = -6L,
    val xpEarned: Long = 60L,
    val startedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RewardItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val costCoins: Long = 50L,
    val iconKey: String = "coffee",
    val redeemedCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class BadgeInfo(
    val id: String,
    val title: String,
    val description: String,
    val iconKey: String
)

data class CortisolInsight(
    val score: Int,
    val zoneLabel: String,
    val guidance: String,
    val ngPerDlEstimate: String,
    val overdueCount: Int,
    val highLoadCount: Int
)

object GamificationCatalog {
    val badges = listOf(
        BadgeInfo("FIRST_TASK", "Momentum Spark", "Complete your first prioritized task", "bolt"),
        BadgeInfo("DEEP_DIVER", "Deep Diver", "Finish a Deep Work focus session", "timer"),
        BadgeInfo("CORTISOL_TAMER", "Cortisol Tamer", "Lower cortisol via a breathing reset or check-in", "spa"),
        BadgeInfo("FIVE_DONE", "Flow Operator", "Complete 5 tasks across your workspace", "verified"),
        BadgeInfo("HOUR_FLOW", "Zen Century", "Accumulate 60+ minutes of deep work", "psychology"),
        BadgeInfo("REWARD_CLAIMER", "Balanced Mind", "Redeem a self-care reward with Calm Coins", "redeem")
    )

    fun rankTitle(level: Long): String = when {
        level >= 15 -> "Sovereign of Stillness"
        level >= 10 -> "Cortisol Alchemist"
        level >= 6 -> "Deep Flow Architect"
        level >= 3 -> "Rhythm Weaver"
        else -> "Calm Apprentice"
    }

    fun xpForNextLevel(level: Long): Long = level * 200L
}

object CortisolEngine {
    fun analyze(
        tasks: List<CalmTask>,
        latestLog: CortisolLog?,
        recentSessions: List<FocusSession>,
        nowMillis: Long = System.currentTimeMillis()
    ): CortisolInsight {
        val pending = tasks.filter { !it.completed }
        val overdueCount = pending.count { it.dueAtMillis in 1 until nowMillis }
        val highLoadCount = pending.count { it.cognitiveLoad >= 4 || it.priorityEnum == TaskPriority.CRITICAL }

        val recentCheckIn = latestLog?.takeIf { nowMillis - it.loggedAtMillis < 6 * 3600_000L }
        val baseScore = recentCheckIn?.level?.toInt() ?: 30

        val taskPressure = (pending.size * 2) + (overdueCount * 9) + (highLoadCount * 4)
        val recentRecovery = recentSessions
            .filter { nowMillis - it.startedAtMillis < 6 * 3600_000L && it.completed }
            .sumOf { (-it.cortisolDelta).coerceAtLeast(0L).toInt() }
            .coerceAtMost(24)

        val computed = if (recentCheckIn != null && nowMillis - recentCheckIn.loggedAtMillis < 20 * 60_000L) {
            // Respect very recent explicit check-in or breathing reset closely
            ((baseScore * 0.78) + (taskPressure * 0.22) - (recentRecovery * 0.2)).roundToInt()
        } else {
            (baseScore + (taskPressure * 0.55) - (recentRecovery * 0.65)).roundToInt()
        }.coerceIn(8, 98)

        val ngPerDl = String.format("%.1f mcg/dL", 4.5 + (computed / 100.0) * 19.5)

        val (zone, guidance) = when {
            computed <= 32 -> "Optimal Calm" to "Cortisol is low and steady. Ideal window for creative or strategic flow."
            computed <= 55 -> "Alert Focus" to "Healthy eustress zone. Tackle 1 high-priority task, then take a 5m breather."
            computed <= 75 -> "Elevated Load" to "Deadline pressure is rising. Offload thoughts to Notes or run a 25m single-task sprint."
            else -> "Cortisol Spike" to "High stress detected ($overdueCount overdue, $highLoadCount heavy tasks). Pause for a 4-4-4-4 breathing reset."
        }

        return CortisolInsight(
            score = computed,
            zoneLabel = zone,
            guidance = guidance,
            ngPerDlEstimate = ngPerDl,
            overdueCount = overdueCount,
            highLoadCount = highLoadCount
        )
    }
}
