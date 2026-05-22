package com.upi.expensetracker.ui.components

import androidx.compose.foundation.background
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
import com.upi.expensetracker.data.TransactionEntity
import com.upi.expensetracker.ui.theme.CardBackground
import com.upi.expensetracker.ui.theme.DarkBackground
import com.upi.expensetracker.ui.theme.PrimaryPurple
import com.upi.expensetracker.ui.theme.TextPrimary
import com.upi.expensetracker.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionSheet(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Form States
    var description by remember { mutableStateOf(transaction.description) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var notes by remember { mutableStateOf(transaction.notes) }
    var isSplit by remember { mutableStateOf(transaction.isSplit) }
    var splitWith by remember { mutableStateOf(transaction.splitWith) }
    var splitAmountStr by remember { mutableStateOf(transaction.splitAmount.toString()) }
    var isSettled by remember { mutableStateOf(transaction.isSettled) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Edit Transaction",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Text(
                text = "Amount: ₹${String.format("%.2f", transaction.amount)} | Ref: ${transaction.refId}",
                fontSize = 14.sp,
                color = TextSecondary
            )

            // Reason/Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Reason") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = Color(0xFF2C2C2C),
                    focusedLabelColor = PrimaryPurple
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Horizontal Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Category",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = category.name == selectedCategory
                        val chipBg = if (isSelected) PrimaryPurple else Color(0xFF2C2C2C)
                        val chipText = if (isSelected) TextPrimary else TextSecondary
                        
                        Box(
                            modifier = Modifier
                                .background(chipBg, RoundedCornerShape(20.dp))
                                .clickable { selectedCategory = category.name }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Simple colored dot for category color
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(android.graphics.Color.parseColor(category.color)), RoundedCornerShape(4.dp))
                                )
                                Text(
                                    text = category.name,
                                    color = chipText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Notes Input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Add Notes") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = Color(0xFF2C2C2C),
                    focusedLabelColor = PrimaryPurple
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Split Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Split this expense?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Switch(
                    checked = isSplit,
                    onCheckedChange = { isSplit = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = PrimaryPurple
                    )
                )
            }

            // Split Inputs
            if (isSplit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = splitWith,
                        onValueChange = { splitWith = it },
                        label = { Text("Split With") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = Color(0xFF2C2C2C)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = splitAmountStr,
                        onValueChange = { splitAmountStr = it },
                        label = { Text("Your Share (₹)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = Color(0xFF2C2C2C)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Marked as Settled / Paid Back?",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Checkbox(
                        checked = isSettled,
                        onCheckedChange = { isSettled = it ?: false },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    val updated = transaction.copy(
                        description = description,
                        category = selectedCategory,
                        notes = notes,
                        isSplit = isSplit,
                        splitWith = if (isSplit) splitWith else "",
                        splitAmount = if (isSplit) (splitAmountStr.toDoubleOrNull() ?: 0.0) else 0.0,
                        isSettled = if (isSplit) isSettled else false
                    )
                    onSave(updated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Save Changes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}
