package com.example.ui

import android.app.Activity
import com.example.data.Task
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAppScreen(viewModel: TaskViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity

    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val totalPoints by viewModel.totalPoints.collectAsStateWithLifecycle()

    val isAdsInit by viewModel.isAdsInitialized.collectAsStateWithLifecycle()
    val isAdLt by viewModel.isAdLoaded.collectAsStateWithLifecycle()
    val adStateMessage by viewModel.adStateMessage.collectAsStateWithLifecycle()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = All, 1 = Active, 2 = Completed

    // Filter tasks
    val filteredTasks = remember(tasks, selectedTab) {
        when (selectedTab) {
            1 -> tasks.filter { !it.isCompleted }
            2 -> tasks.filter { it.isCompleted }
            else -> tasks
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("add_task_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header - Point Balance Card
            PointBalanceCard(
                totalPoints = totalPoints,
                isAdLoaded = isAdLt,
                adStateMessage = adStateMessage,
                onWatchAdClicked = {
                    if (activity != null) {
                        viewModel.watchAdForBonusPoints(activity)
                    }
                },
                onReloadAdClicked = {
                    viewModel.reloadAd()
                }
            )

            // Unity Integration State Information Bar
            AdStatusBadgeRow(
                isAdsInit = isAdsInit,
                isAdLoaded = isAdLt,
                adStateMessage = adStateMessage
            )

            // Filter Tabs
            FilterTabsRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // Tasks List Title
            Text(
                text = when (selectedTab) {
                    0 -> "All Tasks (${filteredTasks.size})"
                    1 -> "Active Tasks (${filteredTasks.size})"
                    2 -> "Completed Logs (${filteredTasks.size})"
                    else -> "Tasks"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Dynamic Empty State or Tasks List
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = "Empty State",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (selectedTab) {
                                1 -> "No active tasks. Tap + to add one!"
                                2 -> "No completed tasks yet. Watch ads to make it double!"
                                else -> "Your task journal is empty. Add tasks to start earning!"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp, start = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            activity = activity,
                            onCompleteToggle = { viewModel.toggleTaskComplete(task) },
                            onDelete = { viewModel.deleteTask(task) },
                            onDoubleRewardClicked = { act -> viewModel.doubleTaskReward(task, act) },
                            onUnlockPremiumClicked = { act -> viewModel.unlockPremiumTask(task, act) }
                        )
                    }
                }
            }
        }
    }

    // Add Task Dialog window
    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onAddTask = { title, desc, points, isPremium ->
                viewModel.addTask(title, desc, points, isPremium)
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun PointBalanceCard(
    totalPoints: Int,
    isAdLoaded: Boolean,
    adStateMessage: String,
    onWatchAdClicked: () -> Unit,
    onReloadAdClicked: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("point_balance_card"),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL EARNED POINTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.testTag("earned_points_display"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$totalPoints",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🪙",
                            fontSize = 28.sp
                        )
                    }
                }

                // Quick Reload Button
                IconButton(
                    onClick = onReloadAdClicked,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload Ad",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bonus Ad Button
            Button(
                onClick = onWatchAdClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAdLoaded) Color(0xFFFFB300) else MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("bonus_ad_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🎬",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = if (isAdLoaded) "Watch Video for +50 Points!" else "Loading Video Reward...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isAdLoaded) Color.Black else MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun AdStatusBadgeRow(
    isAdsInit: Boolean,
    isAdLoaded: Boolean,
    adStateMessage: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Integration status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAdsInit) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Unity Integration: ${if (isAdsInit) "ACTIVE" else "CONNECTED"}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isAdLoaded) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = adStateMessage,
                fontSize = 10.sp,
                color = if (isAdLoaded) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FilterTabsRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("All Items", "To Do", "Completed Log")
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            )
        }
    }
}

@Composable
fun TaskItemCard(
    task: Task,
    activity: Activity?,
    onCompleteToggle: () -> Unit,
    onDelete: () -> Unit,
    onDoubleRewardClicked: (Activity) -> Unit,
    onUnlockPremiumClicked: (Activity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else if (task.isPremium) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox (unless premium locked)
            if (task.isPremium) {
                IconButton(
                    onClick = { if (activity != null) onUnlockPremiumClicked(activity) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Premium Locked",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onCompleteToggle() },
                    modifier = Modifier.testTag("task_complete_checkbox_${task.id}")
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Task info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Points Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (task.isCompleted) Color(0xFFE0E0E0)
                                else if (task.isPremium) Color(0xFFFFEBEE)
                                else MaterialTheme.colorScheme.secondaryContainer
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+${task.points} 🪙",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (task.isCompleted) Color.Gray
                            else if (task.isPremium) Color(0xFFC62828)
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = task.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                // Extra reward actions for Active tasks
                if (!task.isCompleted && activity != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    if (task.isPremium) {
                        Button(
                            onClick = { onUnlockPremiumClicked(activity) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("unlock_task_button")
                        ) {
                            Text("🎬 Watch Ad to Unlock Task", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { onDoubleRewardClicked(activity) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("double_reward_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎬 Complete for 2x Reward", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_task_button_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAddTask: (String, String, Int, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var points by remember { mutableStateOf("10") }
    var isPremium by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("add_task_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Insert New Task",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_task_title"),
                    colors = OutlinedTextFieldDefaults.colors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("add_task_desc"),
                    colors = OutlinedTextFieldDefaults.colors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Points Selector Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Base Reward: ",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    val options = listOf("10", "25", "50", "100")
                    options.forEach { opt ->
                        val isSelected = points == opt
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { points = opt }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$opt 🪙",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Premium Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Premium Locked Task",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Requires watching an ad to unlock",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isPremium,
                        onCheckedChange = { isPremium = it },
                        modifier = Modifier.testTag("premium_task_switch")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onAddTask(title, description, points.toIntOrNull() ?: 10, isPremium)
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Task")
                    }
                }
            }
        }
    }
}
