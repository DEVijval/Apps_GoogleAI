package com.example.widget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.calmFlowDataStore by preferencesDataStore(name = "calmflow_widget_snapshot")

data class WidgetTaskPreview(
    val title: String,
    val priorityLabel: String,
    val dueLabel: String
)

data class WidgetSnapshot(
    val cortisolScore: Int = 32,
    val cortisolZone: String = "Optimal Calm",
    val streakDays: Long = 1L,
    val xp: Long = 0L,
    val level: Long = 1L,
    val pendingCount: Int = 0,
    val topTasks: List<WidgetTaskPreview> = emptyList(),
    val updatedAtMillis: Long = 0L
)

object WidgetSnapshotStore {
    private val KEY_CORTISOL = intPreferencesKey("cortisol_score")
    private val KEY_ZONE = stringPreferencesKey("cortisol_zone")
    private val KEY_STREAK = longPreferencesKey("streak_days")
    private val KEY_XP = longPreferencesKey("xp")
    private val KEY_LEVEL = longPreferencesKey("level")
    private val KEY_PENDING = intPreferencesKey("pending_count")
    private val KEY_UPDATED = longPreferencesKey("updated_at")
    private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")

    private val KEY_T1_TITLE = stringPreferencesKey("t1_title")
    private val KEY_T1_PRIO = stringPreferencesKey("t1_prio")
    private val KEY_T1_DUE = stringPreferencesKey("t1_due")
    private val KEY_T2_TITLE = stringPreferencesKey("t2_title")
    private val KEY_T2_PRIO = stringPreferencesKey("t2_prio")
    private val KEY_T2_DUE = stringPreferencesKey("t2_due")
    private val KEY_T3_TITLE = stringPreferencesKey("t3_title")
    private val KEY_T3_PRIO = stringPreferencesKey("t3_prio")
    private val KEY_T3_DUE = stringPreferencesKey("t3_due")

    fun darkModeFlow(context: Context): Flow<Boolean> =
        context.applicationContext.calmFlowDataStore.data.map { prefs ->
            prefs[KEY_DARK_MODE] ?: true
        }

    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        context.applicationContext.calmFlowDataStore.edit { prefs ->
            prefs[KEY_DARK_MODE] = enabled
        }
    }

    suspend fun saveSnapshot(context: Context, snapshot: WidgetSnapshot) {
        context.applicationContext.calmFlowDataStore.edit { prefs ->
            prefs[KEY_CORTISOL] = snapshot.cortisolScore
            prefs[KEY_ZONE] = snapshot.cortisolZone
            prefs[KEY_STREAK] = snapshot.streakDays
            prefs[KEY_XP] = snapshot.xp
            prefs[KEY_LEVEL] = snapshot.level
            prefs[KEY_PENDING] = snapshot.pendingCount
            prefs[KEY_UPDATED] = snapshot.updatedAtMillis

            val t1 = snapshot.topTasks.getOrNull(0)
            prefs[KEY_T1_TITLE] = t1?.title.orEmpty()
            prefs[KEY_T1_PRIO] = t1?.priorityLabel.orEmpty()
            prefs[KEY_T1_DUE] = t1?.dueLabel.orEmpty()

            val t2 = snapshot.topTasks.getOrNull(1)
            prefs[KEY_T2_TITLE] = t2?.title.orEmpty()
            prefs[KEY_T2_PRIO] = t2?.priorityLabel.orEmpty()
            prefs[KEY_T2_DUE] = t2?.dueLabel.orEmpty()

            val t3 = snapshot.topTasks.getOrNull(2)
            prefs[KEY_T3_TITLE] = t3?.title.orEmpty()
            prefs[KEY_T3_PRIO] = t3?.priorityLabel.orEmpty()
            prefs[KEY_T3_DUE] = t3?.dueLabel.orEmpty()
        }
    }

    suspend fun readSnapshot(context: Context): WidgetSnapshot {
        val prefs = context.applicationContext.calmFlowDataStore.data.first()
        val tasks = buildList {
            val t1 = prefs[KEY_T1_TITLE].orEmpty()
            if (t1.isNotBlank()) add(WidgetTaskPreview(t1, prefs[KEY_T1_PRIO].orEmpty(), prefs[KEY_T1_DUE].orEmpty()))
            val t2 = prefs[KEY_T2_TITLE].orEmpty()
            if (t2.isNotBlank()) add(WidgetTaskPreview(t2, prefs[KEY_T2_PRIO].orEmpty(), prefs[KEY_T2_DUE].orEmpty()))
            val t3 = prefs[KEY_T3_TITLE].orEmpty()
            if (t3.isNotBlank()) add(WidgetTaskPreview(t3, prefs[KEY_T3_PRIO].orEmpty(), prefs[KEY_T3_DUE].orEmpty()))
        }
        return WidgetSnapshot(
            cortisolScore = prefs[KEY_CORTISOL] ?: 32,
            cortisolZone = prefs[KEY_ZONE] ?: "Optimal Calm",
            streakDays = prefs[KEY_STREAK] ?: 1L,
            xp = prefs[KEY_XP] ?: 0L,
            level = prefs[KEY_LEVEL] ?: 1L,
            pendingCount = prefs[KEY_PENDING] ?: 0,
            topTasks = tasks,
            updatedAtMillis = prefs[KEY_UPDATED] ?: 0L
        )
    }
}
