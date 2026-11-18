package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CalmTask
import com.example.model.TaskCategory
import com.example.model.TaskPriority
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    editingTask: CalmTask?,
    onDismiss: () -> Unit,
    onSave: (
        existingTaskId: String?,
        title: String,
        description: String,
        priority: TaskPriority,
        cognitiveLoad: Int,
        category: TaskCategory,
        dueAtMillis: Long,
        reminderAtMillis: Long,
        estimatedMinutes: Int
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(editingTask) { mutableStateOf(editingTask?.title.orEmpty()) }
    var description by remember(editingTask) { mutableStateOf(editingTask?.description.orEmpty()) }
    var priority by remember(editingTask) { mutableStateOf(editingTask?.priorityEnum ?: TaskPriority.HIGH) }
    var cognitiveLoad by remember(editingTask) { mutableIntStateOf(editingTask?.cognitiveLoad?.toInt() ?: 3) }
    var category by remember(editingTask) { mutableStateOf(editingTask?.categoryEnum ?: TaskCategory.DEEP_WORK) }
    var dueAtMillis by remember(editingTask) { mutableLongStateOf(editingTask?.dueAtMillis ?: 0L) }
    var reminderAtMillis by remember(editingTask) { mutableLongStateOf(editingTask?.reminderAtMillis ?: 0L) }
    var estimatedMinutes by remember(editingTask) { mutableIntStateOf(editingTask?.estimatedMinutes?.toInt() ?: 25) }

    val dateTimeFormat = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }

    fun pickDateTime(initialMillis: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply {
            if (initialMillis > 0L) timeInMillis = initialMillis
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val picked = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onPicked(picked.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTask == null) "New Priority Task" else "Edit Task",
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close task editor")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 140) title = it },
                label = { Text("Task title") },
                placeholder = { Text("e.g., Ship Q4 architecture spec") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 600) description = it },
                label = { Text("Context or next physical action") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_desc_input")
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Priority", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TaskPriority.entries.forEach { item ->
                    FilterChip(
                        selected = priority == item,
                        onClick = { priority = item },
                        label = { Text(item.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Cognitive Load (affects Cortisol Meter)", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { level ->
                    FilterChip(
                        selected = cognitiveLoad == level,
                        onClick = { cognitiveLoad = level },
                        label = { Text("$level") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Category", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TaskCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Focus Duration Estimate", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 25, 50, 90).forEach { mins ->
                    FilterChip(
                        selected = estimatedMinutes == mins,
                        onClick = { estimatedMinutes = mins },
                        label = { Text("${mins}m") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Deadline & Date", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        pickDateTime(dueAtMillis) { picked -> dueAtMillis = picked }
                    },
                    modifier = Modifier.testTag("pick_deadline_button")
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (dueAtMillis > 0L) dateTimeFormat.format(Date(dueAtMillis)) else "Set Deadline"
                    )
                }
                if (dueAtMillis > 0L) {
                    OutlinedButton(onClick = { dueAtMillis = 0L }) {
                        Text("Clear")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Reminder Alert", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val base = if (dueAtMillis > System.currentTimeMillis()) dueAtMillis else System.currentTimeMillis() + 3600_000L
                        pickDateTime(if (reminderAtMillis > 0L) reminderAtMillis else base) { picked ->
                            reminderAtMillis = picked
                        }
                    },
                    modifier = Modifier.testTag("pick_reminder_button")
                ) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (reminderAtMillis > 0L) dateTimeFormat.format(Date(reminderAtMillis)) else "Set Reminder"
                    )
                }
                if (dueAtMillis > System.currentTimeMillis() + 900_000L) {
                    FilterChip(
                        selected = reminderAtMillis == dueAtMillis - 900_000L,
                        onClick = { reminderAtMillis = dueAtMillis - 900_000L },
                        label = { Text("15m before") }
                    )
                }
                if (reminderAtMillis > 0L) {
                    OutlinedButton(onClick = { reminderAtMillis = 0L }) {
                        Text("Off")
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            editingTask?.id,
                            title,
                            description,
                            priority,
                            cognitiveLoad,
                            category,
                            dueAtMillis,
                            reminderAtMillis,
                            estimatedMinutes
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_task_button")
            ) {
                Text(if (editingTask == null) "Add Task" else "Save Changes")
            }
        }
    }
}
