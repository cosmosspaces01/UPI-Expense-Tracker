package com.upi.expensetracker.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Manages the encryption key used to protect the Room SQLCipher database.
 *
 * Strategy:
 * - On first launch, generate a cryptographically random 256-bit key.
 * - Store it in EncryptedSharedPreferences, which is itself backed by the
 *   Android Keystore hardware (or software on older devices).
 * - On subsequent launches, retrieve the same key — no re-generation.
 *
 * The key never leaves the device and is never stored in plaintext.
 */
object DatabaseKeyManager {

    private const val PREFS_FILE = "db_key_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"

    /**
     * Returns the database encryption passphrase as a CharArray.
     * Creates and persists one on first call; retrieves it on subsequent calls.
     *
     * @param context Application context (used to access EncryptedSharedPreferences).
     * @return A CharArray passphrase suitable for passing to SQLCipher's SupportFactory.
     */
    fun getOrCreatePassphrase(context: Context): CharArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Return existing passphrase if already generated
        val existing = encryptedPrefs.getString(KEY_DB_PASSPHRASE, null)
        if (!existing.isNullOrEmpty()) {
            return existing.toCharArray()
        }

        // First launch — generate a new random 256-bit (32-byte) passphrase
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)

        // Encode as a hex string (64 chars) for safe storage in SharedPreferences
        val passphrase = randomBytes.joinToString("") { "%02x".format(it) }

        encryptedPrefs.edit().putString(KEY_DB_PASSPHRASE, passphrase).apply()

        return passphrase.toCharArray()
    }
}
