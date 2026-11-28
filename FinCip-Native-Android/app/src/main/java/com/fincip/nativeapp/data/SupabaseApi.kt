package com.fincip.nativeapp.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/** Supabase Auth + PostgREST client. Uses only the public anon key and the signed-in user's JWT. */
class SupabaseApi(private val baseUrl: String, private val anonKey: String) {
    data class Session(val userId: String, val accessToken: String, val refreshToken: String? = null)

    private fun request(path: String, method: String, token: String? = null, body: String? = null, prefer: String? = null): String {
        require(baseUrl.startsWith("https://")) { "Supabase URL must use HTTPS." }
        val url = URL(baseUrl.trimEnd('/') + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("apikey", anonKey)
            setRequestProperty("Content-Type", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (prefer != null) setRequestProperty("Prefer", prefer)
            if (body != null) doOutput = true
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = try { JsonParser.parseString(response).asJsonObject.string("message", response) } catch (_: Exception) { response }
                throw IllegalStateException("Supabase ($code): $message")
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    fun signIn(email: String, password: String): Session {
        val body = JsonObject().apply { addProperty("email", email); addProperty("password", password) }
        val response = JsonParser.parseString(request("/auth/v1/token?grant_type=password", "POST", body = body.toString())).asJsonObject
        val user = response.getAsJsonObject("user") ?: throw IllegalStateException("Supabase did not return a user profile.")
        return Session(user.string("id"), response.string("access_token"), response.string("refresh_token"))
    }

    fun pushWorkspace(session: Session, payload: JsonObject) {
        val row = JsonObject().apply {
            addProperty("user_id", session.userId)
            add("payload", payload)
            addProperty("updated_at", java.time.Instant.now().toString())
        }
        request("/rest/v1/user_finance_data?on_conflict=user_id", "POST", session.accessToken, row.toString(), "resolution=merge-duplicates,return=minimal")
    }

    fun pullWorkspace(session: Session): JsonObject? {
        val response = request("/rest/v1/user_finance_data?select=payload&user_id=eq.${session.userId}&limit=1", "GET", session.accessToken)
        val rows = JsonParser.parseString(response).asJsonArray
        if (rows.size() == 0) return null
        return rows[0].asJsonObject.getAsJsonObject("payload")
    }

    fun createHousehold(session: Session, name: String): JsonObject {
        val body = JsonObject().apply { addProperty("p_name", name) }
        val response = request("/rest/v1/rpc/create_fincip_household", "POST", session.accessToken, body.toString())
        val parsed = JsonParser.parseString(response)
        return if (parsed.isJsonArray && parsed.asJsonArray.size() > 0) parsed.asJsonArray[0].asJsonObject else parsed.asJsonObject
    }

    fun joinHousehold(session: Session, inviteCode: String): String {
        val body = JsonObject().apply { addProperty("p_invite_code", inviteCode) }
        return request("/rest/v1/rpc/join_fincip_household", "POST", session.accessToken, body.toString()).trim().trim('"')
    }

    fun sharedBudgets(session: Session, householdId: String): String =
        request("/rest/v1/shared_budgets?select=*&household_id=eq.$householdId&order=category.asc", "GET", session.accessToken)

    fun addSharedBudget(session: Session, householdId: String, category: String, limit: Double, currency: String) {
        val body = JsonObject().apply {
            addProperty("household_id", householdId); addProperty("category", category)
            addProperty("monthly_limit", limit); addProperty("currency", currency); addProperty("created_by", session.userId)
        }
        request("/rest/v1/shared_budgets", "POST", session.accessToken, body.toString(), "resolution=merge-duplicates,return=minimal")
    }

    // ---- social (see supabase/migrations/002_social.sql)
    private fun rpcJson(session: Session, name: String, body: JsonObject = JsonObject()) = request("/rest/v1/rpc/$name", "POST", session.accessToken, body.toString())

    /** Creates or refreshes my public social profile and returns my QR token. */
    fun ensureSocial(session: Session, handle: String, name: String, avatar: JsonObject, level: Int, signature: JsonObject): String {
        val b = JsonObject().apply { addProperty("p_handle", handle); addProperty("p_name", name); add("p_avatar", avatar); addProperty("p_level", level); add("p_signature", signature) }
        return rpcJson(session, "ensure_social", b).trim().trim('"')
    }
    fun followByQr(session: Session, token: String): JsonObject = JsonParser.parseString(rpcJson(session, "follow_by_qr", JsonObject().apply { addProperty("p_token", token) })).asJsonObject
    fun rotateQr(session: Session): String = rpcJson(session, "rotate_qr").trim().trim('"')
    fun socialSummary(session: Session): JsonObject = JsonParser.parseString(rpcJson(session, "social_summary")).asJsonObject
    fun socialProfiles(session: Session): List<JsonObject> = JsonParser.parseString(request("/rest/v1/social_profiles?select=*", "GET", session.accessToken)).asJsonArray.map { it.asJsonObject }
    fun followEdges(session: Session): List<JsonObject> = JsonParser.parseString(request("/rest/v1/follows?select=follower,followee", "GET", session.accessToken)).asJsonArray.map { it.asJsonObject }
    fun unfollow(session: Session, userId: String) { request("/rest/v1/follows?follower=eq.${session.userId}&followee=eq.$userId", "DELETE", session.accessToken, prefer = "return=minimal") }
    fun posts(session: Session): List<JsonObject> = JsonParser.parseString(request("/rest/v1/posts?select=id,user_id,body,created_at&order=created_at.desc&limit=80", "GET", session.accessToken)).asJsonArray.map { it.asJsonObject }
    fun createPost(session: Session, body: String) {
        val row = JsonObject().apply { addProperty("user_id", session.userId); addProperty("body", body.trim()) }
        request("/rest/v1/posts", "POST", session.accessToken, row.toString(), "return=minimal")
    }
    fun deletePost(session: Session, id: String) { request("/rest/v1/posts?id=eq.$id&user_id=eq.${session.userId}", "DELETE", session.accessToken, prefer = "return=minimal") }
}
