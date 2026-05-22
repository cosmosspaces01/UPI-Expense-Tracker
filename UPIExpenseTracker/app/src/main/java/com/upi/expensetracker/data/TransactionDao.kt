package com.upi.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, time DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date = :date ORDER BY time DESC")
    fun getTransactionsByDate(date: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC, time DESC")
    fun getTransactionsByMonth(monthPrefix: String): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE date = :date")
    fun getTodayTotalSpend(date: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE date LIKE :monthPrefix || '%'")
    fun getMonthTotalSpend(monthPrefix: String): Flow<Double?>

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE date LIKE :monthPrefix || '%' GROUP BY category")
    fun getMonthCategoryBreakdown(monthPrefix: String): Flow<List<CategorySpend>>

    @Query("SELECT * FROM transactions WHERE isRecurring = 1 ORDER BY date DESC")
    fun getRecurringTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isSplit = 1 AND isSettled = 0 ORDER BY date DESC")
    fun getPendingSplits(): Flow<List<TransactionEntity>>

    // OnConflictStrategy.IGNORE ensures we don't overwrite user changes on secondary sync imports
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()
}

data class CategorySpend(
    val category: String,
    val total: Double
)
