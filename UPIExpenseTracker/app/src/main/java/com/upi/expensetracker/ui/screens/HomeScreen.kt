package com.upi.expensetracker.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upi.expensetracker.data.CategoryEntity
import com.upi.expensetracker.data.TransactionEntity
import com.upi.expensetracker.ui.MainViewModel
import com.upi.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onEditTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayTotal by viewModel.todayTotalSpend.collectAsState()
    val monthTotal by viewModel.monthTotalSpend.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val userName = viewModel.userName

    var isSyncing by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val recentTransactions = transactions.take(5)
    val todayFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    // Sync icon rotation
    val infiniteTransition = rememberInfiniteTransition(label = "syncSpin")
    val syncRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "syncRotation"
    )

    val categoryMap = remember(categories) {
        categories.associateBy { it.name }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp)
    ) {
        // Greeting — clean, no emoji
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "$greeting, $userName",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = todayFormatted,
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }
        }

        // Spend overview — two stat cards side by side
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today's spend card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = BorderStroke(1.dp, Divider)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Today",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "₹${String.format("%,.0f", todayTotal ?: 0.0)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        val todayCount = transactions.count { it.date == todayDateString }
                        Text(
                            text = "$todayCount transactions",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                // This month's spend card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = BorderStroke(1.dp, Divider)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "This month",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "₹${String.format("%,.0f", monthTotal ?: 0.0)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${transactions.count { it.date.startsWith(SimpleDateFormat("yyyy-MM", Locale.US).format(Date())) }} transactions",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Sync button — solid teal
        item {
            Button(
                onClick = {
                    isSyncing = true
                    viewModel.syncTransactions { count ->
                        isSyncing = false
                        if (count >= 0) {
                            Toast.makeText(context, "Synced $count new transactions", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Permission denied or sync failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(14.dp),
                enabled = !isSyncing
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (isSyncing) "Syncing" else "Sync",
                        tint = Background,
                        modifier = Modifier
                            .size(20.dp)
                            .then(if (isSyncing) Modifier.rotate(syncRotation) else Modifier)
                    )
                    Text(
                        text = if (isSyncing) "Syncing..." else "Sync Today's Transactions",
                        fontWeight = FontWeight.SemiBold,
                        color = Background,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Sync past date — outlined
        item {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AccentDim),
                enabled = !isSyncing
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Past Date",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Sync a Past Date",
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Skeleton loading
        if (isSyncing) {
            items(3) { SkeletonCard() }
        } else {
            // Recent Transactions header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (transactions.isNotEmpty()) {
                        Text(
                            text = "See All (${transactions.size})",
                            fontSize = 13.sp,
                            color = Accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions yet. Tap sync to load.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(recentTransactions) { txn ->
                    TransactionItemCard(
                        transaction = txn,
                        category = categoryMap[txn.category],
                        onClick = { onEditTransaction(txn) }
                    )
                }
            }
        }
    }

    // Date Picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            showDatePicker = false
                            isSyncing = true
                            val selectedDateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedMillis))
                            viewModel.syncTransactionsForDate(selectedMillis) { count ->
                                isSyncing = false
                                if (count >= 0) {
                                    Toast.makeText(context, "Synced $count transactions for $selectedDateStr", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Sync failed for $selectedDateStr", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("SYNC", color = Accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("CANCEL", color = TextPrimary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Surface)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Surface,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = Accent,
                    selectedYearContainerColor = Accent,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = Accent,
                    todayContentColor = Accent,
                    todayDateBorderColor = Accent
                )
            )
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    category: CategoryEntity? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Letter avatar — merchant initial in a circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(SurfaceElevated, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = transaction.merchant.take(1).uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = transaction.merchant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        maxLines = 1
                    )

                    // Category + time on the same line
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category color dot
                        val catColor = try {
                            category?.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: Accent
                        } catch (_: Exception) { Accent }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(catColor, CircleShape)
                        )

                        Text(
                            text = transaction.category,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        // Small separator dot
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .background(TextMuted, CircleShape)
                        )

                        Text(
                            text = transaction.time,
                            fontSize = 12.sp,
                            color = TextMuted
                        )

                        if (transaction.isRecurring) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .background(TextMuted, CircleShape)
                            )
                            Text(
                                text = "Recurring",
                                fontSize = 11.sp,
                                color = Accent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Source badge — shows where this transaction was captured from
                        val (sourceLabel, sourceColor) = when (transaction.source) {
                            "NOTIFICATION" -> Pair("Notif", Color(0xFF6C63FF))
                            "MANUAL"       -> Pair("Manual", Color(0xFF636E72))
                            else           -> Pair("SMS", Color(0xFF0984E3))   // default = "SMS"
                        }
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .background(TextMuted, CircleShape)
                        )
                        Text(
                            text = sourceLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = sourceColor
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format("%,.2f", transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                if (transaction.isSplit) {
                    Text(
                        text = "Split active",
                        fontSize = 10.sp,
                        color = Accent,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SkeletonCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    val shimmerColor = SurfaceElevated.copy(alpha = shimmerAlpha)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(shimmerColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(14.dp)
                        .background(shimmerColor, RoundedCornerShape(7.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(10.dp)
                        .background(shimmerColor, RoundedCornerShape(5.dp))
                )
            }
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(14.dp)
                    .background(shimmerColor, RoundedCornerShape(7.dp))
            )
        }
    }
}
