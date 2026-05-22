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

    // Filtered transaction list
    val filteredTransactions = allTransactions.filter { txn ->
        val matchesDate = txn.date == selectedDate
        val matchesSearch = txn.merchant.lowercase().contains(searchQuery.lowercase()) ||
                txn.notes.lowercase().contains(searchQuery.lowercase()) ||
                txn.amount.toString().contains(searchQuery)
        val matchesCategory = selectedCategoryFilter == "All" || txn.category == selectedCategoryFilter

        matchesDate && matchesSearch && matchesCategory
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
        // Date Selector Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "Select Transaction Date",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, bottom = 8.dp)
            )
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
}
