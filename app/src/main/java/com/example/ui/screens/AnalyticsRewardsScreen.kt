package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.GamificationCatalog
import com.example.model.RewardItem
import com.example.ui.components.CategoryDistributionCard
import com.example.ui.components.CortisolTrendChart
import com.example.ui.components.DeepWorkWeeklyBarChart
import com.example.ui.theme.AmberGold
import com.example.ui.theme.IndigoCalm
import com.example.ui.theme.TealGlow
import com.example.viewmodel.DashboardBundle
import com.example.widget.WidgetPinHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsRewardsScreen(
    bundle: DashboardBundle,
    darkModeEnabled: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onRedeemReward: (RewardItem) -> Unit,
    onAddReward: (title: String, costCoins: Long, iconKey: String) -> Unit,
    onPinWidgetResult: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val profile = bundle.profile
    val unlockedSet = profile?.unlockedBadges?.toSet() ?: emptySet()
    var showAddRewardDialog by remember { mutableStateOf(false) }

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
                Text("Analytics & Rewards", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Deep work telemetry, cortisol recovery curves, and earned self-care.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Gamified Crest Banner
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .border(1.dp, AmberGold.copy(alpha = 0.35f), MaterialTheme.shapes.large)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_reward_crest),
                        contentDescription = "CalmFlow rank crest",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = GamificationCatalog.rankTitle(profile?.level ?: 1L),
                            style = MaterialTheme.typography.titleLarge,
                            color = AmberGold
                        )
                        Text(
                            text = "Level ${profile?.level ?: 1} • ${profile?.xp ?: 0} XP",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Stars, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${profile?.calmCoins ?: 0} Calm Coins available",
                                style = MaterialTheme.typography.labelMedium,
                                color = TealGlow
                            )
                        }
                    }
                }
            }
        }

        item {
            DeepWorkWeeklyBarChart(
                sessions = bundle.focusSessions,
                modifier = Modifier.widthIn(max = 640.dp)
            )
        }

        item {
            CortisolTrendChart(
                logs = bundle.cortisolLogs,
                currentScore = bundle.cortisolInsight.score,
                modifier = Modifier.widthIn(max = 640.dp)
            )
        }

        item {
            CategoryDistributionCard(
                tasks = bundle.tasks,
                modifier = Modifier.widthIn(max = 640.dp)
            )
        }

        // Badges Grid
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
            ) {
                Text("Achievement Badges", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GamificationCatalog.badges.forEach { badge ->
                        val unlocked = unlockedSet.contains(badge.id)
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (unlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .width(168.dp)
                                .border(
                                    1.dp,
                                    if (unlocked) TealGlow.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.shapes.medium
                                )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = iconForKey(badge.iconKey),
                                        contentDescription = null,
                                        tint = if (unlocked) AmberGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    if (!unlocked) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = "Locked badge",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(badge.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = badge.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Self-Care Rewards Store
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Self-Care Reward Vault", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(
                    onClick = { showAddRewardDialog = true },
                    modifier = Modifier.testTag("add_custom_reward_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Custom")
                }
            }
        }

        items(bundle.rewards, key = { it.id }) { reward ->
            val canAfford = (profile?.calmCoins ?: 0L) >= reward.costCoins
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
                    Icon(
                        imageVector = iconForKey(reward.iconKey),
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(reward.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Claimed ${reward.redeemedCount}x • ${reward.costCoins} Coins",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { onRedeemReward(reward) },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier.testTag("redeem_reward_${reward.id}")
                    ) {
                        Text(if (canAfford) "Claim" else "${reward.costCoins}c")
                    }
                }
            }
        }

        // Settings, Widget & Account
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Workspace & Widgets", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = IndigoCalm)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Dark Obsidian Mode", style = MaterialTheme.typography.bodyLarge)
                        }
                        Switch(
                            checked = darkModeEnabled,
                            onCheckedChange = onToggleDarkMode,
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Widgets, contentDescription = null, tint = TealGlow)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Add Home Screen Widget", style = MaterialTheme.typography.bodyLarge)
                        }
                        OutlinedButton(
                            onClick = {
                                if (WidgetPinHelper.isPinSupported(context)) {
                                    val ok = WidgetPinHelper.requestPinWidget(context)
                                    onPinWidgetResult(
                                        if (ok) "Widget pin request sent"
                                        else "Long-press home screen -> Widgets -> CalmFlow"
                                    )
                                } else {
                                    onPinWidgetResult("Long-press home screen -> Widgets -> CalmFlow")
                                }
                            }
                        ) {
                            Text("Pin Widget")
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }
        }
    }

    if (showAddRewardDialog) {
        AddRewardDialog(
            onDismiss = { showAddRewardDialog = false },
            onConfirm = { title, cost ->
                onAddReward(title, cost, "coffee")
                showAddRewardDialog = false
            }
        )
    }
}

@Composable
private fun AddRewardDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, costCoins: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Self-Care Reward") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 80) title = it },
                    label = { Text("Reward title") },
                    placeholder = { Text("e.g., 30m Novel Reading") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = { Text("Cost in Calm Coins (10-10000)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = (costStr.toLongOrNull() ?: 50L).coerceIn(10L, 10000L)
                    if (title.isNotBlank()) onConfirm(title, cost)
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun iconForKey(key: String): ImageVector = when (key) {
    "bolt" -> Icons.Rounded.Bolt
    "timer" -> Icons.Rounded.Timer
    "spa" -> Icons.Rounded.Spa
    "verified" -> Icons.Rounded.Verified
    "psychology" -> Icons.Rounded.Psychology
    "redeem" -> Icons.Rounded.CardGiftcard
    "walk" -> Icons.Rounded.DirectionsWalk
    "music" -> Icons.Rounded.MusicNote
    else -> Icons.Rounded.LocalCafe
}
