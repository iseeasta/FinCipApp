package com.fincip.nativeapp.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class FinanceStore(context: Context) : SQLiteOpenHelper(context, "fincip-private.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE workspace (id INTEGER PRIMARY KEY CHECK(id = 1), payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    @Synchronized fun saveWorkspace(plainJson: String) {
        val cipherText = LocalKeyVault.encrypt(plainJson)
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO workspace(id, payload, updated_at) VALUES(1, ?, ?)",
            arrayOf<Any>(cipherText, System.currentTimeMillis())
        )
    }

    @Synchronized fun loadWorkspace(): String? {
        readableDatabase.rawQuery("SELECT payload FROM workspace WHERE id = 1", null).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return LocalKeyVault.decrypt(cursor.getString(0))
        }
    }

    companion object { const val ANDROID_KEY_ALIAS = "fincip_workspace_key_v1" }
}

object LocalKeyVault {
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = FinanceStore.ANDROID_KEY_ALIAS
        if (store.containsAlias(alias)) return store.getKey(alias, null) as SecretKey
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(text: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val packed = cipher.iv + cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        val packed = Base64.decode(encoded, Base64.NO_WRAP)
        require(packed.size > 12) { "The local FinCip database record is invalid." }
        val iv = packed.copyOfRange(0, 12)
        val cipherText = packed.copyOfRange(12, packed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }
}
