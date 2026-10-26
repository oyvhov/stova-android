package app.stova.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.stova.AppContainer
import app.stova.R
import app.stova.core.ui.StovaButton
import app.stova.core.ui.PageHeader
import app.stova.home.HomeViewModel
import app.stova.homebutton.HomeButtonSettings
import app.stova.profiles.Profile

enum class SettingsSection(@StringRes val label: Int) {
    PROFILE(R.string.settings_section_profiles),
    ACCOUNTS(R.string.settings_accounts),
    HOME(R.string.settings_home),
    SMART_HOME(R.string.settings_section_connections),
    HOME_BUTTON(R.string.settings_section_system),
    APPEARANCE(R.string.settings_section_look),
    ABOUT(R.string.settings_about),
    OVERVIEW(R.string.settings_overview),
}

/** D-pad order of the four sections; Apps sits between Profiles and System. Enum order is kept for saved destinations. */
internal val settingsOrder = listOf(SettingsSection.APPEARANCE, SettingsSection.SMART_HOME, SettingsSection.PROFILE, SettingsSection.HOME_BUTTON)

/** Where a section that was folded into another now lives, so a restored destination still opens. */
internal val SettingsSection.merged: SettingsSection get() = when (this) {
    SettingsSection.OVERVIEW, SettingsSection.HOME -> SettingsSection.APPEARANCE
    SettingsSection.ACCOUNTS -> SettingsSection.SMART_HOME
    SettingsSection.ABOUT -> SettingsSection.HOME_BUTTON
    else -> this
}

/** Sections on the left, the chosen one on the right, as in Spole's TV settings. */
@Composable
fun SettingsScreen(container: AppContainer, profile: Profile, home: HomeViewModel, onSwitchProfile: () -> Unit, onOpenApps: () -> Unit = {}) {
    if (profile.kid) {
        KidSettings(onSwitchProfile)
        return
    }
    var section by rememberSaveable { mutableStateOf(SettingsSection.APPEARANCE) }
    val context = androidx.compose.ui.platform.LocalContext.current
    SettingsLayout(section.merged, { section = it }, onOpenApps) { shown, _ ->
        if (shown != SettingsSection.APPEARANCE) PageHeader(stringResource(shown.pageTitle), shown.icon,
            subtitle = stringResource(shown.hint), tint = shown.tone)
        when (shown) {
            SettingsSection.PROFILE -> ProfileSettings(container, profile)
            SettingsSection.SMART_HOME -> { app.stova.ha.ui.HaSettings(container); AccountsSettings(profile, container) }
            SettingsSection.HOME_BUTTON -> {
                HomeButtonSettings(container)
                AboutSettings()
                app.stova.core.ui.SettingsNavRow(stringResource(R.string.nav_system_settings), value = stringResource(R.string.settings_android_hint),
                    onClick = { app.stova.navigation.openSystemSettings(context) })
            }
            SettingsSection.APPEARANCE -> AppearanceSettings(container, profile, scrollable = true, home = home)
            SettingsSection.OVERVIEW, SettingsSection.HOME, SettingsSection.ACCOUNTS, SettingsSection.ABOUT -> Unit // see merged
        }
    }
}

/** A kid's settings are one door: back to the profile picker. */
@Composable
private fun KidSettings(onSwitchProfile: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        StovaButton(stringResource(R.string.nav_profile), onSwitchProfile, primary = true)
    }
}
