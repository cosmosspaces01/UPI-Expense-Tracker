package com.upi.expensetracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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

fun getCategoryEmoji(iconName: String): String {
    return when (iconName.lowercase()) {
        "restaurant", "food", "dining" -> "🍔"
        "directions_car", "transport", "travel" -> "🚗"
        "shopping_bag", "shopping" -> "🛒"
        "movie", "entertainment", "play" -> "🎬"
        "favorite", "health", "medical" -> "🩺"
        "bolt", "utilities", "bills" -> "⚡"
        "subscriptions", "recurring" -> "📅"
        "home", "rent" -> "🏠"
        "trending_up", "investments", "savings" -> "📈"
        "more_horiz", "others", "category" -> "📦"
        "pizza" -> "🍕"
        "flight" -> "✈️"
        "laptop" -> "💻"
        "checkroom" -> "👕"
        "book" -> "📚"
        "card_giftcard" -> "🎁"
        else -> {
            if (iconName.isNotEmpty() && iconName.codePointAt(0) > 127) {
                iconName
            } else {
                "🏷️"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoriesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories by viewModel.allCategories.collectAsState()

    // Dialog state
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedCategoryForDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedCategoryForEdit by remember { mutableStateOf<CategoryEntity?>(null) }

    // Create Inputs
    var newCategoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#6C63FF") }
    var selectedIcon by remember { mutableStateOf("restaurant") }
    var newCategoryBudget by remember { mutableStateOf("") }

    // Edit Inputs
    var editCategoryName by remember { mutableStateOf("") }
    var editCategoryColor by remember { mutableStateOf("#6C63FF") }
    var editCategoryIcon by remember { mutableStateOf("restaurant") }
    var editCategoryBudget by remember { mutableStateOf("") }

    val presetColors = listOf(
        "#6C63FF", "#0984E3", "#FF9F43", "#E84393", "#00B894", 
        "#FDCB6E", "#D63031", "#E17055", "#2ECC71", "#636E72"
    )

    val presetEmojis = listOf(
        "restaurant" to "🍔",
        "directions_car" to "🚗",
        "shopping_bag" to "🛒",
        "movie" to "🎬",
        "favorite" to "🩺",
        "bolt" to "⚡",
        "subscriptions" to "📅",
        "home" to "🏠",
        "trending_up" to "📈",
        "more_horiz" to "📦",
        "pizza" to "🍕",
        "flight" to "✈️",
        "laptop" to "💻",
        "checkroom" to "👕",
        "book" to "📚",
        "card_giftcard" to "🎁"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // App Bar
        TopAppBar(
            title = { Text("Manage Categories", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // "Add Category" Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .clickable { showCreateDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentBlueMid)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add Category",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Category list elements
            items(categories) { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .combinedClickable(
                            onClick = {
                                selectedCategoryForEdit = category
                                editCategoryName = category.name
                                editCategoryIcon = category.icon
                                editCategoryColor = category.color
                                editCategoryBudget = category.budget?.toInt()?.toString() ?: ""
                            },
                            onLongClick = {
                                selectedCategoryForDelete = category
                            }
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Colored Circle Dot + Icon Emoji Side by Side
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = getCategoryEmoji(category.icon),
                                fontSize = 24.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        Color(android.graphics.Color.parseColor(category.color)),
                                        RoundedCornerShape(6.dp)
                                    )
                            )
                        }
                        
                        Column {
                            Text(
                                text = category.name,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            val budgetText = if (category.budget != null) {
                                "Limit: ₹${category.budget.toInt()}/m"
                            } else {
                                "No budget set"
                            }
                            Text(
                                text = budgetText,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Category Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(text = "New Category", color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = AccentBlueMid,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newCategoryBudget,
                        onValueChange = { newCategoryBudget = it },
                        label = { Text("Budget Limit (₹) - Optional") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = AccentBlueMid,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Emoji Selection Block
                    Text(text = "Choose Icon/Emoji:", color = TextSecondary, fontSize = 13.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val chunks = presetEmojis.chunked(8)
                        chunks.forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                chunk.forEach { (iconId, emoji) ->
                                    val isSelected = selectedIcon == iconId
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (isSelected) PrimaryPurple.copy(alpha = 0.2f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) PrimaryPurple else AccentBlueMid,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedIcon = iconId },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Color selection block
                    Text(text = "Choose Theme Color:", color = TextSecondary, fontSize = 13.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val colorChunks = presetColors.chunked(5)
                        colorChunks.forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                chunk.forEach { colorHex ->
                                    val isSelected = colorHex == selectedColor
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                Color(android.graphics.Color.parseColor(colorHex)),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable { selectedColor = colorHex }
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) TextPrimary else Color.Transparent,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.trim().isNotEmpty()) {
                            val budgetVal = newCategoryBudget.toDoubleOrNull()
                            viewModel.insertCategory(
                                name = newCategoryName.trim(),
                                color = selectedColor,
                                icon = selectedIcon,
                                budget = budgetVal
                            )
                            newCategoryName = ""
                            selectedColor = "#6C63FF"
                            selectedIcon = "restaurant"
                            newCategoryBudget = ""
                            showCreateDialog = false
                            Toast.makeText(context, "Category added successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(text = "CREATE", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    newCategoryName = ""
                    selectedColor = "#6C63FF"
                    selectedIcon = "restaurant"
                    newCategoryBudget = ""
                    showCreateDialog = false 
                }) {
                    Text(text = "CANCEL", color = TextPrimary)
                }
            },
            containerColor = CardBackground
        )
    }

    // Edit Category Dialog
    if (selectedCategoryForEdit != null) {
        val cat = selectedCategoryForEdit!!
        AlertDialog(
            onDismissRequest = { selectedCategoryForEdit = null },
            title = { Text(text = "Edit Category", color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = editCategoryName,
                        onValueChange = { editCategoryName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = AccentBlueMid,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editCategoryBudget,
                        onValueChange = { editCategoryBudget = it },
                        label = { Text("Budget Limit (₹) - Optional") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = AccentBlueMid,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Emoji Selection Block
                    Text(text = "Choose Icon/Emoji:", color = TextSecondary, fontSize = 13.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val chunks = presetEmojis.chunked(8)
                        chunks.forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                chunk.forEach { (iconId, emoji) ->
                                    val isSelected = editCategoryIcon == iconId
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (isSelected) PrimaryPurple.copy(alpha = 0.2f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) PrimaryPurple else AccentBlueMid,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { editCategoryIcon = iconId },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Color selection block
                    Text(text = "Choose Theme Color:", color = TextSecondary, fontSize = 13.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val colorChunks = presetColors.chunked(5)
                        colorChunks.forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                chunk.forEach { colorHex ->
                                    val isSelected = colorHex == editCategoryColor
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                Color(android.graphics.Color.parseColor(colorHex)),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable { editCategoryColor = colorHex }
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) TextPrimary else Color.Transparent,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Delete Button inside Edit Dialog for premium convenience
                    Button(
                        onClick = {
                            selectedCategoryForDelete = cat
                            selectedCategoryForEdit = null
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningRed.copy(alpha = 0.15f), contentColor = WarningRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete Category", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editCategoryName.trim().isNotEmpty()) {
                            val budgetVal = editCategoryBudget.toDoubleOrNull()
                            val updatedCat = cat.copy(
                                name = editCategoryName.trim(),
                                icon = editCategoryIcon,
                                color = editCategoryColor,
                                budget = budgetVal
                            )
                            viewModel.updateCategory(updatedCat)
                            selectedCategoryForEdit = null
                            Toast.makeText(context, "Category updated successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(text = "SAVE", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCategoryForEdit = null }) {
                    Text(text = "CANCEL", color = TextPrimary)
                }
            },
            containerColor = CardBackground
        )
    }

    // Delete Category dialog
    if (selectedCategoryForDelete != null) {
        val cat = selectedCategoryForDelete!!
        AlertDialog(
            onDismissRequest = { selectedCategoryForDelete = null },
            title = { Text(text = "Delete Category") },
            text = { Text(text = "Are you sure you want to delete the category '${cat.name}'? This will not delete transactions using it, but they will fall back to default styling.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(cat)
                        selectedCategoryForDelete = null
                        Toast.makeText(context, "Category deleted.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(text = "DELETE", color = WarningRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCategoryForDelete = null }) {
                    Text(text = "CANCEL", color = TextPrimary)
                }
            },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}
