package app.stova.settings

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.home.HomeRowKey
import app.stova.home.moveItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class HomeRowOrderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun rowHeadingCanBeEditedWithoutLosingItsLayoutChoices() {
        var style by mutableStateOf(app.stova.profiles.RowStyle(format = app.stova.profiles.RowFormat.ICON))
        compose.setContent { StovaTheme {
            HomeRowOrderSettings(listOf(HomeRowKey.APPS), emptySet(), { _, _ -> }, { _, _ -> },
                rowFormats = mapOf(HomeRowKey.APPS to style), onStyle = { _, changed -> style = changed })
        } }
        compose.onNodeWithText("Endre overskrift").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Mine appar")
        compose.onNodeWithText("Lagre").performClick()
        org.junit.Assert.assertTrue(kotlinx.serialization.json.Json.encodeToString(app.stova.profiles.RowStyle.serializer(), style).contains("Mine appar"))
        org.junit.Assert.assertEquals(app.stova.profiles.RowFormat.ICON, style.format)
        compose.onAllNodesWithText("Mine appar").assertCountEquals(2)
        compose.onNodeWithText("Endre overskrift").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Ikkje lagra")
        compose.onNodeWithText("Avbryt").performClick()
        org.junit.Assert.assertEquals("Mine appar", style.title)
        compose.onNodeWithText("Endre overskrift").performClick()
        compose.onNodeWithText("Bruk standardoverskrift").performClick()
        org.junit.Assert.assertNull(style.title)
        org.junit.Assert.assertEquals(app.stova.profiles.RowFormat.ICON, style.format)
    }

    // The Oppdag row can be shown or hidden as soon as Seerr is there for a grown-up's person, before Home has the row; never for a child.
    @Test fun discoverRowIsListedOnlyWithAccess() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<app.stova.StovaApplication>()
        val container = context.container
        val link = app.stova.profiles.SpoleLink(app.stova.media.model.ServerKind.EMBY, "eilev")
        val (adult, kid) = kotlinx.coroutines.runBlocking {
            container.profiles.create("A", app.stova.core.theme.AvatarColor.LIME, false) to container.profiles.create("K", app.stova.core.theme.AvatarColor.ROSE, true)
        }
        val spole = app.stova.discover.SeerrSpole.install(context, "none")
        kotlinx.coroutines.runBlocking { container.discover.refresh(adult.id, link) }
        var profile by mutableStateOf(adult)
        compose.setContent { StovaTheme { HomeRowSettings(container, profile) } }
        val discover = hasText("Oppdag") and isToggleable()
        compose.onNode(hasText("Hald fram i andre appar") and isToggleable()).assertExists()
        compose.onAllNodes(discover).assertCountEquals(0)
        spole.seerr = "browse"
        kotlinx.coroutines.runBlocking { container.discover.refresh(adult.id, link, force = true) }
        compose.waitUntil(5_000) { compose.onAllNodes(discover).fetchSemanticsNodes().isNotEmpty() }
        // A child's rows never list it, even with Spole saying the person may ask for titles.
        spole.seerr = "personal"
        kotlinx.coroutines.runBlocking { container.discover.refresh(kid.id, link) }
        org.junit.Assert.assertEquals(app.stova.discover.SeerrAccess.PERSONAL, container.discover.state(kid.id).value?.access)
        profile = kid
        compose.waitForIdle()
        compose.onNode(hasText("Favorittappar") and isToggleable()).assertExists()
        compose.onAllNodes(discover).assertCountEquals(0)
    }

    @Test fun focusFollowsRowMovedWithDpad() {
        compose.setContent {
            var rows by remember { mutableStateOf(listOf(HomeRowKey.APPS, HomeRowKey.WATCH_NEXT)) }
            StovaTheme {
                HomeRowOrderSettings(rows, emptySet(), { _, _ -> }, { key, delta -> rows = moveItem(rows, key, delta) })
            }
        }
        compose.onAllNodesWithText("Flytt ned")[0].performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onAllNodesWithText("Flytt ned")[0].performKeyInput {
            keyDown(Key.DirectionCenter); keyUp(Key.DirectionCenter)
        }
        compose.onAllNodesWithText("Flytt ned")[1].assertIsFocused()
    }
}
