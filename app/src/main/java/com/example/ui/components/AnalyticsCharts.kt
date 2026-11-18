package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.model.CalmTask
import com.example.model.CortisolLog
import com.example.model.FocusSession
import com.example.model.TaskCategory
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralStress
import com.example.ui.theme.EmeraldRecovery
import com.example.ui.theme.IndigoCalm
import com.example.ui.theme.TealGlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DeepWorkWeeklyBarChart(
    sessions: List<FocusSession>,
    modifier: Modifier = Modifier
) {
    val dayBuckets = remember(sessions) {
        val fmt = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = Calendar.getInstance()
        val labels = mutableListOf<String>()
        val mins = mutableListOf<Int>()
        for (offset in 6 downTo 0) {
            val dayStart = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -offset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + 86_400_000L
            cal.timeInMillis = dayStart
            labels += fmt.format(Date(dayStart)).take(2)
            val totalMins = sessions
                .filter { it.startedAtMillis in dayStart until dayEnd }
                .sumOf { it.durationMinutes.toInt() }
            mins += totalMins
        }
        labels to mins
    }

    val labels = dayBuckets.first
    val values = dayBuckets.second
    val maxVal = (values.maxOrNull() ?: 60).coerceAtLeast(30)
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("DEEP WORK CADENCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("7-Day Focus Minutes", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "${values.sum()}m total",
                    style = MaterialTheme.typography.labelMedium,
                    color = TealGlow
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val barCount = values.size
                val slotWidth = size.width / barCount
                val barWidth = (slotWidth * 0.48f).coerceAtMost(28.dp.toPx())

                // Subtle horizontal baseline
                drawLine(
                    color = gridColor,
                    start = Offset(0f, size.height - 2.dp.toPx()),
                    end = Offset(size.width, size.height - 2.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )

                values.forEachIndexed { idx, mins ->
                    val ratio = (mins.toFloat() / maxVal.toFloat()).coerceIn(0.06f, 1f)
                    val barHeight = (size.height - 8.dp.toPx()) * ratio
                    val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                    val top = size.height - barHeight

                    drawRoundRect(
                        brush = if (mins > 0) {
                            Brush.verticalGradient(listOf(TealGlow, IndigoCalm))
                        } else {
                            Brush.verticalGradient(listOf(gridColor, gridColor))
                        },
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                labels.forEach { lbl ->
                    Text(
                        text = lbl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun CortisolTrendChart(
    logs: List<CortisolLog>,
    currentScore: Int,
    modifier: Modifier = Modifier
) {
    val points = remember(logs, currentScore) {
        val recent = logs.take(10).reversed().map { it.level.toInt() }
        if (recent.size >= 2) recent else listOf(currentScore, currentScore)
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("NERVOUS SYSTEM RHYTHM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Cortisol Trend Curve", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "${points.last()} / 100",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (points.last() <= 45) TealGlow else AmberGold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width
                val coords = points.mapIndexed { idx, value ->
                    val x = idx * stepX
                    val y = size.height - ((value.coerceIn(5, 98) / 100f) * (size.height - 16.dp.toPx())) - 8.dp.toPx()
                    Offset(x, y)
                }

                val linePath = Path().apply {
                    coords.forEachIndexed { idx, pt ->
                        if (idx == 0) moveTo(pt.x, pt.y)
                        else {
                            val prev = coords[idx - 1]
                            val midX = (prev.x + pt.x) / 2f
                            cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                        }
                    }
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(coords.last().x, size.height)
                    lineTo(coords.first().x, size.height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(IndigoCalm.copy(alpha = 0.35f), Color.Transparent)
                    )
                )

                drawPath(
                    path = linePath,
                    brush = Brush.horizontalGradient(listOf(TealGlow, IndigoCalm, AmberGold)),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                coords.forEach { pt ->
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = TealGlow,
                        radius = 2.5.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDistributionCard(
    tasks: List<CalmTask>,
    modifier: Modifier = Modifier
) {
    val breakdown = remember(tasks) {
        val grouped = tasks.groupingBy { it.categoryEnum }.eachCount()
        TaskCategory.entries.map { cat ->
            cat to (grouped[cat] ?: 0)
        }.filter { it.second > 0 }
    }
    val total = breakdown.sumOf { it.second }.coerceAtLeast(1)

    fun colorFor(cat: TaskCategory): Color = when (cat) {
        TaskCategory.DEEP_WORK -> TealGlow
        TaskCategory.CREATIVE -> IndigoCalm
        TaskCategory.WELLNESS -> EmeraldRecovery
        TaskCategory.ADMIN -> AmberGold
        TaskCategory.PERSONAL -> CoralStress
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("WORKLOAD BALANCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Cognitive Category Mix", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            if (breakdown.isEmpty()) {
                Text(
                    text = "Add tasks across Deep Work, Wellness, or Creative categories to inspect your cognitive mix.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(96.dp)) {
                        var startAngle = -90f
                        breakdown.forEach { (cat, count) ->
                            val sweep = (count.toFloat() / total.toFloat()) * 360f
                            drawArc(
                                color = colorFor(cat),
                                startAngle = startAngle,
                                sweepAngle = (sweep - 4f).coerceAtLeast(2f),
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                            startAngle += sweep
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        breakdown.forEach { (cat, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorFor(cat),
                                    modifier = Modifier.size(10.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${cat.label} (${count})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
