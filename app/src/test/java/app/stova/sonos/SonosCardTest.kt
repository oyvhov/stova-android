package app.stova.sonos

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.*
import app.stova.core.storage.StovaJson
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaTheme
import app.stova.ha.HaEntity
import app.stova.profiles.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SonosCardTest {
    @get:Rule val compose = createComposeRule()
    private val adult = Profile("a", "A", AvatarColor.LIME)
    private fun room(id: String, state: String, attrs: String = "{}", name: String = id) =
        SonosRoom(HaEntity("media_player.$id", state, StovaJson.parseToJsonElement(attrs).jsonObject, 0, 0), name, model = "Era 100")
    private val music = """{"media_title":"Inspector Norse","media_artist":"Todd Terje","entity_picture":"/api/art/a"}"""

    // B1: the favourite goes through the same saved favourites as every other shortcut.
    @Test fun sonosFavouriteShowsOnlyForAnAdultWithTheModuleOnAndItSaved() {
        val key = ShortcutKey.Sonos.key
        assertEquals("stova:sonos", key)
        assertEquals(ShortcutKey.Sonos, ShortcutKey.parse(key))
        assertTrue(isShortcutKey(key))
        val prefs = AppPrefs(favorites = listOf(key), shortcuts = mapOf(key to Shortcut("Sonos")))
        assertEquals(listOf(key), favoriteTiles(prefs, emptyList(), emptyList(), haReady = true, kid = false, sonosReady = true).map { it.key })
        assertTrue(favoriteTiles(prefs, emptyList(), emptyList(), haReady = true, kid = false, sonosReady = true).single() is FavoriteTile.Sonos)
        assertTrue(favoriteTiles(prefs, emptyList(), emptyList(), haReady = true, kid = false, sonosReady = false).isEmpty())
        assertTrue(favoriteTiles(prefs, emptyList(), emptyList(), haReady = true, kid = true, sonosReady = true).isEmpty())
        // A key in the order without its saved shortcut is not shown.
        assertTrue(favoriteTiles(prefs.copy(shortcuts = emptyMap()), emptyList(), emptyList(), true, false, sonosReady = true).isEmpty())
    }

    // B2: one Home session serves the widget, the backdrop and the card.
    @Test fun homeSessionRunsForTheCardEvenWithWidgetAndArtworkOff() {
        val off = SonosPrefs(enabled = true, showNowPlaying = false, homeArtwork = false)
        assertFalse(sonosHomeWanted(off, adult, "http://ha.local:8123", cardFavorite = false))
        assertTrue(sonosHomeWanted(off, adult, "http://ha.local:8123", cardFavorite = true))
        assertFalse(sonosHomeWanted(off, adult.copy(kid = true), "http://ha.local:8123", cardFavorite = true))
        assertFalse(sonosHomeWanted(off, adult, null, cardFavorite = true))
        assertFalse(sonosHomeWanted(off.copy(enabled = false), adult, "http://ha.local:8123", cardFavorite = true))
        assertTrue(sonosHomeWanted(SonosPrefs(enabled = true), adult, "http://ha.local:8123", cardFavorite = false))
    }

    // B3: status and art, never stale album art for the TV or a lost speaker.
    @Test fun cardStatusAndArtNeverShowOldMusicForTvOrOfflineRooms() {
        val playing = room("a", "playing", music)
        fun card(r: SonosRoom, prefs: SonosPrefs = SonosPrefs(enabled = true)) = sonosCard(prefs, SonosState(listOf(r), loading = false))
        assertEquals(SonosCardStatus.PLAYING, card(playing).status)
        assertEquals("/api/art/a", card(playing).artwork)
        assertEquals("Inspector Norse", card(playing).title)
        assertEquals(SonosCardStatus.PAUSED, card(playing.copy(entity = playing.entity.copy(state = "paused"))).status)
        assertEquals(SonosCardStatus.BUFFERING, card(playing.copy(entity = playing.entity.copy(state = "buffering"))).status)
        val tv = room("a", "playing", """{"source":"TV","media_title":"Old","entity_picture":"/api/art/old"}""")
        assertEquals(SonosCardStatus.TV, card(tv).status)
        assertNull(card(tv).artwork)
        assertNull(card(tv).title)
        val offline = playing.copy(entity = playing.entity.copy(state = "unavailable"))
        assertEquals(SonosCardStatus.OFFLINE, card(offline).status)
        assertNull(card(offline).artwork)
        assertEquals(SonosCardStatus.IDLE, card(room("a", "idle")).status)
        assertEquals(SonosCardStatus.NONE, sonosCard(SonosPrefs(enabled = true), SonosState(loading = false)).status)
    }

    @Test fun cardPrefersTheChosenRoomThenMusicAndKeepsTheNativeNameUnderMusicAssistant() {
        val kitchen = room("kitchen", "idle", name = "Kjøkken")
        val living = room("living", "idle", name = "Stova")
        val ma = room("ma_living", "playing", """{"active_queue":"q","media_title":"Inspector Norse","entity_picture":"/api/ma/a"}""", name = "Stova (MA)")
        val state = SonosState(listOf(kitchen, living), loading = false, musicAssistant = mapOf(living.id to ma))
        val card = sonosCard(SonosPrefs(enabled = true), state)
        assertEquals("Stova", card.room?.name)
        assertEquals(SonosCardStatus.PLAYING, card.status)
        assertEquals("/api/ma/a", card.artwork)
        assertEquals("Kjøkken", sonosCard(SonosPrefs(enabled = true, preferredRoom = kitchen.id), state).room?.name)
    }

    // B4: through the real favourite tile: the whole card opens Sonos, a long press opens the menu,
    // nothing plays; a failed picture falls back to the speaker.
    @Test fun wholeCardOpensSonosWithoutPlaybackAndAFailedPictureFallsBack() {
        var opened = 0
        var menu = 0
        var asked = 0
        val card = sonosCard(SonosPrefs(enabled = true), SonosState(listOf(room("a", "playing", music, name = "Stova")), loading = false))
        compose.setContent { StovaTheme(reduceMotion = true) {
            androidx.compose.runtime.CompositionLocalProvider(LocalSonosCard provides SonosCardSource(card) { path, _, _, onError ->
                asked++; androidx.compose.runtime.LaunchedEffect(path) { onError() } }) {
                ShortcutTile(FavoriteTile.Sonos("Sonos"), { opened++ }, { menu++ })
            }
        } }
        compose.onNodeWithText("Inspector Norse", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Stova · Spelar", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("sonos-card-fallback", useUnmergedTree = true).assertExists()
        assertEquals(1, asked)
        compose.onNodeWithContentDescription("Sonos").performClick()
        assertEquals(1, opened)
        compose.onNodeWithContentDescription("Sonos").performTouchInput { longClick() }
        assertEquals(1, menu)
        assertEquals(1, opened)
    }

    @Test fun withoutALiveSessionTheTileIsAQuietSonosMark() {
        compose.setContent { StovaTheme(reduceMotion = true) { ShortcutTile(FavoriteTile.Sonos("Sonos"), {}, {}) } }
        compose.onNodeWithTag("sonos-card-fallback", useUnmergedTree = true).assertExists()
        assertEquals(1,compose.onAllNodesWithText("Sonos",useUnmergedTree=true).fetchSemanticsNodes().size)
    }

    @Test fun tvCardNeverAsksForAPicture() {
        var asked = false
        val card = sonosCard(SonosPrefs(enabled = true), SonosState(listOf(room("a", "playing",
            """{"source":"TV","media_title":"Old","entity_picture":"/api/art/old"}""", name = "Stova")), loading = false))
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosFavoriteCard(card, androidx.compose.ui.Modifier) { _, _, _, _ -> asked = true }
        } }
        compose.onNodeWithText("Stova · TV-lyd", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Old", useUnmergedTree = true).assertDoesNotExist()
        assertFalse(asked)
    }

    @Test fun shortcutOpenerSendsTheSonosFavouriteToTheSonosPage() {
        var sonos = 0
        lateinit var open: (FavoriteTile) -> Unit
        compose.setContent { StovaTheme(reduceMotion = true) {
            open = rememberShortcutOpener(null, adult, onOpenSonos = { sonos++ }) {}
        } }
        compose.runOnIdle { open(FavoriteTile.Sonos("Sonos")) }
        assertEquals(1, sonos)
    }

    @Test fun shortcutMenuOffersFavouriteAndOpenForSonos() {
        var toggled = false
        var opened = false
        compose.setContent { StovaTheme(reduceMotion = true) {
            ShortcutActionsMenu(FavoriteTile.Sonos("Sonos"), { toggled = true }, { opened = true }, {}, {}, favorite = false)
        } }
        compose.onNodeWithText("Legg til i favorittar").performClick()
        assertTrue(toggled)
        assertFalse(opened)
    }
}

