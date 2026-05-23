package com.upi.expensetracker.ui.screens

import androidx.compose.foundation.background
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
            val catColor = categories.find { it.name == catName }?.color ?: "#6C63FF"
            CategoryBreakdownItem(
                categoryName = catName,
                amount = sum,
                percentage = if (totalSpent > 0) (sum / totalSpent) * 100 else 0.0,
                color = catColor
            )
        }.sortedByDescending { it.amount }

    // Top 3 merchants
    val topMerchants = monthTransactions
        .groupBy { it.merchant }
        .map { (name, txns) -> Pair(name, txns.sumOf { it.amount }) }
        .sortedByDescending { it.second }
        .take(5)

    // Day-wise spend for the last 30 days
    val dailySpends = remember(monthTransactions) {
        val spends = DoubleArray(30) { 0.0 }
        val cal = Calendar.getInstance()
        // Map spends for days 1 to 30
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
            .background(DarkBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        // Month Selector
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
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

        // Hero total spend
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL MONTH SPEND",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₹${String.format("%,.2f", totalSpent)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Comparison Badge
                    if (totalSpentLastMonth > 0) {
                        val isIncrease = deltaPercent >= 0
                        val badgeColor = if (isIncrease) DebitRed else SuccessGreen
                        val arrow = if (isIncrease) "↑" else "↓"
                        Text(
                            text = "$arrow ${String.format("%.1f", Math.abs(deltaPercent))}% vs last month (₹${String.format("%,.0f", totalSpentLastMonth)})",
                            fontSize = 12.sp,
                            color = badgeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "No data for previous month",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
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
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Spending Weight Breakdown",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(SurfaceElevated, RoundedCornerShape(6.dp))
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
                                                RoundedCornerShape(2.dp)
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
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Expenditure Trend (30 Days)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    SpendBarChart(dailySpends = dailySpends)
                }
            }
        }

        // Category List breakdown
        if (categoryBreakdown.isNotEmpty()) {
            item {
                Text(
                    text = "Category Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            
            items(categoryBreakdown) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    Color(android.graphics.Color.parseColor(item.color)),
                                    RoundedCornerShape(6.dp)
                                )
                        )
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
                                .background(Divider, RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${String.format("%.1f", item.percentage)}%",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Top 3 Payees / Merchants Card
        if (topMerchants.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Top Payees this Month",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        topMerchants.forEachIndexed { index, (merchant, amount) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${index + 1}.",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlue,
                                        modifier = Modifier.width(20.dp)
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
                                Divider(color = Divider, thickness = 1.dp)
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
    val color: String
)
