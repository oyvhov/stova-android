package app.stova.discover

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Test

/** The Oppdag page's step stack survives the activity being made again, with what each title's card showed. */
class DiscoverStepTest {
    @Test fun stepsSurviveBeingSaved() {
        val heat = DiscoverTitle(DiscoverType.MOVIE, 949, "Heat", 1995, "Om ranarar.", "http://p/949", null, SeerrStatus.AVAILABLE, 8.3,
            listOf("Krim", "Drama"))
        val steps = listOf(DiscoverStep.Rows, DiscoverStep.Title(DiscoverType.MOVIE, 949, heat),
            DiscoverStep.Title(DiscoverType.SERIES, 1399, null), DiscoverStep.Search)
        val saved = with(DiscoverStepsSaver) { SaverScope { true }.save(steps) }!!
        assertEquals(steps, DiscoverStepsSaver.restore(saved))
    }
}
