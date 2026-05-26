package com.upi.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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
import com.upi.expensetracker.ui.components.TrendLineChart
import com.upi.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val recurringTxns by viewModel.recurringTransactions.collectAsState()

    // Rotating accent colors for insight cards
    val insightColors = listOf(PrimaryViolet, PrimaryPink, AccentAmber, AccentMint, AccentSky)

    // 1. Calculate 6-month trend
    val trendData = remember(allTransactions) {
        val list = mutableListOf<Pair<String, Double>>()
        val cal = Calendar.getInstance()
        val monthLabelFormat = SimpleDateFormat("MMM", Locale.US)
        val monthQueryFormat = SimpleDateFormat("yyyy-MM", Locale.US)

        // Generate past 6 months (oldest first)
        cal.add(Calendar.MONTH, -5)
        for (i in 0 until 6) {
            val label = monthLabelFormat.format(cal.time)
            val prefix = monthQueryFormat.format(cal.time)
            val monthSum = allTransactions.filter { it.date.startsWith(prefix) }.sumOf { it.amount }
            list.add(Pair(label, monthSum))
            cal.add(Calendar.MONTH, 1)
        }
        list
    }

    // 2. Dynamic Insights Generation
    val insights = remember(allTransactions) {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        // Insight A: Highest spend day this month
        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val monthTxns = allTransactions.filter { it.date.startsWith(currentMonthPrefix) }
        
        if (monthTxns.isNotEmpty()) {
            val daySpends = monthTxns.groupBy { it.date }.mapValues { it.value.sumOf { t -> t.amount } }
            val maxDayEntry = daySpends.maxByOrNull { it.value }
            if (maxDayEntry != null) {
                try {
                    val dateObj = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(maxDayEntry.key)
                    val formatted = SimpleDateFormat("dd MMM", Locale.US).format(dateObj!!)
                    list.add("📅 Your highest spending day this month was $formatted, where you spent ₹${String.format("%,.0f", maxDayEntry.value)}.")
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Insight B: Top category this month
            val catSpends = monthTxns.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }
            val maxCatEntry = catSpends.maxByOrNull { it.value }
            if (maxCatEntry != null) {
                list.add("🍔 Top category this month: ${maxCatEntry.key} (₹${String.format("%,.0f", maxCatEntry.value)} spent).")
            }
        }

        // Insight C: Unusual spending detection
        if (allTransactions.size > 5) {
            val catAvgs = allTransactions.groupBy { it.category }.mapValues { it.value.map { t -> t.amount }.average() }
            val unusualTxn = allTransactions.find { t ->
                val avg = catAvgs[t.category] ?: 0.0
                avg > 0 && t.amount > (avg * 2.5) && t.date.startsWith(currentMonthPrefix)
            }
            if (unusualTxn != null) {
                list.add("⚠️ Unusual Spend Alert: Your transaction of ₹${String.format("%,.0f", unusualTxn.amount)} at ${unusualTxn.merchant} is significantly higher than your average for ${unusualTxn.category}.")
            }
        }

        // Insight D: Week-over-week comparison per category
        try {
            val thisWeekCal = Calendar.getInstance()
            thisWeekCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val thisWeekStart = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(thisWeekCal.time)

            val lastWeekCal = Calendar.getInstance()
            lastWeekCal.add(Calendar.WEEK_OF_YEAR, -1)
            lastWeekCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val lastWeekStart = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(lastWeekCal.time)

            lastWeekCal.add(Calendar.DAY_OF_YEAR, 6)
            val lastWeekEnd = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(lastWeekCal.time)

            val thisWeekTxns = allTransactions.filter { it.date >= thisWeekStart && it.date <= todayStr }
            val lastWeekTxns = allTransactions.filter { it.date >= lastWeekStart && it.date <= lastWeekEnd }

            if (thisWeekTxns.isNotEmpty() && lastWeekTxns.isNotEmpty()) {
                val thisWeekByCategory = thisWeekTxns.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }
                val lastWeekByCategory = lastWeekTxns.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }

                var biggestIncreaseCat: String? = null
                var biggestIncreasePct = 0.0
                for ((cat, thisWeekTotal) in thisWeekByCategory) {
                    val lastWeekTotal = lastWeekByCategory[cat] ?: 0.0
                    if (lastWeekTotal > 0) {
                        val pctChange = ((thisWeekTotal - lastWeekTotal) / lastWeekTotal) * 100
                        if (pctChange > biggestIncreasePct) {
                            biggestIncreasePct = pctChange
                            biggestIncreaseCat = cat
                        }
                    }
                }
                if (biggestIncreaseCat != null && biggestIncreasePct > 10) {
                    list.add("📊 You spent ${String.format("%.0f", biggestIncreasePct)}% more on $biggestIncreaseCat this week vs last week.")
                }
            }
        } catch (e: Exception) {
            // ignore week-over-week calculation errors
        }

        // Default fallbacks if database is empty
        if (list.isEmpty()) {
            list.add("💡 Check back here after syncing your transactions to receive smart insights and savings tips!")
            list.add("📊 The app automatically groups recurring subscriptions like Netflix or Spotify to help you track repeat costs.")
        }
        list
    }

    // 3. Subscriptions aggregation
    val subscriptionsList = remember(recurringTxns) {
        recurringTxns.groupBy { it.merchant }.map { (merchant, txns) ->
            val lastTxn = txns.first()
            SubscriptionItem(
                merchant = merchant,
                cost = lastTxn.amount,
                date = lastTxn.date
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // App Bar
        TopAppBar(
            title = { Text("💡 Insights & Trends", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Insights Carousel Section with colored left borders
            item {
                Text(
                    text = "✨ Weekly Highlights",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(insights.size) { index ->
                        val insightText = insights[index]
                        val accentColor = insightColors[index % insightColors.size]

                        Card(
                            modifier = Modifier
                                .width(280.dp)
                                .height(110.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface)
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Colorful left accent border
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(
                                            brush = Brush.verticalGradient(
                                                listOf(accentColor, accentColor.copy(alpha = 0.3f))
                                            )
                                        )
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = insightText,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6-Month Trend Line Chart Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📈 6-Month Spending Trend",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        TrendLineChart(monthlySpends = trendData)
                    }
                }
            }

            // Subscriptions List Section
            item {
                Text(
                    text = "🔄 Your Subscriptions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (subscriptionsList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "🎵", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = "No recurring payments detected yet.",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Sync more transactions to auto-detect subscriptions.",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                items(subscriptionsList) { sub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface, RoundedCornerShape(16.dp))
                            .border(
                                width = 0.5.dp,
                                color = PrimaryViolet.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = sub.merchant,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Last payment: ${sub.date}",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "₹${String.format("%.2f", sub.cost)}/m",
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber,
                                fontSize = 14.sp
                            )
                            
                            Box(
                                modifier = Modifier
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(PrimaryViolet.copy(alpha = 0.2f), PrimaryPink.copy(alpha = 0.1f))
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "📅 Recurring",
                                    fontSize = 9.sp,
                                    color = PrimaryViolet,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SubscriptionItem(
    val merchant: String,
    val cost: Double,
    val date: String
)
