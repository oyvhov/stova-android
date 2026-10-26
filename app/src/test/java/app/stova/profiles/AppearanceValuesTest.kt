package app.stova.profiles

import app.stova.core.storage.StovaJson
import app.stova.core.theme.backdropScrimAlpha
import org.junit.Assert.*
import org.junit.Test

class AppearanceValuesTest {
    @Test fun olderProfilesKeepEveryPresetAndExplicitValuesWin() {
        for (enabled in listOf(false, true)) {
            val old = StovaJson.decodeFromString<AppearancePrefs>("""{"appTileBackground":$enabled,"cardSpacing":"AIRY","cornerRadius":"SHARP","spotlightTime":"FAST","backdropDimming":"LIGHT"}""")
            assertEquals(if (enabled) 100 else 0, old.appBackgroundPercent)
            assertEquals(24, old.spacingDp); assertEquals(4, old.cornerDp)
            assertEquals(8000L, old.spotlightMillis); assertEquals(25, old.dimmingPercent)
        }
        val explicit = AppearancePrefs(appTileBackground = false, appTileBackgroundOpacity = 43,
            backdropDimmingPercent = 67, cornerRadiusDp = 17, cardSpacingDp = 11, spotlightSeconds = 23)
        assertEquals(explicit, StovaJson.decodeFromString<AppearancePrefs>(StovaJson.encodeToString(explicit)))
        assertEquals(43, explicit.appBackgroundPercent); assertEquals(67, explicit.dimmingPercent)
        assertEquals(17, explicit.cornerDp); assertEquals(11, explicit.spacingDp); assertEquals(23000L, explicit.spotlightMillis)
    }
    @Test fun invalidStoredValuesAreClampedAtTheRenderingBoundary() {
        val p = AppearancePrefs(appTileBackgroundOpacity = -9, backdropDimmingPercent = 999,
            cornerRadiusDp = -1, cardSpacingDp = 900, spotlightSeconds = 0)
        assertEquals(0, p.appBackgroundPercent); assertEquals(100, p.dimmingPercent)
        assertEquals(0, p.cornerDp); assertEquals(40, p.spacingDp); assertEquals(5000L, p.spotlightMillis)
    }
    @Test fun dimmingPreservesPresetAnchorsAndInterpolatesWithoutJumps() {
        assertEquals(.05f, backdropScrimAlpha(25, .62f), .00001f)
        assertEquals(.18f, backdropScrimAlpha(50, .62f), .00001f)
        assertEquals(.74f, backdropScrimAlpha(75, .62f), .00001f)
        for (x in listOf(0f, .15f, .45f, .62f, .76f, 1f)) {
            assertEquals(0f, backdropScrimAlpha(0, x), .00001f)
            assertEquals(1f, backdropScrimAlpha(100, x), .00001f)
            val steps = (0..100).map { backdropScrimAlpha(it, x) }
            assertTrue(steps.zipWithNext().all { (a, b) -> b >= a && b - a <= .041f })
        }
    }
    @Test fun theFadeAcrossTheScreenHasNoHardKnee() {
        for (percent in listOf(25, 50, 75)) {
            val xs = (0..100).map { it / 100f }
            val slopes = xs.zipWithNext().map { (a, b) -> backdropScrimAlpha(percent, b) - backdropScrimAlpha(percent, a) }
            val sharpest = slopes.zipWithNext().maxOf { (a, b) -> kotlin.math.abs(b - a) }
            assertTrue("scrim at $percent % bends too sharply: $sharpest", sharpest <= .012f)
            assertTrue(slopes.all { it <= .0001f }) // never brightens back towards the text side
        }
    }
}
