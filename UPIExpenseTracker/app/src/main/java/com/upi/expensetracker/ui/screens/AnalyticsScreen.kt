package com.upi.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.upi.expensetracker.ui.MainViewModel
import com.upi.expensetracker.ui.components.SpendBarChart
import com.upi.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    val calendar = remember { Calendar.getInstance() }
    var currentMonthDate by remember { mutableStateOf(calendar.time) }

    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthFilterFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    val currentMonthStr = monthYearFormat.format(currentMonthDate)
    val currentMonthFilter = monthFilterFormat.format(currentMonthDate)

    val lastMonthFilter = remember(currentMonthDate) {
        val cal = Calendar.getInstance()
        cal.time = currentMonthDate
        cal.add(Calendar.MONTH, -1)
        monthFilterFormat.format(cal.time)
    }

    val monthTransactions = allTransactions.filter { it.date.startsWith(currentMonthFilter) }
    val lastMonthTransactions = allTransactions.filter { it.date.startsWith(lastMonthFilter) }

    val totalSpent = monthTransactions.sumOf { it.amount }
    val totalSpentLastMonth = lastMonthTransactions.sumOf { it.amount }

    val deltaPercent = if (totalSpentLastMonth > 0) {
        ((totalSpent - totalSpentLastMonth) / totalSpentLastMonth) * 100
    } else 0.0

    val categoryBreakdown = monthTransactions
        .groupBy { it.category }
        .map { (catName, txns) ->
            val sum = txns.sumOf { it.amount }
            val catColor = categories.find { it.name == catName }?.color ?: "#00BFA6"
            CategoryBreakdownItem(
                categoryName = catName,
                amount = sum,
                percentage = if (totalSpent > 0) (sum / totalSpent) * 100 else 0.0,
                color = catColor
            )
        }.sortedByDescending { it.amount }

    val topMerchants = monthTransactions
        .groupBy { it.merchant }
        .map { (name, txns) -> Pair(name, txns.sumOf { it.amount }) }
        .sortedByDescending { it.second }
        .take(5)

    val dailySpends = remember(monthTransactions) {
        val spends = DoubleArray(30) { 0.0 }
        monthTransactions.forEach { txn ->
            try {
                val day = txn.date.split("-")[2].toInt()
                if (day in 1..30) spends[day - 1] += txn.amount
            } catch (_: Exception) {}
        }
        spends.toList()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        // Month selector
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
                }) { Icon(Icons.Default.KeyboardArrowLeft, "Previous", tint = TextPrimary) }
                Text(currentMonthStr, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                IconButton(onClick = {
                    calendar.time = currentMonthDate
                    calendar.add(Calendar.MONTH, 1)
                    currentMonthDate = calendar.time
                }) { Icon(Icons.Default.KeyboardArrowRight, "Next", tint = TextPrimary) }
            }
        }

        // Total spend card — clean, flat
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Divider)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Spent", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "₹${String.format("%,.2f", totalSpent)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Delta pill — semantic colors
                    if (totalSpentLastMonth > 0) {
                        val isIncrease = deltaPercent >= 0
                        val pillColor = if (isIncrease) DebitRed else SuccessGreen
                        val arrow = if (isIncrease) "↑" else "↓"
                        Box(
                            modifier = Modifier
                                .background(pillColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "$arrow ${String.format("%.1f", Math.abs(deltaPercent))}% vs last month",
                                fontSize = 12.sp,
                                color = pillColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Segmented bar — category colors used here (data viz)
        if (categoryBreakdown.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Divider)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Spending Breakdown", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().height(10.dp),
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
                                                try { Color(android.graphics.Color.parseColor(item.color)) } catch (_: Exception) { Accent },
                                                RoundedCornerShape(3.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily bar chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Divider)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daily Spending", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 16.dp))
                    SpendBarChart(dailySpends = dailySpends)
                }
            }
        }

        // Category details
        if (categoryBreakdown.isNotEmpty()) {
            item {
                Text("Category Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 4.dp))
            }
            items(categoryBreakdown) { item ->
                val catColor = try { Color(android.graphics.Color.parseColor(item.color)) } catch (_: Exception) { Accent }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Divider)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Color dot
                            Box(modifier = Modifier.size(10.dp).background(catColor, CircleShape))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(item.categoryName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("₹${String.format("%,.0f", item.amount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("${String.format("%.1f", item.percentage)}%", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Top merchants — numbered list
        if (topMerchants.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Divider)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Top Payees", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                        topMerchants.forEachIndexed { index, (merchant, amount) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${index + 1}.",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Text(merchant, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                }
                                Text("₹${String.format("%,.2f", amount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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
    val color: String
)
