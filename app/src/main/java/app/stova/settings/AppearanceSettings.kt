package app.stova.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.AppContainer
import app.stova.core.theme.*
import app.stova.profiles.*
import app.stova.profiles.Profile
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@Composable
fun AppearanceSettings(container: AppContainer, profile: Profile, pinnedPreview: Boolean = false, onEditRows: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val packs by container.iconPacks.packs.collectAsStateWithLifecycle()
    LaunchedEffect(container.iconPacks) { container.iconPacks.installed() }
    val profiles by container.profiles.file.collectAsStateWithLifecycle(initialValue = ProfilesFile())
    var picking by remember(profile.id) { mutableStateOf(false) }
    var draft by remember(profile.id) { mutableStateOf(profile.appearance) }
    var pending by remember(profile.id) { mutableIntStateOf(0) }
    LaunchedEffect(profile.appearance, pending) { if (pending == 0) draft = profile.appearance }
    val previewFlow = remember(profile.id, draft.fixedBackground, draft.backgroundSource) {
        if (draft.backgroundSource == BackgroundSource.FIXED) container.media.fixedBackground(profile.id, draft.fixedBackground)
        else container.media.cachedPreview(profile.id)
    }
    val preview by previewFlow.collectAsStateWithLifecycle(initialValue = null)
    val contents: @Composable () -> Unit = {
        AppearanceSettingsContent(draft, packs, onPickBackground = { picking = true }, onEditRows = onEditRows,
            includePreview = !pinnedPreview, previewEntry = preview, homePrefs = profile.home,
            others = profiles.profiles.filter { it.id != profile.id },
            onReset = { scope.launch { container.profiles.resetAppearance(profile.id) } },
            onCopy = { sourceId -> scope.launch {
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
            } }) { prefs ->
                draft = prefs; pending++
                scope.launch {
                    try { container.profiles.update(profile.id) { it.copy(appearance = prefs) } }
                    finally { pending-- }
                }
            }
    }
    if (pinnedPreview) Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ThemePreview(draft, preview, profile.home)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) { contents() }
    } else contents()
    if (picking) BackgroundPicker({ container.media.searchBackgrounds(profile.id, it) }, { entry ->
        picking = false
        scope.launch {
            container.media.rememberBackground(profile.id, entry)
            container.profiles.update(profile.id) { it.copy(appearance = it.appearance.copy(backgroundSource = BackgroundSource.FIXED,
                fixedBackground = BackgroundTitle(entry.account.id, entry.item.id, entry.item.title))) }
        }
    }, { picking = false })
}
