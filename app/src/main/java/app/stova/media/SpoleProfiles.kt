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
        tiles += FavoriteTile.SpoleProfile(ShortcutKey.SpoleProfile(kind, user).key, name, text("avatar")?.takeIf { it.isNotBlank() }, kind, user,
            kid = text("kid") == "1")
    }
    return tiles.distinctBy { it.key }
}

/**
 * The person in Spole who follows [profile]: the one chosen in settings, else the one with an account
 * of the profile's own here (a borrowed account is a grown-up's, and never counts). A child in Stova
 * goes only to a child's profile in Spole.
 */
fun spoleProfileFor(profile: app.stova.profiles.Profile, accounts: List<app.stova.media.model.MediaAccount>,
    spole: List<FavoriteTile.SpoleProfile>): FavoriteTile.SpoleProfile? {
    val chosen = profile.spole?.let { link -> spole.firstOrNull { it.kind == link.kind && it.userId == link.userId } }
    val found = chosen ?: spole.firstOrNull { person ->
        accounts.any { it.profileId == profile.id && it.sharedFrom == null && it.kind == person.kind && it.userId == person.userId }
    }
    return found?.takeIf { !profile.kid || it.kid }
}

/**
 * Each profile's picture. A profile's own account decides first; one without a picture of its own
 * takes the picture of the Spole person chosen for it in settings, so «Kven ser på?» shows the same
 * face as Spole's corner.
 */
fun withSpolePictures(own: Map<String, String>, profiles: List<app.stova.profiles.Profile>,
    spole: List<FavoriteTile.SpoleProfile>): Map<String, String> =
    own + profiles.filter { it.id !in own }.mapNotNull { profile ->
        profile.spole?.let { spoleProfileFor(profile, emptyList(), spole)?.image }?.let { profile.id to it }
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

    /** Switches Spole to the person with this account; Spole refuses to leave a child's profile past its PIN. */
    fun select(kind: ServerKind, userId: String): String? = SpoleHandoff(context).installedPackages().firstNotNullOfOrNull { pkg ->
        runCatching {
            context.contentResolver.call(Uri.parse("content://$pkg.launcher"), "select_profile", null, android.os.Bundle().apply {
                putString("kind", if (kind == ServerKind.JELLYFIN) "jellyfin" else "emby")
                putString("user", userId)
            })?.getString("result")
        }.getOrNull()
    }

    /** Spole follows the person picked in Stova, when Spole has that person. Asks nothing without Spole. */
    suspend fun follow(profile: app.stova.profiles.Profile, accounts: List<app.stova.media.model.MediaAccount>) {
        if (SpoleHandoff(context).installedPackages().isEmpty()) return
        val person = spoleProfileFor(profile, accounts, list()) ?: return
        select(person.kind, person.userId)
    }
}
