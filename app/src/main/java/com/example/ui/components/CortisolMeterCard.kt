package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.CortisolInsight
import com.example.model.CortisolMood
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralStress
import com.example.ui.theme.EmeraldRecovery
import com.example.ui.theme.IndigoCalm
import com.example.ui.theme.TealGlow
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CortisolMeterCard(
    insight: CortisolInsight,
    onOpenCheckIn: () -> Unit,
    onStartBreathingReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateFloatAsState(
        targetValue = insight.score.toFloat(),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "cortisol_score"
    )

    val zoneColor = when {
        insight.score <= 32 -> TealGlow
        insight.score <= 55 -> IndigoCalm
        insight.score <= 75 -> AmberGold
        else -> CoralStress
    }

    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = zoneColor.copy(alpha = 0.28f),
                shape = MaterialTheme.shapes.large
            )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            zoneColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(300f, 80f),
                        radius = 520f
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CORTISOL LEVEL METER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = insight.zoneLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        color = zoneColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = zoneColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = insight.ngPerDlEstimate,
                        style = MaterialTheme.typography.labelMedium,
                        color = zoneColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(132.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        val strokeWidth = 14.dp.toPx()
                        val startAngle = 145f
                        val totalSweep = 250f
                        val arcSize = Size(size.width, size.height)

                        drawArc(
                            color = trackColor,
                            startAngle = startAngle,
                            sweepAngle = totalSweep,
                            useCenter = false,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        val progressSweep = (animatedScore / 100f) * totalSweep
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(TealGlow, IndigoCalm, AmberGold, CoralStress)
                            ),
                            startAngle = startAngle,
                            sweepAngle = progressSweep,
                            useCenter = false,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        val angleRad = Math.toRadians((startAngle + progressSweep).toDouble())
                        val radius = size.width / 2f
                        val cx = size.width / 2f + (radius * cos(angleRad)).toFloat()
                        val cy = size.height / 2f + (radius * sin(angleRad)).toFloat()
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = zoneColor,
                            radius = 3.5.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${animatedScore.roundToInt()}",
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "/ 100",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = insight.guidance,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (insight.overdueCount > 0) {
                            DriverBadge(
                                text = "${insight.overdueCount} overdue",
                                color = CoralStress
                            )
                        }
                        if (insight.highLoadCount > 0) {
                            DriverBadge(
                                text = "${insight.highLoadCount} heavy",
                                color = AmberGold
                            )
                        }
                        if (insight.overdueCount == 0 && insight.highLoadCount == 0) {
                            DriverBadge(
                                text = "Balanced Load",
                                color = EmeraldRecovery
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenCheckIn,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("cortisol_checkin_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Stress", maxLines = 1)
                }

                Button(
                    onClick = onStartBreathingReset,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("breathing_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Air,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Breath Reset", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun DriverBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (color == CoralStress) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CortisolCheckInSheet(
    currentScore: Int,
    onDismiss: () -> Unit,
    onSubmit: (level: Int, mood: CortisolMood, trigger: String, note: String) -> Unit
) {
    var selectedMood by remember { mutableStateOf(CortisolMood.FOCUSED) }
    var levelFloat by remember { mutableFloatStateOf(currentScore.toFloat()) }
    var trigger by remember { mutableStateOf("Midday Check-in") }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Log Cortisol & Stress State",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Calibrate your meter based on how your nervous system feels right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CortisolMood.entries.take(3).forEach { mood ->
                    FilterChip(
                        selected = selectedMood == mood,
                        onClick = {
                            selectedMood = mood
                            levelFloat = mood.baselineLevel.toFloat()
                        },
                        label = { Text(mood.label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CortisolMood.entries.drop(3).forEach { mood ->
                    FilterChip(
                        selected = selectedMood == mood,
                        onClick = {
                            selectedMood = mood
                            levelFloat = mood.baselineLevel.toFloat()
                        },
                        label = { Text(mood.label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Perceived Stress / Cortisol", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${levelFloat.roundToInt()} / 100",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = levelFloat,
                onValueChange = { levelFloat = it },
                valueRange = 5f..98f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            OutlinedTextField(
                value = trigger,
                onValueChange = { trigger = it },
                label = { Text("Trigger (e.g., Deadline, Caffeine, Walk)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Optional reflection") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    onSubmit(levelFloat.roundToInt(), selectedMood, trigger, note)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_cortisol_checkin")
            ) {
                Text("Save Check-In (+15 XP)")
            }
        }
    }
}

@Composable
fun BreathingResetDialog(
    currentScore: Int,
    onDismiss: () -> Unit,
    onCompleted: (newScore: Int) -> Unit
) {
    // 4-4-4-4 Box Breathing: 2 full cycles (32s) for snappy, rewarding interactive recovery
    val totalSeconds = 32
    var secondsLeft by remember { mutableIntStateOf(totalSeconds) }
    var completed by remember { mutableStateOf(false) }

    val elapsed = totalSeconds - secondsLeft
    val cyclePos = elapsed % 16
    val phaseIndex = cyclePos / 4
    val phaseLabel = when (phaseIndex) {
        0 -> "Inhale Deeply"
        1 -> "Hold Still"
        2 -> "Exhale Slowly"
        else -> "Rest Empty"
    }
    val targetScale = when (phaseIndex) {
        0, 1 -> 1.0f
        else -> 0.56f
    }
    val orbScale by animateFloatAsState(
        targetValue = if (completed) 0.8f else targetScale,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "breath_orb"
    )

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
        completed = true
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, TealGlow.copy(alpha = 0.35f), MaterialTheme.shapes.extraLarge)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("4-4-4-4 Cortisol Reset", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close breathing exercise")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxRadius = size.minDimension / 2f
                        drawCircle(
                            color = TealGlow.copy(alpha = 0.12f),
                            radius = maxRadius
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    TealGlow.copy(alpha = 0.65f),
                                    IndigoCalm.copy(alpha = 0.25f)
                                )
                            ),
                            radius = maxRadius * orbScale
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (completed) "Calm Restored" else phaseLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        if (!completed) {
                            Text(
                                text = "${secondsLeft}s",
                                style = MaterialTheme.typography.labelMedium,
                                color = TealGlow
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                val projectedDrop = (currentScore - 14).coerceAtLeast(14)
                Text(
                    text = if (completed) {
                        "Vagus nerve stimulated. Estimated cortisol reduced from $currentScore to $projectedDrop."
                    } else {
                        "Follow the expanding orb to down-regulate stress hormones."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onCompleted(projectedDrop)
                        onDismiss()
                    },
                    enabled = completed || secondsLeft <= 20,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("complete_breathing_reset")
                ) {
                    Text(if (completed) "Log Recovery (+35 XP)" else "Finish Early & Log")
                }
            }
        }
    }
}
