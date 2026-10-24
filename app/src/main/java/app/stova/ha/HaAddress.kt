package app.stova.ha

import java.net.URI
import java.util.Locale

data class HaAddress(val base: String, val websocket: String)

sealed interface AddressCheck {
    data class Ok(val address: HaAddress) : AddressCheck
    data object Invalid : AddressCheck
    data object InsecurePublic : AddressCheck
}

fun isPrivateHost(host: String): Boolean {
    val h = host.lowercase(Locale.ROOT)
    if (listOf(".local", ".lan", ".home.arpa").any { h.endsWith(it) }) return true
    val parts = h.split('.').map { it.toIntOrNull() ?: return false }
    if (parts.size != 4 || parts.any { it !in 0..255 }) return false
    return parts[0] == 10 || parts[0] == 127 || (parts[0] == 172 && parts[1] in 16..31) ||
        (parts[0] == 192 && parts[1] == 168)
}

fun checkHaAddress(input: String): AddressCheck = try {
    val explicit = input.trim().contains("://")
    val uri = URI(if (explicit) input.trim() else "http://${input.trim()}")
    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    val host = uri.host?.lowercase(Locale.ROOT)
    when {
        scheme !in listOf("http", "https") || host.isNullOrBlank() || uri.userInfo != null ||
            uri.query != null || uri.fragment != null || uri.port > 65535 -> AddressCheck.Invalid
        scheme == "http" && !isPrivateHost(host) -> AddressCheck.InsecurePublic
        else -> {
            val port = if (!explicit && uri.port == -1) 8123 else uri.port
            val base = URI(scheme, null, host, port, uri.path.trimEnd('/'), null, null).toASCIIString()
            AddressCheck.Ok(HaAddress(base, base.replaceFirst("http", "ws") + "/api/websocket"))
        }
    }
} catch (_: Exception) { AddressCheck.Invalid }
