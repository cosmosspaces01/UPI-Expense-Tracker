package com.upi.expensetracker.utils

import android.util.Log
import com.upi.expensetracker.BuildConfig

/**
 * A secure logging wrapper that suppresses all output in release builds.
 *
 * In a finance app, raw Log.d/e calls can leak sensitive data (transaction
 * amounts, bank account numbers, SMS bodies) to Logcat, which is readable
 * by any app with READ_LOGS permission. This wrapper ensures zero log output
 * in production while retaining full debug logs during development.
 *
 * Usage: Replace android.util.Log.d(...) with SecureLogger.d(...)
 */
object SecureLogger {

    /** Debug log — emitted only in debug builds. */
    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.d(tag, msg)
    }

    /** Warning log — emitted only in debug builds. */
    fun w(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.w(tag, msg)
    }

    /**
     * Error log — always emitted (without the message body in release).
     * The throwable stack trace is only logged in debug builds to avoid
     * leaking implementation details in production crash logs.
     */
    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, msg, throwable)
        } else {
            // In release, log a generic message without sensitive context
            Log.e(tag, "An error occurred. Enable debug build for details.")
        }
    }
}
