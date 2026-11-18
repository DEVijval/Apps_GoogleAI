package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.CalmFlowTheme
import com.example.widget.WidgetSnapshotStore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var targetTabState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        targetTabState = intent?.getStringExtra("target_tab")

        setContent {
            val darkModeEnabled by WidgetSnapshotStore
                .darkModeFlow(applicationContext)
                .collectAsStateWithLifecycle(initialValue = true)
            val scope = rememberCoroutineScope()

            CalmFlowTheme(darkTheme = darkModeEnabled) {
                AppNavigation(
                    initialTab = targetTabState,
                    darkModeEnabled = darkModeEnabled,
                    onToggleDarkMode = { enabled ->
                        scope.launch {
                            WidgetSnapshotStore.setDarkMode(applicationContext, enabled)
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        targetTabState = intent.getStringExtra("target_tab")
    }
}
