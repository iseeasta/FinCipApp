package com.fincip.nativeapp.data

import android.util.Base64
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoBackup {
    private const val ITERATIONS = 310_000
    private const val KEY_BITS = 256

    fun encrypt(workspaceJson: String, password: String): String {
        require(password.length >= 8) { "Use a backup password with at least 8 characters." }
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val key = derive(password, salt, ITERATIONS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(workspaceJson.toByteArray(Charsets.UTF_8))
        return JsonObject().apply {
            addProperty("format", "fincip-encrypted-backup")
            addProperty("version", 1)
            addProperty("kdf", "PBKDF2-SHA256")
            addProperty("iterations", ITERATIONS)
            addProperty("cipher", "AES-256-GCM")
            addProperty("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            addProperty("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            addProperty("data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        }.toString()
    }

    fun decrypt(backupText: String, password: String): String {
        val root = JsonParser.parseString(backupText).asJsonObject
        require(root.string("format") == "fincip-encrypted-backup" && root.double("version").toInt() == 1) {
            "This is not a supported encrypted FinCip backup."
        }
        val iterations = root.double("iterations", ITERATIONS.toDouble()).toInt().coerceIn(100_000, 1_000_000)
        val salt = Base64.decode(root.string("salt"), Base64.DEFAULT)
        val iv = Base64.decode(root.string("iv"), Base64.DEFAULT)
        val encrypted = Base64.decode(root.string("data"), Base64.DEFAULT)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, derive(password, salt, iterations), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(raw, "AES")
    }
}
