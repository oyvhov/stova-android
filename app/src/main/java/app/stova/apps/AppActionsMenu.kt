package app.stova.apps

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.core.ui.SheetAction
import app.stova.core.ui.SheetHeader
import app.stova.core.ui.SheetPages
import app.stova.core.ui.StovaButton
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.StovaSheet

/**
 * Compact app identity and Open first. Removing a favourite is a secondary row;
 * changing the icon is a page of its own. Every other choice closes the panel.
 */
@Composable
fun AppActionsMenu(
    app: InstalledApp,
    favorite: Boolean,
    hidden: Boolean,
    onToggleFavorite: () -> Unit,
    onMove: (Int) -> Unit,
    onToggleHidden: () -> Unit,
    onAppInfo: () -> Unit,
    onDismiss: () -> Unit,
    onOpen: (() -> Unit)? = null,
    onPickIcon: () -> Unit = {},
    onResetIcon: () -> Unit = {},
    art: (@Composable BoxScope.() -> Unit)? = null,
    rowActions: @Composable (SheetPages) -> Unit = {},
) {
    StovaSheet(onDismiss) { pages ->
        when (pages.current) {
            null -> {
                SheetHeader(app.label, stringResource(if (favorite) R.string.sheet_app_favorite else R.string.sheet_app), StovaIcons.Apps, art = art, compact = true)
                val full = Modifier.fillMaxWidth()
                if (onOpen != null) StovaButton(stringResource(R.string.sheet_open), { onOpen(); onDismiss() }, full, primary = true, icon = StovaIcons.Play)
                if (!favorite) StovaButton(stringResource(R.string.app_add_favorite),
                    { onToggleFavorite(); onDismiss() }, full, primary = onOpen == null, icon = StovaIcons.Star)
                if (favorite) {
                    SheetAction(stringResource(R.string.app_move_left), StovaIcons.ChevronLeft, { onMove(-1); onDismiss() })
                    SheetAction(stringResource(R.string.app_move_right), StovaIcons.ChevronRight, { onMove(+1); onDismiss() })
                }
                SheetAction(stringResource(if (hidden) R.string.app_show else R.string.app_hide), if (hidden) StovaIcons.Show else StovaIcons.Hide,
                    { onToggleHidden(); onDismiss() })
                SheetAction(stringResource(R.string.icon_change), StovaIcons.Palette, { pages.open(ICON_PAGE) },
                    Modifier.focusRequester(pages.opener(ICON_PAGE)), opensPage = true)
                if (favorite) SheetAction(stringResource(R.string.app_remove_favorite), StovaIcons.Star,
                    { onToggleFavorite(); onDismiss() })
                SheetAction(stringResource(R.string.app_info), StovaIcons.Info, { onAppInfo(); onDismiss() })
            }
            ICON_PAGE -> {
                SheetHeader(stringResource(R.string.icon_change), app.label, StovaIcons.Palette)
                SheetAction(stringResource(R.string.icon_from_pack), StovaIcons.Image, onPickIcon)
                SheetAction(stringResource(R.string.icon_reset), StovaIcons.Apps, onResetIcon)
            }
        }
        rowActions(pages)
    }
}

private const val ICON_PAGE = "icon"
