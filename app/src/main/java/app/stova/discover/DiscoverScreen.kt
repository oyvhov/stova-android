package app.stova.discover

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.AppContainer
import app.stova.core.theme.StovaLayout
import app.stova.profiles.Profile
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
    is DiscoverStep.Title -> listOf(TITLE, step.type.name, step.tmdbId) + step.fallback?.let {
        listOf(it.title, it.year, it.overview, it.poster, it.backdrop, it.status.name, it.community, ArrayList(it.genres))
    }.orEmpty()
}

private fun restoredStep(saved: List<*>): DiscoverStep = when (saved.first()) {
    TITLE -> {
        val type = DiscoverType.valueOf(saved[1] as String)
        val tmdbId = saved[2] as Int
        DiscoverStep.Title(type, tmdbId, saved.takeIf { it.size > 3 }?.let {
            DiscoverTitle(type, tmdbId, it[3] as String, it[4] as Int?, it[5] as String?, it[6] as String?, it[7] as String?,
                SeerrStatus.valueOf(it[8] as String), it[9] as Double?, (it[10] as List<*>).map { genre -> genre as String })
        })
    }
    SEARCH -> DiscoverStep.Search
    else -> DiscoverStep.Rows
}

/** Each step's own saved state (focus, scrolling) by its place on the stack and what it shows. */
private fun DiscoverStep.stateKey(depth: Int): String = "$depth:" + when (this) {
    DiscoverStep.Rows -> ROWS
    DiscoverStep.Search -> SEARCH
    is DiscoverStep.Title -> "${type.wire}-$tmdbId"
}

/**
 * The Oppdag page with a stack of its own: the rows, a title's page and search. Back takes the top step away
 * and the step under it comes back as it was left; on the first step Back reaches the shell's own handler,
 * which walks home as from Apps. [onLeave] takes the page home. Spole is asked for the rows on the way in,
 * as the person the profile is in Discover, unless its last answer is younger than ten minutes.
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
    val step = steps.last()
    held.SaveableStateProvider(step.stateKey(steps.lastIndex)) {
        when (step) {
            DiscoverStep.Rows -> DiscoverContent(state,
                onOpenTitle = { steps = steps + DiscoverStep.Title(it.type, it.tmdbId, it) },
                onOpenSearch = { steps = steps + DiscoverStep.Search },
                onOpenRequest = { request ->
                    steps = steps + DiscoverStep.Title(request.type, request.tmdbId, DiscoverTitle(request.type, request.tmdbId, request.title,
                        null, null, request.poster, request.backdrop, SeerrStatus.UNKNOWN, null, emptyList()))
                },
                onRetry = { link?.let { container.appScope.launch { container.discover.refresh(profile.id, it, force = true) } } })
            is DiscoverStep.Title -> TitlePlaceholder(step.fallback)
            // Search is drawn by a later step of the plan (Task 8); until then the step is empty and Back closes it.
            DiscoverStep.Search -> Box(Modifier.fillMaxSize())
        }
    }
}

/** A title's step until its page is drawn (Task 5): the title its card showed, and nothing else. */
@Composable
private fun TitlePlaceholder(title: DiscoverTitle?) {
    Box(Modifier.fillMaxSize().padding(StovaLayout.TvSafeEdge)) {
        if (title != null) Text(title.title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
