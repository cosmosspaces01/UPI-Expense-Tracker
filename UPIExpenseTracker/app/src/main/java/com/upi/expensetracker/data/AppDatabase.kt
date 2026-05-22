package com.upi.expensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(entities = [TransactionEntity::class, CategoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "upi_expense_tracker_db"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDefaultCategories(database.categoryDao())
                }
            }
        }

        private suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
            val defaults = listOf(
                CategoryEntity(UUID.randomUUID().toString(), "Food & Dining", "restaurant", "#FF9F43"),
                CategoryEntity(UUID.randomUUID().toString(), "Transport", "directions_car", "#0984E3"),
                CategoryEntity(UUID.randomUUID().toString(), "Shopping", "shopping_bag", "#E84393"),
                CategoryEntity(UUID.randomUUID().toString(), "Entertainment", "movie", "#6C63FF"),
                CategoryEntity(UUID.randomUUID().toString(), "Health", "favorite", "#00B894"),
                CategoryEntity(UUID.randomUUID().toString(), "Utilities", "bolt", "#FDCB6E"),
                CategoryEntity(UUID.randomUUID().toString(), "Subscriptions", "subscriptions", "#D63031"),
                CategoryEntity(UUID.randomUUID().toString(), "Rent", "home", "#E17055"),
                CategoryEntity(UUID.randomUUID().toString(), "Investments", "trending_up", "#2ECC71"),
                CategoryEntity(UUID.randomUUID().toString(), "Others", "more_horiz", "#636E72")
            )
            categoryDao.insertCategories(defaults)
        }
    }
}
