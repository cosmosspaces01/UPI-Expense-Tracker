package com.upi.expensetracker.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val context = LocalContext.current
    var isSyncing by remember { mutableStateOf(false) }

    val categoryMap = remember(categories) { categories.associateBy { it.name } }

    val infiniteTransition = rememberInfiniteTransition(label = "syncSpin")
    val syncRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "syncRotation"
    )

    val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val displayDateFormat = SimpleDateFormat("dd\nMMM", Locale.US)

    val calendarList = remember {
        val list = mutableListOf<Pair<String, String>>()
        val cal = Calendar.getInstance()
        for (i in 0 until 14) {
            list.add(Pair(dbDateFormat.format(cal.time), displayDateFormat.format(cal.time)))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        list
    }

    var selectedDate by remember { mutableStateOf(calendarList.first().first) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Newest") }
    var minAmountText by remember { mutableStateOf("") }
    var maxAmountText by remember { mutableStateOf("") }

    var useDateRange by remember { mutableStateOf(false) }
    var dateRangeStart by remember { mutableStateOf("") }
    var dateRangeEnd by remember { mutableStateOf("") }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val minAmount = minAmountText.toDoubleOrNull()
    val maxAmount = maxAmountText.toDoubleOrNull()

    val filteredTransactions = allTransactions.filter { txn ->
        val matchesDate = if (useDateRange) {
            (dateRangeStart.isEmpty() || txn.date >= dateRangeStart) &&
            (dateRangeEnd.isEmpty() || txn.date <= dateRangeEnd)
        } else { txn.date == selectedDate }
        val matchesSearch = txn.merchant.lowercase().contains(searchQuery.lowercase()) ||
                txn.notes.lowercase().contains(searchQuery.lowercase()) ||
                txn.amount.toString().contains(searchQuery)
        val matchesCategory = selectedCategoryFilter == "All" || txn.category == selectedCategoryFilter
        val matchesMin = minAmount == null || txn.amount >= minAmount
        val matchesMax = maxAmount == null || txn.amount <= maxAmount
        matchesDate && matchesSearch && matchesCategory && matchesMin && matchesMax
    }.sortedWith { a, b ->
        when (sortBy) {
            "Oldest" -> a.time.compareTo(b.time)
            "Highest" -> b.amount.compareTo(a.amount)
            "Lowest" -> a.amount.compareTo(b.amount)
            else -> b.time.compareTo(a.time)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Date selector header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (useDateRange) "Date Range Filter" else "Select Date",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { useDateRange = !useDateRange }
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Date Range",
                        tint = if (useDateRange) Accent else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Range",
                        fontSize = 12.sp,
                        color = if (useDateRange) Accent else TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (useDateRange) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (dateRangeStart.isNotEmpty()) Accent else Divider),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface)
                    ) {
                        Text(
                            text = if (dateRangeStart.isNotEmpty()) "From: $dateRangeStart" else "Start Date",
                            fontSize = 12.sp,
                            color = if (dateRangeStart.isNotEmpty()) TextPrimary else TextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (dateRangeEnd.isNotEmpty()) Accent else Divider),
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
                // Single-day scroll selector
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    items(calendarList) { (dbDate, displayDate) ->
                        val isSelected = dbDate == selectedDate
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(64.dp)
                                .background(
                                    if (isSelected) Accent else Surface,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedDate = dbDate }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayDate,
                                color = if (isSelected) Background else TextMuted,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Sync selected date
        Button(
            onClick = {
                if (!isSyncing) {
                    isSyncing = true
                    val cal = Calendar.getInstance()
                    try { dbDateFormat.parse(selectedDate)?.let { cal.time = it } } catch (_: Exception) {}
                    cal.set(Calendar.HOUR_OF_DAY, 12)
                    viewModel.syncTransactionsForDate(cal.timeInMillis) { count ->
                        isSyncing = false
                        Toast.makeText(context,
                            if (count > 0) "Synced $count transactions" else "No new transactions found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(42.dp),
            enabled = !isSyncing && !useDateRange,
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                disabledContainerColor = SurfaceElevated
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Refresh, "Sync",
                    tint = if (!isSyncing && !useDateRange) Background else TextMuted,
                    modifier = Modifier.size(16.dp).then(if (isSyncing) Modifier.rotate(syncRotation) else Modifier)
                )
                Text(
                    text = if (isSyncing) "Syncing..." else "Sync SMS for $selectedDate",
                    fontSize = 13.sp,
                    color = if (!isSyncing && !useDateRange) Background else TextMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Search & filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search merchant or notes...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, "Search", tint = TextMuted, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Divider,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Amount range
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
                        focusedContainerColor = SurfaceElevated, unfocusedContainerColor = SurfaceElevated,
                        focusedBorderColor = Accent, unfocusedBorderColor = Divider,
                        focusedLabelColor = Accent, unfocusedLabelColor = TextSecondary,
                        cursorColor = Accent
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
                        focusedContainerColor = SurfaceElevated, unfocusedContainerColor = SurfaceElevated,
                        focusedBorderColor = Accent, unfocusedBorderColor = Divider,
                        focusedLabelColor = Accent, unfocusedLabelColor = TextSecondary,
                        cursorColor = Accent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Category filters — uniform accent color
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == "All",
                        onClick = { selectedCategoryFilter = "All" },
                        label = { Text("All", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Accent,
                            containerColor = Surface,
                            labelColor = TextMuted,
                            selectedLabelColor = Background
                        ),
                        border = null
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat.name,
                        onClick = { selectedCategoryFilter = cat.name },
                        label = { Text(cat.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Accent,
                            containerColor = Surface,
                            labelColor = TextMuted,
                            selectedLabelColor = Background
                        ),
                        border = null
                    )
                }
            }

            // Sort bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${filteredTransactions.size} found", fontSize = 11.sp, color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("New" to "Newest", "Old" to "Oldest", "High" to "Highest", "Low" to "Lowest").forEach { (label, sortOption) ->
                        val isSelected = sortBy == sortOption
                        TextButton(
                            onClick = { sortBy = sortOption },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Accent else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Transaction list
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transactions for this date",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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

    // Date Picker Dialogs
    if (showStartDatePicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { dateRangeStart = dbDateFormat.format(Date(it)) }; showStartDatePicker = false }) { Text("OK", color = Accent, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("CANCEL", color = TextPrimary) } },
            colors = DatePickerDefaults.colors(containerColor = Surface)
        ) { DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = Surface, titleContentColor = TextPrimary, headlineContentColor = TextPrimary, weekdayContentColor = TextSecondary, dayContentColor = TextPrimary, selectedDayContainerColor = Accent, todayContentColor = Accent, todayDateBorderColor = Accent)) }
    }
    if (showEndDatePicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { dateRangeEnd = dbDateFormat.format(Date(it)) }; showEndDatePicker = false }) { Text("OK", color = Accent, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("CANCEL", color = TextPrimary) } },
            colors = DatePickerDefaults.colors(containerColor = Surface)
        ) { DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = Surface, titleContentColor = TextPrimary, headlineContentColor = TextPrimary, weekdayContentColor = TextSecondary, dayContentColor = TextPrimary, selectedDayContainerColor = Accent, todayContentColor = Accent, todayDateBorderColor = Accent)) }
    }
}
