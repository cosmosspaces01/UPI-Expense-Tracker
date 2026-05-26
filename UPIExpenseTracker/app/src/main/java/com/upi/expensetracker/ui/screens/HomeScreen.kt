package com.upi.expensetracker.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val transactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val userName = viewModel.userName
    
    var isSyncing by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val recentTransactions = transactions.take(5)
    val todayFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
    
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "☀️ Good morning"
        in 12..16 -> "🌤️ Good afternoon"
        else -> "🌙 Good evening"
    }

    // Animated sync icon rotation
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

    // Build a category lookup map for emoji/color retrieval
    val categoryMap = remember(categories) {
        categories.associateBy { it.name }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        // Header with emoji greeting
        item {
            Column {
                Text(
                    text = "$greeting, $userName",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = todayFormatted,
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Today's Spent Hero Card — Gradient background
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    GradientStart,
                                    GradientMid,
                                    GradientEnd
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Today's spend",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        val spend = todayTotal ?: 0.0
                        Text(
                            text = "₹${String.format("%,.2f", spend)}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        val todayTxnCount = transactions.filter { it.date == todayDateString }.size
                        val txnText = if (todayTxnCount == 1) "1 transaction" else "$todayTxnCount transactions"
                        
                        // Transaction count pill
                        Box(
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "📊 $txnText tracked today",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Sync Today Button — Gradient
        item {
            Button(
                onClick = {
                    isSyncing = true
                    viewModel.syncTransactions { count ->
                        isSyncing = false
                        if (count >= 0) {
                            Toast.makeText(context, "Synced $count new transactions!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Permission denied or Sync failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp),
                enabled = !isSyncing,
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PrimaryViolet, PrimaryPink)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSyncing) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Syncing",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(syncRotation)
                            )
                            Text(
                                text = "Syncing...",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync",
                                tint = Color.White
                            )
                            Text(
                                text = "Sync Today's Transactions",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Sync Past Date Button
        item {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, PrimaryMuted),
                enabled = !isSyncing
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📅", fontSize = 16.sp)
                    Text(
                        text = "Sync a Past Date",
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Skeleton loading simulations
        if (isSyncing) {
            items(3) {
                SkeletonCard()
            }
        } else {
            // Recent Transactions Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            color = PrimaryViolet,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Transactions Cards
            if (recentTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🎉", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No transactions found. Tap sync to load.",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
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

    // Date Picker Dialog for syncing a past date
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
                    Text("SYNC", color = PrimaryViolet, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("CANCEL", color = TextPrimary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Surface
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Surface,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = PrimaryViolet,
                    selectedYearContainerColor = PrimaryViolet,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = PrimaryViolet,
                    todayContentColor = PrimaryViolet,
                    todayDateBorderColor = PrimaryViolet
                )
            )
        }
    }
}

/**
 * Helper to get category emoji, resolving from the entity's icon field.
 * Falls back to merchant initial if no category mapping found.
 */
private fun getCategoryEmojiForTransaction(category: CategoryEntity?): String {
    if (category == null) return "💸"
    return com.upi.expensetracker.ui.screens.getCategoryEmoji(category.icon)
}

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    category: CategoryEntity? = null,
    onClick: () -> Unit
) {
    // Parse category color for the avatar background
    val avatarBgColor = remember(category) {
        try {
            if (category != null) {
                Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.2f)
            } else {
                SurfaceElevated
            }
        } catch (e: Exception) {
            SurfaceElevated
        }
    }
    val avatarAccentColor = remember(category) {
        try {
            if (category != null) {
                Color(android.graphics.Color.parseColor(category.color))
            } else {
                PrimaryViolet
            }
        } catch (e: Exception) {
            PrimaryViolet
        }
    }

    // Amount-based color: small=mint, medium=amber, large=coral
    val amountColor = when {
        transaction.amount >= 5000 -> DebitRed
        transaction.amount >= 1000 -> AccentAmber
        else -> AccentCoral
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Emoji avatar circle with category color background
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(avatarBgColor, RoundedCornerShape(14.dp))
                        .border(
                            BorderStroke(1.dp, avatarAccentColor.copy(alpha = 0.3f)),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getCategoryEmojiForTransaction(category),
                        fontSize = 20.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    // Show merchant name as primary title
                    Text(
                        text = transaction.merchant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        maxLines = 1
                    )

                    // Show user's description/reason if they've edited it from the default
                    val hasCustomDesc = transaction.description.isNotEmpty() &&
                        !transaction.description.startsWith("UPI Transfer to")
                    val hasNotes = transaction.notes.isNotEmpty()

                    if (hasCustomDesc || hasNotes) {
                        Text(
                            text = if (hasCustomDesc) transaction.description else transaction.notes,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Category pill with category color tint
                        Box(
                            modifier = Modifier
                                .background(
                                    avatarAccentColor.copy(alpha = 0.12f),
                                    RoundedCornerShape(20.dp)
                                )
                                .border(
                                    BorderStroke(0.5.dp, avatarAccentColor.copy(alpha = 0.25f)),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = transaction.category,
                                fontSize = 10.sp,
                                color = avatarAccentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Recurring badge
                        if (transaction.isRecurring) {
                            Box(
                                modifier = Modifier
                                    .background(SuccessGreen.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🔁 Recurring",
                                    fontSize = 9.sp,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = transaction.time,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format("%,.2f", transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = amountColor
                )
                if (transaction.isSplit) {
                    Text(
                        text = "Split active",
                        fontSize = 10.sp,
                        color = SuccessGreen,
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
    // Rainbow shimmer gradient animation
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    val shimmerBrush = Brush.horizontalGradient(
        colors = listOf(
            SurfaceElevated,
            PrimaryViolet.copy(alpha = 0.15f),
            PrimaryPink.copy(alpha = 0.1f),
            SurfaceElevated
        ),
        startX = shimmerOffset * 800f,
        endX = shimmerOffset * 800f + 400f
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
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
                    .size(46.dp)
                    .background(shimmerBrush, RoundedCornerShape(14.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .background(shimmerBrush, RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(12.dp)
                        .background(shimmerBrush, RoundedCornerShape(6.dp))
                )
            }
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(20.dp)
                    .background(shimmerBrush, RoundedCornerShape(8.dp))
            )
        }
    }
}
