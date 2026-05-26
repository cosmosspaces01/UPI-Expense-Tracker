package com.upi.expensetracker.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
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
            .background(Background)
    ) {
        // App Bar
        TopAppBar(
            title = { Text("🎯 Category Budgets", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Overview Dashboard — Gradient card
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
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "BUDGET OVERVIEW",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Total Limit", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                                    Text(
                                        text = "₹${String.format("%,.0f", totalBudgeted)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Spent", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                                    Text(
                                        text = "₹${String.format("%,.0f", totalSpentInMonth)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Remaining", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                                    val remColor = if (remainingBudget >= 0) SuccessGreen else DebitRed
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
            }

            item {
                Text(
                    text = "📋 Category Allowances",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Categories Budget list with gradient progress bars
            items(categories) { category ->
                val limit = category.budget
                val spent = categorySpends[category.name] ?: 0.0
                val emoji = getCategoryEmoji(category.icon)
                val catColor = try {
                    Color(android.graphics.Color.parseColor(category.color))
                } catch (e: Exception) {
                    PrimaryViolet
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface, RoundedCornerShape(16.dp))
                        .border(
                            width = 0.5.dp,
                            color = catColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        )
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
                                // Emoji avatar
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(catColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 16.sp)
                                }
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
                                    color = PrimaryViolet,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (limit != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val pct = (spent / limit).coerceIn(0.0, 1.0)

                            // Gradient progress bar: mint → amber → red based on percentage
                            val barGradient = when {
                                pct >= 0.8 -> Brush.horizontalGradient(listOf(AccentAmber, DebitRed))
                                pct >= 0.5 -> Brush.horizontalGradient(listOf(AccentMint, AccentAmber))
                                else -> Brush.horizontalGradient(listOf(AccentMint, AccentSky))
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(SurfaceElevated, RoundedCornerShape(4.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(pct.toFloat())
                                        .fillMaxHeight()
                                        .background(barGradient, RoundedCornerShape(4.dp))
                                )
                            }
                            
                            if (pct >= 0.8) {
                                Text(
                                    text = if (pct >= 1.0) "🚨 Budget Limit Exceeded!" else "📢 Budget exceeds 80% capacity!",
                                    fontSize = 10.sp,
                                    color = DebitRed,
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
                            focusedBorderColor = PrimaryViolet,
                            unfocusedBorderColor = PrimaryMuted,
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
                    Text(text = "SAVE", color = PrimaryViolet, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCategoryForBudget = null }) {
                    Text(text = "CANCEL", color = TextPrimary)
                }
            },
            containerColor = Surface
        )
    }
}
