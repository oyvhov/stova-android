package app.stova.settings

import app.stova.kids.stepBedtime
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.core.ui.StovaDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.AppContainer
import app.stova.R
import app.stova.core.theme.StovaShapes
import app.stova.core.ui.PinDialog
import app.stova.core.ui.SettingsGroup
import app.stova.core.ui.SettingsNavRow
import app.stova.core.ui.SettingsSwitchRow
import app.stova.core.ui.StovaButton
import app.stova.profiles.PinCheck
import app.stova.profiles.Profile
import app.stova.profiles.ProfileEditor
import kotlinx.coroutines.launch

private enum class PinStep { NONE, VERIFY_FOR_CHANGE, VERIFY_FOR_REMOVE, NEW, CONFIRM }

@Composable
fun ProfileSettings(container: AppContainer, profile: Profile) {
    val enterPane = LocalSettingsPaneEntry.current
    val repo = container.profiles
    val file by repo.file.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAccounts by rememberSaveable { mutableStateOf(false) }
    var showKidApps by rememberSaveable { mutableStateOf(false) }
    var editSelf by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var pinStep by remember { mutableStateOf(PinStep.NONE) }
    // The first entry of a new PIN, kept only until it is typed again; a hidden typo would lock the adult out.
    var pendingPin by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = detailId != null) { enterPane(); if (showAccounts) showAccounts = false else if (showKidApps) showKidApps = false else detailId = null }

    val detail = detailId?.let { id -> file?.profiles?.firstOrNull { it.id == id } }
    if (detail != null) {
        if (showAccounts) AccountsSettings(detail, container)
        else if (showKidApps) KidAppsSettings(container, detail)
        else ProfileDetail(container, detail, onKidApps = { enterPane(); showKidApps = true }, onAccounts = { enterPane(); showAccounts = true }, onDeleted = { enterPane(); detailId = null })
        return
    }

    SettingsGroup(stringResource(R.string.settings_me)) {
        SettingsNavRow(stringResource(R.string.profile_name_and_colour), value = profile.name) { editSelf = true }
        if (profile.pin == null) {
            SettingsNavRow(stringResource(R.string.pin_set)) { pinStep = PinStep.NEW }
        } else {
            SettingsNavRow(stringResource(R.string.pin_change)) { pinStep = PinStep.VERIFY_FOR_CHANGE }
            SettingsNavRow(stringResource(R.string.pin_remove)) { pinStep = PinStep.VERIFY_FOR_REMOVE }
        }
        SpoleProfileRow(container, profile)
        SettingsSwitchRow(
            stringResource(R.string.settings_start_last),
            file?.startInLastProfile ?: true,
            { enabled -> scope.launch { repo.setStartInLastProfile(enabled) } },
        )
    }
    SettingsGroup(stringResource(R.string.settings_profiles)) {
        file?.profiles.orEmpty().filter { it.id != profile.id }.forEach { other ->
            SettingsNavRow(other.name, value = stringResource(if (other.kid) R.string.profile_kid else R.string.profile_adult)) {
                enterPane()
                detailId = other.id
            }
        }
        SettingsNavRow(stringResource(R.string.profile_add)) { adding = true }
    }

    if (editSelf) {
        EditorDialog(onDismiss = { editSelf = false }) {
            ProfileEditor(profile, allowKid = false, onSave = { name, avatar, _ ->
                editSelf = false
                scope.launch { repo.update(profile.id) { it.copy(name = name, avatar = avatar) } }
            }, onCancel = { editSelf = false })
        }
    }
    if (adding) {
        EditorDialog(onDismiss = { adding = false }) {
            ProfileEditor(null, allowKid = true, onSave = { name, avatar, kid ->
                adding = false
                scope.launch { repo.create(name, avatar, kid) }
            }, onCancel = { adding = false })
        }
    }
    when (pinStep) {
        PinStep.NONE -> Unit
        PinStep.VERIFY_FOR_CHANGE, PinStep.VERIFY_FOR_REMOVE -> PinDialog(
            title = stringResource(R.string.pin_for_profile, profile.name),
            onSubmit = { pin -> repo.verifyPin(profile.id, pin) },
            onAccepted = {
                if (pinStep == PinStep.VERIFY_FOR_CHANGE) pinStep = PinStep.NEW
                else { pinStep = PinStep.NONE; scope.launch { repo.setPin(profile.id, null) } }
            },
            onDismiss = { pinStep = PinStep.NONE },
        )
        PinStep.NEW -> PinDialog(
            title = stringResource(R.string.pin_new),
            onSubmit = { pin -> pendingPin = pin; PinCheck.Accepted },
            onAccepted = { pinStep = PinStep.CONFIRM },
            onDismiss = { pinStep = PinStep.NONE; pendingPin = null },
        )
        PinStep.CONFIRM -> PinDialog(
            title = stringResource(R.string.pin_confirm),
            onSubmit = { pin ->
                if (pin == pendingPin) { repo.setPin(profile.id, pin); PinCheck.Accepted } else PinCheck.Rejected(0)
            },
            onAccepted = { pinStep = PinStep.NONE; pendingPin = null },
            onDismiss = { pinStep = PinStep.NONE; pendingPin = null },
        )
    }
}

@Composable
private fun ProfileDetail(container: AppContainer, other: Profile, onKidApps: () -> Unit, onAccounts: () -> Unit, onDeleted: () -> Unit) {
    val enterPane = LocalSettingsPaneEntry.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var refused by remember { mutableStateOf(false) }
    var appearance by remember(other.id) { mutableStateOf(false) }
    val closeAppearance = { enterPane(); appearance = false }
    BackHandler(enabled = appearance, onBack = closeAppearance)
    if (appearance) {
        Text(other.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        StovaButton(stringResource(R.string.custom_back), closeAppearance)
        AppearanceSettings(container, other)
        return
    }
    Text(other.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
    if (other.kid) {
        SettingsGroup(stringResource(R.string.profile_kid)) {
            SettingsNavRow(stringResource(R.string.kid_apps), onClick = onKidApps)
            SettingsNavRow(stringResource(R.string.settings_accounts), onClick = onAccounts)
            // Libraries borrowed from a grown-up's account on this TV: the child's rows come from these alone.
            val libraries by remember(other.id) { container.mediaAccounts.sharedLibraries(other.id) }.collectAsStateWithLifecycle(initialValue = emptySet())
            var pickingLibraries by remember(other.id) { mutableStateOf(false) }
            SettingsNavRow(stringResource(R.string.kids_libraries), value = librarySelectionLabel(libraries)) { pickingLibraries = true }
            if (pickingLibraries) BackgroundLibraryPicker(libraries, { container.media.lendableLibraries(other.id) },
                { chosen -> scope.launch { container.mediaAccounts.shareLibraries(other.id, chosen.orEmpty()) }; pickingLibraries = false },
                { pickingLibraries = false }, forChild = true)
        }
        // Pip on the child's Home: whether he is there, whether he reads aloud, bedtime and the background.
        var pip by remember(other.id) { mutableStateOf(other.pip) }
        fun save(next: app.stova.kids.PipPrefs) { pip = next; scope.launch { container.profiles.update(other.id) { it.copy(pip = next) } } }
        val on = stringResource(R.string.appearance_on); val off = stringResource(R.string.appearance_off)
        SettingsGroup(stringResource(R.string.kids_pip_group)) {
            app.stova.core.ui.SettingsChoiceRow(stringResource(R.string.kids_pip_show), if (pip.shown) on else off) { save(pip.copy(shown = !pip.shown)) }
            app.stova.core.ui.SettingsChoiceRow(stringResource(R.string.kids_pip_read), if (pip.readAloud) on else off) { save(pip.copy(readAloud = !pip.readAloud)) }
            app.stova.core.ui.SettingsChoiceRow(stringResource(R.string.kids_bedtime),
                pip.bedtime?.let { java.time.LocalTime.of(it / 60, it % 60).toString() } ?: stringResource(R.string.kids_bedtime_off)) { step -> save(pip.stepBedtime(step)) }
            app.stova.core.ui.SettingsChoiceRow(stringResource(R.string.kids_background), stringResource(
                if (pip.background == app.stova.kids.KidsBackground.ART) R.string.kids_background_art else R.string.kids_background_world)) {
                save(pip.copy(background = if (pip.background == app.stova.kids.KidsBackground.ART) app.stova.kids.KidsBackground.WORLD else app.stova.kids.KidsBackground.ART))
            }
        }
    }
    SettingsGroup(stringResource(R.string.settings_profile)) {
        SpoleProfileRow(container, other)
        SettingsNavRow(stringResource(R.string.settings_appearance)) { enterPane(); appearance = true }
        SettingsNavRow(stringResource(R.string.profile_delete)) { confirm = true }
    }
    if (refused) {
        Text(stringResource(R.string.profile_delete_last), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    }
    if (confirm) {
        EditorDialog(onDismiss = { confirm = false }) {
            Text(stringResource(R.string.profile_delete_confirm, other.name), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StovaButton(stringResource(R.string.profile_delete_yes), {
                    confirm = false
                    scope.launch {
                        try {
                            container.profiles.delete(other.id)
                            container.mediaAccounts.removeForProfile(other.id)
                            onDeleted()
                        } catch (_: IllegalStateException) {
                            refused = true
                        }
                    }
                }, primary = true)
                StovaButton(stringResource(R.string.profile_cancel), { confirm = false })
            }
        }
    }
}

@Composable
private fun EditorDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    StovaDialog(onDismiss) {
        Column(
            Modifier.background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraLarge)
                .padding(32.dp)
                .width(600.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) { content() }
    }
}

/**
 * Who follows this profile in Spole when it is picked here: the person with the profile's own account,
 * or one chosen with left and right. A child chooses among Spole's children only. Shown only when Spole
 * has profiles; a change to the profile in use takes Spole along at once.
 */
@Composable
private fun SpoleProfileRow(container: AppContainer, profile: Profile) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val people by androidx.compose.runtime.produceState(emptyList<app.stova.apps.FavoriteTile.SpoleProfile>(), profile.id, profile.kid) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { app.stova.media.SpoleProfiles(context).list() }
            .filter { !profile.kid || it.kid }
    }
    if (people.isEmpty()) return
    val accounts by remember(profile.id) { container.mediaAccounts.forProfile(profile.id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val options = listOf<app.stova.profiles.SpoleLink?>(null) + people.map { app.stova.profiles.SpoleLink(it.kind, it.userId) }
    val chosen = profile.spole?.let { link -> people.firstOrNull { it.kind == link.kind && it.userId == link.userId } }
    val auto = app.stova.media.spoleProfileFor(profile.copy(spole = null), accounts, people)
    val value = chosen?.label ?: auto?.let { stringResource(R.string.spole_profile_auto_named, it.label) } ?: stringResource(R.string.spole_profile_auto)
    app.stova.core.ui.SettingsChoiceRow(stringResource(R.string.spole_profile), value) { step ->
        val next = options[(options.indexOf(chosen?.let { app.stova.profiles.SpoleLink(it.kind, it.userId) }).coerceAtLeast(0) + step).mod(options.size)]
        scope.launch {
            val updated = container.profiles.update(profile.id) { it.copy(spole = next) }
            if (container.session.active.value?.id == profile.id) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                app.stova.media.SpoleProfiles(context).follow(updated, accounts)
            }
        }
    }
}
