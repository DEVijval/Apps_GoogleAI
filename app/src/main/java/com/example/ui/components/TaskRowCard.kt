package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CalmTask
import com.example.model.TaskPriority
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralStress
import com.example.ui.theme.EmeraldRecovery
import com.example.ui.theme.IndigoCalm
import com.example.ui.theme.TealGlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskRowCard(
    task: CalmTask,
    onToggleComplete: () -> Unit,
    onStartFocus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = when (task.priorityEnum) {
        TaskPriority.CRITICAL -> CoralStress
        TaskPriority.HIGH -> AmberGold
        TaskPriority.BALANCED -> TealGlow
        TaskPriority.LOW -> IndigoCalm
    }

    val now = System.currentTimeMillis()
    val isOverdue = !task.completed && task.dueAtMillis in 1 until now
    val fmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isOverdue) CoralStress.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium
            )
            .clickable(onClick = onEdit)
            .testTag("task_card_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left priority indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (task.completed) EmeraldRecovery.copy(alpha = 0.4f) else priorityColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 48dp accessible toggle button
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable(onClick = onToggleComplete)
                    .testTag("toggle_task_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.completed) EmeraldRecovery else Color.Transparent
                        )
                        .border(
                            width = 2.dp,
                            color = if (task.completed) EmeraldRecovery else priorityColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.completed) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Mark incomplete",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = priorityColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${task.priorityEnum.label} • L${task.cognitiveLoad}",
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (task.dueAtMillis > 0L) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = if (isOverdue) CoralStress else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isOverdue) "Overdue" else fmt.format(Date(task.dueAtMillis)),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverdue) CoralStress else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (task.reminderAtMillis > now && !task.completed) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "Reminder active",
                            tint = TealGlow,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = "+${task.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberGold
                    )
                }
            }

            if (!task.completed) {
                IconButton(
                    onClick = onStartFocus,
                    modifier = Modifier.testTag("focus_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Start focus timer for task",
                        tint = TealGlow
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
