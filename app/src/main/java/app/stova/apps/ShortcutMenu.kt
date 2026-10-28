package app.stova.apps

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.SheetAction
import app.stova.core.ui.SheetHeader
import app.stova.core.ui.SheetPages
import app.stova.core.ui.StovaButton
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.StovaSheet
import app.stova.ha.HaIconPicker
import app.stova.profiles.Profile
import coil3.compose.AsyncImage

/**
 * What an adult can do with a library or scene, in the same panel as for apps: add it to or remove
 * it from the favourites, open or run it, and move a favourite. A favourite scene also changes its
 * icon, on a page of its own as an app does. Every other choice closes the panel.
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
    onPickIcon: () -> Unit = {},
    onResetIcon: () -> Unit = {},
) {
    val scene = tile is FavoriteTile.Scene
    val image = (tile as? FavoriteTile.Library)?.image
    val tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    val art: (@Composable BoxScope.() -> Unit)? = when {
        image != null -> { { AsyncImage(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) } }
        tile is FavoriteTile.Scene -> { {
            app.stova.ha.HaIcon(tile.icon ?: app.stova.ha.HaIconFont.defaultFor(tile.entityId), StovaIcons.Sun, SceneArtSize, tint,
                Modifier.align(androidx.compose.ui.Alignment.Center))
        } }
        else -> null
    }
    StovaSheet(onDismiss) { pages ->
        when (pages.current) {
            null -> {
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
                if (scene && favorite) SheetAction(stringResource(R.string.icon_change), StovaIcons.Palette, { pages.open(ICON_PAGE) },
                    Modifier.focusRequester(pages.opener(ICON_PAGE)), opensPage = true)
            }
            ICON_PAGE -> {
                SheetHeader(stringResource(R.string.icon_change), tile.label, StovaIcons.Palette)
                SheetAction(stringResource(R.string.ha_icon_pick), StovaIcons.Image, onPickIcon)
                SheetAction(stringResource(R.string.ha_icon_from_ha), StovaIcons.Sun, onResetIcon)
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
        var picking by remember { mutableStateOf(false) }
        fun setIcon(icon: String?) { apps.launchChange { setShortcutIcon(profile.id, tile.key, icon) }; onDismiss() }
        if (picking) HaIconPicker((tile as? FavoriteTile.Scene)?.icon, ::setIcon) { picking = false }
        else ShortcutActionsMenu(tile,
            onToggleFavorite = {
                apps.launchChange { if (favorite) toggleFavorite(profile.id, tile.key) else addShortcut(profile.id, tile.key, tile.saved()) }
            },
            onOpen = { onOpen(tile) },
            onMove = { delta -> apps.launchChange { moveFavorite(profile.id, tile.key, delta) } },
            onDismiss = onDismiss, rowActions = rowActions, favorite = favorite,
            onPickIcon = { picking = true }, onResetIcon = { setIcon(null) })
    }
}

private val SceneArtSize = 88.dp
private const val ICON_PAGE = "icon"
