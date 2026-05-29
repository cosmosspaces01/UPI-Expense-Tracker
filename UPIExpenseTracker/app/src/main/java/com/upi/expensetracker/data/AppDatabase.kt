package com.upi.expensetracker.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.util.UUID

@Database(entities = [TransactionEntity::class, CategoryEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DB_NAME = "upi_expense_tracker_db"
        private const val TAG   = "AppDatabase"

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildEncryptedDatabase(context, scope)
                INSTANCE = instance
                instance
            }
        }

        /**
         * Builds the Room database with SQLCipher encryption.
         *
         * Key lifecycle:
         * 1. [DatabaseKeyManager] retrieves (or generates on first launch) a
         *    random 256-bit passphrase stored in EncryptedSharedPreferences.
         * 2. The passphrase is converted to bytes and handed to SQLCipher's
         *    [SupportFactory], which handles all encryption/decryption transparently.
         * 3. Room sees a standard [SupportSQLiteDatabase] — no Room code changes needed.
         *
         * Migration from unencrypted DB (fresh install after reinstall):
         * Since the user confirmed they will reinstall the app, there is no existing
         * unencrypted database to migrate — this will always create a fresh encrypted DB.
         */
        private fun buildEncryptedDatabase(
            context: Context,
            scope: CoroutineScope
        ): AppDatabase {
            // Load SQLCipher native library
            SQLiteDatabase.loadLibs(context)

            // Retrieve or generate the database encryption passphrase
            val passphrase   = DatabaseKeyManager.getOrCreatePassphrase(context)
            val passphraseBytes = SQLiteDatabase.getBytes(passphrase)
            val factory      = SupportFactory(passphraseBytes)

            // Clear passphrase from memory immediately after creating the factory
            passphrase.fill('\u0000')

            Log.d(TAG, "Building encrypted Room database")

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2)
            .addCallback(AppDatabaseCallback(scope))
            .build()
        }

        /**
         * Migration 1→2: Adds the `source` column to track where a transaction
         * originated ("SMS", "NOTIFICATION", or "MANUAL"). Defaults to "SMS" for
         * all existing transactions so no data is lost.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN source TEXT NOT NULL DEFAULT 'SMS'")
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
