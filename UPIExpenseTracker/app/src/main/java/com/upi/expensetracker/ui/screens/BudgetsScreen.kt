package com.upi.expensetracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    // Filter current month transactions
    val currentMonthFilter = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
    val monthTransactions = allTransactions.filter { it.date.startsWith(currentMonthFilter) }

    // Dialog State
    var selectedCategoryForBudget by remember { mutableStateOf<CategoryEntity?>(null) }
    var budgetInputStr by remember { mutableStateOf("") }

    // Calculations
    val categorySpends = remember(monthTransactions) {
        monthTransactions.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val totalSpentInMonth = monthTransactions.sumOf { it.amount }
    val totalBudgeted = categories.filter { it.budget != null }.sumOf { it.budget ?: 0.0 }
    val remainingBudget = totalBudgeted - totalSpentInMonth

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // App Bar
        TopAppBar(
            title = { Text("Category Budgets", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Overview Dashboard
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "BUDGET OVERVIEW",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Total Limit", fontSize = 13.sp, color = TextSecondary)
                                Text(
                                    text = "₹${String.format("%,.0f", totalBudgeted)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Spent", fontSize = 13.sp, color = TextSecondary)
                                Text(
                                    text = "₹${String.format("%,.0f", totalSpentInMonth)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Remaining", fontSize = 13.sp, color = TextSecondary)
                                val remColor = if (remainingBudget >= 0) SuccessGreen else WarningRed
                                Text(
                                    text = "₹${String.format("%,.0f", remainingBudget)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = remColor
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Category Allowances",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Categories Budget list
            items(categories) { category ->
                val limit = category.budget
                val spent = categorySpends[category.name] ?: 0.0
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(16.dp))
                        .clickable {
                            selectedCategoryForBudget = category
                            budgetInputStr = limit?.toString() ?: ""
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(android.graphics.Color.parseColor(category.color)), RoundedCornerShape(5.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = category.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            
                            if (limit != null) {
                                Text(
                                    text = "₹${spent.toInt()} / ₹${limit.toInt()}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            } else {
                                Text(
                                    text = "Set Limit",
                                    fontSize = 12.sp,
                                    color = PrimaryPurple,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (limit != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val pct = (spent / limit).coerceIn(0.0, 1.0)
                            val barColor = if (pct >= 0.8) WarningRed else SuccessGreen

                            // Native Compose progress bar matching design system
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(Color(0xFF2C2C2C), RoundedCornerShape(4.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(pct.toFloat())
                                        .fillMaxHeight()
                                        .background(barColor, RoundedCornerShape(4.dp))
                                )
                            }
                            
                            if (pct >= 0.8) {
                                Text(
                                    text = if (pct >= 1.0) "⚠️ Budget Limit Exceeded!" else "📢 Budget exceeds 80% capacity!",
                                    fontSize = 10.sp,
                                    color = WarningRed,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Set Budget Dialog
    if (selectedCategoryForBudget != null) {
        val cat = selectedCategoryForBudget!!
        AlertDialog(
            onDismissRequest = { selectedCategoryForBudget = null },
            title = { Text(text = "Limit for ${cat.name}", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Enter monthly limit in rupees (leave blank to clear budget):", color = TextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = budgetInputStr,
                        onValueChange = { budgetInputStr = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = Color(0xFF2C2C2C),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val inputVal = budgetInputStr.toDoubleOrNull()
                        val updatedCat = cat.copy(budget = inputVal)
                        viewModel.updateCategory(updatedCat)
                        selectedCategoryForBudget = null
                        Toast.makeText(context, "Budget limit updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(text = "SAVE", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCategoryForBudget = null }) {
                    Text(text = "CANCEL", color = TextPrimary)
                }
            },
            containerColor = CardBackground
        )
    }
}
