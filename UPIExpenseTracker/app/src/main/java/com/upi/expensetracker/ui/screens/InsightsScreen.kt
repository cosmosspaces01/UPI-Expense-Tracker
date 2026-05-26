package com.upi.expensetracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val trendData = remember(allTransactions) {
        val list = mutableListOf<Pair<String, Double>>()
        val cal = Calendar.getInstance()
        val monthLabel = SimpleDateFormat("MMM", Locale.US)
        val monthQuery = SimpleDateFormat("yyyy-MM", Locale.US)
        cal.add(Calendar.MONTH, -5)
        for (i in 0 until 6) {
            val label = monthLabel.format(cal.time)
            val prefix = monthQuery.format(cal.time)
            list.add(Pair(label, allTransactions.filter { it.date.startsWith(prefix) }.sumOf { it.amount }))
            cal.add(Calendar.MONTH, 1)
        }
        list
    }

    val insights = remember(allTransactions) {
        val list = mutableListOf<String>()
        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val monthTxns = allTransactions.filter { it.date.startsWith(currentMonthPrefix) }

        if (monthTxns.isNotEmpty()) {
            val daySpends = monthTxns.groupBy { it.date }.mapValues { it.value.sumOf { t -> t.amount } }
            daySpends.maxByOrNull { it.value }?.let {
                try {
                    val formatted = SimpleDateFormat("dd MMM", Locale.US).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it.key)!!)
                    list.add("Your highest spending day this month was $formatted — ₹${String.format("%,.0f", it.value)} spent.")
                } catch (_: Exception) {}
            }
            monthTxns.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }.maxByOrNull { it.value }?.let {
                list.add("Top category this month: ${it.key} (₹${String.format("%,.0f", it.value)}).")
            }
        }
        if (allTransactions.size > 5) {
            val catAvgs = allTransactions.groupBy { it.category }.mapValues { it.value.map { t -> t.amount }.average() }
            allTransactions.find { t -> val avg = catAvgs[t.category] ?: 0.0; avg > 0 && t.amount > avg * 2.5 && t.date.startsWith(currentMonthPrefix) }?.let {
                list.add("Unusual spend: ₹${String.format("%,.0f", it.amount)} at ${it.merchant} is much higher than your ${it.category} average.")
            }
        }
        if (list.isEmpty()) {
            list.add("Sync more transactions to see spending insights and trends here.")
        }
        list
    }

    val subscriptionsList = remember(recurringTxns) {
        recurringTxns.groupBy { it.merchant }.map { (merchant, txns) ->
            Triple(merchant, txns.first().amount, txns.first().date)
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Background)) {
        TopAppBar(
            title = { Text("Insights", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Insight carousel — subtle accent left border
            item {
                Text("Highlights", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(insights.size) { index ->
                        Card(
                            modifier = Modifier.width(280.dp).height(100.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            border = BorderStroke(1.dp, Divider)
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(Accent))
                                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.CenterStart) {
                                    Text(insights[index], fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Trend chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = BorderStroke(1.dp, Divider)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("6-Month Trend", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 16.dp))
                        TrendLineChart(monthlySpends = trendData)
                    }
                }
            }

            // Subscriptions
            item { Text("Subscriptions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }

            if (subscriptionsList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        border = BorderStroke(1.dp, Divider)
                    ) {
                        Text(
                            "No recurring payments detected yet. Sync more transactions to auto-detect subscriptions.",
                            fontSize = 13.sp, color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(subscriptionsList) { (merchant, cost, date) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        border = BorderStroke(1.dp, Divider)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(merchant, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                                Text("Last: $date", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${String.format("%.0f", cost)}/mo", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                Text("Recurring", fontSize = 11.sp, color = Accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
