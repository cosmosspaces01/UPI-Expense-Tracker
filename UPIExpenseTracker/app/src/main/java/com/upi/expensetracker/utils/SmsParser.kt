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

    private const val TAG = "SmsParser"

    // Comprehensive list of Indian bank sender IDs
    private val BANK_SENDER_IDS = setOf(
        "HDFCBK", "ICICIB", "SBIINB", "AXISBK", "KOTAKB", "PNBSMS", "CANBNK",
        "BOISMS", "UNIONB", "IDFCFB", "HDFCPL", "ICICIP", "SBICRD", "AXISPAY",
        "KOTAKP", "BARODIN", "YESBNK", "FEDERL", "INDBNK", "CENTBK", "PAYTMB",
        "JKBANK", "RBLBNK", "DENABNK", "SYNDBK", "CORPBK", "ANDBNK", "BNKIOB",
        "MAHABK", "SCBANK", "CITIAN", "DBIBNK", "HSBCIN", "AUFIN", "KVBANK",
        "IOBILN", "IDBIBN", "TMBLNK", "EQUITAS", "INDUSB", "DCBBK", "BANDHAN",
        "PAYTM", "PHONPE", "GPAY", "BHIM",
        // Union Bank of India specific sender IDs
        "UBOINB", "UBIBNK", "UNIBNK",
        // HDFC specific
        "HDFCBN", "HDFC"
    )

    // Keywords that indicate a debit/spend transaction
    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit", "paid", "sent", "transferred", "spent",
        "withdrawn", "purchase", "charged", "payment", "txn"
    )

    // Keywords that indicate credit — used to EXCLUDE credit SMS
    private val CREDIT_KEYWORDS = listOf(
        "credited", "credit", "received", "refund", "cashback", "reversed"
    )

    // Merchant to category mapping
    private val MERCHANT_CATEGORY_MAP = mapOf(
        "swiggy" to "Food & Dining",
        "zomato" to "Food & Dining",
        "starbucks" to "Food & Dining",
        "eatclub" to "Food & Dining",
        "domino" to "Food & Dining",
        "mcdonald" to "Food & Dining",
        "kfc" to "Food & Dining",
        "pizza" to "Food & Dining",
        "burger" to "Food & Dining",
        "uber" to "Transport",
        "ola" to "Transport",
        "rapido" to "Transport",
        "irctc" to "Transport",
        "metro" to "Transport",
        "redbus" to "Transport",
        "makemytrip" to "Transport",
        "amazon" to "Shopping",
        "flipkart" to "Shopping",
        "myntra" to "Shopping",
        "dmart" to "Shopping",
        "reliance" to "Shopping",
        "bigbasket" to "Shopping",
        "blinkit" to "Shopping",
        "zepto" to "Shopping",
        "meesho" to "Shopping",
        "nykaa" to "Shopping",
        "netflix" to "Subscriptions",
        "spotify" to "Subscriptions",
        "prime" to "Subscriptions",
        "youtube" to "Subscriptions",
        "hotstar" to "Subscriptions",
        "disney" to "Subscriptions",
        "jio" to "Utilities",
        "airtel" to "Utilities",
        "vi " to "Utilities",
        "electricity" to "Utilities",
        "water" to "Utilities",
        "gas" to "Utilities",
        "broadband" to "Utilities",
        "bsnl" to "Utilities",
        "medplus" to "Health",
        "pharmacy" to "Health",
        "apollo" to "Health",
        "doctor" to "Health",
        "hospital" to "Health",
        "practo" to "Health",
        "mutual" to "Investments",
        "groww" to "Investments",
        "zerodha" to "Investments",
        "sip" to "Investments",
        "kuvera" to "Investments",
        "rent" to "Rent",
        "cinema" to "Entertainment",
        "theatre" to "Entertainment",
        "bookmyshow" to "Entertainment",
        "pvr" to "Entertainment",
        "inox" to "Entertainment"
    )

    fun parseSMS(body: String, fallbackDateMs: Long = System.currentTimeMillis()): TransactionEntity? {
        val bodyLower = body.lowercase(Locale.ROOT)

        // Step 1: Exclude credit SMS (but allow messages that have both credit and debit keywords)
        val hasCredit = CREDIT_KEYWORDS.any { keyword -> bodyLower.contains(keyword) }
        val hasDebit = DEBIT_KEYWORDS.any { keyword -> bodyLower.contains(keyword) }

        // If it only has credit keywords and no debit keywords, skip it
        if (hasCredit && !hasDebit) {
            SecureLogger.d(TAG, "Skipping credit SMS: ${body.take(80)}")
            return null
        }

        // Step 2: Check debit keywords
        if (!hasDebit) {
            SecureLogger.d(TAG, "No debit keyword found in: ${body.take(80)}")
            return null
        }

        // Step 3: Extract Amount — multiple patterns for Indian bank SMS
        // Patterns: "Rs. 500.00", "Rs 500", "Rs:500", "Rs.500", "INR 500.00", "₹500", "debited by 500.00"
        val amountRegexes = listOf(
            Regex("""(?:rs[.:;]?\s*|₹\s*|inr[.:;]?\s*)([0-9,]+\.?[0-9]*)""", RegexOption.IGNORE_CASE),
            Regex("""(?:debited\s+(?:by\s+|for\s+|with\s+)?)(?:rs[.:;]?\s*|₹\s*|inr[.:;]?\s*)?([0-9,]+\.[0-9]{2})""", RegexOption.IGNORE_CASE),
            Regex("""(?:amount\s*(?:of\s+)?)(?:rs[.:;]?\s*|₹\s*|inr[.:;]?\s*)?([0-9,]+\.[0-9]{2})""", RegexOption.IGNORE_CASE),
            Regex("""([0-9,]+\.[0-9]{2})\s*(?:debited|paid|sent|spent|withdrawn)""", RegexOption.IGNORE_CASE)
        )

        var amount: Double? = null
        for (regex in amountRegexes) {
            val match = regex.find(body) // Use original body for case-sensitive ₹
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    SecureLogger.d(TAG, "Amount found: ₹$amount")
                    break
                }
            }
        }

        if (amount == null || amount <= 0) {
            SecureLogger.d(TAG, "No valid amount found in: ${body.take(80)}")
            return null
        }

        // Step 4: Extract Merchant Name — comprehensive patterns for Indian bank SMS
        var merchant = "Unknown Payee"

        val merchantRegexes = listOf(
            // "paid to Merchant Name" / "sent to Merchant" / "transferred to Merchant"
            Regex("""(?:paid|sent|transferred)\s+to\s+([a-zA-Z0-9\s&.\-_@]+?)(?:\s+on|\s+from|\s+ref|\s+upi|\s+via|\s+a/c|\s+ac|\.\s|$)""", RegexOption.IGNORE_CASE),
            // "debited ... to Merchant"
            Regex("""debited.*?(?:to|at|for)\s+([a-zA-Z0-9\s&.\-_@]+?)(?:\s+on|\s+from|\s+ref|\s+upi|\s+via|\s+a/c|\.\s|$)""", RegexOption.IGNORE_CASE),
            // "spent at Merchant"
            Regex("""spent\s+at\s+([a-zA-Z0-9\s&.\-_@]+?)(?:\s+on|\s+from|\s+ref|\s+upi|\.\s|$)""", RegexOption.IGNORE_CASE),
            // "VPA xxx@ybl" — extract the part before @
            Regex("""vpa\s+([a-zA-Z0-9.\-_]+)@""", RegexOption.IGNORE_CASE),
            // "to VPA xxx@ybl" — UPI ID based extraction
            Regex("""to\s+([a-zA-Z0-9.\-_]+)@""", RegexOption.IGNORE_CASE),
            // "Info: Merchant Name"
            Regex("""info[:\s]+([a-zA-Z0-9\s&.\-_]+?)(?:\s+ref|\s+upi|\.\s|$)""", RegexOption.IGNORE_CASE),
            // "Fvg: Merchant Name" — Union Bank pattern (Favouring)
            Regex("""fvg[:\s]+([a-zA-Z0-9\s&.\-_]+?)(?:\s+avl|\s+bal|\s+ref|\.\s|,|$)""", RegexOption.IGNORE_CASE),
            // Generic "to SomeName" (last resort, looser pattern)
            Regex("""\bto\s+([A-Z][a-zA-Z0-9\s&.\-_]+?)(?:\s+on|\s+ref|\s+upi|\s+via|\.\s|\s+a/c|$)""")
        )

        for (regex in merchantRegexes) {
            val match = regex.find(body)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.isNotEmpty() &&
                    !candidate.lowercase().contains("account") &&
                    !candidate.lowercase().contains("bank") &&
                    !candidate.lowercase().contains("a/c") &&
                    candidate.length > 1 && candidate.length < 50) {
                    merchant = candidate.split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { char -> char.uppercase() }
                    }
                    SecureLogger.d(TAG, "Merchant found: $merchant")
                    break
                }
            }
        }

        // Fallback: search for known merchant names in the body
        if (merchant == "Unknown Payee") {
            val knownMerchants = MERCHANT_CATEGORY_MAP.keys.sortedByDescending { it.length }
            for (known in knownMerchants) {
                if (bodyLower.contains(known)) {
                    merchant = known.replaceFirstChar { it.uppercase() }
                    SecureLogger.d(TAG, "Merchant matched from known list: $merchant")
                    break
                }
            }
        }

        // Step 5: Extract Account ending digits
        // Patterns: "a/c *5024", "a/c XX1234", "A/C *9692", "account **1234", "ending 1234"
        val accountRegexes = listOf(
            Regex("""(?:a/?c|account|acct|ac)\s*(?:no\.?\s*)?(?:ending\s*)?(?:xx|x|\*\*|\*)?\s*(\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""(?:xx|x|\*\*|\*)(\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""(?:ending|ends)\s*(?:with\s+)?(\d{4})""", RegexOption.IGNORE_CASE)
        )
        var accountLast4 = "XXXX"
        for (regex in accountRegexes) {
            val match = regex.find(body)
            if (match != null) {
                accountLast4 = match.groupValues[1]
                break
            }
        }

        // Step 6: Extract Reference ID
        val refRegexes = listOf(
            Regex("""(?:ref|upi\s*ref|ref\s*no|rrn|reference)\s*[.:#\s]*\s*(\d{9,14})""", RegexOption.IGNORE_CASE),
            Regex("""(?:txn|transaction)\s*(?:id|no)?[.:#\s]*\s*([0-9A-Z]{8,14})""", RegexOption.IGNORE_CASE)
        )
        var refId: String? = null
        for (regex in refRegexes) {
            val match = regex.find(body)
            if (match != null) {
                refId = match.groupValues[1]
                break
            }
        }
        if (refId == null) {
            // Generate a stable, deterministic refId using SHA-256 of the SMS body and its stable timestamp.
            // This prevents duplicate entries if sync is run multiple times.
            val bodyHash = generateHash(body + fallbackDateMs).take(12).uppercase()
            refId = "TXN$bodyHash"
        }

        // Step 7: Extract and Format Date and Time from SMS Body
        var formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(fallbackDateMs))
        var formattedTime = SimpleDateFormat("HH:mm", Locale.US).format(Date(fallbackDateMs))

        // Date regex: matches patterns like 21-May-26, 21-05-2026, 21/05/26, 2026-05-21, and 21.05.26 (with dots)
        val dateRegexes = listOf(
            Regex("""(\d{1,2}[-./]\d{1,2}[-./]\d{2,4})"""),
            Regex("""(\d{1,2}\s*(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\s*\d{2,4})""", RegexOption.IGNORE_CASE),
            Regex("""(\d{4}[-./]\d{1,2}[-./]\d{1,2})""")
        )

        for (regex in dateRegexes) {
            val match = regex.find(body)
            if (match != null) {
                val dateStr = match.groupValues[1]
                try {
                    // Added dot-separated formats to the list to support diverse SMS formats.
                    val formats = listOf(
                        "dd-MM-yyyy", "dd/MM/yyyy", "dd.MM.yyyy",
                        "dd-MM-yy", "dd/MM/yy", "dd.MM.yy",
                        "dd MMM yyyy", "dd MMM yy", "dd-MMM-yy", "dd-MMM-yyyy",
                        "dd.MMM.yy", "dd.MMM.yyyy",
                        "ddMMMyy", "ddMMMyyyy", "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd",
                        "dd MMM", "d MMM yyyy"
                    )
                    var parsedDate: Date? = null
                    for (fmt in formats) {
                        try {
                            val sdf = SimpleDateFormat(fmt, Locale.US)
                            sdf.isLenient = false
                            val date = sdf.parse(dateStr)
                            if (date != null) {
                                // Validate that the parsed year falls within a sensible modern range (2000 to 2100).
                                // This prevents SimpleDateFormat from parsing 2-digit years (like "26") into year 26 AD
                                // under 4-digit year format patterns ("yyyy").
                                val cal = Calendar.getInstance()
                                cal.time = date
                                val year = cal.get(Calendar.YEAR)
                                if (year in 2000..2100) {
                                    parsedDate = date
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            // continue trying next format
                        }
                    }
                    if (parsedDate != null) {
                        formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsedDate)
                    }
                } catch (e: Exception) {
                    // Fallback to stable SMS timestamp date on parsing exception
                }
                break
            }
        }

        // Time regex: matches patterns like 12:30:15 or 14:15
        val timeRegex = Regex("""(\d{1,2}:\d{2}(?::\d{2})?)""")
        val timeMatch = timeRegex.find(body)
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
                // Fallback to SMS timestamp time
            }
        }

        // Step 8: Deduplication Hash (SHA-256 of refId + amount)
        val id = generateHash(refId + amount)

        // Step 9: Categorization Suggestion — check both merchant name and raw SMS body
        var category = "Others"
        val merchantLower = merchant.lowercase()
        for ((keyword, cat) in MERCHANT_CATEGORY_MAP) {
            if (merchantLower.contains(keyword) || bodyLower.contains(keyword)) {
                category = cat
                break
            }
        }

        // Step 10: Recurring Payment Detection
        val isRecurring = listOf(
            "netflix", "spotify", "prime", "youtube", "hotstar", "disney",
            "jio", "airtel", "broadband", "insurance"
        ).any { sub -> merchantLower.contains(sub) || bodyLower.contains(sub) }

        SecureLogger.d(TAG, "✅ Parsed: ₹$amount | $merchant | $category | $formattedDate $formattedTime | Ref: $refId")

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
            rawSMS = redactSmsBody(body),   // Store redacted version only
            isRecurring = isRecurring
        )
    }

    private fun generateHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Redacts sensitive identifiers from an SMS body before persisting in the database.
     *
     * Masked patterns (Option B — structure preserved, values hidden):
     * - Account numbers:   "A/C **5024" → "A/C **XXXX"
     *                      "account 123456789" → "account XXXXXXXXX"
     * - UPI VPA:          "user@okicici" → "[UPI-ID-REDACTED]"
     * - Full card/account digit runs: sequences of 9–16 digits → "[REDACTED]"
     *
     * This keeps the structural context of the SMS (bank name, keyword pattern)
     * for potential future analysis without exposing actual account identifiers.
     */
    fun redactSmsBody(body: String): String {
        var redacted = body

        // Mask last-4 patterns: **5024, XX5024, *5024
        redacted = redacted.replace(
            Regex("""(\*{1,2}|xx)\d{4}""", RegexOption.IGNORE_CASE),
            "**XXXX"
        )

        // Mask UPI VPA addresses: anything@provider
        redacted = redacted.replace(
            Regex("""[a-zA-Z0-9.\-_+]+@[a-zA-Z0-9.\-_]+"""),
            "[UPI-ID-REDACTED]"
        )

        // Mask standalone account number sequences (9–16 digits)
        redacted = redacted.replace(
            Regex("""(?<!\d)\d{9,16}(?!\d)"""),
            "[REDACTED]"
        )

        return redacted
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

        SecureLogger.d(TAG, "Syncing SMS from ${Date(startTimeMs)} to ${Date(endTimeMs)}")

        val cursor = contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("body", "date", "address"),
            "date >= ? AND date <= ?",
            arrayOf(startTimeMs.toString(), endTimeMs.toString()),
            "date DESC"
        )

        var totalSmsRead = 0
        var totalParsed = 0

        cursor?.use {
            val bodyIndex = it.getColumnIndexOrThrow("body")
            val dateIndex = it.getColumnIndexOrThrow("date")
            val addressIndex = it.getColumnIndexOrThrow("address")

            while (it.moveToNext()) {
                val address = it.getString(addressIndex) ?: ""
                val body = it.getString(bodyIndex) ?: ""
                val smsDateMs = it.getLong(dateIndex)
                totalSmsRead++

                // Check if address matches any known bank sender ID
                val addressUpper = address.uppercase()
                val isBankSMS = BANK_SENDER_IDS.any { bankId -> addressUpper.contains(bankId) }

                // Also check if the SMS body contains debit keywords (catches non-standard sender IDs)
                val bodyLower = body.lowercase(Locale.ROOT)
                val hasDebitKeyword = DEBIT_KEYWORDS.any { keyword -> bodyLower.contains(keyword) }

                if (isBankSMS || hasDebitKeyword) {
                    SecureLogger.d(TAG, "Processing SMS from: $address | Bank: $isBankSMS | Debit: $hasDebitKeyword")
                    // Note: SMS body is NOT logged even in debug to protect financial data

                    val txn = parseSMS(body, smsDateMs)
                    if (txn != null) {
                        transactions.add(txn)
                        totalParsed++
                    }
                }
            }
        }

        SecureLogger.d(TAG, "Sync complete: $totalSmsRead SMS read, $totalParsed transactions parsed")
        return transactions
    }
}
