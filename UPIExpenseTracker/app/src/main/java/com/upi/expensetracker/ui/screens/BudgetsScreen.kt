package com.upi.expensetracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upi.expensetracker.data.CategoryEntity
import com.upi.expensetracker.ui.MainViewModel
import com.upi.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories by viewModel.allCategories.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    val currentMonthFilter = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
    val monthTransactions = allTransactions.filter { it.date.startsWith(currentMonthFilter) }
    val categorySpends = remember(monthTransactions) {
        monthTransactions.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val totalSpentInMonth = monthTransactions.sumOf { it.amount }
    val totalBudgeted = categories.filter { it.budget != null }.sumOf { it.budget ?: 0.0 }
    val remainingBudget = totalBudgeted - totalSpentInMonth

    var selectedCategoryForBudget by remember { mutableStateOf<CategoryEntity?>(null) }
    var budgetInputStr by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().background(Background)) {
        TopAppBar(
            title = { Text("Budgets", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Overview card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = BorderStroke(1.dp, Divider)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Budget", fontSize = 12.sp, color = TextSecondary)
                            Text("₹${String.format("%,.0f", totalBudgeted)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Spent", fontSize = 12.sp, color = TextSecondary)
                            Text("₹${String.format("%,.0f", totalSpentInMonth)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Remaining", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                "₹${String.format("%,.0f", remainingBudget)}",
                                fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                color = if (remainingBudget >= 0) SuccessGreen else DebitRed
                            )
                        }
                    }
                }
            }

            item { Text("Categories", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }

            items(categories) { category ->
                val limit = category.budget
                val spent = categorySpends[category.name] ?: 0.0
                val catColor = try { Color(android.graphics.Color.parseColor(category.color)) } catch (_: Exception) { Accent }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedCategoryForBudget = category
                        budgetInputStr = limit?.toString() ?: ""
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = BorderStroke(1.dp, Divider)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(catColor, CircleShape))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(category.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            if (limit != null) {
                                Text("₹${spent.toInt()} / ₹${limit.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            } else {
                                Text("Set limit", fontSize = 12.sp, color = Accent, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (limit != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val pct = (spent / limit).coerceIn(0.0, 1.0)
                            // Semantic progress color
                            val barColor = when {
                                pct >= 1.0 -> DebitRed
                                pct >= 0.8 -> WarningAmber
                                else -> Accent
                            }
                            Box(
                                modifier = Modifier.fillMaxWidth().height(6.dp).background(SurfaceElevated, RoundedCornerShape(3.dp))
                            ) {
                                Box(modifier = Modifier.fillMaxWidth(pct.toFloat()).fillMaxHeight().background(barColor, RoundedCornerShape(3.dp)))
                            }
                            if (pct >= 0.8) {
                                Text(
                                    if (pct >= 1.0) "Over budget" else "Approaching limit",
                                    fontSize = 10.sp, color = barColor, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedCategoryForBudget != null) {
        val cat = selectedCategoryForBudget!!
        AlertDialog(
            onDismissRequest = { selectedCategoryForBudget = null },
            title = { Text("Budget for ${cat.name}", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter monthly limit in rupees (leave blank to clear):", color = TextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = budgetInputStr,
                        onValueChange = { budgetInputStr = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Divider, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.updateCategory(cat.copy(budget = budgetInputStr.toDoubleOrNull())); selectedCategoryForBudget = null; Toast.makeText(context, "Budget updated", Toast.LENGTH_SHORT).show() }) { Text("SAVE", color = Accent, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { selectedCategoryForBudget = null }) { Text("CANCEL", color = TextPrimary) } },
            containerColor = Surface
        )
    }
}
