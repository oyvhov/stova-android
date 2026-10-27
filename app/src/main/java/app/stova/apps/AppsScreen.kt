package app.stova.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.StovaShapes
import app.stova.core.ui.focusOutline
import app.stova.core.ui.PageHeader
import app.stova.core.ui.SectionHeading
import app.stova.core.ui.StovaIcons
import app.stova.profiles.Profile
import app.stova.media.model.MediaAccount
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Every app, favourites first. Adults get a menu per app; a kid only opens what is allowed. With
 * [shortcuts], an adult also sees the profile's libraries and the house's scenes, to add to the
 * favourites like an app.
 */
@Composable
fun AppsScreen(apps: AppRepository, profile: Profile, art: AppArtCache, catalog: AppCatalog,
    shortcuts: ShortcutCatalog? = null, haConnection: app.stova.ha.HaConnection? = null) {
    val currentProfile by rememberUpdatedState(profile)
    val offers = shortcuts?.takeIf { !profile.kid }
    val arrangedFlow = remember(apps, offers, profile.id) {
        val local = offers?.let { combine(it.accounts(profile.id), it.haReady) { owned, ha -> owned to ha } } ?: flowOf(emptyList<MediaAccount>() to false)
        combine(apps.arranged(snapshotFlow { currentProfile }), local) { arranged, ready -> AppsPage(arranged, ready.first, ready.second) }
            .map<AppsPage, AppsPage?> { it }
    }
    // Null until the first scan: a grid laid out empty would anchor on "All apps" and then be
    // pushed down by the favourites arriving above it, so the page would open scrolled.
    val loaded by arrangedFlow.collectAsStateWithLifecycle(initialValue = null)
    // Asked once per visit, and only what is set up; a fresh list also renames saved favourites.
    val libraries by produceState(emptyList<FavoriteTile.Library>(), offers, profile.id) {
        value = offers?.let { runCatching { it.libraries(profile.id) }.getOrNull() }.orEmpty()
        if (value.isNotEmpty()) apps.refreshShortcuts(profile.id, value.associate { it.key to it.saved() })
    }
    val scenes by produceState(emptyList<FavoriteTile.Scene>(), offers) {
        value = offers?.let { runCatching { it.scenes() }.getOrNull() }.orEmpty()
        if (value.isNotEmpty()) apps.refreshShortcuts(profile.id, value.associate { it.key to it.saved() })
    }

    var menuFor by remember(profile.id) { mutableStateOf<InstalledApp?>(null) }
    var shortcutMenu by remember(profile.id) { mutableStateOf<FavoriteTile?>(null) }
    var showHidden by remember { mutableStateOf(false) }
    var spoleMissing by remember { mutableStateOf(false) }
    val openShortcut = rememberShortcutOpener(haConnection, profile) { spoleMissing = true }

    val onLong: (InstalledApp) -> Unit = { app -> if (!profile.kid) menuFor = app }
    val page = loaded ?: return
    val arranged = page.apps
    val favorites = favoriteTiles(profile.apps, arranged.favorites, page.accounts, page.haReady, profile.kid)
    val appearance = app.stova.core.theme.LocalAppearance.current
    val format = if (appearance.appStyle == app.stova.profiles.AppStyle.ICON)
        app.stova.profiles.RowFormat.ICON else app.stova.profiles.RowFormat.WIDE
    val cardWidth = app.stova.home.artworkDimensions(format, appearance.cardSize).width.dp

    LazyVerticalGrid(
        columns = GridCells.FixedSize(cardWidth),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(StovaLayout.TvSafeEdge),
        horizontalArrangement = Arrangement.spacedBy(StovaLayout.RowGap),
        verticalArrangement = Arrangement.spacedBy(StovaLayout.RowGap),
    ) {
        item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
            PageHeader(stringResource(R.string.nav_apps), StovaIcons.Apps,
                subtitle = stringResource(if (profile.kid) R.string.apps_intro_kid else R.string.apps_intro))
        }
        if (favorites.isNotEmpty()) {
            header(R.string.apps_favorites)
            items(favorites, key = { "fav:${it.key}" }) { tile ->
                if (tile is FavoriteTile.App) AppTile(tile.app, art, { catalog.launch(tile.app) }, { onLong(tile.app) }, iconOverride = profile.apps.iconOverrides[tile.key])
                else ShortcutTile(tile, { openShortcut(tile) }, { shortcutMenu = tile })
            }
        }
        header(R.string.apps_all)
        items(arranged.all, key = { "all:${it.key}" }) { app ->
            AppTile(app, art, { catalog.launch(app) }, { onLong(app) }, iconOverride = profile.apps.iconOverrides[app.key])
        }
        if (libraries.isNotEmpty()) {
            header(R.string.apps_libraries)
            items(libraries, key = { "lib:${it.key}" }) { tile -> ShortcutTile(tile, { openShortcut(tile) }, { shortcutMenu = tile }) }
        }
        if (scenes.isNotEmpty()) {
            header(R.string.apps_scenes)
            items(scenes, key = { "ha:${it.key}" }) { tile -> ShortcutTile(tile, { openShortcut(tile) }, { shortcutMenu = tile }) }
        }
        if (arranged.hidden.isNotEmpty()) {
            item(key = "hidden-header", span = { GridItemSpan(maxLineSpan) }) {
                HiddenToggle(stringResource(R.string.apps_hidden, arranged.hidden.size)) { showHidden = !showHidden }
            }
            if (showHidden) {
                items(arranged.hidden, key = { "hidden:${it.key}" }) { app ->
                    AppTile(app, art, { catalog.launch(app) }, { onLong(app) }, iconOverride = profile.apps.iconOverrides[app.key])
                }
            }
        }
    }

    menuFor?.let { app ->
        AppMenuHost(app, apps, profile, art, catalog) { menuFor = null }
    }
    shortcutMenu?.let { tile -> ShortcutMenuHost(tile, apps, profile, openShortcut) { shortcutMenu = null } }
    if (spoleMissing) app.stova.media.ui.SpoleMissingDialog { spoleMissing = false }
}

/** The installed apps and the local facts that say which favourite shortcuts can show. */
private data class AppsPage(val apps: ArrangedApps, val accounts: List<MediaAccount>, val haReady: Boolean)

private fun LazyGridScope.header(title: Int) {
    item(key = "header:$title", span = { GridItemSpan(maxLineSpan) }) {
        SectionHeading(stringResource(title), when (title) {
            R.string.apps_favorites -> StovaIcons.Star
            R.string.apps_libraries -> StovaIcons.Rows
            R.string.apps_scenes -> StovaIcons.Sun
            else -> StovaIcons.Apps
        },
            Modifier.padding(top = StovaLayout.SectionTop, bottom = StovaLayout.SectionBottom))
    }
}

@Composable
private fun HiddenToggle(label: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium
    Text(
        label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(top = StovaLayout.SectionTop)
            .focusOutline(source, shape)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}
