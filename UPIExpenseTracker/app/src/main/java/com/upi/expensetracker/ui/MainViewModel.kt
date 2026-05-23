package com.upi.expensetracker.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.upi.expensetracker.data.CategoryDao
import com.upi.expensetracker.data.CategoryEntity
import com.upi.expensetracker.data.TransactionDao
import com.upi.expensetracker.data.TransactionEntity
import com.upi.expensetracker.utils.SmsParser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainViewModel(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val context: Context
) : ViewModel() {

    // Fetch lists from Room
    val allTransactions: StateFlow<List<TransactionEntity>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recurringTransactions: StateFlow<List<TransactionEntity>> = transactionDao.getRecurringTransactions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pendingSplits: StateFlow<List<TransactionEntity>> = transactionDao.getPendingSplits()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Date formatting helper
    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun getCurrentMonthPrefix(): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    }

    val todayTotalSpend: StateFlow<Double?> = transactionDao.getTodayTotalSpend(getTodayDateString())
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val monthTotalSpend: StateFlow<Double?> = transactionDao.getMonthTotalSpend(getCurrentMonthPrefix())
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // User preference settings (backed by SharedPreferences)
    private val prefs = context.getSharedPreferences("upi_tracker_prefs", Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString("user_name", "Arjun") ?: "Arjun"
        set(value) = prefs.edit().putString("user_name", value).apply()

    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean("app_lock", false)
        set(value) = prefs.edit().putBoolean("app_lock", value).apply()

    // SMS Sync Execution
    fun syncTransactions(onSyncComplete: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val todayTxns = SmsParser.syncTodayTransactions(context)
                if (todayTxns.isNotEmpty()) {
                    transactionDao.insertTransactions(todayTxns)
                }
                onSyncComplete(todayTxns.size)
            } catch (e: Exception) {
                e.printStackTrace()
                onSyncComplete(-1)
            }
        }
    }

    // Sync SMS for a specific selected date (allows syncing past dates)
    fun syncTransactionsForDate(dateMs: Long, onSyncComplete: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val txns = SmsParser.syncTransactionsForDate(context, dateMs)
                if (txns.isNotEmpty()) {
                    transactionDao.insertTransactions(txns)
                }
                onSyncComplete(txns.size)
            } catch (e: Exception) {
                e.printStackTrace()
                onSyncComplete(-1)
            }
        }
    }

    // CRUD transactions
    fun updateTransaction(txn: TransactionEntity) {
        viewModelScope.launch {
            transactionDao.updateTransaction(txn)
        }
    }

    // Manually add a transaction (when user didn't receive SMS)
    fun addTransaction(
        amount: Double,
        merchant: String,
        category: String,
        date: String,
        time: String,
        description: String,
        notes: String
    ) {
        viewModelScope.launch {
            val txn = TransactionEntity(
                id = UUID.randomUUID().toString(),
                amount = amount,
                merchant = merchant.ifBlank { "Manual Entry" },
                accountLast4 = "XXXX",
                refId = "MANUAL-${System.currentTimeMillis()}",
                date = date,
                time = time,
                category = category,
                description = description.ifBlank { "Manual transaction" },
                notes = notes,
                isSplit = false,
                splitWith = "",
                splitAmount = 0.0,
                isSettled = false,
                rawSMS = "",
                isRecurring = false
            )
            transactionDao.insertTransactions(listOf(txn))
        }
    }

    // CRUD categories
    fun insertCategory(name: String, color: String, icon: String, budget: Double?) {
        viewModelScope.launch {
            val newCat = CategoryEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                icon = icon,
                color = color,
                budget = budget
            )
            categoryDao.insertCategory(newCat)
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryDao.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryDao.deleteCategory(category)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            transactionDao.clearAllTransactions()
        }
    }

    // Dynamic mock SMS generator specifically to support real-time data testing on Emulators or Devices without SIMs
    fun injectMockSMS() {
        viewModelScope.launch {
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val tf = SimpleDateFormat("HH:mm", Locale.US)
            val now = Date()
            val dateStr = df.format(now)
            val timeStr = tf.format(now)

            val mocks = listOf(
                TransactionEntity(
                    UUID.randomUUID().toString(), 349.00, "Swiggy Food", "4321", "REF100234120", dateStr, timeStr,
                    "Food & Dining", "UPI Transfer to Swiggy", "Dinner order", false, "", 0.0, false, "Paid Rs.349 to Swiggy via UPI ref 100234120 from a/c 4321", false
                ),
                TransactionEntity(
                    UUID.randomUUID().toString(), 120.00, "Uber ride", "9876", "REF200984112", dateStr, timeStr,
                    "Transport", "UPI Transfer to Uber", "Office travel", false, "", 0.0, false, "Rs. 120 debited to Uber on a/c ending 9876. Ref 200984112", false
                ),
                TransactionEntity(
                    UUID.randomUUID().toString(), 199.00, "Spotify Premium", "1234", "REF300129031", dateStr, timeStr,
                    "Subscriptions", "UPI Subscription payment", "Monthly music plan", false, "", 0.0, false, "Sent Rs. 199 to Spotify Premium. Ref: 300129031 A/c 1234", true
                ),
                TransactionEntity(
                    UUID.randomUUID().toString(), 2500.00, "DMart Store", "4321", "REF400495819", dateStr, timeStr,
                    "Shopping", "UPI Transfer to DMart", "Monthly groceries", true, "Siddharth", 1250.0, false, "Spent Rs.2500.00 at DMart. A/c ending 4321 ref 400495819", false
                )
            )
            transactionDao.insertTransactions(mocks)
        }
    }
}

class MainViewModelFactory(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(transactionDao, categoryDao, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
