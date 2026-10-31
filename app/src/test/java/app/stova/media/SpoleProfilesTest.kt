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
            arrayOf("bad", "Bad", "plex", "x", null, 0),
            arrayOf("bad2", "Bad", "emby", "../x", null, 0),
            arrayOf("emby-eilev", "Eilev", "emby", "eilev", null, 0)))
        assertEquals(listOf(
            FavoriteTile.SpoleProfile("spole:jellyfin:oyvind", "Øyvind", "http://jf.local/Users/oyvind/Images/Primary", ServerKind.JELLYFIN, "oyvind"),
            FavoriteTile.SpoleProfile("spole:emby:eilev", "Eilev", "http://emby.local/Users/eilev/Images/Primary?tag=a", ServerKind.EMBY, "eilev")), tiles)
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
