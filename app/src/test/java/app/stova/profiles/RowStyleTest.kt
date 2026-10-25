package app.stova.profiles

import app.stova.core.storage.StovaJson
import org.junit.Assert.*
import org.junit.Test

class RowStyleTest {
    @Test fun oldRowSettingsKeepTheirLayoutAndLocalisedHeadingDefault() {
        val old=StovaJson.decodeFromString<RowStyle>("""{"format":"WIDE","size":"LARGE","showTitle":false}""")
        assertEquals(RowStyle(RowFormat.WIDE,ArtworkSize.LARGE,false),old)
        assertEquals("Films",old.heading("Films"))
        assertEquals("Filmar",old.heading("Filmar"))
    }

    @Test fun customHeadingSurvivesStorageAndBlankRestoresTheDefaultWithoutChangingLayout() {
        val row=RowStyle(RowFormat.POSTER,ArtworkSize.SMALL,headingSize=HeadingSize.LARGE,showSource=false)
            .withHeading("  Mi   filmhylle\n ")
        val saved=StovaJson.decodeFromString<RowStyle>(StovaJson.encodeToString(row))
        assertEquals("Mi filmhylle",saved.heading("Filmar"))
        assertEquals(row,saved)
        assertEquals("Filmar",saved.withHeading(" \n ").heading("Filmar"))
        assertEquals(row.copy(title=null),saved.withHeading(""))
    }

    @Test fun legacyDefaultRingBecomesCalmFocusButAnExplicitOutlineRemainsAvailable() {
        val old=StovaJson.decodeFromString<AppearancePrefs>("""{"focusEffect":"RING"}""")
        assertEquals(FocusEffect.LIFT,old.focusEffect.resolved())
        assertEquals(FocusEffect.OUTLINE,FocusEffect.OUTLINE.resolved())
    }
}
