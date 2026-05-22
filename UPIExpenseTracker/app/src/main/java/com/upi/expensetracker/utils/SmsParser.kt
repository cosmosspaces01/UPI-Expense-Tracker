package com.upi.expensetracker.utils

import android.content.Context
import android.net.Uri
import com.upi.expensetracker.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.security.MessageDigest

object SmsParser {

    private val BANK_SENDER_IDS = setOf(
        "HDFCBK", "ICICIB", "SBIINB", "AXISBK", "KOTAKB", "PNBSMS", "CANARA", "BOISMS", "UNIONB", "IDFCFB",
        "HDFCPL", "ICICIP", "SBICRD", "AXISPAY", "KOTAKP"
    )

    private val DEBIT_KEYWORDS = listOf("debited", "debit", "paid", "sent", "transferred", "spent")

    // Dynamic categorizer map
    private val MERCHANT_CATEGORY_MAP = mapOf(
        "swiggy" to "Food & Dining",
        "zomato" to "Food & Dining",
        "starbucks" to "Food & Dining",
        "eatclub" to "Food & Dining",
        "domino" to "Food & Dining",
        "uber" to "Transport",
        "ola" to "Transport",
        "rapido" to "Transport",
        "irctc" to "Transport",
        "metro" to "Transport",
        "amazon" to "Shopping",
        "flipkart" to "Shopping",
        "myntra" to "Shopping",
        "dmart" to "Shopping",
        "reliance" to "Shopping",
        "netflix" to "Subscriptions",
        "spotify" to "Subscriptions",
        "prime" to "Subscriptions",
        "youtube" to "Subscriptions",
        "hotstar" to "Subscriptions",
        "jio" to "Utilities",
        "airtel" to "Utilities",
        "vi" to "Utilities",
        "electricity" to "Utilities",
        "water" to "Utilities",
        "gas" to "Utilities",
        "medplus" to "Health",
        "pharmacy" to "Health",
        "apollo" to "Health",
        "doctor" to "Health",
        "mutual" to "Investments",
        "groww" to "Investments",
        "zerodha" to "Investments",
        "sip" to "Investments",
        "rent" to "Rent",
        "cinema" to "Entertainment",
        "theatre" to "Entertainment",
        "bookmyshow" to "Entertainment"
    )

    fun parseSMS(body: String, fallbackDateMs: Long = System.currentTimeMillis()): TransactionEntity? {
        val bodyLower = body.lowercase()
        
        // Step 1: Check debit keywords
        val isDebit = DEBIT_KEYWORDS.any { keyword -> bodyLower.contains(keyword) }
        if (!isDebit) return null

        // Step 2: Extract Amount (₹ or Rs. followed by numbers/commas/dots)
        val amountRegex = Regex("""(?:rs\.?|₹|inr)\s*([0-9,]+\.?[0-9]*)""", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(bodyLower) ?: return null
        val amountStr = amountMatch.groupValues[1].replace(",", "")
        val amount = amountStr.toDoubleOrNull() ?: return null

        // Step 3: Extract Merchant Name
        var merchant = "Unknown Payee"
        
        // Typical patterns: "paid to X", "debited ... to X", "sent to X", "spent at X"
        val merchantRegexes = listOf(
            Regex("""\b(?:paid to|transferred to|sent to|spent at|debited to|debited at|info:|vpa|to|at)\s+([a-z0-9\s&.\-_]+?)(?:\s+on|\s+from|\s+ref|\s+upi|\s+via|\.|\z)"""),
            Regex("""(?:debited.*to)\s+([a-z0-9\s&.\-_]+?)(?:\s+on|\s+from|\s+ref|\s+upi|\.|\z)"""),
            Regex("""ref:\s*([a-z0-9\s&.\-_]+?)\s+upi""")
        )

        for (regex in merchantRegexes) {
            val match = regex.find(bodyLower)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.isNotEmpty() && !candidate.contains("account") && !candidate.contains("bank") && candidate.length > 1) {
                    merchant = candidate.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                    break
                }
            }
        }

        // Fallback: If merchant is still unknown, search for known merchant names in the body
        if (merchant == "Unknown Payee") {
            val knownMerchants = MERCHANT_CATEGORY_MAP.keys.sortedByDescending { it.length }
            for (known in knownMerchants) {
                if (bodyLower.contains(known)) {
                    merchant = known.replaceFirstChar { it.uppercase() }
                    break
                }
            }
        }

        // Step 4: Extract Account ending digits
        val accountRegex = Regex("""(?:a/c|account|ending|acct)\s*.*?([0-9]{4})""", RegexOption.IGNORE_CASE)
        val accountMatch = accountRegex.find(bodyLower)
        val accountLast4 = accountMatch?.groupValues?.get(1) ?: "XXXX"

        // Step 5: Extract Reference ID
        val refRegex = Regex("""(?:ref|upi ref|ref no|rrn)\.?\s*(?:no\.?)?\s*([0-9]{12})""", RegexOption.IGNORE_CASE)
        val refMatch = refRegex.find(bodyLower)
        val refId = refMatch?.groupValues?.get(1) ?: "TXN${System.currentTimeMillis()}"

        // Step 6: Extract and Format Date and Time from SMS Body
        var formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(fallbackDateMs))
        var formattedTime = SimpleDateFormat("HH:mm", Locale.US).format(Date(fallbackDateMs))

        // Date regex: matches patterns like 21-May-26, 21-05-2026, 21/05/26, 21May26
        val dateRegex = Regex("""([0-9]{1,2}[-/][0-9]{1,2}[-/][0-9]{2,4}|[0-9]{1,2}\s*(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\s*[0-9]{2,4})""", RegexOption.IGNORE_CASE)
        val dateMatch = dateRegex.find(bodyLower)
        if (dateMatch != null) {
            val dateStr = dateMatch.groupValues[1]
            try {
                val formats = listOf(
                    "dd-MM-yyyy", "dd/MM/yyyy", "dd-MM-yy", "dd/MM/yy",
                    "dd MMM yyyy", "dd MMM yy", "dd-MMM-yy", "dd-MMM-yyyy",
                    "ddMMMyy", "ddMMMyyyy"
                )
                var parsedDate: Date? = null
                for (fmt in formats) {
                    try {
                        val sdf = SimpleDateFormat(fmt, Locale.US)
                        sdf.isLenient = false
                        parsedDate = sdf.parse(dateStr)
                        if (parsedDate != null) break
                    } catch (e: Exception) {
                        // continue trying
                    }
                }
                if (parsedDate != null) {
                    formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsedDate)
                }
            } catch (e: Exception) {
                // Fallback to default
            }
        }

        // Time regex: matches patterns like 12:30:15 or 14:15
        val timeRegex = Regex("""([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)""")
        val timeMatch = timeRegex.find(bodyLower)
        if (timeMatch != null) {
            val timeStr = timeMatch.groupValues[1]
            try {
                val sdf = if (timeStr.split(":").size == 3) {
                    SimpleDateFormat("HH:mm:ss", Locale.US)
                } else {
                    SimpleDateFormat("HH:mm", Locale.US)
                }
                val parsedTime = sdf.parse(timeStr)
                if (parsedTime != null) {
                    formattedTime = SimpleDateFormat("HH:mm", Locale.US).format(parsedTime)
                }
            } catch (e: Exception) {
                // Fallback to default
            }
        }

        // Step 7: Deduplication Hash (SHA-256 of refId + amount)
        val id = generateHash(refId + amount)

        // Step 8: Categorization Suggestion
        var category = "Others"
        val merchantLower = merchant.lowercase()
        for ((keyword, cat) in MERCHANT_CATEGORY_MAP) {
            if (merchantLower.contains(keyword)) {
                category = cat
                break
            }
        }

        // Step 9: Recurring Payment Detection
        // If merchant matches a known subscription service, flag it
        val isRecurring = listOf("netflix", "spotify", "prime", "youtube", "hotstar").any { sub -> merchantLower.contains(sub) }

        return TransactionEntity(
            id = id,
            amount = amount,
            merchant = merchant,
            accountLast4 = accountLast4,
            refId = refId,
            date = formattedDate,
            time = formattedTime,
            category = category,
            description = "UPI Transfer to $merchant",
            notes = "",
            isSplit = false,
            splitWith = "",
            splitAmount = 0.0,
            isSettled = false,
            rawSMS = body,
            isRecurring = isRecurring
        )
    }

    private fun generateHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun syncTodayTransactions(context: Context): List<TransactionEntity> {
        return syncTransactionsForDate(context, System.currentTimeMillis())
    }

    /**
     * Syncs SMS transactions for a specific date.
     * @param dateMs Any timestamp within the target day (milliseconds since epoch)
     */
    fun syncTransactionsForDate(context: Context, dateMs: Long): List<TransactionEntity> {
        val transactions = mutableListOf<TransactionEntity>()
        val contentResolver = context.contentResolver
        
        // Define day boundaries for the given date
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateMs
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTimeMs = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endTimeMs = calendar.timeInMillis

        val cursor = contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("body", "date", "address"),
            "date >= ? AND date <= ?",
            arrayOf(startTimeMs.toString(), endTimeMs.toString()),
            "date DESC"
        )

        cursor?.use {
            val bodyIndex = it.getColumnIndexOrThrow("body")
            val dateIndex = it.getColumnIndexOrThrow("date")
            val addressIndex = it.getColumnIndexOrThrow("address")

            while (it.moveToNext()) {
                val address = it.getString(addressIndex) ?: ""
                val body = it.getString(bodyIndex) ?: ""
                val smsDateMs = it.getLong(dateIndex)

                // Optional bank sender filter:
                // Check if address ends with any of the sender IDs
                val isBankSMS = BANK_SENDER_IDS.any { bankId -> address.uppercase().contains(bankId) }
                
                // Parse the SMS if it's either a bank address or matches general keywords
                if (isBankSMS || address.isNotEmpty()) {
                    val txn = parseSMS(body, smsDateMs)
                    if (txn != null) {
                        transactions.add(txn)
                    }
                }
            }
        }
        return transactions
    }
}

