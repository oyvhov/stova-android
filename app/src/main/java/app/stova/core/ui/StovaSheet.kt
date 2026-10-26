package app.stova.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaLayout

/**
 * The pages of one long-press panel. A row opens a page by name; Back returns to the main page
 * and focuses the row that opened it again. Content reads [current] on every composition, so a
 * page always shows the latest values.
 */
@Stable
class SheetPages internal constructor() {
    var current by mutableStateOf<String?>(null)
        private set
    internal var returning: String? = null
    private val openers = mutableMapOf<String, FocusRequester>()

    /** Put this on the row that opens [page]. */
    fun opener(page: String): FocusRequester = openers.getOrPut(page) { FocusRequester() }
    fun open(page: String) { returning = null; current = page }
    /** True when it went back a page; false on the main page, where Back closes the panel. */
    fun back(): Boolean {
        val from = current ?: return false
        returning = from
        current = null
        return true
    }
}

/**
 * Every long-press menu: a full-height panel from the right with artwork, the main actions as
 * large buttons and calm rows below. Pages replace the content inside the same panel.
 * Focus enters the first control of each page; the held OK that opened it chooses nothing.
 */
@OptIn(ExperimentalFoundationApi::class) // LocalBringIntoViewSpec, the TV pivot hook
@Composable
fun StovaSheet(onDismissRequest: () -> Unit, content: @Composable ColumnScope.(SheetPages) -> Unit) {
    val pages = remember { SheetPages() }
    val page = remember { FocusRequester() }
    StovaSheetWindow(onDismissRequest, onBack = pages::back) {
        val shape = MaterialTheme.shapes.extraLarge.copy(topEnd = ZeroCornerSize, bottomEnd = ZeroCornerSize)
        // Drawn, not clipped: a clip with square right corners would also cut away touches.
        Box(Modifier.fillMaxHeight().width(StovaLayout.SheetWidth).background(MaterialTheme.colorScheme.surface, shape)) {
            // The TV pivot would pull every focused row up a third of the panel and cut the artwork.
            CompositionLocalProvider(LocalBringIntoViewSpec provides ScrollOnlyWhenHidden) { key(pages.current) {
                Column(Modifier.fillMaxSize().softEntrance(pages.current).focusGroup()
                    .verticalScroll(rememberScrollState())
                    .padding(start = StovaLayout.SheetPadding, end = StovaLayout.TvSafeEdge,
                        top = StovaLayout.SheetTop, bottom = StovaLayout.SheetPadding),
                    verticalArrangement = Arrangement.spacedBy(StovaLayout.SheetGap)) {
                    // The way back sits above the page for touch and for Up; focus starts on the page itself.
                    if (pages.current != null) SheetAction(stringResource(R.string.custom_back), StovaIcons.ChevronLeft, { pages.back() })
                    Column(Modifier.fillMaxWidth().focusRequester(page).focusGroup(),
                        verticalArrangement = Arrangement.spacedBy(StovaLayout.SheetGap)) { content(pages) }
                }
            } }
        }
        LaunchedEffect(pages.current) {
            withFrameNanos { }
            val back = pages.returning
            pages.returning = null
            val returned = back != null && runCatching { pages.opener(back).requestFocus() }.getOrDefault(false)
            if (!returned) runCatching { page.requestFocus(FocusDirection.Enter) }
        }
    }
}

/** The platform's own rule: scroll just enough to show the focused row, and not at all when it shows. */
@OptIn(ExperimentalFoundationApi::class)
private val ScrollOnlyWhenHidden = object : BringIntoViewSpec {}

/** Artwork across the panel, then the title and one line of detail. Without art, an icon leads. */
@Composable
fun SheetHeader(title: String, subtitle: String? = null, icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant, art: (@Composable BoxScope.() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (art != null) Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center, content = art)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (art == null && icon != null) CategoryIcon(icon, tint = tint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** A small label over a group of rows, with the thing it belongs to beside it. */
@Composable
fun SheetSection(title: String, detail: String? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 14.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        detail?.let {
            Text(it, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** One calm row: icon, label, an optional value, a tick when chosen and a chevron when it opens a page. */
@Composable
fun SheetAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier,
    value: String? = null, opensPage: Boolean = false, chosen: Boolean = false, danger: Boolean = false) {
    val source = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().heightIn(min = StovaLayout.SheetRowMinHeight)
        .semantics { if (chosen) selected = true }
        .focusOutline(source, shape).clip(shape)
        .clickable(source, indication = null, role = Role.Button, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = if (danger) colors.error else colors.onSurfaceVariant)
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
            color = if (danger) colors.error else colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        value?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (chosen) Icon(StovaIcons.Check, null, Modifier.size(18.dp), tint = colors.primary)
        if (opensPage) Icon(StovaIcons.ChevronRight, null, Modifier.size(14.dp), tint = colors.onSurfaceVariant)
    }
}
