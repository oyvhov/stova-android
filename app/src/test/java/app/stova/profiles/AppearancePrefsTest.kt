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
        assertEquals(HomeRows.ONE, old.homeRows)
        assertEquals(HomeRowInset.LOW, old.homeRowInset)
    }

    @Test fun allChoicesSurviveJson() {
        val prefs = AppearancePrefs(cardSize = ArtworkSize.LARGE, showTitles = true, appStyle = AppStyle.ICON, appTileBackground = false,
            movieStyle = MovieStyle.WIDE, heroSize = HeroSize.LARGE, showOverview = true, showGenres = false,
            spotlightTime = SpotlightTime.SLOW, backdropDimming = BackdropDimming.LIGHT, corners = Corners.SHARP,
            focusEffect = FocusEffect.ZOOM, accent = Accent.OCEAN, clock = ClockMode.H12, rowSpacing = RowSpacing.AIRY,
            homeRows = HomeRows.TWO, homeRowInset = HomeRowInset.HIGH)
        assertEquals(prefs, Json.decodeFromString<AppearancePrefs>(Json.encodeToString(prefs)))
    }

    @Test fun everyRowPresentationSurvivesJsonWithoutChangingOtherPreferences() {
        HomeRows.entries.forEach { mode -> HomeRowInset.entries.forEach { inset ->
            val prefs = AppearancePrefs(homeRows = mode, homeRowInset = inset, cardSize = ArtworkSize.LARGE)
            assertEquals(prefs, Json.decodeFromString<AppearancePrefs>(Json.encodeToString(prefs)))
        } }
    }
}
