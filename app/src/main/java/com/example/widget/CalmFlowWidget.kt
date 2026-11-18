package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.R

class CalmFlowWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotStore.readSnapshot(context)
        provideContent {
            GlanceTheme {
                WidgetBody(snapshot = snapshot)
            }
        }
    }

    @Composable
    private fun WidgetBody(snapshot: WidgetSnapshot) {
        val context = LocalContext.current
        val bgSurface = ColorProvider(day = Color(0xFF0E1420), night = Color(0xFF0E1420))
        val bgCard = ColorProvider(day = Color(0xFF162033), night = Color(0xFF162033))
        val textWhite = ColorProvider(day = Color(0xFFF1F5F9), night = Color(0xFFF1F5F9))
        val textMuted = ColorProvider(day = Color(0xFF94A3B8), night = Color(0xFF94A3B8))
        val tealAccent = ColorProvider(day = Color(0xFF2DD4BF), night = Color(0xFF2DD4BF))
        val amberAccent = ColorProvider(day = Color(0xFFFBBF24), night = Color(0xFFFBBF24))
        val coralAccent = ColorProvider(day = Color(0xFFFB7185), night = Color(0xFFFB7185))

        val cortisolColor = when {
            snapshot.cortisolScore <= 35 -> tealAccent
            snapshot.cortisolScore <= 65 -> amberAccent
            else -> coralAccent
        }

        val openDashboardIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openFocusIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_tab", "focus")
        }
        val openTasksIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_tab", "tasks")
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_bg_dark))
                .padding(14.dp)
                .clickable(actionStartActivity(openDashboardIntent))
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "CALMFLOW",
                        style = TextStyle(
                            color = tealAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Cortisol ${snapshot.cortisolScore}/100 • ${snapshot.cortisolZone}",
                        style = TextStyle(
                            color = cortisolColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }

                Box(
                    modifier = GlanceModifier
                        .background(bgCard)
                        .cornerRadius(10.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable(actionRunCallback<RefreshCalmWidgetAction>())
                ) {
                    Text(
                        text = "Lv.${snapshot.level} • ${snapshot.streakDays}d",
                        style = TextStyle(
                            color = amberAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            if (snapshot.topTasks.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .background(bgCard)
                        .cornerRadius(12.dp)
                        .padding(10.dp)
                        .clickable(actionStartActivity(openTasksIntent)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "No pending priorities. Tap to plan your calm focus day.",
                        style = TextStyle(color = textMuted, fontSize = 12.sp),
                        maxLines = 2
                    )
                }
            } else {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                ) {
                    snapshot.topTasks.take(3).forEachIndexed { index, task ->
                        if (index > 0) Spacer(modifier = GlanceModifier.height(5.dp))
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(bgCard)
                                .cornerRadius(10.dp)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .clickable(actionStartActivity(openTasksIntent)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "•",
                                style = TextStyle(
                                    color = tealAccent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = task.title,
                                style = TextStyle(
                                    color = textWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight()
                            )
                            if (task.dueLabel.isNotBlank()) {
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                Text(
                                    text = task.dueLabel,
                                    style = TextStyle(
                                        color = textMuted,
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${snapshot.pendingCount} active tasks",
                    style = TextStyle(color = textMuted, fontSize = 11.sp),
                    modifier = GlanceModifier.defaultWeight()
                )
                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(day = Color(0xFF113537), night = Color(0xFF113537)))
                        .cornerRadius(10.dp)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .clickable(actionStartActivity(openFocusIntent))
                ) {
                    Text(
                        text = "Start Focus",
                        style = TextStyle(
                            color = tealAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

class RefreshCalmWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        CalmFlowWidget().update(context, glanceId)
    }
}

class CalmFlowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalmFlowWidget()
}
