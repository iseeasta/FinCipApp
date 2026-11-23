package com.fincip.nativeapp.data

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * App-lock settings. The password is never stored: only a salted PBKDF2 hash. Lives in its own private
 * preferences file so it is not part of the exported workspace or backups.
 */
object AppLock {
    private const val FILE = "fincip_lock"
    private fun prefs(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun hasPassword(c: Context) = prefs(c).getString("hash", null) != null
    fun lockEnabled(c: Context) = hasPassword(c) && prefs(c).getBoolean("enabled", true)
    fun setLockEnabled(c: Context, on: Boolean) = prefs(c).edit().putBoolean("enabled", on).apply()
    fun biometricEnabled(c: Context) = hasPassword(c) && prefs(c).getBoolean("bio", false)
    fun setBiometric(c: Context, on: Boolean) = prefs(c).edit().putBoolean("bio", on).apply()
    /** Seconds the app may stay in the background before it locks again (0 = immediately). */
    fun timeoutSeconds(c: Context) = prefs(c).getInt("timeout", 30)
    fun setTimeoutSeconds(c: Context, s: Int) = prefs(c).edit().putInt("timeout", s).apply()
    /** One "Privacy screen" switch: hides the app in Recents and blocks screenshots/recording. */
    fun privacyScreen(c: Context) = prefs(c).getBoolean("privacy_screen", false)
    fun setPrivacyScreen(c: Context, on: Boolean) = prefs(c).edit().putBoolean("privacy_screen", on).apply()

    private fun hash(password: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password.toCharArray(), salt, 60_000, 256)).encoded

    fun setPassword(c: Context, password: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs(c).edit()
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hash(password, salt), Base64.NO_WRAP))
            .putBoolean("enabled", true).apply()
    }

    fun verify(c: Context, password: String): Boolean {
        val p = prefs(c)
        val salt = p.getString("salt", null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val stored = p.getString("hash", null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        return MessageDigest.isEqual(stored, hash(password, salt))
    }

    fun removePassword(c: Context) = prefs(c).edit().remove("hash").remove("salt").remove("tpl").remove("type").putBoolean("enabled", false).putBoolean("bio", false).apply()

    /** "password", "pin", "pattern", "draw", "rhythm", "emoji" or "orbs". */
    fun lockType(c: Context): String = prefs(c).getString("type", "password") ?: "password"
    private fun fuzzy(type: String) = type == "draw" || type == "rhythm"

    fun same(type: String, a: String, b: String): Boolean = when (type) {
        "draw" -> LockShape.sameDrawing(a, b)
        "rhythm" -> LockShape.sameRhythm(a, b)
        else -> a == b
    }

    /** Stores the secret for any lock style. Exact styles are hashed; gesture styles also keep a template for fuzzy matching. */
    fun setSecret(c: Context, type: String, secret: String) {
        setPassword(c, secret)
        prefs(c).edit().putString("type", type).putString("tpl", if (fuzzy(type)) secret else null).apply()
    }

    fun check(c: Context, input: String): Boolean {
        val t = lockType(c)
        return if (fuzzy(t)) same(t, prefs(c).getString("tpl", "") ?: "", input) else verify(c, input)
    }

    @Suppress("DEPRECATION")
    fun biometricAvailable(c: Context): Boolean = when {
        Build.VERSION.SDK_INT >= 30 -> c.getSystemService(BiometricManager::class.java)?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        Build.VERSION.SDK_INT >= 29 -> c.getSystemService(BiometricManager::class.java)?.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
        Build.VERSION.SDK_INT >= 28 -> c.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        else -> false
    }

    /** Shows the system fingerprint/face sheet. [onResult] gets true only on a successful match. */
    fun authenticate(c: Context, title: String, onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < 28) { onResult(false); return }
        val executor = c.mainExecutor
        val prompt = BiometricPrompt.Builder(c)
            .setTitle(title)
            .setNegativeButton("Use password", executor) { _, _ -> onResult(false) }
            .build()
        prompt.authenticate(CancellationSignal(), executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) { onResult(true) }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) { onResult(false) }
        })
    }
}
