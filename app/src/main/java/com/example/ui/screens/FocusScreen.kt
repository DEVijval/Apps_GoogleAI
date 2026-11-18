package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CalmTask
import com.example.model.FocusSession
import com.example.ui.theme.AmberGold
import com.example.ui.theme.EmeraldRecovery
import com.example.ui.theme.IndigoCalm
import com.example.ui.theme.TealGlow
import com.example.viewmodel.FocusTimerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FocusScreen(
    timerState: FocusTimerState,
    activeTasks: List<CalmTask>,
    sessions: List<FocusSession>,
    onSelectTask: (CalmTask?) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    onFinishEarly: () -> Unit,
    onToggleBreathing: (Boolean) -> Unit
) {
    val totalSeconds = (timerState.durationMinutes * 60).coerceAtLeast(1)
    val progress = (timerState.remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val mins = timerState.remainingSeconds / 60
    val secs = timerState.remainingSeconds % 60
    val timeText = String.format(Locale.US, "%02d:%02d", mins, secs)

    val infiniteTransition = rememberInfiniteTransition(label = "focus_halo")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val fmt = remember { SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
            ) {
                Text("Deep Work Sanctuary", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Single-task immersion lowers cortisol and earns focus XP.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Attached Task Picker
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
            ) {
                Text("Focus Target", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = timerState.selectedTaskId == null,
                        onClick = { onSelectTask(null) },
                        enabled = !timerState.isRunning,
                        label = { Text("Deep Work Flow") }
                    )
                    activeTasks.take(8).forEach { task ->
                        FilterChip(
                            selected = timerState.selectedTaskId == task.id,
                            onClick = { onSelectTask(task) },
                            enabled = !timerState.isRunning,
                            label = { Text(task.title, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Orbital Countdown Card
        item {
            val ringTrack = MaterialTheme.colorScheme.outlineVariant
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .border(1.dp, TealGlow.copy(alpha = 0.25f), MaterialTheme.shapes.extraLarge)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = timerState.selectedTaskTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TealGlow,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier.size(230.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            val haloScale = if (timerState.isRunning && timerState.breathingMode) pulse else 1f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        TealGlow.copy(alpha = if (timerState.isRunning) 0.14f else 0.05f),
                                        Color.Transparent
                                    )
                                ),
                                radius = (size.minDimension / 2f) * haloScale
                            )
                            val stroke = 14.dp.toPx()
                            drawArc(
                                color = ringTrack,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(listOf(TealGlow, IndigoCalm, TealGlow)),
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (timerState.isRunning) {
                                    if (timerState.breathingMode) "Breathe 4-4-4-4" else "In Deep Flow"
                                } else "Ready",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 25, 50, 90).forEach { preset ->
                            FilterChip(
                                selected = timerState.durationMinutes == preset,
                                onClick = { onSelectPreset(preset) },
                                enabled = !timerState.isRunning,
                                label = { Text("${preset}m") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Breathing Pacer Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Air, contentDescription = null, tint = TealGlow, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Cortisol-Lowering Breathing", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Extra -6 cortisol reduction on completion",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = timerState.breathingMode,
                            onCheckedChange = onToggleBreathing
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("timer_reset_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Reset timer")
                        }

                        Button(
                            onClick = onToggleRunning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("timer_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (timerState.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (timerState.isRunning) "Pause" else "Start Focus")
                        }

                        if (timerState.remainingSeconds < totalSeconds) {
                            OutlinedButton(
                                onClick = onFinishEarly,
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("timer_finish_early_button")
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = "Finish and log", tint = EmeraldRecovery)
                            }
                        }
                    }
                }
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
                Text("Recent Deep Work Sessions", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${sessions.size} logged",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (sessions.isEmpty()) {
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
                        Icon(Icons.Rounded.Spa, contentDescription = null, tint = TealGlow, modifier = Modifier.size(34.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Focus Sessions Yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Start a 25m sprint above to build your deep work analytics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(sessions.take(15), key = { it.id }) { session ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(session.taskTitle, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            Text(
                                text = "${session.durationMinutes}m • ${fmt.format(Date(session.startedAtMillis))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TealGlow.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "${session.cortisolDelta} Cortisol",
                                style = MaterialTheme.typography.labelSmall,
                                color = TealGlow,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+${session.xpEarned} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberGold
                        )
                    }
                }
            }
        }
    }
}
