package app.stova.sonos

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.*
import app.stova.core.theme.StovaTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SonosScreensTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Before fun enabled() = assumeScreensEnabled()
    private fun render(scale: Float = 1f, group: Boolean = false, empty: Boolean = false) {
        val rooms = if (empty) emptyList() else sonosPreviewRooms()
        compose.setContent { StovaTheme(reduceMotion = true) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale)) {
                SonosContent(SonosState(rooms, loading = false), rooms.firstOrNull()?.id, {}, {}, {}, {}, {},
                    artwork = { _, modifier -> SonosPreviewArtwork(modifier) }, onBrowseMusicAssistant = {})
                if (group) SonosGroupDialog(rooms.first(), rooms, false, false, {}, {})
            }
        } }
        if (group) compose.saveScreen("sonos-group", org.robolectric.shadows.ShadowDialog.getLatestDialog().window!!.decorView)
        else compose.saveScreen(if (empty) "sonos-empty" else if (scale > 1f) "sonos-large" else "sonos-playing")
    }
    @Test fun nowPlaying() = render()
    @Test fun largeFont() = render(2f)
    @Test fun groupRooms() = render(group = true)
    @Test fun emptyRooms() = render(empty = true)
    @Test fun homeWidget() {
        val room = sonosPreviewRooms().first()
        compose.setContent { StovaTheme(reduceMotion = true) {
            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
                app.stova.navigation.TopBar(app.stova.profiles.Profile("p", "Øyvind", app.stova.core.theme.AvatarColor.LIME), {}, {},
                    onSonos = {}, nowPlaying = { modifier ->
                        SonosNowPlaying(room, room, false, false, {}, {}, modifier, artwork = { _, art -> SonosPreviewArtwork(art) })
                    }, leading = {
                        app.stova.ha.ui.HomeStatusChips(listOf(app.stova.ha.ui.ChipContent(app.stova.ha.cards.CardType.LIGHT, "3 lys"),
                            app.stova.ha.ui.ChipContent(app.stova.ha.cards.CardType.CLIMATE, "21°")))
                    })
            }
        } }
        compose.saveScreen("sonos-home-widget")
    }
}
