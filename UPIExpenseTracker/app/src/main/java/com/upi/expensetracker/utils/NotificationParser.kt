package com.upi.expensetracker.utils

import android.util.Log
import com.upi.expensetracker.data.TransactionEntity

/**
 * Parses UPI payment notifications from known UPI apps into TransactionEntity objects.
 *
 * Strategy: Notification text from UPI apps (Google Pay, PhonePe, Paytm, etc.) is
 * structurally similar to bank SMS bodies. We concatenate the notification title and
 * text, then delegate to the proven SmsParser.parseSMS() regex engine. This avoids
 * code duplication and ensures both channels apply the same merchant/category logic.
 */
object NotificationParser {

    private const val TAG = "NotificationParser"

    /**
     * Maps Android package names of known UPI apps to human-readable source labels.
     * Used to filter irrelevant notifications and tag the source app in logs.
     */
    val KNOWN_UPI_PACKAGES: Map<String, String> = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.phonepe.app"                        to "PhonePe",
        "net.one97.paytm"                        to "Paytm",
        "com.dreamplug.androidapp"               to "CRED",
        "in.org.npci.upiapp"                     to "BHIM",
        "com.amazon.mShop.android.shopping"      to "Amazon Pay",
        "com.whatsapp"                           to "WhatsApp Pay",
        "com.freecharge.android"                 to "Freecharge",
        "com.mobikwik_new"                       to "MobiKwik"
    )

    /**
     * Keywords in notification text that indicate a debit / outgoing payment.
     * We skip notifications that don't contain any of these to reduce false positives
     * (e.g. promotional messages from the same app).
     */
    private val PAYMENT_KEYWORDS = listOf(
        "paid", "sent", "debited", "payment", "transferred", "₹", "rs.", "rs ", "inr"
    )

    /**
     * Attempts to parse a UPI app notification into a TransactionEntity.
     *
     * @param packageName   Android package name of the app that posted the notification.
     * @param title         Notification title string (may be null).
     * @param text          Notification body/text string (may be null).
     * @param timestampMs   Epoch ms of when the notification was received (used as fallback date).
     * @return              A [TransactionEntity] with source = "NOTIFICATION", or null if the
     *                      notification doesn't represent a debit transaction.
     */
    fun parseNotification(
        packageName: String,
        title: String?,
        text: String?,
        timestampMs: Long = System.currentTimeMillis()
    ): TransactionEntity? {

        val appName = KNOWN_UPI_PACKAGES[packageName] ?: return null

        // Combine title + text into one string for the SmsParser to analyse.
        // The separator " " ensures words don't bleed into each other.
        val combinedText = listOfNotNull(title?.trim(), text?.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")

        if (combinedText.isBlank()) {
            Log.d(TAG, "[$appName] Empty notification body — skipping")
            return null
        }

        // Quick pre-filter: skip if no payment-related keyword is found.
        // This avoids feeding promotional / status notifications to the parser.
        val lowerCombined = combinedText.lowercase()
        val hasPaymentKeyword = PAYMENT_KEYWORDS.any { lowerCombined.contains(it) }
        if (!hasPaymentKeyword) {
            Log.d(TAG, "[$appName] No payment keyword found in: ${combinedText.take(80)}")
            return null
        }

        Log.d(TAG, "[$appName] Processing notification: ${combinedText.take(120)}")

        // Delegate to the existing SMS parser which handles all amount/merchant/date extraction.
        val parsed = SmsParser.parseSMS(combinedText, timestampMs) ?: return null

        // Override the source field to mark this transaction as captured from a notification.
        return parsed.copy(source = "NOTIFICATION", rawSMS = combinedText)
    }
}
