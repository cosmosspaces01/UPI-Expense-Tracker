package com.upi.expensetracker.service

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.upi.expensetracker.data.AppDatabase
import com.upi.expensetracker.utils.NotificationParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * A [NotificationListenerService] that intercepts notifications from known UPI apps
 * (Google Pay, PhonePe, Paytm, etc.) and automatically saves detected payment
 * transactions into the local Room database.
 *
 * Lifecycle:
 * - Android binds this service when the user grants "Notification Access" in system settings.
 * - It is NOT started manually — the OS manages its lifecycle.
 * - We create a dedicated coroutine scope tied to the service's own job so that IO work
 *   is cancelled cleanly when the service is destroyed.
 *
 * Permission model:
 * - Declared in AndroidManifest with BIND_NOTIFICATION_LISTENER_SERVICE permission.
 * - The user must manually enable it via Settings → Apps → Special app access → Notification access.
 */
class UpiNotificationListenerService : NotificationListenerService() {

    private val TAG = "UpiNotifListener"

    // Coroutine scope for database inserts — cancelled when service is destroyed.
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Preferences key used by MainViewModel to enable/disable the listener.
     * Even when the OS has granted access, we respect this user-level toggle.
     */
    companion object {
        const val PREF_NAME = "upi_tracker_prefs"
        const val PREF_NOTIFICATION_SYNC = "notification_sync_enabled"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val packageName = sbn.packageName ?: return

        // 1. Filter: only process notifications from known UPI apps
        if (!NotificationParser.KNOWN_UPI_PACKAGES.containsKey(packageName)) return

        // 2. Respect the user's toggle — if sync is disabled in settings, skip
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(PREF_NOTIFICATION_SYNC, false)) {
            Log.d(TAG, "Notification sync disabled by user — skipping")
            return
        }

        // 3. Extract notification text from the bundle
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text  = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        Log.d(TAG, "Notification from ${NotificationParser.KNOWN_UPI_PACKAGES[packageName]}: title=$title | text=$text")

        // 4. Parse and insert on IO thread
        serviceScope.launch {
            try {
                val transaction = NotificationParser.parseNotification(
                    packageName  = packageName,
                    title        = title,
                    text         = text,
                    timestampMs  = sbn.postTime
                )

                if (transaction != null) {
                    val dao = AppDatabase.getDatabase(applicationContext, serviceScope).transactionDao()
                    // OnConflictStrategy.IGNORE on insertTransactions ensures that if the same
                    // transaction was already captured via SMS (same hash), it won't be duplicated.
                    dao.insertTransactions(listOf(transaction))
                    Log.d(TAG, "✅ Transaction saved from notification: ₹${transaction.amount} @ ${transaction.merchant}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process notification transaction", e)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected — watching UPI apps")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all pending coroutines to avoid leaks
        serviceScope.cancel()
    }
}
