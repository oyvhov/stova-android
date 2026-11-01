package app.stova.media

import android.database.MatrixCursor
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.AppPrefsFixture
import app.stova.apps.FavoriteTile
import app.stova.apps.Shortcut
import app.stova.apps.ShortcutKey
import app.stova.apps.favoriteTiles
import app.stova.media.model.ServerKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class SpoleProfilesTest {
    private fun rows(vararg row: Array<Any?>) = MatrixCursor(arrayOf("id", "name", "kind", "user", "avatar", "kid")).apply { row.forEach(::addRow) }

    @Test fun spolesProfilesBecomeTilesWithTheirPicture() {
        val tiles = parseSpoleProfiles(rows(
            arrayOf("", "Øyvind", "jellyfin", "oyvind", "http://jf.local/Users/oyvind/Images/Primary", 0),
            arrayOf("emby-eilev", "Eilev", "emby", "eilev", "http://emby.local/Users/eilev/Images/Primary?tag=a", 0),
            arrayOf("kid", "Barn", "jellyfin", "child", null, 1),
            arrayOf("bad", "Bad", "plex", "x", null, 0),
            arrayOf("bad2", "Bad", "emby", "../x", null, 0),
            arrayOf("emby-eilev", "Eilev", "emby", "eilev", null, 0)))
        assertEquals(listOf(
            FavoriteTile.SpoleProfile("spole:jellyfin:oyvind", "Øyvind", "http://jf.local/Users/oyvind/Images/Primary", ServerKind.JELLYFIN, "oyvind"),
            FavoriteTile.SpoleProfile("spole:emby:eilev", "Eilev", "http://emby.local/Users/eilev/Images/Primary?tag=a", ServerKind.EMBY, "eilev"),
            FavoriteTile.SpoleProfile("spole:jellyfin:child", "Barn", null, ServerKind.JELLYFIN, "child", kid = true)), tiles)
    }

    private val adult = app.stova.profiles.Profile("p1", "Eilev", app.stova.core.theme.AvatarColor.LIME)
    private val main = FavoriteTile.SpoleProfile("spole:jellyfin:oyvind", "Øyvind", null, ServerKind.JELLYFIN, "oyvind")
    private val eilev = FavoriteTile.SpoleProfile("spole:emby:eilev", "Eilev", null, ServerKind.EMBY, "eilev")
    private val kidInSpole = FavoriteTile.SpoleProfile("spole:jellyfin:child", "Barn", null, ServerKind.JELLYFIN, "child", kid = true)
    private fun account(user: String, kind: ServerKind = ServerKind.EMBY, profile: String = "p1", sharedFrom: String? = null) =
        app.stova.media.model.MediaAccount("a-$user", profile, kind, "s", "Heim", "http://x", user, user, sharedFrom = sharedFrom)

    @Test fun spoleFollowsTheChosenProfileElseTheOneWithAnAccountOfItsOwnHere() {
        val all = listOf(main, eilev, kidInSpole)
        assertEquals(eilev, spoleProfileFor(adult.copy(spole = app.stova.profiles.SpoleLink(ServerKind.EMBY, "eilev")), emptyList(), all))
        assertEquals(eilev, spoleProfileFor(adult, listOf(account("eilev")), all))
        assertEquals(null, spoleProfileFor(adult, emptyList(), all))
        // A borrowed account is a grown-up's: it never takes Spole to the grown-up's profile.
        assertEquals(null, spoleProfileFor(adult.copy(kid = true), listOf(account("oyvind", ServerKind.JELLYFIN, sharedFrom = "a-src")), all))
        // A child in Stova goes only to a child's profile in Spole.
        assertEquals(null, spoleProfileFor(adult.copy(kid = true, spole = app.stova.profiles.SpoleLink(ServerKind.EMBY, "eilev")), emptyList(), all))
        assertEquals(kidInSpole, spoleProfileFor(adult.copy(kid = true), listOf(account("child", ServerKind.JELLYFIN)), all))
    }

    @Test fun aProfileWithoutAPictureOfItsOwnShowsTheSpolePersonItFollows() {
        val pictured = eilev.copy(image = "http://emby/Users/eilev/Images/Primary")
        val follows = adult.copy(spole = app.stova.profiles.SpoleLink(ServerKind.EMBY, "eilev"))
        assertEquals(mapOf("p1" to pictured.image), withSpolePictures(emptyMap(), listOf(follows), listOf(main, pictured)))
        // The profile's own account picture wins, and a profile that follows nobody keeps its letter.
        assertEquals(mapOf("p1" to "own"), withSpolePictures(mapOf("p1" to "own"), listOf(follows), listOf(pictured)))
        assertEquals(emptyMap<String, String>(), withSpolePictures(emptyMap(), listOf(adult), listOf(pictured)))
        // A child never wears a grown-up's face from Spole.
        assertEquals(emptyMap<String, String>(), withSpolePictures(emptyMap(), listOf(follows.copy(kid = true)), listOf(pictured)))
    }

    @Test fun aSpoleProfileKeyRoundTripsAndNeverLooksLikeAnApp() {
        assertEquals(ShortcutKey.SpoleProfile(ServerKind.EMBY, "eilev"), ShortcutKey.parse("spole:emby:eilev"))
        assertEquals(null, ShortcutKey.parse("spole:plex:eilev"))
        assertEquals(null, ShortcutKey.parse("spole:emby:"))
        assertTrue(':' in ShortcutKey.SpoleProfile(ServerKind.JELLYFIN, "u").key)
    }

    @Test fun aFavouriteSpoleProfileShowsForGrownUpsWithItsSavedPicture() {
        val prefs = AppPrefsFixture.withShortcut("spole:emby:eilev", Shortcut("Eilev", image = "http://emby.local/a"))
        assertEquals(listOf(FavoriteTile.SpoleProfile("spole:emby:eilev", "Eilev", "http://emby.local/a", ServerKind.EMBY, "eilev")),
            favoriteTiles(prefs, emptyList(), emptyList(), haReady = false, kid = false))
        assertEquals(emptyList<FavoriteTile>(), favoriteTiles(prefs, emptyList(), emptyList(), haReady = false, kid = true))
    }
}
