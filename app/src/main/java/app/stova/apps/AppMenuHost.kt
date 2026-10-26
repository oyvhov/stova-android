package app.stova.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.stova.core.ui.SheetPages
import app.stova.profiles.Profile
import kotlinx.coroutines.launch

/** Shared by Home and Apps. Profile switches discard the pending menu and picker. */
@Composable
fun AppMenuHost(app: InstalledApp, apps: AppRepository, profile: Profile, art: AppArtCache, catalog: AppCatalog,
    rowActions: @Composable (SheetPages) -> Unit = {}, onDismiss: () -> Unit) {
    if (profile.kid) return
    key(profile.id, app.key) {
        val scope = rememberCoroutineScope()
        var picking by remember { mutableStateOf(false) }
        if (picking) IconPicker(art, profile.apps.iconOverrides[app.key]?.packageName ?: profile.appearance.iconPack,
            { icon -> scope.launch { apps.setIcon(profile.id, app.key, icon); onDismiss() } }, { picking = false })
        else AppActionsMenu(app, app.key in profile.apps.favorites, app.key in profile.apps.hidden,
            onToggleFavorite = { apps.launchChange { toggleFavorite(profile.id, app.key) } },
            onMove = { delta -> apps.launchChange { moveFavorite(profile.id, app.key, delta) } },
            onToggleHidden = { apps.launchChange { setHidden(profile.id, app.key, app.key !in profile.apps.hidden) } },
            onAppInfo = { catalog.openAppInfo(app.packageName) }, onDismiss = onDismiss,
            onOpen = { catalog.launch(app) },
            onPickIcon = { picking = true },
            onResetIcon = { scope.launch { apps.setIcon(profile.id, app.key, null); onDismiss() } },
            art = { AppSheetArt(app, art) }, rowActions = rowActions)
    }
}

/** The app's own TV banner across the panel; an app without one shows its icon on the raised surface. */
@Composable
private fun BoxScope.AppSheetArt(app: InstalledApp, art: AppArtCache) {
    val banner by produceState<ImageBitmap?>(null, app.key, app.lastUpdateTime) { value = art.banner(app) }
    val icon by produceState<ImageBitmap?>(null, app.key, app.lastUpdateTime) { value = art.icon(app) }
    banner?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
        ?: icon?.let { Image(it, contentDescription = null, modifier = Modifier.align(Alignment.Center).size(SheetIconSize)) }
}

private val SheetIconSize = 88.dp
