package app.stova.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class StaggeredEntranceTest {
    @get:Rule val compose = createComposeRule()

    private fun show(enabled: Boolean, reduceMotion: Boolean = false) {
        compose.mainClock.autoAdvance = false
        compose.setContent { StovaTheme(reduceMotion = reduceMotion) {
            Column {
                Box(Modifier.size(40.dp).staggeredEntrance(0, enabled).testTag("first"))
                Box(Modifier.size(40.dp).staggeredEntrance(4, enabled).testTag("fifth"))
            }
        } }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun top(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top

    @Test fun rowsArriveInTurnAndSettleWhereTheyBelong() {
        show(enabled = true)
        compose.mainClock.advanceTimeBy(100)
        val firstEarly = top("first")
        val fifthEarly = top("fifth")
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        // The fifth row starts later, so at the same moment it is still further from home.
        assertTrue(fifthEarly - top("fifth") > firstEarly - top("first"))
        assertTrue(fifthEarly > top("fifth"))
    }

    @Test fun withoutTheArrivalOrUnderReducedMotionRowsAreSimplyThere() {
        show(enabled = false)
        assertEquals(40f * compose.density.density, top("fifth"), 0.5f)
    }

    @Test fun reducedMotionSkipsTheArrival() {
        show(enabled = true, reduceMotion = true)
        assertEquals(40f * compose.density.density, top("fifth"), 0.5f)
    }
}
