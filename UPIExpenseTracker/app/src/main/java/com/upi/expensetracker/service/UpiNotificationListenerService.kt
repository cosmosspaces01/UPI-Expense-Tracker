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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
 * Deduplication strategy (Option A — time + amount window):
 * - Before inserting a notification-sourced transaction, we query the DB for any
 *   existing transaction with the same amount within ±[DEDUP_WINDOW_MINUTES] of the
 *   notification's timestamp.
 * - If a match is found (likely captured from a bank SMS), the notification is discarded.
 * - This handles the case where bank SMS and UPI app notification arrive for the same
 *   payment, since notification text typically lacks the bank ref ID needed for hash-based dedup.
 */
class UpiNotificationListenerService : NotificationListenerService() {

    private val TAG = "UpiNotifListener"

    // Coroutine scope for database IO — cancelled when service is destroyed.
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val PREF_NAME               = "upi_tracker_prefs"
        const val PREF_NOTIFICATION_SYNC  = "notification_sync_enabled"

        /** Half-width of the dedup time window in minutes. A transaction already in the
         *  DB within ±5 minutes of the same amount will suppress the notification entry. */
        private const val DEDUP_WINDOW_MINUTES = 5

        private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.US)
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
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

        val appName = NotificationParser.KNOWN_UPI_PACKAGES[packageName]
        Log.d(TAG, "[$appName] title=$title | text=$text")

        // 4. Parse and attempt to insert on IO thread
        serviceScope.launch {
            try {
                val transaction = NotificationParser.parseNotification(
                    packageName = packageName,
                    title       = title,
                    text        = text,
                    timestampMs = sbn.postTime
                ) ?: return@launch

                val dao = AppDatabase.getDatabase(applicationContext, serviceScope).transactionDao()

                // 5. Deduplication — Option A: time + amount window check
                //    Compute a ±DEDUP_WINDOW_MINUTES window around the parsed transaction time.
                val (timeStart, timeEnd) = computeTimeWindow(transaction.time, DEDUP_WINDOW_MINUTES)
                val duplicateCount = dao.countDuplicates(
                    amount    = transaction.amount,
                    date      = transaction.date,
                    timeStart = timeStart,
                    timeEnd   = timeEnd
                )

                if (duplicateCount > 0) {
                    // A transaction for the same amount already exists nearby in time —
                    // most likely captured via a bank SMS. Skip to avoid duplicates.
                    Log.d(TAG, "⏭ Duplicate detected (₹${transaction.amount} within ±${DEDUP_WINDOW_MINUTES}min) — skipping notification insert")
                    return@launch
                }

                // 6. No duplicate found — safe to insert
                dao.insertTransactions(listOf(transaction))
                Log.d(TAG, "✅ Saved from [$appName]: ₹${transaction.amount} @ ${transaction.merchant} [${transaction.date} ${transaction.time}]")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to process notification transaction", e)
            }
        }
    }

    /**
     * Computes a [timeStart, timeEnd] HH:mm window of ±[windowMinutes] around [centerTime].
     *
     * Works correctly across midnight boundaries by clamping to "00:00" and "23:59".
     *
     * @param centerTime    HH:mm string (e.g. "14:30")
     * @param windowMinutes Half-width of the window in minutes.
     * @return Pair of (timeStart, timeEnd) as HH:mm strings.
     */
    private fun computeTimeWindow(centerTime: String, windowMinutes: Int): Pair<String, String> {
        return try {
            // Parse center time into a Calendar so we can add/subtract minutes safely
            val parsed = TIME_FORMAT.parse(centerTime) ?: return Pair("00:00", "23:59")
            val cal = Calendar.getInstance().apply { time = parsed }

            val startCal = (cal.clone() as Calendar).apply { add(Calendar.MINUTE, -windowMinutes) }
            val endCal   = (cal.clone() as Calendar).apply { add(Calendar.MINUTE, +windowMinutes) }

            Pair(TIME_FORMAT.format(startCal.time), TIME_FORMAT.format(endCal.time))
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse time '$centerTime' for dedup window — using full-day range")
            Pair("00:00", "23:59")
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
        // Cancel all pending coroutines to avoid memory/thread leaks
        serviceScope.cancel()
    }
}
