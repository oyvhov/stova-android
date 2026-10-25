package app.stova.home

import androidx.compose.runtime.staticCompositionLocalOf
import app.stova.profiles.*

val LocalRowStyle = staticCompositionLocalOf { RowStyle() }
val LocalRowHasFocus = staticCompositionLocalOf { false }
/** Layout-only fitting for a two-row deck; saved artwork choices remain unchanged. */
val LocalArtworkScale = staticCompositionLocalOf { 1f }

fun defaultRowFormat(key: HomeRowKey, p: AppearancePrefs): RowFormat = when {
    key == HomeRowKey.APPS && p.appStyle == AppStyle.ICON -> RowFormat.ICON
    key.value.startsWith("latest_movies:") && p.movieStyle == MovieStyle.POSTER -> RowFormat.POSTER
    else -> RowFormat.WIDE
}

data class ArtworkDimensions(val width: Float, val height: Float)

fun artworkDimensions(format: RowFormat, size: ArtworkSize): ArtworkDimensions {
    val ratio = size.width / 248f
    val width = when (format) {
        RowFormat.POSTER -> 132f * ratio
        RowFormat.ICON -> 139.5f * ratio
        RowFormat.LARGE_WIDE -> size.width * 1.2f
        RowFormat.SMALL_WIDE -> size.width * 0.8f
        RowFormat.WIDE -> size.width.toFloat()
    }
    return ArtworkDimensions(width, when (format) {
        RowFormat.POSTER -> width * 1.5f
        RowFormat.ICON -> width
        else -> width * 9 / 16
    })
}

/** Move among visible rows, retaining hidden and temporarily unavailable identities in the order. */
fun moveHomeRow(prefs: HomePrefs, available: List<HomeRowKey>, key: HomeRowKey, delta: Int): HomePrefs {
    val order = (prefs.rowOrder + available).distinct()
    val visible = arrangeRows(available, prefs)
    val index = visible.indexOf(key)
    val neighbor = visible.getOrNull(index + delta) ?: return prefs
    if (index < 0) return prefs
    val updated = order.toMutableList()
    val from = updated.indexOf(key)
    val to = updated.indexOf(neighbor)
    updated.add(to, updated.removeAt(from))
    return prefs.copy(rowOrder = updated)
}
