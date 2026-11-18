package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CalmTask
import com.example.model.TaskPriority
import com.example.ui.components.TaskRowCard
import com.example.viewmodel.TaskFilter
import com.example.viewmodel.TaskSort
import java.util.Calendar

@Composable
fun TasksScreen(
    tasks: List<CalmTask>,
    cortisolScore: Int,
    activeFilter: TaskFilter,
    activeSort: TaskSort,
    onSelectFilter: (TaskFilter) -> Unit,
    onSelectSort: (TaskSort) -> Unit,
    onOpenNewTask: () -> Unit,
    onToggleTask: (CalmTask) -> Unit,
    onStartTaskFocus: (CalmTask) -> Unit,
    onEditTask: (CalmTask) -> Unit,
    onDeleteTask: (CalmTask) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val filteredTasks = remember(tasks, activeFilter, activeSort, cortisolScore) {
        val now = System.currentTimeMillis()
        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        val subset = when (activeFilter) {
            TaskFilter.ALL -> tasks.filter { !it.completed }
            TaskFilter.TODAY -> tasks.filter { !it.completed && it.dueAtMillis in 1..endOfToday }
            TaskFilter.OVERDUE -> tasks.filter { !it.completed && it.dueAtMillis in 1 until now }
            TaskFilter.HIGH_PRIORITY -> tasks.filter {
                !it.completed && (it.priorityEnum == TaskPriority.CRITICAL || it.priorityEnum == TaskPriority.HIGH)
            }
            TaskFilter.COMPLETED -> tasks.filter { it.completed }
        }

        when (activeSort) {
            TaskSort.STRESS_SMART -> subset.sortedWith(
                compareByDescending<CalmTask> {
                    val overdue = if (!it.completed && it.dueAtMillis in 1 until now) 10 else 0
                    val stressAdjustment = if (cortisolScore > 70) (6 - it.cognitiveLoad.toInt()) else 0
                    (it.priorityEnum.weight * 3) + overdue + stressAdjustment
                }.thenBy { if (it.dueAtMillis == 0L) Long.MAX_VALUE else it.dueAtMillis }
            )
            TaskSort.DEADLINE -> subset.sortedBy {
                if (it.dueAtMillis == 0L) Long.MAX_VALUE else it.dueAtMillis
            }
            TaskSort.COGNITIVE_LOAD -> subset.sortedByDescending { it.cognitiveLoad }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenNewTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_task")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Priority Tasks", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "${tasks.count { !it.completed }} active • ${tasks.count { it.completed }} done",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column {
                        OutlinedButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Rounded.Sort, contentDescription = "Sort tasks")
                            Text(" ${activeSort.label}")
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            TaskSort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.label) },
                                    onClick = {
                                        onSelectSort(sort)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { onSelectFilter(filter) },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No tasks in ${activeFilter.label}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add a prioritized task with a deadline and reminder to stay ahead without overwhelm.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onOpenNewTask) {
                        Text("Add Task")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
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
            }
        }
    }
}
