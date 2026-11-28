package com.fincip.nativeapp.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.Signature
import com.fincip.nativeapp.data.Social
import com.fincip.nativeapp.data.SupabaseApi
import com.fincip.nativeapp.data.objectOrEmpty
import com.fincip.nativeapp.data.string
import com.fincip.nativeapp.data.double
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SProfile(val id: String, val handle: String, val name: String, val seed: Long, val mode: String, val preset: Int, val level: Int, val sig: Signature)
data class SPost(val id: String, val userId: String, val body: String, val at: String)
data class SocialData(val token: String, val me: SProfile, val others: List<SProfile>, val following: Set<String>, val followers: Set<String>, val posts: List<SPost>, val posts_n: Int)

fun socialHandle(handle: String, name: String): String {
    val raw = handle.ifBlank { name }.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }
    return (if (raw.length < 3) raw + "_fc" else raw).take(24)
}

private fun parseProfile(o: JsonObject): SProfile {
    val av = o.objectOrEmpty("avatar")
    return SProfile(o.string("user_id"), o.string("handle"), o.string("display_name"), av.double("seed").toLong(), av.string("mode", "seed"), av.double("preset").toInt(), o.double("level").toInt().coerceAtLeast(1),
        Signature.fromJson(if (o.has("signature") && o.get("signature").isJsonObject) o.getAsJsonObject("signature") else null))
}

/** Pushes my profile (if I share my signature), then reads everything I'm allowed to see. */
suspend fun loadSocial(cloud: JsonObject, workspace: JsonObject, level: Int, shareSig: Boolean): SocialData = withContext(Dispatchers.IO) {
    val api = SupabaseApi(cloud.string("url"), cloud.string("anonKey"))
    val s = SupabaseApi.Session(cloud.string("userId"), cloud.string("accessToken"))
    val p = workspace.objectOrEmpty("profile")
    val avatar = JsonObject().apply { addProperty("mode", p.string("avatarMode", "seed").let { if (it == "photo") "seed" else it }); addProperty("seed", p.double("avatarSeed").toLong()); addProperty("preset", p.double("preset").toInt()) }
    val sig = if (shareSig) Social.signature(workspace, level).toJson() else JsonObject()
    val token = api.ensureSocial(s, socialHandle(p.string("handle"), p.string("name")), p.string("name").ifBlank { "FinCip user" }, avatar, level, sig)
    val profiles = api.socialProfiles(s).map { parseProfile(it) }
    val edges = api.followEdges(s)
    val following = edges.filter { it.string("follower") == s.userId }.map { it.string("followee") }.toSet()
    val followers = edges.filter { it.string("followee") == s.userId }.map { it.string("follower") }.toSet()
    val summary = api.socialSummary(s)
    val me = profiles.firstOrNull { it.id == s.userId } ?: SProfile(s.userId, "", p.string("name"), p.double("avatarSeed").toLong(), "seed", 0, level, Signature.fromJson(sig))
    SocialData(token, me, profiles.filter { it.id != s.userId }, following, followers, api.posts(s).map { SPost(it.string("id"), it.string("user_id"), it.string("body"), it.string("created_at")) }, summary.double("posts").toInt())
}

fun startQrScan(context: Context, onResult: (String?) -> Unit) {
    runCatching {
        val opts = com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder().setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE).build()
        com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context, opts).startScan()
            .addOnSuccessListener { onResult(it.rawValue) }.addOnFailureListener { onResult(null) }
    }.onFailure { onResult(null) }
}

@Composable
fun SocialScreen(workspace: JsonObject, cloud: JsonObject, level: Int, onLocal: (JsonObject.() -> Unit) -> Unit, onOpenCloud: () -> Unit, onUniverse: () -> Unit) {
    val s = MaterialTheme.colorScheme
    val context = LocalContext.current
    val tap = rememberTap()
    val scope = rememberCoroutineScope()
    val signedIn = cloud.string("url").isNotBlank() && cloud.string("userId").isNotBlank() && cloud.string("accessToken").isNotBlank()
    val social = workspace.objectOrEmpty("social")
    val shareSig = !social.has("shareSignature") || social.get("shareSignature").asBoolean
    val style = QrStyle.fromJson(if (social.has("qr") && social.get("qr").isJsonObject) social.getAsJsonObject("qr") else null)
    var data by remember { mutableStateOf<SocialData?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf("") }
    var manual by remember { mutableStateOf("") }

    LaunchedEffect(reload, signedIn, shareSig) {
        if (!signedIn) return@LaunchedEffect
        busy = true
        runCatching { loadSocial(cloud, workspace, level, shareSig) }.onSuccess { d ->
            data = d; msg = null
            onLocal { val so = getAsJsonObject("social") ?: JsonObject().also { add("social", it) }; so.add("summary", JsonObject().apply { addProperty("posts", d.posts_n); addProperty("followers", d.followers.size); addProperty("following", d.following.size) }) }
        }.onFailure { msg = if ((it.message ?: "").contains("JWT", true) || (it.message ?: "").contains("401")) "Your Supabase sign-in expired. Sign in again under Household & sync." else (it.message ?: "Couldn't reach Supabase") }
        busy = false
    }
    fun act(block: suspend () -> String?) { scope.launch { busy = true; val r = runCatching { block() }.getOrElse { it.message }; if (r != null) msg = r; busy = false; reload++ } }
    fun follow(token: String) = act {
        withContext(Dispatchers.IO) { SupabaseApi(cloud.string("url"), cloud.string("anonKey")).followByQr(SupabaseApi.Session(cloud.string("userId"), cloud.string("accessToken")), token) }
            .let { "Now following ${it.string("name").ifBlank { "them" }}. Ask them to scan yours so they can follow you back." }
    }
    fun session() = SupabaseApi.Session(cloud.string("userId"), cloud.string("accessToken"))
    fun api() = SupabaseApi(cloud.string("url"), cloud.string("anonKey"))

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp + LocalNavTop.current, bottom = 28.dp + LocalNavInset.current), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (!signedIn) {
            NeoCard {
                Text("Friends need a server", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("Followers, posts and QR friends sync through your own Supabase project. Sign in under Household & sync, and run supabase/migrations/002_social.sql in that project once.", Modifier.padding(top = 6.dp), fontSize = 13.sp, color = s.onSurface.copy(alpha = .7f))
                Spacer(Modifier.height(12.dp)); NeoButton("Open Household & sync", Modifier.fillMaxWidth(), onClick = onOpenCloud)
            }
            return@Column
        }
        msg?.let { Text(it, color = s.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        val d = data
        // ---- stats
        NeoCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat("Posts", d?.posts_n?.toString() ?: "—"); Stat("Followers", d?.followers?.size?.toString() ?: "—"); Stat("Following", d?.following?.size?.toString() ?: "—")
            }
            if (busy) Text("Syncing…", Modifier.padding(top = 8.dp), fontSize = 12.sp, color = s.onSurface.copy(alpha = .5f))
        }
        NeoButton("Open my universe", Modifier.fillMaxWidth(), onClick = onUniverse)

        // ---- my QR
        NeoCard {
            Text("My QR", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("The only way anyone can find you. Design it, show it, or save it.", fontSize = 12.sp, color = s.onSurface.copy(alpha = .6f))
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { if (d != null) QrView(Social.qrPayload(d.token), style, 220.dp) else Box(Modifier.size(220.dp)) }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QrPalettes.forEachIndexed { i, p ->
                    Box(Modifier.size(38.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(p.a, p.b))).border(if (i == style.palette) 3.dp else 1.dp, if (i == style.palette) s.primary else s.outline, CircleShape).clickable { tap(); onLocal { saveStyle(style.copy(palette = i)) } })
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QrDots.forEach { dt -> Chip(dt.replaceFirstChar { it.uppercase() }, style.dots == dt) { tap(); onLocal { saveStyle(style.copy(dots = dt)) } } }
                Chip("Gradient", style.gradient) { tap(); onLocal { saveStyle(style.copy(gradient = !style.gradient)) } }
                Chip("Logo", style.badge) { tap(); onLocal { saveStyle(style.copy(badge = !style.badge)) } }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoOutlineButton("Save image", { if (d != null) msg = saveQrToGallery(context, Social.qrPayload(d.token), style) }, Modifier.weight(1f))
                NeoOutlineButton("New code", { act { withContext(Dispatchers.IO) { api().rotateQr(session()) }; "Old code stopped working. Your new code is ready." } }, Modifier.weight(1f))
            }
        }

        // ---- scan
        NeoCard {
            Text("Add a friend", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("Scan their FinCip QR. You'll follow them; when they scan yours, you're friends.", fontSize = 12.sp, color = s.onSurface.copy(alpha = .6f))
            Spacer(Modifier.height(10.dp))
            NeoButton("Scan QR", Modifier.fillMaxWidth(), onClick = {
                startQrScan(context) { raw -> val t = Social.parseQr(raw); if (t != null) follow(t) else msg = "That wasn't a FinCip friend code, or the scanner isn't available on this phone. You can paste a code below." }
            })
            Spacer(Modifier.height(8.dp))
            PopField(value = manual, onValueChange = { manual = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Or paste fincip://friend/… code") })
            Spacer(Modifier.height(8.dp))
            NeoOutlineButton("Follow with code", { val t = Social.parseQr(manual); if (t != null) { follow(t); manual = "" } else msg = "That doesn't look like a FinCip code." }, Modifier.fillMaxWidth())
        }

        // ---- privacy of the universe
        NeoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Share my signature", fontWeight = FontWeight.Bold); Text("Lets friends' planets sit near or far from you. Only spending-category shares, tools you use and your level. No amounts, no payees.", fontSize = 12.sp, color = s.onSurface.copy(alpha = .6f)) }
                Switch(shareSig, { v -> onLocal { val so = getAsJsonObject("social") ?: JsonObject().also { add("social", it) }; so.addProperty("shareSignature", v) } })
            }
        }

        // ---- people and posts
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Feed", "Followers", "Following").forEachIndexed { i, t -> Chip(t, tab == i) { tap(); tab = i } } }
        if (d != null) when (tab) {
            0 -> {
                NeoCard {
                    PopField(value = draft, onValueChange = { draft = it.take(500) }, modifier = Modifier.fillMaxWidth(), label = { Text("Share a win with people who follow you") })
                    Spacer(Modifier.height(8.dp))
                    NeoButton("Post", Modifier.fillMaxWidth(), onClick = { val b = draft; draft = ""; act { withContext(Dispatchers.IO) { api().createPost(session(), b) }; null } }, enabled = draft.isNotBlank())
                }
                if (d.posts.isEmpty()) Text("No posts yet. Followers see what you post; you see posts from people you follow.", fontSize = 13.sp, color = s.onSurface.copy(alpha = .6f))
                d.posts.forEach { p ->
                    val who = if (p.userId == cloud.string("userId")) d.me else d.others.firstOrNull { it.id == p.userId }
                    NeoCard {
                        Text((who?.name ?: "Someone") + (who?.handle?.takeIf { it.isNotBlank() }?.let { "  @$it" } ?: ""), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(p.body, Modifier.padding(top = 4.dp))
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(p.at.take(10), fontSize = 11.sp, color = s.onSurface.copy(alpha = .5f), modifier = Modifier.weight(1f))
                            if (p.userId == cloud.string("userId")) Text("Delete", Modifier.clickable { act { withContext(Dispatchers.IO) { api().deletePost(session(), p.id) }; null } }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = s.error)
                        }
                    }
                }
            }
            else -> {
                val ids = if (tab == 1) d.followers else d.following
                val list = d.others.filter { it.id in ids }
                if (list.isEmpty()) Text(if (tab == 1) "Nobody follows you yet. They follow you by scanning your QR." else "You're not following anyone yet. Scan a friend's QR.", fontSize = 13.sp, color = s.onSurface.copy(alpha = .6f))
                list.forEach { p ->
                    NeoCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProfileAvatar(p.seed, size = 44.dp)
                            Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold); Text("@${p.handle} · Lv ${p.level}" + if (p.id in d.following && p.id in d.followers) " · friends" else "", fontSize = 12.sp, color = s.onSurface.copy(alpha = .6f)) }
                            if (p.id in d.following) Text("Unfollow", Modifier.clickable { act { withContext(Dispatchers.IO) { api().unfollow(session(), p.id) }; null } }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = s.error)
                        }
                    }
                }
            }
        }
    }
}

private fun JsonObject.saveStyle(st: QrStyle) { val so = getAsJsonObject("social") ?: JsonObject().also { add("social", it) }; so.add("qr", st.toJson()) }

@Composable private fun Stat(label: String, value: String) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, fontWeight = FontWeight.Black, fontSize = 24.sp); Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
}

@Composable private fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    Text(text, Modifier.clip(RoundedCornerShape(50)).background(if (on) s.primary else s.surfaceVariant).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp), color = if (on) s.onPrimary else s.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
}

/** Posts / followers / following as last synced. Shown on the profile; tap to open the Social page. */
@Composable
fun SocialStatsRow(workspace: JsonObject, onClick: () -> Unit) {
    val sum = workspace.objectOrEmpty("social").objectOrEmpty("summary")
    val has = workspace.objectOrEmpty("social").has("summary")
    NeoCard(Modifier.clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat("Posts", if (has) sum.double("posts").toInt().toString() else "—"); Stat("Followers", if (has) sum.double("followers").toInt().toString() else "—"); Stat("Following", if (has) sum.double("following").toInt().toString() else "—")
        }
        if (!has) Text("Tap to set up friends with a QR code", Modifier.padding(top = 6.dp).fillMaxWidth(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
