package app.stova.discover

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.AppContainer
import app.stova.R
import app.stova.background.BackdropState
import app.stova.background.rememberBackdropState
import app.stova.core.ui.stepEntrance
import app.stova.media.HandoffResult
import app.stova.media.SpoleHandoff
import app.stova.media.ui.SpoleMissingDialog
import app.stova.profiles.Profile
import app.stova.profiles.SpoleLink
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Where the Oppdag page is: its rows, a title's page, or search. A title's [Title.fallback] is what its card
 * showed, drawn while Seerr is asked for the rest. Not to be mixed up with a search's answer, [DiscoverPage].
 */
sealed interface DiscoverStep {
    data object Rows : DiscoverStep
    data class Title(val type: DiscoverType, val tmdbId: Int, val fallback: DiscoverTitle?) : DiscoverStep
    data object Search : DiscoverStep
}

/** The step stack as lists of plain values, so it outlives the activity being made again. */
internal val DiscoverStepsSaver: Saver<List<DiscoverStep>, Any> = listSaver(
    save = { steps -> steps.map(::savedStep) },
    restore = { saved -> saved.map(::restoredStep) },
)

private const val ROWS = "rows"
private const val TITLE = "title"
private const val SEARCH = "search"

private fun savedStep(step: DiscoverStep): List<Any?> = when (step) {
    DiscoverStep.Rows -> listOf(ROWS)
    DiscoverStep.Search -> listOf(SEARCH)
    is DiscoverStep.Title -> listOf(TITLE, step.type.name, step.tmdbId, step.fallback?.let(::savedTitle))
}

private fun restoredStep(saved: List<*>): DiscoverStep = when (saved.first()) {
    TITLE -> DiscoverStep.Title(DiscoverType.valueOf(saved[1] as String), saved[2] as Int, (saved[3] as List<*>?)?.let(::restoredTitle))
    SEARCH -> DiscoverStep.Search
    else -> DiscoverStep.Rows
}

/** The most titles of a search saved with its step: three of Seerr's pages. */
private const val SAVED_SEARCH_TITLES = 60

/** The most titles one page of Seerr's search holds. */
private const val SEERR_PAGE_SIZE = 20

/**
 * A search's answer as plain values, saved with the search step so Back from a title finds the titles found again. It is
 * written each time a title's page goes over the search and with the activity whenever an app opens, so it is kept small:
 * no story and no genres, which a title's page asks Seerr for anyway, and no more than [SAVED_SEARCH_TITLES] titles. Cut
 * short, it holds at least the first three pages whole (a page holds [SEERR_PAGE_SIZE] titles at most), and «Hent fleire»
 * goes on from the fourth; a title it brings again is shown once.
 */
internal val DiscoverPageSaver: Saver<DiscoverPage?, Any> = Saver(
    save = { page ->
        page?.let {
            val cut = it.items.size > SAVED_SEARCH_TITLES
            val items = it.items.take(SAVED_SEARCH_TITLES).map { title -> savedTitle(title.copy(overview = null, genres = emptyList())) }
            arrayListOf(if (cut) minOf(it.page, SAVED_SEARCH_TITLES / SEERR_PAGE_SIZE) else it.page, cut || it.hasMore, ArrayList(items))
        }
    },
    restore = { saved ->
        val (page, hasMore, items) = saved as List<*>
        DiscoverPage((items as List<*>).map { restoredTitle(it as List<*>) }, page as Int, hasMore as Boolean)
    },
)

/** A title as plain values, for a title's step and a search's answer. */
private fun savedTitle(title: DiscoverTitle): ArrayList<Any?> = arrayListOf(title.type.name, title.tmdbId, title.title, title.year,
    title.overview, title.poster, title.backdrop, title.status.name, title.community, ArrayList(title.genres))

private fun restoredTitle(saved: List<*>): DiscoverTitle = DiscoverTitle(DiscoverType.valueOf(saved[0] as String), saved[1] as Int,
    saved[2] as String, saved[3] as Int?, saved[4] as String?, saved[5] as String?, saved[6] as String?, SeerrStatus.valueOf(saved[7] as String),
    saved[8] as Double?, (saved[9] as List<*>).map { it as String })

/** Each step's own saved state (focus, scrolling) by its place on the stack and what it shows. */
private fun DiscoverStep.stateKey(depth: Int): String = "$depth:" + when (this) {
    DiscoverStep.Rows -> ROWS
    DiscoverStep.Search -> SEARCH
    is DiscoverStep.Title -> "${type.wire}-$tmdbId"
}

/**
 * The Oppdag page with a stack of its own: the rows, a title's page and search. Back takes the top step away
 * and the step under it comes back as it was left; on the first step Back reaches the shell's own handler,
 * which walks home as from Apps. [onLeave] takes the page home; the page goes there by itself when it has
 * nothing to show. Spole is asked for the rows and the person's requests on the way in, as the person the
 * profile is in Discover, unless its last answer is younger than ten minutes. The steps share one backdrop,
 * so a title's page and the rows under it hand the picture over without a fade from dark.
 */
@Composable
fun DiscoverScreen(container: AppContainer, profile: Profile, start: DiscoverStep = DiscoverStep.Rows, onLeave: () -> Unit) {
    var steps by rememberSaveable(stateSaver = DiscoverStepsSaver) { mutableStateOf(listOf(start)) }
    val held = rememberSaveableStateHolder()
    BackHandler(enabled = steps.size > 1) {
        held.removeState(steps.last().stateKey(steps.lastIndex))
        steps = steps.dropLast(1)
    }
    val accounts by remember(container, profile.id) { container.mediaAccounts.forProfile(profile.id) }
        .collectAsStateWithLifecycle(initialValue = null)
    val link = accounts?.let { container.discover.identity(profile, it) }
    val state by remember(container, profile.id) { container.discover.state(profile.id) }.collectAsStateWithLifecycle()
    // In the app's scope, so an answer that arrives after the page has closed is there for the next visit.
    LaunchedEffect(link) { link?.let { container.appScope.launch { container.discover.refresh(profile.id, it) } } }
    // Nothing to show for good: nobody to ask as once the accounts are read, or Spole saying Seerr is not there for
    // the person. The page goes home instead of standing empty. The state is read again here, since a refresh begun
    // just above for an old answer has already said it is asking.
    val nothing = accounts != null && (link == null || state?.nothingToShow == true)
    LaunchedEffect(nothing) {
        if (nothing && (link == null || container.discover.state(profile.id).value?.nothingToShow == true)) onLeave()
    }
    val backdrop = rememberBackdropState()
    val context = LocalContext.current
    val handoff = remember(context) { SpoleHandoff(context) }
    var missing by remember { mutableStateOf(false) }
    // As Home opens a title in Spole: a Spole too old for the link opens at its own home and says so.
    fun openInSpole(type: DiscoverType, tmdbId: Int) {
        val person = link ?: return
        when (handoff.openDiscover(person, type, tmdbId)) {
            HandoffResult.Opened -> Unit
            HandoffResult.OpenedWithoutLink -> Toast.makeText(context, R.string.spole_update_for_links, Toast.LENGTH_LONG).show()
            HandoffResult.SpoleMissing -> missing = true
            HandoffResult.InvalidIds -> Toast.makeText(context, R.string.invalid_media_ids, Toast.LENGTH_SHORT).show()
        }
    }
    val step = steps.last()
    val stepKey = step.stateKey(steps.lastIndex)
    Box(Modifier.fillMaxSize().stepEntrance(stepKey, steps.lastIndex)) {
    held.SaveableStateProvider(stepKey) {
        when (step) {
            DiscoverStep.Rows -> DiscoverContent(state,
                onOpenTitle = { steps = steps + DiscoverStep.Title(it.type, it.tmdbId, it) },
                onOpenSearch = { steps = steps + DiscoverStep.Search },
                onOpenRequest = { steps = steps + DiscoverStep.Title(it.type, it.tmdbId, it.asDiscoverTitle()) },
                onRetry = { link?.let { container.appScope.launch { container.discover.refresh(profile.id, it, force = true) } } },
                backdrop = backdrop)
            is DiscoverStep.Title -> TitleStep(container, profile.id, link, step, state?.access ?: SeerrAccess.NONE, backdrop,
                onOpenSpole = { openInSpole(step.type, step.tmdbId) },
                onOpenTitle = { steps = steps + DiscoverStep.Title(it.type, it.tmdbId, it) })
            DiscoverStep.Search -> SearchStep(container, link, onOpenTitle = { steps = steps + DiscoverStep.Title(it.type, it.tmdbId, it) })
        }
    }
    }
    if (missing) SpoleMissingDialog { missing = false }
}

/** How long typing must pause before Spole is asked. */
private const val SEARCH_DEBOUNCE_MILLIS = 400L

/**
 * Search's step. Spole is asked as the profile's person once typing has paused for [SEARCH_DEBOUNCE_MILLIS] and the query,
 * spaces at either end left out, has [MIN_QUERY] characters or more; a shorter one shows nothing, and a new query stops
 * the ask before it. «Hent fleire» asks for the next page and adds it. When Spole trips, what is shown stays; with nothing
 * shown, the page says Seerr did not answer. The query and what it found are saved with the step, so Back from a title
 * finds both again without asking Spole twice.
 */
@Composable
private fun SearchStep(container: AppContainer, link: SpoleLink?, onOpenTitle: (DiscoverTitle) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable(stateSaver = DiscoverPageSaver) { mutableStateOf<DiscoverPage?>(null) }
    // The query [page] answers.
    var answered by rememberSaveable { mutableStateOf<String?>(null) }
    // True from the first key of a query to Spole's answer, and while the next page is on its way: one ask at a time.
    var loading by remember { mutableStateOf(false) }
    // Spole gave nothing for the first page of the query typed.
    var failed by remember { mutableStateOf(false) }
    var nextPage by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val asked = query.trim()
    LaunchedEffect(link, asked) {
        // A next page of the query before belongs to that query: it is not added under this one's answer.
        nextPage?.cancel()
        when {
            asked.length < MIN_QUERY -> { page = null; answered = null; failed = false; loading = false }
            asked == answered || link == null -> { failed = false; loading = false }
            else -> {
                loading = true
                delay(SEARCH_DEBOUNCE_MILLIS)
                val found = container.discover.search(link, asked, 1)
                found?.let { page = it; answered = asked }
                failed = found == null
                loading = false
            }
        }
    }
    DiscoverSearchContent(query, page, answered, loading, failed, onQuery = { query = it }, onOpenTitle = onOpenTitle, onMore = more@{
        val shown = page ?: return@more
        val person = link ?: return@more
        val words = answered ?: return@more
        if (!shown.hasMore || loading) return@more
        loading = true
        nextPage = scope.launch {
            // Pages can overlap as Seerr's order shifts: a title already shown stays where it is.
            container.discover.search(person, words, shown.page + 1)?.let { next ->
                page = DiscoverPage((shown.items + next.items).distinctBy { it.key }, shown.page + 1, next.hasMore)
            }
            loading = false
        }
    })
}

/** What a title's step has open over its page. */
private enum class TitleSheet { REQUEST, WITHDRAW }

/**
 * A title's step: the page with what its card showed at once, then Seerr's page and related rows from Spole.
 * When Spole trips the page stays as the card showed it, with «Opne i Spole»; the rows page tells of Seerr failing.
 * «Be om» and «Trekk tilbake» open their sheets; what a sheet sends goes through Spole as the profile's person,
 * and a toast tells how it went once the sheet has closed.
 */
@Composable
private fun TitleStep(container: AppContainer, profileId: String, link: SpoleLink?, step: DiscoverStep.Title, access: SeerrAccess,
    backdrop: BackdropState, onOpenSpole: () -> Unit, onOpenTitle: (DiscoverTitle) -> Unit) {
    var details by remember { mutableStateOf<DiscoverDetails?>(null) }
    // Null until Spole has answered for the related rows, so Back from a related title knows when its card can be found.
    var related by remember { mutableStateOf<List<DiscoverRow>?>(null) }
    var sheet by remember { mutableStateOf<TitleSheet?>(null) }
    // Busy while a request or a withdrawal is on its way: one at a time, so a second OK sends nothing twice.
    var busy by remember { mutableStateOf(false) }
    val context = LocalContext.current.applicationContext
    // With the link too: opened straight from Home, the profile's accounts may not be read yet.
    LaunchedEffect(link, step.type, step.tmdbId) {
        if (link == null) return@LaunchedEffect
        launch { related = container.discover.related(link, step.type, step.tmdbId) }
        details = container.discover.details(link, step.type, step.tmdbId)
    }
    // In the app's scope: Spole has what was sent whether or not the page is still open, so the answer is told either way.
    // When it went through, Spole first asks Seerr for the person's requests again (a new request gets its id only then),
    // and only after that is the title's page asked for anew, so «Trekk tilbake» can follow a request.
    fun act(@StringRes done: Int, send: suspend (SpoleLink) -> DiscoverActionResult) {
        val person = link ?: return
        if (busy) return
        busy = true
        container.appScope.launch {
            try {
                val result = send(person)
                val message = if (result == DiscoverActionResult.DONE) done else discoverActionMessage(result)
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                if (result == DiscoverActionResult.DONE) {
                    container.discover.refresh(profileId, person, force = true)
                    container.discover.details(person, step.type, step.tmdbId)?.let { details = it }
                }
            } finally {
                busy = false
            }
        }
    }
    // A step saved without its card has only Seerr's page to show, once it comes.
    val title = step.fallback ?: DiscoverTitle(step.type, step.tmdbId, "", null, null, null, null, SeerrStatus.UNKNOWN, null, emptyList())
    DiscoverTitleContent(title, details, related, access, busy, onRequest = { if (!busy) sheet = TitleSheet.REQUEST },
        onWithdraw = { if (!busy) sheet = TitleSheet.WITHDRAW }, onOpenSpole = onOpenSpole, onOpenTitle = onOpenTitle, backdrop = backdrop)
    val page = details
    when (sheet) {
        TitleSheet.REQUEST -> if (page != null) RequestSheet(page, onSend = { seasons ->
            sheet = null
            act(R.string.discover_sent) { container.discover.request(it, step.type, step.tmdbId, seasons) }
        }, onDismiss = { sheet = null })
        TitleSheet.WITHDRAW -> page?.requestId?.let { id ->
            WithdrawSheet(page.title.title.ifBlank { title.title }, onConfirm = {
                sheet = null
                act(R.string.discover_withdrawn) { container.discover.withdraw(it, id) }
            }, onDismiss = { sheet = null })
        }
        null -> Unit
    }
}
