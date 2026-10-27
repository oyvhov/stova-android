package app.stova.apps

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.core.ui.SheetAction
import app.stova.core.ui.SheetHeader
import app.stova.core.ui.SheetPages
import app.stova.core.ui.StovaButton
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.StovaSheet
import app.stova.profiles.Profile
import coil3.compose.AsyncImage

/**
 * What an adult can do with a library or scene, in the same panel as for apps: add it to or remove
 * it from the favourites, open or run it, and move a favourite. Every choice closes the panel.
 */
@Composable
fun ShortcutActionsMenu(
    tile: FavoriteTile,
    onToggleFavorite: () -> Unit,
    onOpen: () -> Unit,
    onMove: (Int) -> Unit,
    onDismiss: () -> Unit,
    rowActions: @Composable (SheetPages) -> Unit = {},
    favorite: Boolean = true,
) {
    val scene = tile is FavoriteTile.Scene
    val image = (tile as? FavoriteTile.Library)?.image
    val art: (@Composable BoxScope.() -> Unit)? = image?.let { url ->
        { AsyncImage(url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
    }
    StovaSheet(onDismiss) { pages ->
        if (pages.current == null) {
            val kind = when {
                scene && favorite -> R.string.sheet_scene_favorite
                scene -> R.string.sheet_scene
                favorite -> R.string.sheet_library_favorite
                else -> R.string.sheet_library
            }
            SheetHeader(tile.label, stringResource(kind), if (scene) StovaIcons.Sun else StovaIcons.Rows, art = art)
            val full = Modifier.fillMaxWidth()
            StovaButton(stringResource(if (favorite) R.string.app_remove_favorite else R.string.app_add_favorite),
                { onToggleFavorite(); onDismiss() }, full, primary = true, icon = StovaIcons.Star)
            StovaButton(stringResource(if (scene) R.string.shortcut_run else R.string.sheet_open), { onOpen(); onDismiss() }, full, icon = StovaIcons.Play)
            if (favorite) {
                SheetAction(stringResource(R.string.app_move_left), StovaIcons.ChevronLeft, { onMove(-1); onDismiss() })
                SheetAction(stringResource(R.string.app_move_right), StovaIcons.ChevronRight, { onMove(+1); onDismiss() })
            }
        }
        rowActions(pages)
    }
}

/** The shortcut panel wired to the profile's favourites. Kids get no panel. */
@Composable
fun ShortcutMenuHost(tile: FavoriteTile, apps: AppRepository, profile: Profile, onOpen: (FavoriteTile) -> Unit,
    rowActions: @Composable (SheetPages) -> Unit = {}, onDismiss: () -> Unit) {
    if (profile.kid) return
    key(profile.id, tile.key) {
        val favorite = tile.key in profile.apps.favorites
        ShortcutActionsMenu(tile,
            onToggleFavorite = {
                apps.launchChange { if (favorite) toggleFavorite(profile.id, tile.key) else addShortcut(profile.id, tile.key, tile.saved()) }
            },
            onOpen = { onOpen(tile) },
            onMove = { delta -> apps.launchChange { moveFavorite(profile.id, tile.key, delta) } },
            onDismiss = onDismiss, rowActions = rowActions, favorite = favorite)
    }
}
