package com.upi.expensetracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upi.expensetracker.data.CategoryEntity
import com.upi.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bottom sheet for manually adding a transaction when the user
 * didn't receive an SMS or wants to record a cash/other expense.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    categories: List<CategoryEntity>,
    preselectedDate: String, // yyyy-MM-dd format
    onDismiss: () -> Unit,
    onAdd: (amount: Double, merchant: String, category: String, date: String, time: String, description: String, notes: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Form states
    var amountText by remember { mutableStateOf("") }
    var merchantName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(if (categories.isNotEmpty()) categories[0].name else "Others") }
    var selectedDate by remember { mutableStateOf(preselectedDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Current time as default
    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.US).format(Date())
    }

    // Validation
    val isValid = amountText.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(SurfaceElevated, RoundedCornerShape(2.dp))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title
            Text(
                text = "Add Transaction",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Manually record an expense for $selectedDate",
                fontSize = 13.sp,
                color = TextMuted
            )

            // Amount Input (required)
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount (₹) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceElevated,
                    unfocusedContainerColor = SurfaceElevated,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = AccentBlueMid,
                    focusedLabelColor = AccentBlue,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentBlue
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Merchant / Payee Name
            OutlinedTextField(
                value = merchantName,
                onValueChange = { merchantName = it },
                label = { Text("Merchant / Payee Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceElevated,
                    unfocusedContainerColor = SurfaceElevated,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = AccentBlueMid,
                    focusedLabelColor = AccentBlue,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentBlue
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Reason / Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Reason / Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceElevated,
                    unfocusedContainerColor = SurfaceElevated,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = AccentBlueMid,
                    focusedLabelColor = AccentBlue,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentBlue
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Date selector row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Date", fontSize = 12.sp, color = TextMuted)
                    Text(selectedDate, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AccentBlueMid),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
                ) {
                    Text("Change Date", fontSize = 12.sp, color = AccentBlue)
                }
            }

            // Category Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = category.name == selectedCategory
                        val chipBg = if (isSelected) AccentBlueMid else Divider
                        val chipText = if (isSelected) TextPrimary else TextMuted
                        val chipBorder = if (isSelected) AccentBlue else AccentBlueMid

                        Box(
                            modifier = Modifier
                                .background(chipBg, RoundedCornerShape(20.dp))
                                .border(BorderStroke(1.dp, chipBorder), RoundedCornerShape(20.dp))
                                .clickable { selectedCategory = category.name }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            Color(android.graphics.Color.parseColor(category.color)),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                                Text(
                                    text = category.name,
                                    color = chipText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Notes (optional)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceElevated,
                    unfocusedContainerColor = SurfaceElevated,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = AccentBlueMid,
                    focusedLabelColor = AccentBlue,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentBlue
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Add Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onAdd(
                            amount,
                            merchantName,
                            selectedCategory,
                            selectedDate,
                            currentTime,
                            description,
                            notes
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    disabledContainerColor = SurfaceElevated
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Add Expense",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isValid) TextPrimary else TextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // DatePickerDialog for changing date
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = AccentBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
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
                    weekdayContentColor = TextMuted,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = AccentBlue,
                    todayContentColor = AccentBlue,
                    todayDateBorderColor = AccentBlue
                )
            )
        }
    }
}
