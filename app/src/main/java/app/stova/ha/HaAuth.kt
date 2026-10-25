package app.stova.ha

import app.stova.core.security.SecretStore
import app.stova.core.storage.StovaJson
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

const val HA_CLIENT_ID = "https://oyvhov.github.io/stova/"
const val HA_REDIRECT_URI = "https://oyvhov.github.io/stova/auth-callback"

@Serializable data class HaTokens(val access: String, val expiresAt: Long, val refresh: String? = null)
enum class HaAuthState { SIGNED_OUT, SIGNED_IN }
class HaSignedOut : Exception()

fun authorizeUrl(address: HaAddress, state: String): String =
    "${address.base}/auth/authorize?response_type=code&client_id=${encode(HA_CLIENT_ID)}&redirect_uri=${encode(HA_REDIRECT_URI)}&state=${encode(state)}"
private fun encode(s: String) = URLEncoder.encode(s, "UTF-8")

fun parseCallback(url: String, expectedState: String): String? = runCatching {
    val uri = URI(url)
    val destination = URI(HA_REDIRECT_URI)
    if (uri.scheme != destination.scheme || uri.rawAuthority != destination.rawAuthority || uri.path != destination.path || uri.fragment != null) return null
    val params = uri.rawQuery.orEmpty().split('&').map { it.split('=', limit = 2) }
        .groupBy({ URLDecoder.decode(it[0], "UTF-8") }, { URLDecoder.decode(it.getOrElse(1) { "" }, "UTF-8") })
    if (params["state"] != listOf(expectedState)) null else params["code"]?.singleOrNull()?.takeIf { it.isNotBlank() }
}.getOrNull()

class HaAuth(
    private val http: OkHttpClient,
    private val secrets: SecretStore,
    private val address: suspend () -> HaAddress?,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private var loaded = false
    private var cached: HaTokens? = null
    private val mutableState = MutableStateFlow(HaAuthState.SIGNED_OUT)
    val state = mutableState.asStateFlow()
    private suspend fun tokens(): HaTokens? {
        if (!loaded) {
            cached = secrets.read("ha/tokens")?.let { StovaJson.decodeFromString<HaTokens>(it) }
            loaded = true
        }
        return cached
    }
    private suspend fun save(tokens: HaTokens) {
        secrets.write("ha/tokens", StovaJson.encodeToString(tokens))
        cached = tokens; loaded = true
        mutableState.value = HaAuthState.SIGNED_IN
    }
    suspend fun restore() = mutex.withLock { mutableState.value = if (tokens() == null) HaAuthState.SIGNED_OUT else HaAuthState.SIGNED_IN }
    suspend fun exchange(code: String, at: HaAddress? = null) = mutex.withLock {
        save(postToken(mapOf("grant_type" to "authorization_code", "code" to code, "client_id" to HA_CLIENT_ID), null, at))
    }
    suspend fun useLongLivedToken(token: String) = mutex.withLock { require(token.isNotBlank()); save(HaTokens(token.trim(), Long.MAX_VALUE)) }
    suspend fun accessToken(): String = mutex.withLock {
        var t = tokens() ?: throw HaSignedOut()
        if (t.refresh != null && t.expiresAt - 60_000 < clock()) {
            t = postToken(mapOf("grant_type" to "refresh_token", "refresh_token" to t.refresh, "client_id" to HA_CLIENT_ID), t.refresh)
            save(t)
        }
        mutableState.value = HaAuthState.SIGNED_IN
        t.access
    }
    suspend fun invalidateAccess() = mutex.withLock { tokens()?.let { save(it.copy(expiresAt = 0)) } }
    internal suspend fun reject() = mutex.withLock { secrets.delete("ha/tokens"); cached = null; loaded = true; mutableState.value = HaAuthState.SIGNED_OUT }
    suspend fun signOut() = mutex.withLock {
        val t = tokens()
        secrets.delete("ha/tokens"); cached = null; loaded = true; mutableState.value = HaAuthState.SIGNED_OUT
        val base = address()?.base
        if (t?.refresh != null && base != null) {
            try { withContext(Dispatchers.IO) { http.newCall(Request.Builder().url("$base/auth/token").post(form(mapOf("token" to t.refresh, "action" to "revoke"))).build()).execute().close() } }
            catch (_: IOException) { /* Local sign-out succeeds even while HA is offline. */ }
        }
    }
    private suspend fun postToken(fields: Map<String, String>, oldRefresh: String?, at: HaAddress? = null): HaTokens {
        val base = (at ?: address())?.base ?: throw HaSignedOut()
        return withContext(Dispatchers.IO) {
            http.newCall(Request.Builder().url("$base/auth/token").post(form(fields)).build()).execute().use { response ->
                if (oldRefresh != null && response.code in listOf(400, 401)) {
                    secrets.delete("ha/tokens"); cached = null; loaded = true; mutableState.value = HaAuthState.SIGNED_OUT; throw HaSignedOut()
                }
                if (!response.isSuccessful) throw IOException("HA token request failed (${response.code})")
                val json = StovaJson.parseToJsonElement(response.body!!.string()).jsonObject
                HaTokens(json.getValue("access_token").jsonPrimitive.content,
                    clock() + json.getValue("expires_in").jsonPrimitive.long * 1000,
                    json["refresh_token"]?.jsonPrimitive?.content ?: oldRefresh)
            }
        }
    }
    private fun form(fields: Map<String, String>) = FormBody.Builder().apply { fields.forEach { (k, v) -> add(k, v) } }.build()
}
