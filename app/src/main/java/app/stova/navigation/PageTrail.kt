package app.stova.navigation

import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.graphics.TransformOrigin

internal data class PageOpening(val focusKey: String? = null, val origin: TransformOrigin = TransformOrigin.Center)
internal data class PageEntry(val destination: Destination, val depth: Int, val opening: PageOpening = PageOpening())

internal data class PageTrail(
    val entries: List<PageEntry> = listOf(PageEntry(Destination.HOME, 0)),
    val returnFocus: String? = null,
) {
    val current get() = entries.last()
    fun open(destination: Destination, opening: PageOpening = PageOpening()) =
        if (current.destination == destination) this else PageTrail(entries + PageEntry(destination, entries.size, opening))
    fun back() = if (entries.size == 1) this else PageTrail(entries.dropLast(1), current.opening.focusKey)
    fun home() = PageTrail()
}

/** Restore the route, with a centred fallback for coordinates from the previous layout. */
internal val PageTrailSaver = listSaver<PageTrail, String>(
    save = { trail -> trail.entries.map { it.destination.name } },
    restore = { names -> PageTrail(names.mapIndexed { depth, name -> PageEntry(Destination.valueOf(name), depth) }) },
)

/** Scoped to the synchronous click, so opening a popup cannot leave a stale page origin. */
internal class PageOpenContext {
    var opening: PageOpening = PageOpening()
        private set
    fun from(source: PageOpening, navigate: () -> Unit) {
        val previous = opening
        opening = source
        try { navigate() } finally { opening = previous }
    }
}
