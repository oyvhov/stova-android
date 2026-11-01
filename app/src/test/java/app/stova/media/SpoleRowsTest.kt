package app.stova.media

import android.database.MatrixCursor
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaTheme
import app.stova.home.HomeRowKey
import app.stova.home.HomeRowUi
import app.stova.media.model.ItemType
import app.stova.media.model.MediaAccount
import app.stova.media.model.MediaItem
import app.stova.media.model.ServerKind
import app.stova.media.ui.ItemActionsMenu
import app.stova.media.ui.MediaItemAction
import app.stova.profiles.Profile
import app.stova.profiles.SpoleLink
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Eilev's profile in Stova follows his Spole profile and has no account here: his rows come from
 * Spole, which fetches them with its own sign-in. A long press lets Spole act on a title.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@OptIn(ExperimentalCoroutinesApi::class)
class SpoleRowsTest {
    @get:Rule val compose = createComposeRule()

    private fun cursor(vararg rows: Array<Any?>) = MatrixCursor(arrayOf("row", "kind", "user", "server", "id", "type", "title", "series",
        "season", "episode", "year", "runtime", "progress", "overview", "wide", "poster", "backdrop", "logo", "community", "critic", "official"))
        .apply { rows.forEach(::addRow) }
    private fun card(row: String, id: String, type: String = "Episode", progress: Double? = null) = arrayOf<Any?>(row, "emby", "eilev", "Heime", id,
        type, "Pingu", "Pingu", 1, 2, 1990, 5, progress, "Pingu får besøk.", "http://emby/w/$id", "http://emby/p/$id", null, null, 7.2, null, "A")

    @Test fun spolesAnswerBecomesTheRowsAnAccountWouldGive() {
        val rows = parseSpoleRows(cursor(card("latest_movies", "f1", "Movie"), card("continue", "e1", progress = .4),
            card("continue", "e1"), card("continue", "bad id"), card("unknown", "x1")), "p1")
        assertEquals(listOf(HomeRowKey.continueWatching("spole-emby-eilev"), HomeRowKey.latestMovies("spole-emby-eilev")), rows.map { it.key })
        assertEquals(listOf(MediaRowKind.CONTINUE, MediaRowKind.LATEST_MOVIES), rows.map { it.kind })
        val watching = rows.first()
        // One card per title; an id spole://open would refuse is left out.
        assertEquals(listOf("e1"), watching.items.map { it.id })
        val item = watching.items.single()
        assertEquals(ItemType.EPISODE, item.type)
        assertEquals("Pingu", item.seriesName)
        assertEquals(.4f, item.progress!!, .001f)
        assertEquals(7.2, item.ratings.community!!, .001)
        assertEquals("A", item.ratings.official)
        assertEquals("http://emby/w/e1", item.art.wide)
        // The account opens titles in Spole as Eilev and is never asked by Stova itself.
        assertEquals(MediaAccount("spole-emby-eilev", "p1", ServerKind.EMBY, "spole", "Heime", "", "eilev", ""), watching.account)
        assertEquals(watching.account.id, item.accountId)
    }

    private val eilev = Profile("p1", "Eilev", AvatarColor.LIME, spole = SpoleLink(ServerKind.EMBY, "eilev"))
    private val answer = listOf(HomeRowUi.Media(HomeRowKey.continueWatching("spole-emby-eilev"),
        spoleRowAccount("p1", ServerKind.EMBY, "eilev", "Heime"), MediaRowKind.CONTINUE,
        listOf(MediaItem("e1", "spole-emby-eilev", ItemType.EPISODE, "Pingu")), offline = false))

    @Test fun onlyAProfileThatFollowsSomeoneAndHasNoAccountAsksSpole() = runTest(UnconfinedTestDispatcher()) {
        val asked = mutableListOf<String>()
        val accounts = MutableStateFlow(emptyList<MediaAccount>())
        val active = MutableStateFlow<Profile?>(eilev)
        var clock = 0L
        val source = SpoleRowSource(active, { accounts }, { id, _ -> asked += id; answer }, now = { clock })
        val seen = mutableListOf<List<HomeRowUi>>()
        val job = launch { source.rows(eilev).toList(seen) }
        assertEquals(listOf(emptyList<HomeRowUi>(), answer), seen)
        assertEquals(listOf("p1"), asked)
        // Back on Home at once: the last answer stands. After a minute, or after an action, Spole is asked again.
        source.refresh("p1"); assertEquals(1, asked.size)
        clock = 61_000; source.refresh("p1"); assertEquals(2, asked.size)
        source.refresh("p1", force = true); assertEquals(3, asked.size)
        // An account of its own: Stova's rows instead, and Spole is asked nothing more.
        accounts.value = listOf(MediaAccount("a", "p1", ServerKind.EMBY, "s", "Heime", "http://emby", "eilev", "Eilev"))
        assertEquals(emptyList<HomeRowUi>(), seen.last())
        clock = 200_000; source.refresh("p1", force = true); assertEquals(3, asked.size)
        job.cancel()
    }

    // Eilev's Home on the Shield had a hero with no picture: the hero only knew Stova's own accounts.
    @Test fun theHeroShowsTheTitlesFromSpolesRowsThatHaveABackdrop() = runTest(UnconfinedTestDispatcher()) {
        val account = spoleRowAccount("p1", ServerKind.EMBY, "eilev", "Heime")
        fun item(id: String, backdrop: String?, rated: String? = "12") = MediaItem(id, account.id, ItemType.MOVIE, id,
            ratings = app.stova.media.model.Ratings(official = rated), art = app.stova.media.model.ItemArt(backdrop = backdrop))
        val rows = listOf(HomeRowUi.Media(HomeRowKey.continueWatching(account.id), account, MediaRowKind.CONTINUE,
            listOf(item("a", "http://emby/b/a"), item("plain", null)), offline = false),
            HomeRowUi.Media(HomeRowKey.latestMovies(account.id), account, MediaRowKind.LATEST_MOVIES,
                listOf(item("a", "http://emby/b/a"), item("b", "http://emby/b/b"), item("grown", "http://emby/b/g", "18"),
                    item("unrated", "http://emby/b/u", null)), offline = false))
        val accounts = MutableStateFlow(emptyList<MediaAccount>())
        val source = SpoleRowSource(MutableStateFlow<Profile?>(eilev), { accounts }, { _, _ -> rows })
        val own = MutableStateFlow(listOf(SpotlightEntry(item("own", "http://stova/own"), account, false)))
        val heroes = mutableListOf<List<String>>()
        val job = launch { source.spotlight(eilev, own).collect { heroes += it.map { e -> e.item.id } } }
        val rowsJob = launch { source.rows(eilev).collect {} }
        // Once each, only with a backdrop, in the rows' order, and within the profile's age (15 unless chosen otherwise).
        assertEquals(listOf("a", "b"), heroes.last())
        // Home's own check lets the title through even with Stova libraries chosen for the background.
        val chosen = app.stova.profiles.BackgroundFilter(libraries = setOf(app.stova.profiles.BackgroundLibraryRef("x", "kids")))
        assertEquals("a", backgroundForProfile(source.spotlight(eilev, own).first().first(), "p1", chosen)?.item?.id)
        // With an account of its own the profile has Stova's hero again.
        accounts.value = listOf(MediaAccount("x", "p1", ServerKind.EMBY, "s", "Heime", "http://emby", "eilev", "Eilev"))
        assertEquals(listOf("own"), heroes.last())
        job.cancel(); rowsJob.cancel()
    }

    @Test fun aProfileThatFollowsNobodyNeverAsks() = runTest(UnconfinedTestDispatcher()) {
        var asked = 0
        val plain = eilev.copy(spole = null)
        val source = SpoleRowSource(MutableStateFlow(plain), { MutableStateFlow(emptyList()) }, { _, _ -> asked++; answer })
        val seen = mutableListOf<List<HomeRowUi>>()
        val job = launch { source.rows(plain).toList(seen) }
        assertEquals(listOf(emptyList<HomeRowUi>()), seen)
        assertEquals(0, asked)
        job.cancel()
    }

    @Test fun aPlayLinkCanAskForTheStart() {
        assertEquals("spole://open?kind=emby&user=eilev&item=e1&action=play&start=0",
            SpoleContract.uri(ServerKind.EMBY, "eilev", "e1", SpoleAction.PLAY, fromStart = true).toString())
        assertEquals("spole://open?kind=emby&user=eilev&item=e1&action=details",
            SpoleContract.uri(ServerKind.EMBY, "eilev", "e1", SpoleAction.DETAILS, fromStart = true).toString())
    }

    @Test fun aTitlePartWayThroughOffersAllThreeOfSpolesActions() {
        val chosen = mutableListOf<MediaItemAction>()
        compose.setContent { StovaTheme(reduceMotion = true) {
            ItemActionsMenu(MediaItem("e1", "a", ItemType.EPISODE, "Pingu", runtimeMinutes = 5, progress = .4f), {}, {}, {},
                onAction = { chosen += it })
        } }
        compose.onNodeWithText("Spel frå starten").performClick()
        compose.onNodeWithText("Merk som sett").performClick()
        compose.onNodeWithText("Fjern frå Hald fram").performClick()
        assertEquals(listOf(MediaItemAction.PLAY_FROM_START, MediaItemAction.MARK_WATCHED, MediaItemAction.REMOVE_FROM_CONTINUE), chosen)
    }

    @Test fun anUnstartedFilmOffersOnlyWatchedAndAChildNone() {
        compose.setContent { StovaTheme(reduceMotion = true) {
            ItemActionsMenu(MediaItem("f1", "a", ItemType.MOVIE, "Oppdrag Nemo"), {}, {}, {}, onAction = {})
        } }
        compose.onAllNodesWithText("Spel frå starten").assertCountEquals(0)
        compose.onAllNodesWithText("Fjern frå Hald fram").assertCountEquals(0)
        compose.onAllNodesWithText("Merk som sett").assertCountEquals(1)
        assertTrue(true)
    }
}
