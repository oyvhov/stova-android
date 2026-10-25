package app.stova.profiles

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AppearancePrefsTest {
    @Test fun oldProfilesKeepTheNewDefaults() {
        val old = Json.decodeFromString<AppearancePrefs>("""{"reduceMotion":true,"strongFocus":true}""")
        assertEquals(AppearancePrefs(reduceMotion = true, strongFocus = true), old)
        assertEquals(ArtworkSize.MEDIUM, old.cardSize)
        assertFalse(old.showTitles)
        assertEquals(AppStyle.BANNER, old.appStyle)
        assertEquals(MovieStyle.POSTER, old.movieStyle)
        assertEquals(HeroSize.COMPACT, old.heroSize)
        assertFalse(old.showOverview)
        assertTrue(old.showGenres)
        assertEquals(SpotlightTime.NORMAL, old.spotlightTime)
        assertEquals(BackdropDimming.MEDIUM, old.backdropDimming)
        assertEquals(Corners.ROUND, old.corners)
        assertEquals(FocusEffect.LIFT, old.focusEffect)
        assertEquals(Accent.LIME, old.accent)
        assertEquals(ClockMode.H24, old.clock)
        assertEquals(RowSpacing.NORMAL, old.rowSpacing)
    }

    @Test fun allChoicesSurviveJson() {
        val prefs = AppearancePrefs(cardSize = ArtworkSize.LARGE, showTitles = true, appStyle = AppStyle.ICON,
            movieStyle = MovieStyle.WIDE, heroSize = HeroSize.LARGE, showOverview = true, showGenres = false,
            spotlightTime = SpotlightTime.SLOW, backdropDimming = BackdropDimming.LIGHT, corners = Corners.SHARP,
            focusEffect = FocusEffect.ZOOM, accent = Accent.OCEAN, clock = ClockMode.H12, rowSpacing = RowSpacing.AIRY)
        assertEquals(prefs, Json.decodeFromString<AppearancePrefs>(Json.encodeToString(prefs)))
    }
}
