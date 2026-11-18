package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CalmNote
import com.example.model.CalmTask
import com.example.model.NoteTag
import com.example.ui.theme.AmberGold
import com.example.ui.theme.IndigoCalm
import com.example.ui.theme.TealGlow

@Composable
fun NotesScreen(
    notes: List<CalmNote>,
    tasks: List<CalmTask>,
    searchQuery: String,
    selectedTag: NoteTag?,
    openEditorTrigger: Boolean,
    onConsumeOpenEditorTrigger: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectTag: (NoteTag?) -> Unit,
    onSaveNote: (id: String?, title: String, content: String, tag: NoteTag, pinned: Boolean, linkedTask: String) -> Unit,
    onTogglePin: (CalmNote) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    var editingNote by remember { mutableStateOf<CalmNote?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    if (openEditorTrigger) {
        editingNote = null
        showEditor = true
        onConsumeOpenEditorTrigger()
    }

    val filteredNotes = remember(notes, searchQuery, selectedTag) {
        notes.filter { note ->
            val matchesTag = selectedTag == null || note.tagEnum == selectedTag
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                note.title.lowercase().contains(q) ||
                note.content.lowercase().contains(q) ||
                note.linkedTaskTitle.lowercase().contains(q)
            matchesTag && matchesQuery
        }.sortedWith(
            compareByDescending<CalmNote> { it.pinned }
                .thenByDescending { it.updatedAt?.seconds ?: 0L }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingNote = null
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_note")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add note")
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
                Text("Cognitive Offload & Notes", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Park stray thoughts and reflections to quiet mental chatter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search notes, brain dumps, or linked tasks") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTag == null,
                        onClick = { onSelectTag(null) },
                        label = { Text("All") }
                    )
                    NoteTag.entries.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { onSelectTag(if (selectedTag == tag) null else tag) },
                            label = { Text(tag.label) }
                        )
                    }
                }
            }

            if (filteredNotes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.Description, contentDescription = null, tint = IndigoCalm, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Notes Yet", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Capture a brain dump, creative idea, or reflection linked to any task.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(onClick = {
                        editingNote = null
                        showEditor = true
                    }) {
                        Text("Write First Note")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (note.pinned) AmberGold.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.shapes.medium
                                )
                                .clickable {
                                    editingNote = note
                                    showEditor = true
                                }
                                .testTag("note_card_${note.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = IndigoCalm.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = note.tagEnum.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = IndigoCalm,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(onClick = { onTogglePin(note) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.PushPin,
                                            contentDescription = if (note.pinned) "Unpin note" else "Pin note",
                                            tint = if (note.pinned) AmberGold else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeleteNote(note.id) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete note",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(note.title, style = MaterialTheme.typography.titleMedium)
                                if (note.content.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (note.linkedTaskTitle.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Link, contentDescription = null, tint = TealGlow, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = note.linkedTaskTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TealGlow,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        NoteEditorSheet(
            editingNote = editingNote,
            tasks = tasks,
            onDismiss = { showEditor = false },
            onSave = { id, title, content, tag, pinned, linked ->
                onSaveNote(id, title, content, tag, pinned, linked)
                showEditor = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NoteEditorSheet(
    editingNote: CalmNote?,
    tasks: List<CalmTask>,
    onDismiss: () -> Unit,
    onSave: (id: String?, title: String, content: String, tag: NoteTag, pinned: Boolean, linkedTask: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(editingNote) { mutableStateOf(editingNote?.title.orEmpty()) }
    var content by remember(editingNote) { mutableStateOf(editingNote?.content.orEmpty()) }
    var tag by remember(editingNote) { mutableStateOf(editingNote?.tagEnum ?: NoteTag.BRAIN_DUMP) }
    var pinned by remember(editingNote) { mutableStateOf(editingNote?.pinned ?: false) }
    var linkedTask by remember(editingNote) { mutableStateOf(editingNote?.linkedTaskTitle.orEmpty()) }

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
                    text = if (editingNote == null) "New Note" else "Edit Note",
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close note editor")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 120) title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { if (it.length <= 4000) content = it },
                label = { Text("Write freely...") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_content_input")
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("Tag", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NoteTag.entries.forEach { t ->
                    FilterChip(
                        selected = tag == t,
                        onClick = { tag = t },
                        label = { Text(t.label) }
                    )
                }
            }

            if (tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Link to Task (Optional)", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = linkedTask.isBlank(),
                        onClick = { linkedTask = "" },
                        label = { Text("None") }
                    )
                    tasks.take(6).forEach { task ->
                        FilterChip(
                            selected = linkedTask == task.title,
                            onClick = { linkedTask = task.title },
                            label = { Text(task.title, maxLines = 1) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pin to top of vault", style = MaterialTheme.typography.titleSmall)
                Switch(checked = pinned, onCheckedChange = { pinned = it })
            }

            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(editingNote?.id, title, content, tag, pinned, linkedTask)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_note_button")
            ) {
                Text(if (editingNote == null) "Save Note" else "Update Note")
            }
        }
    }
}
