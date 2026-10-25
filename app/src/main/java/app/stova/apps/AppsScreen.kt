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
import app.stova.profiles.Profile
import kotlinx.coroutines.flow.map

/** Every app, favourites first. Adults get a menu per app; a kid only opens what is allowed. */
@Composable
fun AppsScreen(apps: AppRepository, profile: Profile, art: AppArtCache, catalog: AppCatalog) {
    val currentProfile by rememberUpdatedState(profile)
    val arrangedFlow = remember(apps) { apps.arranged(snapshotFlow { currentProfile }).map<ArrangedApps, ArrangedApps?> { it } }
    // Null until the first scan: a grid laid out empty would anchor on "All apps" and then be
    // pushed down by the favourites arriving above it, so the page would open scrolled.
    val loaded by arrangedFlow.collectAsStateWithLifecycle(initialValue = null)

    var menuFor by remember(profile.id) { mutableStateOf<InstalledApp?>(null) }
    var showHidden by remember { mutableStateOf(false) }

    val onLong: (InstalledApp) -> Unit = { app -> if (!profile.kid) menuFor = app }
    val arranged = loaded ?: return

    LazyVerticalGrid(
        columns = GridCells.Adaptive(StovaLayout.WideWidth),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(StovaLayout.TvSafeEdge),
        horizontalArrangement = Arrangement.spacedBy(StovaLayout.RowGap),
        verticalArrangement = Arrangement.spacedBy(StovaLayout.RowGap),
    ) {
        item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
            Text(
                stringResource(R.string.nav_apps),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (arranged.favorites.isNotEmpty()) {
            header(R.string.apps_favorites)
            items(arranged.favorites, key = { "fav:${it.key}" }) { app ->
                AppTile(app, art, { catalog.launch(app) }, { onLong(app) }, iconOverride = profile.apps.iconOverrides[app.key])
            }
        }
        header(R.string.apps_all)
        items(arranged.all, key = { "all:${it.key}" }) { app ->
            AppTile(app, art, { catalog.launch(app) }, { onLong(app) }, iconOverride = profile.apps.iconOverrides[app.key])
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
}

private fun LazyGridScope.header(title: Int) {
    item(key = "header:$title", span = { GridItemSpan(maxLineSpan) }) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = StovaLayout.SectionTop, bottom = StovaLayout.SectionBottom),
        )
    }
}

@Composable
private fun HiddenToggle(label: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(StovaShapes.Medium)
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
