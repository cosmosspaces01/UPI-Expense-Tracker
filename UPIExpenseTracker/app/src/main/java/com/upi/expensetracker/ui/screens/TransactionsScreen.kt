package com.upi.expensetracker.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.animation.core.*
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
    val context = LocalContext.current
    var isSyncing by remember { mutableStateOf(false) }

    // Build category lookup map
    val categoryMap = remember(categories) {
        categories.associateBy { it.name }
    }

    // Sync icon rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "syncSpin")
    val syncRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "syncRotation"
    )

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
            .background(Background)
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
                    .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (useDateRange) "📅 Date Range Filter" else "📅 Select Transaction Date",
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
                        tint = if (useDateRange) PrimaryViolet else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Range",
                        fontSize = 12.sp,
                        color = if (useDateRange) PrimaryViolet else TextMuted,
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
                        border = BorderStroke(1.dp, if (dateRangeStart.isNotEmpty()) PrimaryViolet else Divider),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface)
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
                        border = BorderStroke(1.dp, if (dateRangeEnd.isNotEmpty()) PrimaryViolet else Divider),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface)
                    ) {
                        Text(
                            text = if (dateRangeEnd.isNotEmpty()) "To: $dateRangeEnd" else "End Date",
                            fontSize = 12.sp,
                            color = if (dateRangeEnd.isNotEmpty()) TextPrimary else TextSecondary
                        )
                    }
                }
            } else {
                // Single-day scroll selector with gradient selected chip
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    items(calendarList) { (dbDate, displayDate) ->
                        val isSelected = dbDate == selectedDate

                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(68.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(PrimaryViolet, PrimaryPink)
                                            ),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                    } else {
                                        Modifier.background(Surface, RoundedCornerShape(14.dp))
                                    }
                                )
                                .clickable { selectedDate = dbDate }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = displayDate,
                                    color = if (isSelected) Color.White else TextMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                                // Dot indicator for selected
                                if (isSelected) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(Color.White, RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sync button for the selected date — gradient style
        Button(
            onClick = {
                if (!isSyncing) {
                    isSyncing = true
                    val cal = Calendar.getInstance()
                    try {
                        val parsed = dbDateFormat.parse(selectedDate)
                        if (parsed != null) cal.time = parsed
                    } catch (e: Exception) { /* use today */ }
                    cal.set(Calendar.HOUR_OF_DAY, 12)
                    viewModel.syncTransactionsForDate(cal.timeInMillis) { count ->
                        isSyncing = false
                        Toast.makeText(
                            context,
                            if (count > 0) "✅ Synced $count transactions" else "No new transactions found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            enabled = !isSyncing && !useDateRange,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = SurfaceElevated
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        brush = if (!isSyncing && !useDateRange) {
                            Brush.horizontalGradient(listOf(PrimaryViolet, PrimaryPink))
                        } else {
                            Brush.horizontalGradient(listOf(SurfaceElevated, SurfaceElevated))
                        },
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSyncing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Syncing",
                            tint = TextPrimary,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(syncRotation)
                        )
                        Text("Syncing...", fontSize = 13.sp, color = TextPrimary)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Sync SMS for $selectedDate",
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Search & Filter Toolbar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search Input with gradient focus border
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by merchant or notes...", color = TextSecondary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                    focusedBorderColor = PrimaryViolet,
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
                    label = { Text("Min ₹", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated,
                        focusedBorderColor = PrimaryViolet,
                        unfocusedBorderColor = PrimaryMuted,
                        focusedLabelColor = PrimaryViolet,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryViolet
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = maxAmountText,
                    onValueChange = { maxAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Max ₹", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated,
                        focusedBorderColor = PrimaryViolet,
                        unfocusedBorderColor = PrimaryMuted,
                        focusedLabelColor = PrimaryViolet,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryViolet
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Category filters — each chip uses its own category color
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    val isSelected = selectedCategoryFilter == "All"
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = "All" },
                        label = { Text("All", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryViolet,
                            containerColor = Surface,
                            labelColor = TextMuted,
                            selectedLabelColor = Color.White
                        ),
                        border = null
                    )
                }
                items(categories) { cat ->
                    val isSelected = selectedCategoryFilter == cat.name
                    val catColor = try {
                        Color(android.graphics.Color.parseColor(cat.color))
                    } catch (e: Exception) {
                        PrimaryViolet
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = cat.name },
                        label = { Text(cat.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = catColor,
                            containerColor = Surface,
                            labelColor = TextMuted,
                            selectedLabelColor = Color.White
                        ),
                        border = null
                    )
                }
            }

            // Sorting bar — count + sort options with gradient active pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredTransactions.size} found",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf("New", "Old", "High", "Low").forEachIndexed { index, label ->
                        val fullLabels = listOf("Newest", "Oldest", "Highest", "Lowest")
                        val sortOption = fullLabels[index]
                        val isSelected = sortBy == sortOption
                        Box(
                            modifier = Modifier
                                .then(
                                    if (isSelected) {
                                        Modifier.background(
                                            brush = Brush.horizontalGradient(
                                                listOf(PrimaryViolet, PrimaryPink)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    } else {
                                        Modifier.background(Color.Transparent, RoundedCornerShape(8.dp))
                                    }
                                )
                                .clickable { sortBy = sortOption }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
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
                        text = "🎉",
                        fontSize = 40.sp
                    )
                    Text(
                        text = "No spending here!",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your wallet is smiling 😊",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { txn ->
                    TransactionItemCard(
                        transaction = txn,
                        category = categoryMap[txn.category],
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
                    Text("OK", color = PrimaryViolet, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("CANCEL", color = TextPrimary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Surface)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Surface,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = PrimaryViolet,
                    todayContentColor = PrimaryViolet,
                    todayDateBorderColor = PrimaryViolet
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
                    Text("OK", color = PrimaryViolet, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("CANCEL", color = TextPrimary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Surface)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Surface,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = PrimaryViolet,
                    todayContentColor = PrimaryViolet,
                    todayDateBorderColor = PrimaryViolet
                )
            )
        }
    }
}
