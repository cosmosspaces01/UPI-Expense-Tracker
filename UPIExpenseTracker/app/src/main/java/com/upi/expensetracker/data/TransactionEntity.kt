package com.upi.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val merchant: String,
    val accountLast4: String,
    val refId: String,
    val date: String,     // YYYY-MM-DD
    val time: String,     // HH:MM
    val category: String, // Reference name of the category (e.g. "Food")
    val description: String,
    val notes: String,
    val isSplit: Boolean,
    val splitWith: String,
    val splitAmount: Double,
    val isSettled: Boolean,
    val rawSMS: String,
    val isRecurring: Boolean,
    // Source of this transaction: "SMS", "NOTIFICATION", or "MANUAL"
    val source: String = "SMS"
)
