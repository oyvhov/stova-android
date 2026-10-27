package app.stova.settings

import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.AppContainer
import app.stova.profiles.*
import app.stova.profiles.Profile
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.width

@Composable
fun AppearanceSettings(container: AppContainer, profile: Profile, scrollable: Boolean = false, home: app.stova.home.HomeViewModel? = null) {
    val packs by container.iconPacks.packs.collectAsStateWithLifecycle()
    LaunchedEffect(container.iconPacks) { container.iconPacks.installed() }
    val profiles by container.profiles.file.collectAsStateWithLifecycle(initialValue = ProfilesFile())
    val appFlow = remember(profile.id, profile.apps) { container.apps.arranged(kotlinx.coroutines.flow.flowOf(profile)) }
    val previewApps by appFlow.collectAsStateWithLifecycle(initialValue = app.stova.apps.ArrangedApps())
    var picking by remember(profile.id) { mutableStateOf(false) }
    var pickingLibraries by remember(profile.id) { mutableStateOf(false) }
    var pickingKidsLibraries by remember(profile.id) { mutableStateOf(false) }
    var restoreLibraries by remember(profile.id) { mutableStateOf(false) }
    val libraryFocus=remember(profile.id) {FocusRequester()}
    val edits = remember(container, profile.id) { container.appearanceEdits.open(profile) }
    val draft = edits.draft
    LaunchedEffect(profile.appearance, edits.pending) { edits.sync(profile.appearance) }
    androidx.compose.runtime.DisposableEffect(edits) { onDispose { edits.flush() } }
    fun save(prefs: AppearancePrefs) {
        val before = edits.draft
        // Only continuous slider input waits; buttons and content restrictions save at once.
        val slidersOnly = prefs.copy(appTileBackground = before.appTileBackground,
            appTileBackgroundOpacity = before.appTileBackgroundOpacity, backdropDimmingPercent = before.backdropDimmingPercent,
            cornerRadiusDp = before.cornerRadiusDp, cardSpacingDp = before.cardSpacingDp,
            spotlightSeconds = before.spotlightSeconds) == before
        edits.change(prefs, delayed = slidersOnly)
    }
    LaunchedEffect(pickingLibraries,restoreLibraries) {
        if(!pickingLibraries && restoreLibraries) {withFrameNanos {};runCatching {libraryFocus.requestFocus()};restoreLibraries=false}
    }
    val previewFlow = remember(profile.id, draft.fixedBackground, draft.backgroundSource,draft.backgroundFilter,draft.spotlightSource) {
        if (draft.backgroundSource == BackgroundSource.FIXED) container.media.fixedBackground(profile.id, draft.fixedBackground,draft.backgroundFilter)
        else if(draft.backgroundFilter.libraries!=null) container.media.spotlight(profile.id,source=draft.spotlightSource,filter=draft.backgroundFilter).map {it.firstOrNull()}
        else container.media.cachedPreview(profile.id,draft.backgroundFilter,draft.spotlightSource)
    }
    val rawPreview by previewFlow.collectAsStateWithLifecycle(initialValue = null)
    val preview=app.stova.media.backgroundForProfile(rawPreview,profile.id,draft.backgroundFilter)?.takeIf {
        if(draft.backgroundSource==BackgroundSource.FIXED) it.item.id==draft.fixedBackground?.itemId && it.account.id==draft.fixedBackground?.accountId
        else app.stova.media.acceptsSpotlight(it.item,draft.spotlightSource)}
    val contents: @Composable (AppearanceCategory, Modifier) -> Unit = { category, firstControl ->
        if (category == AppearanceCategory.ROWS) HomeRowSettings(container, profile, home, firstControl = firstControl) else
        AppearanceSettingsContent(draft, packs, onPickBackground = { picking = true },
            onPickLibraries={restoreLibraries=true;pickingLibraries=true},onPickKidsLibraries={pickingKidsLibraries=true},libraryControl=Modifier.focusRequester(libraryFocus),
            category = category, firstControl = firstControl,
            others = profiles.profiles.filter { it.id != profile.id },
            onReset = { container.appScope.launch { edits.awaitSaved(); container.profiles.resetAppearance(profile.id) } },
            onCopy = { sourceId -> container.appScope.launch {
                edits.awaitSaved()
                val source = profiles.profiles.firstOrNull { it.id == sourceId } ?: return@launch
                val accounts = container.mediaAccounts.all.first()
                val sourceAccounts = accounts.filter { it.profileId == sourceId }
                val targetAccounts = accounts.filter { it.profileId == profile.id }
                val keys = (source.home.rowOrder + source.home.hiddenRows + source.home.rowFormats.keys).distinct()
                val mapping = keys.mapNotNull { key ->
                    val prefix = key.value.substringBefore(':')
                    val sourceAccount = sourceAccounts.firstOrNull { it.id == key.value.substringAfter(':', "") }
                    val target = sourceAccount?.let { from -> targetAccounts.firstOrNull { it.serverId == from.serverId && it.kind == from.kind } }
                    target?.let { key to app.stova.home.HomeRowKey("$prefix:${it.id}") }
                }.toMap()
                container.profiles.copyAppearance(profile.id, sourceId, mapping)
            } },onChange=::save)
    }
    // Home's own rows and start row; another profile's page has no Home to show, only its hero.
    val noRows = remember { kotlinx.coroutines.flow.MutableStateFlow<List<app.stova.home.HomeRowUi>?>(null) }
    val noStart = remember { kotlinx.coroutines.flow.MutableStateFlow<app.stova.home.HomeRowKey?>(null) }
    val rows by (home?.rows ?: noRows).collectAsStateWithLifecycle()
    val start by (home?.initialFocus ?: noStart).collectAsStateWithLifecycle()
    val household by container.household.data.collectAsStateWithLifecycle(initialValue = null)
    val smartHome = household?.haBaseUrl != null
    val miniature: @Composable (AppearanceCategory) -> Unit = {
        // Chips are asked for only when Home Assistant is set up.
        app.stova.home.HomeChips(container.haRepository.takeIf { smartHome }) { chips ->
            androidx.compose.runtime.key(profile.id, draft.backgroundFilter, draft.backgroundSource, draft.fixedBackground, draft.spotlightSource) {
                HomeMiniature(draft, profile, rows, start, preview, container.appArt, container.appCatalog,
                    Modifier.width(app.stova.core.theme.StovaLayout.SettingsPreviewWidth), smartHome, chips)
            }
        }
    }
    AppearanceMenu(scrollable = scrollable, preview = miniature) { category, firstControl ->
        if (category == AppearanceCategory.ICONS) AppIconPreview(draft,
            (previewApps.favorites + previewApps.all).distinctBy { it.key }, container.appArt, profile.apps.iconOverrides)
        contents(category, firstControl)
    }
    if(pickingLibraries) BackgroundLibraryPicker(draft.backgroundFilter.libraries,{container.media.backgroundLibraries(profile.id)},
        {selected->save(draft.copy(backgroundFilter=draft.backgroundFilter.copy(libraries=selected)));pickingLibraries=false},
        {pickingLibraries=false})
    // "All" means no kids libraries chosen: every library cannot be the kids one.
    if(pickingKidsLibraries) BackgroundLibraryPicker(draft.kidsHours.libraries,{container.media.backgroundLibraries(profile.id)},
        {selected->save(draft.copy(kidsHours=draft.kidsHours.copy(libraries=selected.orEmpty())));pickingKidsLibraries=false},
        {pickingKidsLibraries=false})
    if (picking) BackgroundPicker({ container.media.searchBackgrounds(profile.id, it,draft.backgroundFilter) }, { entry ->
        picking = false
        container.appScope.launch {
            container.media.rememberBackground(profile.id, entry)
            save(edits.draft.copy(backgroundSource = BackgroundSource.FIXED,
                fixedBackground = BackgroundTitle(entry.account.id, entry.item.id, entry.item.title)))
        }
    }, { picking = false })
}
