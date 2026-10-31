package app.stova.apps

import kotlinx.serialization.Serializable

/**
 * What Home shows for a favourite that is not an app: a library's image, a scene's Home Assistant
 * icon. Saved when it is added, so Home needs no network for it. [chosenIcon] is one the adult
 * picked in Stova; it wins over [icon], which follows Home Assistant.
 */
@Serializable
data class Shortcut(val label: String, val image: String? = null, val icon: String? = null, val chosenIcon: String? = null)

/**
 * A favourite that is not an app: a media library, or a Home Assistant scene or script. Its key
 * has a ':' and an app key never does (package names cannot), so tidying apps leaves it alone.
 */
sealed interface ShortcutKey {
    val key: String

    data class Library(val accountId: String, val libraryId: String) : ShortcutKey {
        override val key get() = "library:$accountId:$libraryId"
    }

    data class Ha(val entityId: String) : ShortcutKey {
        override val key get() = "ha:$entityId"
    }

    /** A person in Spole: the account `spole://profile` opens Spole as. */
    data class SpoleProfile(val kind: app.stova.media.model.ServerKind, val userId: String) : ShortcutKey {
        override val key get() = "spole:${if (kind == app.stova.media.model.ServerKind.JELLYFIN) "jellyfin" else "emby"}:$userId"
    }

    companion object {
        fun parse(key: String): ShortcutKey? = when {
            key.startsWith("library:") -> key.removePrefix("library:").split(':')
                .takeIf { it.size == 2 && it.all(String::isNotBlank) }?.let { Library(it[0], it[1]) }
            key.startsWith("ha:") -> key.removePrefix("ha:").takeIf { '.' in it }?.let(::Ha)
            key.startsWith("spole:") -> key.removePrefix("spole:").split(':').takeIf { it.size == 2 && it[1].isNotBlank() }?.let { (kind, user) ->
                when (kind) {
                    "jellyfin" -> SpoleProfile(app.stova.media.model.ServerKind.JELLYFIN, user)
                    "emby" -> SpoleProfile(app.stova.media.model.ServerKind.EMBY, user)
                    else -> null
                }
            }
            else -> null
        }
    }
}

fun isShortcutKey(key: String) = ':' in key
