package app.stova.apps

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.profiles.*
import app.stova.settings.IconPackSettingsContent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class IconPickerTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun settingsCyclePackColourAndFillWithDpadAndOk() {
        var prefs by mutableStateOf(AppearancePrefs())
        val packs = FakeIconPackSource().packs
        compose.setContent { StovaTheme { Column { IconPackSettingsContent(prefs, packs) { prefs = it } } } }
        for (label in listOf("Ikonpakke", "Farge", "Fyll")) {
            val row = compose.onNodeWithText(label)
            row.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            row.assertIsFocused()
            val before = prefs
            row.performKeyInput { pressKey(Key.DirectionRight) }
            compose.waitForIdle()
            assertNotEquals(before, prefs)
            row.assertIsFocused()
            val stepped = prefs
            // Left leads back to the section list and keeps the value; OK steps like Right.
            row.performKeyInput { pressKey(Key.DirectionLeft) }
            compose.waitForIdle()
            assertEquals(label, stepped, prefs)
            compose.runOnIdle { prefs = before }
            row.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            row.performKeyInput { pressKey(Key.Enter) }
            assertEquals(label, stepped, prefs)
        }
        assertEquals(packs.single().packageName, prefs.iconPack)
        assertEquals(IconColor.ACCENT, prefs.iconColor)
        assertEquals(IconFill.CROP, prefs.iconFill)
    }

    @Test fun gridSearchSelectsAnIconFromTheChosenPack() {
        val source = FakeIconPackSource().apply { catalog = """<item drawable="kodi"/><item drawable="vlc"/>""" }
        val art = AppArtCache(context, iconPacks = IconPackRepository(source, tmp.root))
        runBlocking { art.iconPacks.refresh() }
        var selected: IconOverride? = null
        compose.setContent { StovaTheme { IconPicker(art, null, { selected = it }, {}) } }
        compose.onNodeWithText("Søk etter ikon").performTextInput("vlc")
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("vlc") and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("kodi").assertDoesNotExist()
        val icon = compose.onNode(hasText("vlc") and !hasSetTextAction())
        icon.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        icon.performKeyInput { pressKey(Key.Enter) }
        assertEquals(IconOverride(source.packs.single().packageName, "vlc"), selected)
    }

    @Test fun noPacksExplainsHowToGetArcticons() {
        val source = FakeIconPackSource().apply { packs = emptyList() }
        val art = AppArtCache(context, iconPacks = IconPackRepository(source, tmp.root))
        runBlocking { art.iconPacks.refresh() }
        compose.setContent { StovaTheme { IconPicker(art, null, {}, {}) } }
        compose.onNodeWithText("Ingen ikonpakkar er installerte.").assertExists()
        compose.onNodeWithText("Arcticons", substring = true).assertExists()
    }
}
