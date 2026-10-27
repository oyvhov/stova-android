package app.stova.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.media.SpotlightEntry
import app.stova.media.model.*
import app.stova.profiles.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HeroSizeTest {
    @get:Rule val compose = createComposeRule()
    private val account = MediaAccount("a", "p", ServerKind.JELLYFIN, "s", "Demo", "https://demo.invalid", "u", "U")

    // Before: Compact and Large only changed an empty box around the same title and logo.
    @Test fun aLargeHeroDrawsItsTitleClearlyBiggerThanACompactOne() {
        var size by mutableStateOf(HeroSize.COMPACT)
        val entry = SpotlightEntry(MediaItem("f", "a", ItemType.MOVIE, "Fjordlys", year = 2024), account, false)
        compose.setContent { StovaTheme(appearance = AppearancePrefs(heroSize = size), reduceMotion = true) {
            HeroPanel(entry, Modifier.fillMaxWidth().height(300.dp))
        } }
        val compact = compose.onNodeWithText("Fjordlys").getBoundsInRoot().height
        compose.runOnIdle { size = HeroSize.LARGE }
        val large = compose.onNodeWithText("Fjordlys").getBoundsInRoot().height
        assertTrue("compact $compact, large $large", large >= compact * 1.25f)
    }

    @Test fun aLargeLogoMayGrowFarBeyondACompactOne() {
        assertTrue(HeroSize.LARGE.logoHeight >= HeroSize.COMPACT.logoHeight * 1.5f)
        assertTrue(HeroSize.LARGE.logoWidth >= HeroSize.COMPACT.logoWidth * 1.4f)
        assertTrue(HeroSize.LARGE.textWidth > HeroSize.COMPACT.textWidth)
    }
}
