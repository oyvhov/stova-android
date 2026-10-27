package app.stova.ha

import app.stova.apps.FavoriteTile
import app.stova.apps.ShortcutKey
import app.stova.ha.cards.canControl
import app.stova.profiles.Profile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** The Home Assistant entities a favourite tile can start with one press. */
val shortcutDomains = setOf("scene", "script")

/**
 * Starts a favourite scene or script from Home. The connection is held only while the call runs,
 * so Home keeps no socket open for its tiles. False when it is not allowed or did not get through.
 */
suspend fun runHaShortcut(connection: HaConnection, entityId: String, profile: Profile): Boolean {
    val domain = entityId.substringBefore('.')
    if (domain !in shortcutDomains || !canControl(profile)) return false
    connection.hold()
    try {
        withTimeoutOrNull(6_000) { connection.status.first { it == HaStatus.CONNECTED } } ?: return false
        return connection.callService(domain, "turn_on", entityId)
    } finally {
        connection.release()
    }
}

/**
 * The scenes and scripts the house offers, named as in Home Assistant, for the Apps page. Hidden and
 * disabled ones are left out. The connection is held only for the lookup.
 */
suspend fun haShortcutScenes(connection: HaConnection, registry: suspend () -> HaRegistry?): List<FavoriteTile.Scene> {
    connection.hold()
    try {
        withTimeoutOrNull(6_000) { connection.status.first { it == HaStatus.CONNECTED } } ?: return emptyList()
        val states = connection.states().filter { it.id.substringBefore('.') in shortcutDomains }
        val known = registry() ?: HaRegistry()
        return entityCatalog(states, known)
            .filter { it.entity.id.substringBefore('.') in shortcutDomains && !it.hidden && known.entities[it.entity.id]?.disabled != true }
            .map { FavoriteTile.Scene(ShortcutKey.Ha(it.entity.id).key, it.name, it.entity.id) }
            .sortedBy { it.label.lowercase() }
    } finally {
        connection.release()
    }
}
