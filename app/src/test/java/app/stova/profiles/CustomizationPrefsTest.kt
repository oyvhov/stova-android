package app.stova.profiles

import app.stova.core.storage.StovaJson
import app.stova.home.HomeRowKey
import org.junit.Assert.*
import org.junit.Test

class CustomizationPrefsTest {
    @Test fun legacyCornersAndStrongFocusMigrate() {
        val sharp = StovaJson.decodeFromString<AppearancePrefs>("""{"corners":"SHARP","strongFocus":true}""")
        assertEquals(CornerRadius.SHARP, sharp.cornerRadius)
        assertEquals(RingWidth.THICK, sharp.ringWidth)
        val round = StovaJson.decodeFromString<AppearancePrefs>("""{"corners":"ROUND","strongFocus":false}""")
        assertEquals(CornerRadius.ROUND, round.cornerRadius)
        assertEquals(RingWidth.NORMAL, round.ringWidth)
    }

    @Test fun explicitNewChoicesWinOverLegacyFields() {
        val p = StovaJson.decodeFromString<AppearancePrefs>("""{"corners":"SHARP","strongFocus":true,"cornerRadius":"EXTRA_ROUND","ringWidth":"THIN"}""")
        assertEquals(CornerRadius.EXTRA_ROUND, p.cornerRadius)
        assertEquals(RingWidth.THIN, p.ringWidth)
        assertEquals(p, StovaJson.decodeFromString<AppearancePrefs>(StovaJson.encodeToString(p)))
    }

    @Test fun rowOverridesRoundTripWithoutChangingOtherRows() {
        val key = HomeRowKey.latestMovies("one")
        val p = HomePrefs(rowFormats = mapOf(key to RowStyle(RowFormat.ICON, ArtworkSize.LARGE, false)))
        assertEquals(p, StovaJson.decodeFromString<HomePrefs>(StovaJson.encodeToString(p)))
        assertEquals(RowStyle(), p.rowFormats[HomeRowKey.latestMovies("two")] ?: RowStyle())
        assertEquals(emptyMap<HomeRowKey, RowStyle>(), StovaJson.decodeFromString<HomePrefs>("{}").rowFormats)
    }
}
