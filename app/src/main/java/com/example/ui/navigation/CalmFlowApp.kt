package com.example.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.GoogleSignInHelper
import com.example.data.CalmFlowRepository
import com.example.model.CalmTask
import com.example.model.CortisolMood
import com.example.model.UiState
import com.example.ui.components.BreathingResetDialog
import com.example.ui.components.CortisolCheckInSheet
import com.example.ui.components.TaskEditorSheet
import com.example.ui.screens.AnalyticsRewardsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.SanctuaryScreen
import com.example.ui.screens.TasksScreen
import com.example.viewmodel.CalmFlowEvent
import com.example.viewmodel.CalmFlowViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

const val ROUTE_SANCTUARY = "sanctuary"
const val ROUTE_TASKS = "tasks"
const val ROUTE_FOCUS = "focus"
const val ROUTE_NOTES = "notes"
const val ROUTE_INSIGHTS = "insights"

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val CalmFlowDestinations = listOf(
    NavDestination(ROUTE_SANCTUARY, "Flow", Icons.Rounded.Spa, Icons.Outlined.Spa, "nav_sanctuary"),
    NavDestination(ROUTE_TASKS, "Tasks", Icons.Rounded.CheckCircle, Icons.Outlined.CheckCircle, "nav_tasks"),
    NavDestination(ROUTE_FOCUS, "Focus", Icons.Rounded.Timer, Icons.Outlined.Timer, "nav_focus"),
    NavDestination(ROUTE_NOTES, "Notes", Icons.Rounded.Description, Icons.Outlined.Description, "nav_notes"),
    NavDestination(ROUTE_INSIGHTS, "Insights", Icons.Rounded.AutoGraph, Icons.Outlined.AutoGraph, "nav_insights")
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(
    initialTab: String?,
    darkModeEnabled: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener drives transition */ })
    } else {
        key(user.uid) {
            AuthenticatedCalmFlowShell(
                currentUserId = user.uid,
                displayName = user.displayName,
                initialTab = initialTab,
                darkModeEnabled = darkModeEnabled,
                onToggleDarkMode = onToggleDarkMode
            )
        }
    }
}

@Composable
private fun AuthenticatedCalmFlowShell(
    currentUserId: String,
    displayName: String?,
    initialTab: String?,
    darkModeEnabled: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: CalmFlowViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repo = CalmFlowRepository(db = db, appContext = app.applicationContext)
                CalmFlowViewModel(
                    repository = repo,
                    currentUserId = currentUserId,
                    initialDisplayName = displayName
                )
            }
        }
    )

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val taskFilter by viewModel.taskFilter.collectAsStateWithLifecycle()
    val taskSort by viewModel.taskSort.collectAsStateWithLifecycle()
    val noteQuery by viewModel.noteQuery.collectAsStateWithLifecycle()
    val noteTagFilter by viewModel.noteTagFilter.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_SANCTUARY

    var showTaskSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<CalmTask?>(null) }
    var showCheckInSheet by remember { mutableStateOf(false) }
    var showBreathingDialog by remember { mutableStateOf(false) }
    var openNewNoteTrigger by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar("Enable notifications in Settings to receive scheduled task reminders.")
            }
        }
    }

    fun ensureNotificationPermissionIfNeeded(hasReminder: Boolean) {
        if (!hasReminder) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (status != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(initialTab) {
        when (initialTab) {
            "tasks" -> navigateToTab(ROUTE_TASKS)
            "focus" -> navigateToTab(ROUTE_FOCUS)
            "notes" -> navigateToTab(ROUTE_NOTES)
            "insights" -> navigateToTab(ROUTE_INSIGHTS)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CalmFlowEvent.Banner -> snackbarHostState.showSnackbar(event.message)
                is CalmFlowEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        CalmFlowDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CalmFlowDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (val state = dashboardState) {
                        is UiState.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Syncing CalmFlow Sanctuary...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        is UiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(28.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Sync Interrupted",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = {
                                    GoogleSignInHelper.signOut(credentialManager, scope)
                                }) {
                                    Text("Switch Account / Retry")
                                }
                            }
                        }

                        is UiState.Success -> {
                            val bundle = state.data
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_SANCTUARY,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(ROUTE_SANCTUARY) {
                                    SanctuaryScreen(
                                        bundle = bundle,
                                        onOpenCheckIn = { showCheckInSheet = true },
                                        onStartBreathingReset = { showBreathingDialog = true },
                                        onOpenNewTask = {
                                            editingTask = null
                                            showTaskSheet = true
                                        },
                                        onOpenNewNote = {
                                            openNewNoteTrigger = true
                                            navigateToTab(ROUTE_NOTES)
                                        },
                                        onOpenFocusTab = { navigateToTab(ROUTE_FOCUS) },
                                        onStartTaskFocus = { task ->
                                            viewModel.selectFocusTask(task)
                                            navigateToTab(ROUTE_FOCUS)
                                        },
                                        onToggleTask = { task -> viewModel.toggleTaskCompletion(task, bundle.profile) },
                                        onEditTask = { task ->
                                            editingTask = task
                                            showTaskSheet = true
                                        },
                                        onDeleteTask = { task -> viewModel.deleteTask(task.id) },
                                        onOpenAllTasks = { navigateToTab(ROUTE_TASKS) },
                                        onPinWidgetResult = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    )
                                }

                                composable(ROUTE_TASKS) {
                                    TasksScreen(
                                        tasks = bundle.tasks,
                                        cortisolScore = bundle.cortisolInsight.score,
                                        activeFilter = taskFilter,
                                        activeSort = taskSort,
                                        onSelectFilter = viewModel::setTaskFilter,
                                        onSelectSort = viewModel::setTaskSort,
                                        onOpenNewTask = {
                                            editingTask = null
                                            showTaskSheet = true
                                        },
                                        onToggleTask = { task -> viewModel.toggleTaskCompletion(task, bundle.profile) },
                                        onStartTaskFocus = { task ->
                                            viewModel.selectFocusTask(task)
                                            navigateToTab(ROUTE_FOCUS)
                                        },
                                        onEditTask = { task ->
                                            editingTask = task
                                            showTaskSheet = true
                                        },
                                        onDeleteTask = { task -> viewModel.deleteTask(task.id) }
                                    )
                                }

                                composable(ROUTE_FOCUS) {
                                    FocusScreen(
                                        timerState = timerState,
                                        activeTasks = bundle.tasks.filter { !it.completed },
                                        sessions = bundle.focusSessions,
                                        onSelectTask = viewModel::selectFocusTask,
                                        onSelectPreset = viewModel::setTimerPreset,
                                        onToggleRunning = { viewModel.toggleTimerRunning(bundle.profile) },
                                        onReset = viewModel::resetTimer,
                                        onFinishEarly = { viewModel.finishFocusEarly(bundle.profile) },
                                        onToggleBreathing = viewModel::toggleBreathingMode
                                    )
                                }

                                composable(ROUTE_NOTES) {
                                    NotesScreen(
                                        notes = bundle.notes,
                                        tasks = bundle.tasks.filter { !it.completed },
                                        searchQuery = noteQuery,
                                        selectedTag = noteTagFilter,
                                        openEditorTrigger = openNewNoteTrigger,
                                        onConsumeOpenEditorTrigger = { openNewNoteTrigger = false },
                                        onSearchChange = viewModel::setNoteQuery,
                                        onSelectTag = viewModel::setNoteTagFilter,
                                        onSaveNote = viewModel::saveNote,
                                        onTogglePin = viewModel::toggleNotePin,
                                        onDeleteNote = viewModel::deleteNote
                                    )
                                }

                                composable(ROUTE_INSIGHTS) {
                                    AnalyticsRewardsScreen(
                                        bundle = bundle,
                                        darkModeEnabled = darkModeEnabled,
                                        onToggleDarkMode = onToggleDarkMode,
                                        onRedeemReward = { reward ->
                                            val prof = bundle.profile
                                            if (prof != null) viewModel.redeemReward(reward, prof)
                                        },
                                        onAddReward = viewModel::addCustomReward,
                                        onPinWidgetResult = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        },
                                        onSignOut = {
                                            GoogleSignInHelper.signOut(credentialManager, scope)
                                        }
                                    )
                                }
                            }

                            if (showCheckInSheet) {
                                CortisolCheckInSheet(
                                    currentScore = bundle.cortisolInsight.score,
                                    onDismiss = { showCheckInSheet = false },
                                    onSubmit = { level, mood, trigger, note ->
                                        viewModel.logCortisolCheckIn(
                                            level = level,
                                            mood = mood,
                                            trigger = trigger,
                                            note = note,
                                            profile = bundle.profile,
                                            isBreathingRecovery = false
                                        )
                                    }
                                )
                            }

                            if (showBreathingDialog) {
                                BreathingResetDialog(
                                    currentScore = bundle.cortisolInsight.score,
                                    onDismiss = { showBreathingDialog = false },
                                    onCompleted = { newScore ->
                                        viewModel.logCortisolCheckIn(
                                            level = newScore,
                                            mood = CortisolMood.RECOVERING,
                                            trigger = "4-4-4-4 Box Breathing",
                                            note = "Completed parasympathetic breathing reset.",
                                            profile = bundle.profile,
                                            isBreathingRecovery = true
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTaskSheet) {
        TaskEditorSheet(
            editingTask = editingTask,
            onDismiss = { showTaskSheet = false },
            onSave = { id, title, desc, prio, load, cat, dueAt, remAt, mins ->
                ensureNotificationPermissionIfNeeded(remAt > System.currentTimeMillis())
                viewModel.saveTask(id, title, desc, prio, load, cat, dueAt, remAt, mins)
            }
        )
    }
}
