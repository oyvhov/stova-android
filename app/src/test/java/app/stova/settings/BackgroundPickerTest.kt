package app.stova.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.media.SpotlightEntry
import app.stova.media.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class BackgroundPickerTest {
    @get:Rule val compose=createComposeRule()
    @Test fun libraryPickerSelectsAnActualTitle() {
        val account=MediaAccount("a","p",ServerKind.JELLYFIN,"s","Demo","https://demo.invalid","u","Demo")
        val entry=SpotlightEntry(MediaItem("f","a",ItemType.MOVIE,"Fjordlys",art=ItemArt(backdrop="b")),account,false)
        var selected:SpotlightEntry?=null
        compose.setContent { StovaTheme { BackgroundPicker({ listOf(entry) },{selected=it},{}) } }
        compose.mainClock.advanceTimeBy(500)
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Fjordlys").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Fjordlys").performClick()
        assertEquals(entry,selected)
    }
}
