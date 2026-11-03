package app.stova.navigation

import androidx.compose.ui.graphics.TransformOrigin
import org.junit.Assert.*
import org.junit.Test

class PageTrailTest {
    @Test fun eachNestedReturnKeepsItsOwnOriginAndFocusKey() {
        val apps = PageOpening("all-apps", TransformOrigin(.1f, .8f))
        val sonos = PageOpening("sonos-tile", TransformOrigin(.6f, .4f))
        val trail = PageTrail().open(Destination.APPS, apps).open(Destination.SONOS, sonos)
        assertEquals(sonos, trail.current.opening)
        val toApps = trail.back()
        assertEquals(Destination.APPS, toApps.current.destination)
        assertEquals("sonos-tile", toApps.returnFocus)
        assertEquals(apps, toApps.current.opening)
        val toHome = toApps.back()
        assertEquals(Destination.HOME, toHome.current.destination)
        assertEquals("all-apps", toHome.returnFocus)
    }

    @Test fun homeClearsNestedHistoryAndDoesNotRestoreAnOldAnchor() {
        val trail = PageTrail().open(Destination.APPS, PageOpening("apps")).open(Destination.DISCOVER, PageOpening("discover"))
        assertEquals(PageTrail(), trail.home())
        assertEquals(PageTrail(), trail.home().back())
    }
}
