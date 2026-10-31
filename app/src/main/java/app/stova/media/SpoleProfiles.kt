package app.stova.media

import android.content.Context
import android.database.Cursor
import android.net.Uri
import app.stova.apps.FavoriteTile
import app.stova.apps.ShortcutKey
import app.stova.media.model.ServerKind

/**
 * Spole's profiles as tiles: one per person, with their picture, opening Spole as them. Rows that
 * name no Jellyfin or Emby account, or carry an id `spole://` would refuse, are left out.
 */
fun parseSpoleProfiles(cursor: Cursor): List<FavoriteTile.SpoleProfile> {
    fun text(column: String) = cursor.getColumnIndex(column).takeIf { it >= 0 }?.let { if (cursor.isNull(it)) null else cursor.getString(it) }
    val tiles = mutableListOf<FavoriteTile.SpoleProfile>()
    while (cursor.moveToNext()) {
        val kind = when (text("kind")) { "jellyfin" -> ServerKind.JELLYFIN; "emby" -> ServerKind.EMBY; else -> null } ?: continue
        val user = text("user")?.takeIf { SpoleContract.ID_PATTERN.matches(it) } ?: continue
        val name = text("name")?.takeIf { it.isNotBlank() } ?: continue
        tiles += FavoriteTile.SpoleProfile(ShortcutKey.SpoleProfile(kind, user).key, name, text("avatar")?.takeIf { it.isNotBlank() }, kind, user)
    }
    return tiles.distinctBy { it.key }
}

/**
 * Asks the installed Spole which profiles it has (`content://<package>.launcher/profiles`). Nothing
 * is asked when Spole is not installed, and a Spole too old to answer gives no tiles.
 */
class SpoleProfiles(private val context: Context) {
    fun list(): List<FavoriteTile.SpoleProfile> = SpoleHandoff(context).installedPackages().flatMap { pkg ->
        runCatching {
            context.contentResolver.query(Uri.parse("content://$pkg.launcher/profiles"), null, null, null, null)?.use(::parseSpoleProfiles)
        }.getOrNull().orEmpty()
    }.distinctBy { it.key }
}
