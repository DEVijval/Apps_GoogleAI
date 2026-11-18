package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CalmTask
import com.example.model.CortisolEngine
import com.example.model.CortisolLog
import com.example.model.FocusSession
import com.example.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches CalmFlow`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CalmFlow", appName)
    }

    @Test
    fun `cortisol engine reflects overdue pressure and focus recovery`() {
        val now = 1_700_000_000_000L
        val overdueHighTask = CalmTask(
            id = "t1",
            title = "Critical deadline",
            priority = TaskPriority.CRITICAL.name,
            cognitiveLoad = 5L,
            completed = false,
            dueAtMillis = now - 60_000L
        )
        val stressed = CortisolEngine.analyze(
            tasks = listOf(overdueHighTask),
            latestLog = CortisolLog(level = 65L, loggedAtMillis = now - 3_600_000L),
            recentSessions = emptyList(),
            nowMillis = now
        )
        val recovered = CortisolEngine.analyze(
            tasks = emptyList(),
            latestLog = CortisolLog(level = 25L, loggedAtMillis = now - 60_000L),
            recentSessions = listOf(
                FocusSession(durationMinutes = 25L, completed = true, cortisolDelta = -14L, startedAtMillis = now - 600_000L)
            ),
            nowMillis = now
        )
        assertTrue(stressed.score > recovered.score)
        assertEquals(1, stressed.overdueCount)
        assertEquals("Optimal Calm", recovered.zoneLabel)
    }
}
