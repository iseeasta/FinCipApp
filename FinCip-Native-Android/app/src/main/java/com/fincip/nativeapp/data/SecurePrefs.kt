package com.fincip.nativeapp.data

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParser

class SecurePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("fincip_private_settings", Context.MODE_PRIVATE)
    fun saveSupabase(url: String, anonKey: String, accessToken: String, userId: String, householdId: String = "") {
        val secret = JsonObject().apply {
            addProperty("accessToken", accessToken); addProperty("userId", userId); addProperty("householdId", householdId)
        }.toString()
        prefs.edit().putString("supabaseUrl", url).putString("anonKey", anonKey)
            .putString("secret", LocalKeyVault.encrypt(secret)).apply()
    }
    fun readSupabase(): JsonObject {
        val result = JsonObject()
        result.addProperty("url", prefs.getString("supabaseUrl", "") ?: "")
        result.addProperty("anonKey", prefs.getString("anonKey", "") ?: "")
        val secret = prefs.getString("secret", null)
        if (!secret.isNullOrBlank()) runCatching { JsonParser.parseString(LocalKeyVault.decrypt(secret)).asJsonObject }.getOrNull()?.entrySet()?.forEach { (k, v) -> result.add(k, v) }
        return result
    }
    fun clearSession() { prefs.edit().remove("secret").apply() }
}
