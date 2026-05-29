# ─── Room Database ─────────────────────────────────────────────────────────────
# Keep all Room entity and DAO classes from being renamed/removed by R8.
-keep class com.upi.expensetracker.data.** { *; }

# ─── SQLCipher ──────────────────────────────────────────────────────────────────
# SQLCipher's native JNI bridge must not be obfuscated.
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# ─── Jetpack Compose ────────────────────────────────────────────────────────────
# Compose relies on reflection for composable function names in stack traces.
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ─── AndroidX Security (EncryptedSharedPreferences) ────────────────────────────
-keep class androidx.security.crypto.** { *; }

# ─── AndroidX Biometric ─────────────────────────────────────────────────────────
-keep class androidx.biometric.** { *; }

# ─── Kotlin coroutines ──────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ─── Kotlin serialization / reflection ──────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
