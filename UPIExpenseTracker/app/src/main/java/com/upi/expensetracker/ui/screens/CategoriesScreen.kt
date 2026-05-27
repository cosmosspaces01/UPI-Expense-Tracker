package com.upi.expensetracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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



@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoriesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories by viewModel.allCategories.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedCategoryForDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedCategoryForEdit by remember { mutableStateOf<CategoryEntity?>(null) }

    var newCategoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#00BFA6") }

    var newCategoryBudget by remember { mutableStateOf("") }

    var editCategoryName by remember { mutableStateOf("") }
    var editCategoryColor by remember { mutableStateOf("#00BFA6") }

    var editCategoryBudget by remember { mutableStateOf("") }

    val presetColors = listOf(
        "#00BFA6", "#EF5350", "#FFA726", "#66BB6A", "#5ED4F5",
        "#AB47BC", "#42A5F5", "#FF7043", "#26A69A", "#78909C"
    )



    Column(modifier = modifier.fillMaxSize().background(Background)) {
        TopAppBar(
            title = { Text("Categories", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Add button card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clickable { showCreateDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentDim)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Add, "Add", tint = Accent, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Add Category", color = Accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            items(categories) { category ->
                val catColor = try { Color(android.graphics.Color.parseColor(category.color)) } catch (_: Exception) { Accent }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .combinedClickable(
                            onClick = {
                                selectedCategoryForEdit = category
                                editCategoryName = category.name

                                editCategoryColor = category.color
                                editCategoryBudget = category.budget?.toInt()?.toString() ?: ""
                            },
                            onLongClick = { selectedCategoryForDelete = category }
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Divider)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color dot
                            Box(modifier = Modifier.size(12.dp).background(catColor, CircleShape))
                            if (category.budget != null) {
                                Text("₹${category.budget.toInt()}/m", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        Column {
                            Text(category.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text(
                                if (category.budget != null) "Budget set" else "No budget",
                                fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Create dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Category", color = TextPrimary) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Divider, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = newCategoryBudget, onValueChange = { newCategoryBudget = it }, label = { Text("Budget (₹) - Optional") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Divider, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary), shape = RoundedCornerShape(12.dp))


                    Text("Color:", color = TextSecondary, fontSize = 13.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        presetColors.chunked(5).forEach { chunk ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                chunk.forEach { hex ->
                                    val isSelected = hex == selectedColor
                                    Box(
                                        modifier = Modifier.size(32.dp)
                                            .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                            .border(if (isSelected) 3.dp else 0.dp, if (isSelected) TextPrimary else Color.Transparent, CircleShape)
                                            .clickable { selectedColor = hex }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCategoryName.trim().isNotEmpty()) {
                        viewModel.insertCategory(newCategoryName.trim(), selectedColor, "restaurant", newCategoryBudget.toDoubleOrNull())
                        newCategoryName = ""; selectedColor = "#00BFA6"; newCategoryBudget = ""
                        showCreateDialog = false
                        Toast.makeText(context, "Category created", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("CREATE", color = Accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { newCategoryName = ""; showCreateDialog = false }) { Text("CANCEL", color = TextPrimary) } },
            containerColor = Surface
        )
    }

    // Edit dialog
    if (selectedCategoryForEdit != null) {
        val cat = selectedCategoryForEdit!!
        AlertDialog(
            onDismissRequest = { selectedCategoryForEdit = null },
            title = { Text("Edit Category", color = TextPrimary) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = editCategoryName, onValueChange = { editCategoryName = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Divider, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = editCategoryBudget, onValueChange = { editCategoryBudget = it }, label = { Text("Budget (₹) - Optional") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Divider, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary), shape = RoundedCornerShape(12.dp))


                    Text("Color:", color = TextSecondary, fontSize = 13.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        presetColors.chunked(5).forEach { chunk ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                chunk.forEach { hex ->
                                    val isSelected = hex == editCategoryColor
                                    Box(
                                        modifier = Modifier.size(32.dp)
                                            .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                            .border(if (isSelected) 3.dp else 0.dp, if (isSelected) TextPrimary else Color.Transparent, CircleShape)
                                            .clickable { editCategoryColor = hex }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { selectedCategoryForDelete = cat; selectedCategoryForEdit = null },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DebitRed.copy(alpha = 0.12f), contentColor = DebitRed),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Delete Category", fontWeight = FontWeight.Bold) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editCategoryName.trim().isNotEmpty()) {
                        viewModel.updateCategory(cat.copy(name = editCategoryName.trim(), color = editCategoryColor, budget = editCategoryBudget.toDoubleOrNull()))
                        selectedCategoryForEdit = null
                        Toast.makeText(context, "Category updated", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("SAVE", color = Accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { selectedCategoryForEdit = null }) { Text("CANCEL", color = TextPrimary) } },
            containerColor = Surface
        )
    }

    // Delete dialog
    if (selectedCategoryForDelete != null) {
        val cat = selectedCategoryForDelete!!
        AlertDialog(
            onDismissRequest = { selectedCategoryForDelete = null },
            title = { Text("Delete Category") },
            text = { Text("Delete '${cat.name}'? Transactions using it will fall back to default styling.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteCategory(cat); selectedCategoryForDelete = null; Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show() }) { Text("DELETE", color = DebitRed, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { selectedCategoryForDelete = null }) { Text("CANCEL", color = TextPrimary) } },
            containerColor = Surface, titleContentColor = TextPrimary, textContentColor = TextSecondary
        )
    }
}
