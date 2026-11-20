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
import app.stova.home.HomeViewModel
import app.stova.home.HomeRowSource
import app.stova.home.HomeRowUi
import app.stova.media.MediaRowKind
import app.stova.media.model.MediaAccount
import app.stova.media.model.ServerKind
import app.stova.profiles.HomePrefs
import app.stova.profiles.RowStyle
import app.stova.profiles.StartFocus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class HomeRowOrderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pressingPastTheFirstOrLastRowNeverStartsAManualOrder() {
        var moves = 0
        compose.setContent { StovaTheme {
            HomeRowOrderSettings(listOf(HomeRowKey.APPS, HomeRowKey.WATCH_NEXT), emptySet(), { _, _ -> }, { _, _ -> moves++ })
        } }
        compose.onAllNodesWithText("Flytt opp")[0].performClick()
        compose.onAllNodesWithText("Flytt ned")[1].performClick()
        org.junit.Assert.assertEquals(0, moves)
    }

    @Test fun firstManualMoveUsesTheCurrentHomeOrderAndResetKeepsTheOtherSettings() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<app.stova.StovaApplication>()
        val container = context.container
        val account = MediaAccount("a", "p", ServerKind.JELLYFIN, "Demo", "Demo", "https://demo.invalid", "u", "U")
        val movies = HomeRowUi.Media(HomeRowKey.latestMovies("a"), account, MediaRowKind.LATEST_MOVIES, emptyList(), false)
        val resume = movies.copy(key = HomeRowKey.continueWatching("a"), kind = MediaRowKind.CONTINUE)
        val unavailable = HomeRowKey.latestSeries("offline")
        val prefs = HomePrefs(startFocus = StartFocus.FAVORITE_APPS, hiddenRows = setOf(unavailable),
            rowFormats = mapOf(unavailable to RowStyle(title = "Arkiv")))
        val adult = runBlocking {
            val created = container.profiles.create("Rutine", app.stova.core.theme.AvatarColor.LIME, false)
            container.profiles.update(created.id) { it.copy(home = prefs) }.also { container.session.enter(it.id) }
        }
        val rows = listOf(HomeRowUi.Apps(HomeRowKey.APPS, emptyList()), movies, resume, HomeRowUi.WatchNext(entries = emptyList()))
        lateinit var vm: HomeViewModel
        compose.setContent { StovaTheme {
            vm = remember { HomeViewModel(container.session, listOf(HomeRowSource { flowOf(rows) })).also { it.setHour(8) } }
            val profile by container.session.active.collectAsState()
            HomeRowSettings(container, profile ?: adult, vm)
        } }
        compose.waitUntil(5_000) { vm.rows.value?.size == 4 }
        compose.runOnIdle { vm.setHour(19) }
        compose.waitForIdle()
        compose.onAllNodesWithText("Flytt ned")[0].performClick()
        fun saved() = runBlocking { container.profiles.current().profiles.first { it.id == adult.id }.home }
        compose.waitUntil(5_000) { saved().rowOrder.isNotEmpty() }
        org.junit.Assert.assertEquals(listOf(resume.key, HomeRowKey.APPS, movies.key, HomeRowKey.WATCH_NEXT, unavailable), saved().rowOrder)
        compose.onNodeWithText("Bruk standardrekkjefølgja").performClick()
        compose.waitUntil(5_000) { saved() == prefs }
        compose.onNodeWithText("Bruk standardrekkjefølgja").assertDoesNotExist()
    }

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
