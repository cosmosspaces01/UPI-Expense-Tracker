package com.upi.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upi.expensetracker.data.TransactionEntity
import com.upi.expensetracker.ui.MainViewModel
import com.upi.expensetracker.ui.components.SpendBarChart
import com.upi.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    // Category lookup for emoji
    val categoryMap = remember(categories) {
        categories.associateBy { it.name }
    }

    // Monthly selector state
    val calendar = remember { Calendar.getInstance() }
    var currentMonthDate by remember { mutableStateOf(calendar.time) }
    
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthFilterFormat = SimpleDateFormat("yyyy-MM", Locale.US)

    val currentMonthStr = monthYearFormat.format(currentMonthDate)
    val currentMonthFilter = monthFilterFormat.format(currentMonthDate)

    // Calculate last month's filter
    val lastMonthFilter = remember(currentMonthDate) {
        val cal = Calendar.getInstance()
        cal.time = currentMonthDate
        cal.add(Calendar.MONTH, -1)
        monthFilterFormat.format(cal.time)
    }

    // Filter transactions
    val monthTransactions = allTransactions.filter { it.date.startsWith(currentMonthFilter) }
    val lastMonthTransactions = allTransactions.filter { it.date.startsWith(lastMonthFilter) }

    val totalSpent = monthTransactions.sumOf { it.amount }
    val totalSpentLastMonth = lastMonthTransactions.sumOf { it.amount }

    // Delta comparison
    val deltaPercent = if (totalSpentLastMonth > 0) {
        ((totalSpent - totalSpentLastMonth) / totalSpentLastMonth) * 100
    } else {
        0.0
    }

    // Category breakdown
    val categoryBreakdown = monthTransactions
        .groupBy { it.category }
        .map { (catName, txns) ->
            val sum = txns.sumOf { it.amount }
            val catColor = categories.find { it.name == catName }?.color ?: "#7C5CFC"
            val catIcon = categories.find { it.name == catName }?.icon ?: "more_horiz"
            CategoryBreakdownItem(
                categoryName = catName,
                amount = sum,
                percentage = if (totalSpent > 0) (sum / totalSpent) * 100 else 0.0,
                color = catColor,
                icon = catIcon
            )
        }.sortedByDescending { it.amount }

    // Top 5 merchants
    val topMerchants = monthTransactions
        .groupBy { it.merchant }
        .map { (name, txns) -> Pair(name, txns.sumOf { it.amount }) }
        .sortedByDescending { it.second }
        .take(5)

    // Day-wise spend for the last 30 days
    val dailySpends = remember(monthTransactions) {
        val spends = DoubleArray(30) { 0.0 }
        monthTransactions.forEach { txn ->
            try {
                val day = txn.date.split("-")[2].toInt()
                if (day in 1..30) {
                    spends[day - 1] += txn.amount
                }
            } catch (e: Exception) {
                // ignore parsing exceptions
            }
        }
        spends.toList()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        // Month Selector with gradient text
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    calendar.time = currentMonthDate
                    calendar.add(Calendar.MONTH, -1)
                    currentMonthDate = calendar.time
                }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev Month", tint = TextPrimary)
                }

                Text(
                    text = currentMonthStr,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryViolet
                )

                IconButton(onClick = {
                    calendar.time = currentMonthDate
                    calendar.add(Calendar.MONTH, 1)
                    currentMonthDate = calendar.time
                }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Month", tint = TextPrimary)
                }
            }
        }

        // Hero total spend — Gradient card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(GradientStart, GradientMid)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOTAL MONTH SPEND",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "₹${String.format("%,.2f", totalSpent)}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Comparison Badge — pill style with icon
                        if (totalSpentLastMonth > 0) {
                            val isIncrease = deltaPercent >= 0
                            val badgeBg = if (isIncrease) DebitRed.copy(alpha = 0.25f) else SuccessGreen.copy(alpha = 0.25f)
                            val badgeText = if (isIncrease) DebitRed else SuccessGreen
                            val arrow = if (isIncrease) "📈" else "📉"
                            Box(
                                modifier = Modifier
                                    .background(badgeBg, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$arrow ${String.format("%.1f", Math.abs(deltaPercent))}% vs last month",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text(
                                text = "No data for previous month",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // Segmented Distribution Bar
        if (categoryBreakdown.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎯 Spending Weight Breakdown",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Segmented bar with spacing between segments
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            categoryBreakdown.forEach { item ->
                                val weight = item.percentage.toFloat() / 100f
                                if (weight > 0.01f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(weight)
                                            .background(
                                                Color(android.graphics.Color.parseColor(item.color)),
                                                RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Spending Bar Chart Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 Daily Expenditure Trend",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    SpendBarChart(dailySpends = dailySpends)
                }
            }
        }

        // Category List breakdown with emojis
        if (categoryBreakdown.isNotEmpty()) {
            item {
                Text(
                    text = "🏷️ Category Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            
            items(categoryBreakdown) { item ->
                val catColor = try {
                    Color(android.graphics.Color.parseColor(item.color))
                } catch (e: Exception) {
                    PrimaryViolet
                }
                val emoji = getCategoryEmoji(item.icon)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface, RoundedCornerShape(14.dp))
                        .border(
                            width = 0.5.dp,
                            color = catColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Category emoji avatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(catColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item.categoryName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹${String.format("%,.0f", item.amount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .background(catColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${String.format("%.1f", item.percentage)}%",
                                fontSize = 11.sp,
                                color = catColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Top Payees / Merchants Card with medals
        if (topMerchants.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🏆 Top Payees this Month",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        topMerchants.forEachIndexed { index, (merchant, amount) ->
                            // Medal emojis for top 3, then numbers
                            val rankLabel = when (index) {
                                0 -> "🥇"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "${index + 1}."
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = rankLabel,
                                        fontSize = if (index < 3) 20.sp else 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index >= 3) TextMuted else Color.Unspecified,
                                        modifier = Modifier.width(32.dp)
                                    )
                                    Text(
                                        text = merchant,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "₹${String.format("%,.2f", amount)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DebitRed
                                )
                            }
                            if (index < topMerchants.size - 1) {
                                HorizontalDivider(color = Divider, thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CategoryBreakdownItem(
    val categoryName: String,
    val amount: Double,
    val percentage: Double,
    val color: String,
    val icon: String = "more_horiz"
)
