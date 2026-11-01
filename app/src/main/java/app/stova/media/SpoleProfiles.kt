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
 * The account a row from Spole opens its titles with: Spole's own account for the person, by kind and
 * user. It is never stored and never asked by Stova itself, so it has no address.
 */
/** The account behind rows Spole gave: its titles live in Spole, not in any of Stova's libraries. */
val app.stova.media.model.MediaAccount.viaSpole: Boolean get() = serverId == "spole" && baseUrl.isEmpty()

fun spoleRowAccount(profileId: String, kind: ServerKind, userId: String, server: String) = app.stova.media.model.MediaAccount(
    id = "spole-${if (kind == ServerKind.JELLYFIN) "jellyfin" else "emby"}-$userId", profileId = profileId, kind = kind,
    serverId = "spole", serverName = server, baseUrl = "", userId = userId, userName = "")

/**
 * The person's rows Spole answers (`rows?kind=&user=`), as Home rows: per account «Hald fram», new films
 * and new episodes, in that order, with the same keys an account of Stova's own would give them.
 * Cards with an id `spole://open` would refuse are left out, and so are empty rows.
 */
fun parseSpoleRows(cursor: Cursor, profileId: String): List<app.stova.home.HomeRowUi.Media> {
    fun text(column: String) = cursor.getColumnIndex(column).takeIf { it >= 0 }?.let { if (cursor.isNull(it)) null else cursor.getString(it) }
    fun int(column: String) = cursor.getColumnIndex(column).takeIf { it >= 0 && !cursor.isNull(it) }?.let { cursor.getInt(it) }
    fun double(column: String) = cursor.getColumnIndex(column).takeIf { it >= 0 && !cursor.isNull(it) }?.let { cursor.getDouble(it) }
    val rows = linkedMapOf<Pair<String, MediaRowKind>, MutableList<app.stova.media.model.MediaItem>>()
    val accounts = linkedMapOf<String, app.stova.media.model.MediaAccount>()
    while (cursor.moveToNext()) {
        val kind = when (text("kind")) { "jellyfin" -> ServerKind.JELLYFIN; "emby" -> ServerKind.EMBY; else -> null } ?: continue
        val user = text("user")?.takeIf { SpoleContract.ID_PATTERN.matches(it) } ?: continue
        val id = text("id")?.takeIf { SpoleContract.ID_PATTERN.matches(it) } ?: continue
        val rowKind = when (text("row")) {
            "continue" -> MediaRowKind.CONTINUE
            "latest_movies" -> MediaRowKind.LATEST_MOVIES
            "latest_series" -> MediaRowKind.LATEST_SERIES
            else -> null
        } ?: continue
        val account = spoleRowAccount(profileId, kind, user, text("server").orEmpty())
        accounts.getOrPut(account.id) { account }
        val type = when (text("type")?.lowercase()) {
            "movie" -> app.stova.media.model.ItemType.MOVIE
            "episode" -> app.stova.media.model.ItemType.EPISODE
            "series" -> app.stova.media.model.ItemType.SERIES
            "season" -> app.stova.media.model.ItemType.SEASON
            else -> app.stova.media.model.ItemType.OTHER
        }
        val items = rows.getOrPut(account.id to rowKind) { mutableListOf() }
        if (items.any { it.id == id }) continue
        items += app.stova.media.model.MediaItem(id = id, accountId = account.id, type = type, title = text("title").orEmpty(),
            seriesName = text("series"), season = int("season"), episode = int("episode"), year = int("year"),
            runtimeMinutes = int("runtime"), overview = text("overview"),
            ratings = app.stova.media.model.Ratings(community = double("community"), criticPercent = int("critic"), official = text("official")),
            progress = double("progress")?.toFloat(),
            art = app.stova.media.model.ItemArt(backdrop = text("backdrop"), logo = text("logo"), poster = text("poster"), wide = text("wide")))
    }
    val order = listOf(MediaRowKind.CONTINUE, MediaRowKind.LATEST_MOVIES, MediaRowKind.LATEST_SERIES)
    return accounts.values.flatMap { account ->
        order.mapNotNull { kind ->
            val items = rows[account.id to kind]?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val key = when (kind) {
                MediaRowKind.CONTINUE -> app.stova.home.HomeRowKey.continueWatching(account.id)
                MediaRowKind.LATEST_MOVIES -> app.stova.home.HomeRowKey.latestMovies(account.id)
                else -> app.stova.home.HomeRowKey.latestSeries(account.id)
            }
            app.stova.home.HomeRowUi.Media(key, account, kind, items, offline = false)
        }
    }
}

/** What Spole can do with a title on Stova's behalf. */
enum class SpoleItemAction(val wire: String) { WATCHED("watched"), REMOVE_FROM_CONTINUE("remove_resume") }

/** How an action went: done, failed on the server, Spole has no such account, or Spole is too old or missing. */
enum class SpoleActionResult { DONE, FAILED, UNKNOWN_ACCOUNT, NOT_SUPPORTED }

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

    /**
     * The rows of the Spole person [link] names, fetched by Spole from the server with its own sign-in.
     * Blocks while Spole asks the server: call it off the main thread. Nothing is asked without Spole.
     */
    fun rows(profileId: String, link: app.stova.profiles.SpoleLink): List<app.stova.home.HomeRowUi.Media> =
        SpoleHandoff(context).installedPackages().firstNotNullOfOrNull { pkg ->
            runCatching {
                context.contentResolver.query(Uri.parse("content://$pkg.launcher/rows").buildUpon()
                    .appendQueryParameter("kind", if (link.kind == ServerKind.JELLYFIN) "jellyfin" else "emby")
                    .appendQueryParameter("user", link.userId).build(), null, null, null, null)?.use { parseSpoleRows(it, profileId) }
            }.getOrNull()?.takeIf { it.isNotEmpty() }
        }.orEmpty()

    /** Spole marks a title watched or takes it out of «Hald fram», with its own account for [kind] and [userId]. Blocks; call it off the main thread. */
    fun itemAction(kind: ServerKind, userId: String, itemId: String, action: SpoleItemAction): SpoleActionResult {
        if (!SpoleContract.ID_PATTERN.matches(userId) || !SpoleContract.ID_PATTERN.matches(itemId)) return SpoleActionResult.NOT_SUPPORTED
        val answers = SpoleHandoff(context).installedPackages().map { pkg ->
            runCatching {
                context.contentResolver.call(Uri.parse("content://$pkg.launcher"), "item_action", null, android.os.Bundle().apply {
                    putString("kind", if (kind == ServerKind.JELLYFIN) "jellyfin" else "emby")
                    putString("user", userId); putString("item", itemId); putString("action", action.wire)
                })?.getString("result")
            }.getOrNull()
        }
        return when {
            "ok" in answers -> SpoleActionResult.DONE
            "failed" in answers -> SpoleActionResult.FAILED
            "unknown" in answers -> SpoleActionResult.UNKNOWN_ACCOUNT
            else -> SpoleActionResult.NOT_SUPPORTED
        }
    }

    /** Spole follows the person picked in Stova, when Spole has that person. Asks nothing without Spole. */
    suspend fun follow(profile: app.stova.profiles.Profile, accounts: List<app.stova.media.model.MediaAccount>) {
        if (SpoleHandoff(context).installedPackages().isEmpty()) return
        val person = spoleProfileFor(profile, accounts, list()) ?: return
        select(person.kind, person.userId)
    }
}
