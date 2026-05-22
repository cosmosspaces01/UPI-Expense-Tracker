package com.upi.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
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
import com.upi.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: MainViewModel,
    onEditTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    // Date Format Helpers
    val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val displayDateFormat = SimpleDateFormat("dd\nMMM", Locale.US)

    // Calendar state list (past 14 days)
    val calendarList = remember {
        val list = mutableListOf<Pair<String, String>>() // Pair of dbDateString, displayString
        val cal = Calendar.getInstance()
        for (i in 0 until 14) {
            val date = cal.time
            list.add(Pair(dbDateFormat.format(date), displayDateFormat.format(date)))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        list
    }

    var selectedDate by remember { mutableStateOf(calendarList.first().first) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Newest") } // Newest, Oldest, Highest, Lowest

    // Amount range filter state
    var minAmountText by remember { mutableStateOf("") }
    var maxAmountText by remember { mutableStateOf("") }

    // Date range mode state
    var useDateRange by remember { mutableStateOf(false) }
    var dateRangeStart by remember { mutableStateOf("") } // yyyy-MM-dd
    var dateRangeEnd by remember { mutableStateOf("") }   // yyyy-MM-dd
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Parse amount filters safely
    val minAmount = minAmountText.toDoubleOrNull()
    val maxAmount = maxAmountText.toDoubleOrNull()

    // Filtered transaction list
    val filteredTransactions = allTransactions.filter { txn ->
        // Date filter: either single-day or date range mode
        val matchesDate = if (useDateRange) {
            val afterStart = dateRangeStart.isEmpty() || txn.date >= dateRangeStart
            val beforeEnd = dateRangeEnd.isEmpty() || txn.date <= dateRangeEnd
            afterStart && beforeEnd
        } else {
            txn.date == selectedDate
        }

        val matchesSearch = txn.merchant.lowercase().contains(searchQuery.lowercase()) ||
                txn.notes.lowercase().contains(searchQuery.lowercase()) ||
                txn.amount.toString().contains(searchQuery)
        val matchesCategory = selectedCategoryFilter == "All" || txn.category == selectedCategoryFilter

        // Amount range filter
        val matchesMinAmount = minAmount == null || txn.amount >= minAmount
        val matchesMaxAmount = maxAmount == null || txn.amount <= maxAmount

        matchesDate && matchesSearch && matchesCategory && matchesMinAmount && matchesMaxAmount
    }.sortedWith { a, b ->
        when (sortBy) {
            "Oldest" -> a.time.compareTo(b.time)
            "Highest" -> b.amount.compareTo(a.amount)
            "Lowest" -> a.amount.compareTo(b.amount)
            else -> b.time.compareTo(a.time) // "Newest"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Date Mode Toggle + Date Selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // Toggle between single-day and date range
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (useDateRange) "Date Range Filter" else "Select Transaction Date",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Date Range",
                        tint = if (useDateRange) PrimaryPurple else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Range",
                        fontSize = 12.sp,
                        color = if (useDateRange) PrimaryPurple else TextSecondary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { useDateRange = !useDateRange }
                    )
                }
            }

            if (useDateRange) {
                // Date Range Picker Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start date button
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (dateRangeStart.isNotEmpty()) PrimaryPurple else Color(0xFF2C2C2C)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBackground)
                    ) {
                        Text(
                            text = if (dateRangeStart.isNotEmpty()) "From: $dateRangeStart" else "Start Date",
                            fontSize = 12.sp,
                            color = if (dateRangeStart.isNotEmpty()) TextPrimary else TextSecondary
                        )
                    }

                    // End date button
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (dateRangeEnd.isNotEmpty()) PrimaryPurple else Color(0xFF2C2C2C)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBackground)
                    ) {
                        Text(
                            text = if (dateRangeEnd.isNotEmpty()) "To: $dateRangeEnd" else "End Date",
                            fontSize = 12.sp,
                            color = if (dateRangeEnd.isNotEmpty()) TextPrimary else TextSecondary
                        )
                    }
                }
            } else {
                // Single-day scroll selector (existing)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    items(calendarList) { (dbDate, displayDate) ->
                        val isSelected = dbDate == selectedDate
                        val boxBg = if (isSelected) PrimaryPurple else CardBackground
                        val textColor = if (isSelected) TextPrimary else TextSecondary

                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(64.dp)
                                .background(boxBg, RoundedCornerShape(12.dp))
                                .clickable { selectedDate = dbDate }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayDate,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Search & Filter Toolbar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by merchant or notes...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Amount Range Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = minAmountText,
                    onValueChange = { minAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                    placeholder = { Text("Min ₹", color = TextSecondary, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = maxAmountText,
                    onValueChange = { maxAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                    placeholder = { Text("Max ₹", color = TextSecondary, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Category filters horizontal row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    val isSelected = selectedCategoryFilter == "All"
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = "All" },
                        label = { Text("All Categories") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple,
                            containerColor = CardBackground,
                            labelColor = TextSecondary,
                            selectedLabelColor = TextPrimary
                        ),
                        border = null
                    )
                }
                items(categories) { cat ->
                    val isSelected = selectedCategoryFilter == cat.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = cat.name },
                        label = { Text(cat.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple,
                            containerColor = CardBackground,
                            labelColor = TextSecondary,
                            selectedLabelColor = TextPrimary
                        ),
                        border = null
                    )
                }
            }

            // Sorting bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredTransactions.size} transactions found",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "Sort:", fontSize = 12.sp, color = TextSecondary)
                    listOf("Newest", "Oldest", "Highest", "Lowest").forEach { sortOption ->
                        val isSelected = sortBy == sortOption
                        val color = if (isSelected) PrimaryPurple else TextSecondary
                        Text(
                            text = sortOption,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = color,
                            modifier = Modifier
                                .clickable { sortBy = sortOption }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        // Transactions List
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💸",
                        fontSize = 32.sp
                    )
                    Text(
                        text = "No transactions found for this selection.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { txn ->
                    TransactionItemCard(
                        transaction = txn,
                        onClick = { onEditTransaction(txn) }
                    )
                }
            }
        }
    }

    // DatePickerDialog for Start Date
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        dateRangeStart = dbDateFormat.format(Date(millis))
                    }
                    showStartDatePicker = false
                }) {
                    Text("OK", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("CANCEL", color = TextPrimary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = CardBackground)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = CardBackground,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = PrimaryPurple,
                    todayContentColor = PrimaryPurple,
                    todayDateBorderColor = PrimaryPurple
                )
            )
        }
    }

    // DatePickerDialog for End Date
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        dateRangeEnd = dbDateFormat.format(Date(millis))
                    }
                    showEndDatePicker = false
                }) {
                    Text("OK", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("CANCEL", color = TextPrimary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = CardBackground)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = CardBackground,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = PrimaryPurple,
                    todayContentColor = PrimaryPurple,
                    todayDateBorderColor = PrimaryPurple
                )
            )
        }
    }
}
