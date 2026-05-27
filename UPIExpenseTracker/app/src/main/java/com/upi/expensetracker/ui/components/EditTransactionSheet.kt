package com.upi.expensetracker.ui.components

import android.widget.Toast

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upi.expensetracker.data.CategoryEntity
import com.upi.expensetracker.data.TransactionEntity
import com.upi.expensetracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionSheet(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    var merchantName by remember { mutableStateOf(transaction.merchant) }
    var description by remember { mutableStateOf(transaction.description) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var notes by remember { mutableStateOf(transaction.notes) }
    var isSplit by remember { mutableStateOf(transaction.isSplit) }
    var splitWith by remember { mutableStateOf(transaction.splitWith) }
    var splitAmountStr by remember { mutableStateOf(transaction.splitAmount.toString()) }
    var isSettled by remember { mutableStateOf(transaction.isSettled) }

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Surface,
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 8.dp).width(40.dp).height(4.dp).background(SurfaceElevated, RoundedCornerShape(2.dp))) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Edit Transaction", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("₹${String.format("%.2f", transaction.amount)}  •  Ref: ${transaction.refId}", fontSize = 13.sp, color = TextMuted)

            OutlinedTextField(value = merchantName, onValueChange = { merchantName = it },
                label = { Text("Merchant / Payee Name") }, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = SurfaceElevated, unfocusedContainerColor = SurfaceElevated, focusedBorderColor = Accent, unfocusedBorderColor = Divider, focusedLabelColor = Accent, unfocusedLabelColor = TextSecondary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = Accent),
                shape = RoundedCornerShape(12.dp))

            OutlinedTextField(value = description, onValueChange = { description = it },
                label = { Text("Description") }, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = SurfaceElevated, unfocusedContainerColor = SurfaceElevated, focusedBorderColor = Accent, unfocusedBorderColor = Divider, focusedLabelColor = Accent, unfocusedLabelColor = TextSecondary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = Accent),
                shape = RoundedCornerShape(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        val isSelected = category.name == selectedCategory
                        FilterChip(
                            selected = isSelected, onClick = { selectedCategory = category.name },
                            label = { Text(category.name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent, containerColor = SurfaceElevated, labelColor = TextMuted, selectedLabelColor = Background),
                            border = null
                        )
                    }
                }
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it },
                label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = SurfaceElevated, unfocusedContainerColor = SurfaceElevated, focusedBorderColor = Accent, unfocusedBorderColor = Divider, focusedLabelColor = Accent, unfocusedLabelColor = TextSecondary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = Accent),
                shape = RoundedCornerShape(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Split this expense?", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Switch(checked = isSplit, onCheckedChange = { isSplit = it }, colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = Accent, uncheckedThumbColor = TextMuted, uncheckedTrackColor = SurfaceElevated))
            }

            if (isSplit) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = splitWith, onValueChange = { splitWith = it }, label = { Text("Split With") }, modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = SurfaceElevated, unfocusedContainerColor = SurfaceElevated, focusedBorderColor = Accent, unfocusedBorderColor = Divider, focusedLabelColor = Accent, unfocusedLabelColor = TextSecondary, cursorColor = Accent),
                        shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = splitAmountStr, onValueChange = { splitAmountStr = it }, label = { Text("Your Share (₹)") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = SurfaceElevated, unfocusedContainerColor = SurfaceElevated, focusedBorderColor = Accent, unfocusedBorderColor = Divider, focusedLabelColor = Accent, unfocusedLabelColor = TextSecondary, cursorColor = Accent),
                        shape = RoundedCornerShape(12.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Settled / Paid Back?", fontSize = 14.sp, color = TextSecondary)
                    Checkbox(checked = isSettled, onCheckedChange = { isSettled = it ?: false }, colors = CheckboxDefaults.colors(checkedColor = Accent, uncheckedColor = TextMuted))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    onSave(transaction.copy(merchant = merchantName.trim().ifEmpty { transaction.merchant }, description = description, category = selectedCategory, notes = notes, isSplit = isSplit,
                        splitWith = if (isSplit) splitWith else "", splitAmount = if (isSplit) (splitAmountStr.toDoubleOrNull() ?: 0.0) else 0.0, isSettled = if (isSplit) isSettled else false))
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Save Changes", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Background) }

            // Delete transaction button
            OutlinedButton(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DebitRed.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DebitRed)
            ) { Text("Delete Transaction", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DebitRed) }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Transaction", color = TextPrimary) },
            text = { Text("Are you sure you want to delete this ₹${String.format("%.2f", transaction.amount)} transaction to ${transaction.merchant}? This cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete(transaction)
                }) { Text("DELETE", color = DebitRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("CANCEL", color = TextPrimary) }
            },
            containerColor = Surface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}
