package com.upi.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String, // String identifier for the icon
    val color: String, // Hex string color (e.g., "#6C63FF")
    val budget: Double? = null // Monthly budget limit, null means no budget
)
