package app.stova.apps

import kotlinx.serialization.Serializable

/** What Home shows for a favourite that is not an app. Saved when it is added, so Home needs no network for it. */
@Serializable
data class Shortcut(val label: String, val image: String? = null)

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

    companion object {
        fun parse(key: String): ShortcutKey? = when {
            key.startsWith("library:") -> key.removePrefix("library:").split(':')
                .takeIf { it.size == 2 && it.all(String::isNotBlank) }?.let { Library(it[0], it[1]) }
            key.startsWith("ha:") -> key.removePrefix("ha:").takeIf { '.' in it }?.let(::Ha)
            else -> null
        }
    }
}

fun isShortcutKey(key: String) = ':' in key
