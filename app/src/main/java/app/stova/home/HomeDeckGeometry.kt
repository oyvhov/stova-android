package app.stova.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.profiles.*

internal data class HomeDeckGeometry(val slotHeight: Dp, val visibleRows: Int, val artworkScale: Float, val bottom: Dp) {
    val height: Dp get() = slotHeight * visibleRows
}

/** Fixed from preferences and text metrics. Artwork loading and scrolling never resize the deck. */
@Composable
internal fun homeDeckGeometry(rows: List<HomeRowUi>, home: HomePrefs, p: AppearancePrefs, width: Dp, height: Dp): HomeDeckGeometry {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val body = MaterialTheme.typography.bodyMedium
    val offline = stringResource(R.string.offline)
    val measures = rows.map { row ->
        val style = home.rowFormats[row.key] ?: RowStyle()
        val textStyle = style.headingTextStyle()
        val sourceWidth = (row as? HomeRowUi.Media)?.let { media ->
            (if (style.showSource) measurer.measure(media.account.serverName, body, maxLines = 1).size.width
                .coerceAtMost(with(density) { StovaLayout.RowSourceMaxWidth.roundToPx() }) else 0) +
                (if (media.offline) measurer.measure(offline, body, maxLines = 1).size.width else 0)
        } ?: 0
        val titleWidth = (with(density) { (width - StovaLayout.TvSafeEdge * 2 - 40.dp).roundToPx() } - sourceWidth).coerceAtLeast(1)
        val titleHeight = with(density) {
            measurer.measure(style.heading(rowLabel(row.key)), textStyle, maxLines = 2, constraints = Constraints(maxWidth = titleWidth)).size.height.toDp()
        }
        val heading = p.rowSpacing.top.dp + if (style.showTitle) titleHeight + StovaLayout.SectionBottom else 0.dp
        val captions = if (p.showTitles) (8 + 18 * density.fontScale).dp +
            (if (row is HomeRowUi.WatchNext && row.entries.any { !it.subtitle.isNullOrEmpty() }) (17 * density.fontScale).dp else 0.dp) else 0.dp
        val art = artworkDimensions(style.format ?: defaultRowFormat(row.key, p), style.size ?: p.cardSize).height.dp
        (heading + captions + 8.dp) to art
    }
    val bottom = p.homeRowInset.dp.dp
    val overviewSpace = if (p.heroSize != HeroSize.OFF && p.showOverview && density.fontScale < 1.5f) 208.dp else 0.dp
    val available = (height - bottom - overviewSpace).coerceAtLeast(1.dp)
    val natural = measures.maxOfOrNull { it.first + it.second } ?: 1.dp
    fun scaleFor(slot: Dp) = measures.minOfOrNull { (fixed, art) -> ((slot - fixed) / art).coerceIn(0.1f, 1f) } ?: 1f
    // At double text size, prefer one complete row over two unreadably small strips.
    val count = if (p.homeRows == HomeRows.TWO && density.fontScale < 1.5f && rows.size > 1 && scaleFor(available / 2) >= 0.6f) 2 else 1
    val slot = minOf(natural, available / count)
    return HomeDeckGeometry(slot, count, scaleFor(slot), bottom)
}
