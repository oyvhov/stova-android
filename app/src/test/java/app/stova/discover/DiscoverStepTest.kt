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

    // What search found is saved with its step each time a title's page goes over it, and with the activity whenever an app
    // opens: without the story and the genres, which the title's page asks Seerr for anyway, and never more than sixty titles.
    @Test fun aSearchIsSavedLeanAndShort() {
        val found = (1..70).map { DiscoverTitle(DiscoverType.MOVIE, it, "Film $it", 2000 + it, "Om film $it.", "http://p/$it", "http://b/$it",
            SeerrStatus.PENDING, 7.5, listOf("Drama", "Krim")) }
        val lean = found.map { it.copy(overview = null, genres = emptyList()) }
        fun roundTrip(page: DiscoverPage): DiscoverPage? = DiscoverPageSaver.restore(with(DiscoverPageSaver) { SaverScope { true }.save(page) }!!)
        assertEquals(DiscoverPage(lean.take(5), 1, hasMore = false), roundTrip(DiscoverPage(found.take(5), 1, hasMore = false)))
        assertEquals(DiscoverPage(lean.take(60), 3, hasMore = true), roundTrip(DiscoverPage(found.take(60), 3, hasMore = true)))
        // Four of Seerr's pages: the first sixty titles are kept, three pages' worth, and «Hent fleire» goes on from the
        // fourth page, so nothing after them is skipped; a title asked for twice is shown once.
        assertEquals(DiscoverPage(lean.take(60), 3, hasMore = true), roundTrip(DiscoverPage(found, 4, hasMore = false)))
    }
}
