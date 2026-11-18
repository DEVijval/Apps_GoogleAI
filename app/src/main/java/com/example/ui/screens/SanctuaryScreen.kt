package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CalmTask
import com.example.model.GamificationCatalog
import com.example.ui.components.CortisolMeterCard
import com.example.ui.components.TaskRowCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralStress
import com.example.ui.theme.IndigoCalm
import com.example.ui.theme.TealGlow
import com.example.viewmodel.DashboardBundle
import com.example.widget.WidgetPinHelper

@Composable
fun SanctuaryScreen(
    bundle: DashboardBundle,
    onOpenCheckIn: () -> Unit,
    onStartBreathingReset: () -> Unit,
    onOpenNewTask: () -> Unit,
    onOpenNewNote: () -> Unit,
    onOpenFocusTab: () -> Unit,
    onStartTaskFocus: (CalmTask) -> Unit,
    onToggleTask: (CalmTask) -> Unit,
    onEditTask: (CalmTask) -> Unit,
    onDeleteTask: (CalmTask) -> Unit,
    onOpenAllTasks: () -> Unit,
    onPinWidgetResult: (String) -> Unit
) {
    val context = LocalContext.current
    val profile = bundle.profile
    val level = profile?.level ?: 1L
    val xp = profile?.xp ?: 0L
    val coins = profile?.calmCoins ?: 0L
    val streak = profile?.streakDays ?: 1L
    val nextLevelXp = GamificationCatalog.xpForNextLevel(level)
    val levelProgress = ((xp % nextLevelXp).toFloat() / nextLevelXp.toFloat()).coerceIn(0.05f, 1f)

    val topPriorityTasks = remember(bundle.tasks, bundle.cortisolInsight.score) {
        val now = System.currentTimeMillis()
        bundle.tasks
            .filter { !it.completed }
            .sortedWith(
                compareByDescending<CalmTask> {
                    val overdueBoost = if (it.dueAtMillis in 1 until now) 10 else 0
                    // When cortisol is high (>70), gently boost lower-load wins to restore momentum
                    val stressFit = if (bundle.cortisolInsight.score > 70) (6 - it.cognitiveLoad.toInt()) else 0
                    (it.priorityEnum.weight * 3) + overdueBoost + stressFit
                }.thenBy { if (it.dueAtMillis == 0L) Long.MAX_VALUE else it.dueAtMillis }
            )
            .take(3)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 640.dp)) {
                // Header + Gamification Telemetry Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, ${profile?.displayName ?: "Flow Seeker"}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Lv.$level • ${GamificationCatalog.rankTitle(level)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TealGlow
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatChip(Icons.Rounded.LocalFireDepartment, "${streak}d", CoralStress)
                        StatChip(Icons.Rounded.Stars, "$coins", AmberGold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { levelProgress },
                    color = TealGlow,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$xp Total XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${bundle.profile?.totalFocusMinutes ?: 0L}m Deep Work",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            CortisolMeterCard(
                insight = bundle.cortisolInsight,
                onOpenCheckIn = onOpenCheckIn,
                onStartBreathingReset = onStartBreathingReset,
                modifier = Modifier.widthIn(max = 640.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    icon = Icons.Rounded.AddTask,
                    label = "New Task",
                    accent = TealGlow,
                    onClick = onOpenNewTask,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_task")
                )
                QuickActionTile(
                    icon = Icons.Rounded.Timer,
                    label = "Focus",
                    accent = IndigoCalm,
                    onClick = onOpenFocusTab,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_open_focus")
                )
                QuickActionTile(
                    icon = Icons.Rounded.NoteAdd,
                    label = "Brain Dump",
                    accent = AmberGold,
                    onClick = onOpenNewNote,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_note")
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (bundle.cortisolInsight.score > 70) "Gentle Priority Queue" else "Top Priority Queue",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                TextButton(onClick = onOpenAllTasks) {
                    Text("See all (${bundle.tasks.count { !it.completed }})")
                }
            }
        }

        if (topPriorityTasks.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Clear Horizon",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your priority queue is clear. Add a task with a deadline and cognitive load estimate to begin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = onOpenNewTask) {
                            Text("Create Priority Task")
                        }
                    }
                }
            }
        } else {
            items(topPriorityTasks, key = { it.id }) { task ->
                TaskRowCard(
                    task = task,
                    onToggleComplete = { onToggleTask(task) },
                    onStartFocus = { onStartTaskFocus(task) },
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTask(task) },
                    modifier = Modifier.widthIn(max = 640.dp)
                )
            }
        }

        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = null,
                        tint = IndigoCalm,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Home Screen Widget", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Keep cortisol & top 3 tasks pinned on your launcher.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (WidgetPinHelper.isPinSupported(context)) {
                                val requested = WidgetPinHelper.requestPinWidget(context)
                                onPinWidgetResult(
                                    if (requested) "Widget pin prompt sent to your launcher"
                                    else "Long-press your home screen to add the CalmFlow widget"
                                )
                            } else {
                                onPinWidgetResult("Long-press your home screen -> Widgets -> CalmFlow")
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealGlow),
                        modifier = Modifier.testTag("pin_widget_button")
                    ) {
                        Text("Pin")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(icon: ImageVector, value: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}
