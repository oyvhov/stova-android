package app.stova.core.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.profiles.AppearancePrefs
import app.stova.profiles.ClockStyle
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The old clock stays as it was; the new styles always carry the date, in their own place. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class ClockStyleTest {
    @get:Rule val compose = createComposeRule()
    private val monday = 1_793_037_600_000L // 2026-10-26, a Monday

    private fun show(style: ClockStyle, showDate: Boolean = false) {
        compose.setContent { StovaTheme(appearance = AppearancePrefs(clockStyle = style, showDate = showDate)) { Clock(clock = { monday }) } }
    }

    @Test fun standardShowsTheDateOnlyWhenAsked() {
        show(ClockStyle.STANDARD)
        compose.onNodeWithTag("top-clock").assertExists()
        compose.onNodeWithTag("top-date").assertDoesNotExist()
    }

    @Test fun thinClockCarriesTheDateBelowTheTime() {
        show(ClockStyle.THIN)
        val time = compose.onNodeWithTag("top-clock").getUnclippedBoundsInRoot()
        val date = compose.onNodeWithTag("top-date").getUnclippedBoundsInRoot()
        assertTrue("date below the time: $date / $time", date.top >= time.bottom - 4f.let { androidx.compose.ui.unit.Dp(it) })
    }

    @Test fun sideClockCarriesTheDateToTheLeftOfTheTime() {
        show(ClockStyle.SIDE)
        val time = compose.onNodeWithTag("top-clock").getUnclippedBoundsInRoot()
        val date = compose.onNodeWithTag("top-date").getUnclippedBoundsInRoot()
        assertTrue("date left of the time: $date / $time", date.right <= time.left)
    }
}
