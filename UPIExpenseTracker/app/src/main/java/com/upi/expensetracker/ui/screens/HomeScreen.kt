package com.upi.expensetracker.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onEditTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayTotal by viewModel.todayTotalSpend.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "$greeting, $userName",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = todayFormatted,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Today's Spent Gradient Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryPurple, Color(0xFF8E2DE2), Color(0xFF4A00E0))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TODAY'S SPEND",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    val spend = todayTotal ?: 0.0
                    Text(
                        text = "₹${String.format("%,.2f", spend)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val todayTxnCount = transactions.filter { it.date == todayDateString }.size
                    val txnText = if (todayTxnCount == 1) "1 transaction tracked today" else "$todayTxnCount transactions tracked today"
                    Text(
                        text = "$txnText • Secured Offline",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Sync Today Button
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
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSyncing
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PrimaryPurple,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = PrimaryPurple
                        )
                        Text(
                            text = "Sync Today's Transactions",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
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
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2C)),
                enabled = !isSyncing
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Pick Date",
                        tint = TextSecondary
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
                            color = PrimaryPurple,
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
                        Text(
                            text = "No transactions found. Tap sync to load.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(recentTransactions) { txn ->
                    TransactionItemCard(
                        transaction = txn,
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
                    Text("SYNC", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("CANCEL", color = TextPrimary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = CardBackground
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = CardBackground,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = PrimaryPurple,
                    selectedYearContainerColor = PrimaryPurple,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = PrimaryPurple,
                    todayContentColor = PrimaryPurple,
                    todayDateBorderColor = PrimaryPurple
                )
            )
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Short name indicator circles
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = if (transaction.merchant.isNotEmpty()) transaction.merchant[0].toString() else "T"
                    Text(
                        text = initial.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple,
                        fontSize = 18.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = transaction.merchant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Category pill
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = transaction.category,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = transaction.time,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format("%,.2f", transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = WarningRed
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
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .background(Color(0xFF2C2C2C), RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(12.dp)
                        .background(Color(0xFF2C2C2C), RoundedCornerShape(4.dp))
                )
            }
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(20.dp)
                    .background(Color(0xFF2C2C2C), RoundedCornerShape(4.dp))
            )
        }
    }
}
