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
    PROFILE(R.string.settings_profile),
    ACCOUNTS(R.string.settings_accounts),
    HOME(R.string.settings_home),
    SMART_HOME(R.string.nav_smart_home),
    HOME_BUTTON(R.string.settings_home_button),
    APPEARANCE(R.string.settings_appearance),
    ABOUT(R.string.settings_about),
    OVERVIEW(R.string.settings_overview),
}

/** Visual groups and D-pad order share this list. Enum order is kept for saved destinations. */
internal val settingsGroups = listOf(
    R.string.settings_group_home to listOf(SettingsSection.OVERVIEW, SettingsSection.HOME, SettingsSection.APPEARANCE),
    R.string.settings_group_services to listOf(SettingsSection.ACCOUNTS, SettingsSection.SMART_HOME),
    R.string.settings_group_system to listOf(SettingsSection.PROFILE, SettingsSection.HOME_BUTTON, SettingsSection.ABOUT),
)
internal val settingsOrder = settingsGroups.flatMap { it.second }

/** Sections on the left, the chosen one on the right, as in Spole's TV settings. */
@Composable
fun SettingsScreen(container: AppContainer, profile: Profile, home: HomeViewModel, onSwitchProfile: () -> Unit) {
    if (profile.kid) {
        KidSettings(onSwitchProfile)
        return
    }
    var section by rememberSaveable { mutableStateOf(SettingsSection.OVERVIEW) }
    val context = androidx.compose.ui.platform.LocalContext.current
    SettingsLayout(section, { section = it }, { app.stova.navigation.openSystemSettings(context) }) { shown, openSection ->
        if (shown != SettingsSection.APPEARANCE) PageHeader(stringResource(shown.pageTitle), shown.icon,
            subtitle = stringResource(shown.hint), tint = shown.tone)
        when (shown) {
            SettingsSection.OVERVIEW -> SettingsOverview(openSection)
            SettingsSection.PROFILE -> ProfileSettings(container, profile)
            SettingsSection.ACCOUNTS -> AccountsSettings(profile, container)
            SettingsSection.HOME -> HomeRowSettings(container, profile, home, onEditAppearance = { openSection(SettingsSection.APPEARANCE) })
            SettingsSection.SMART_HOME -> app.stova.ha.ui.HaSettings(container)
            SettingsSection.HOME_BUTTON -> HomeButtonSettings(container)
            SettingsSection.APPEARANCE -> AppearanceSettings(container, profile, scrollable = true, onEditRows = { openSection(SettingsSection.HOME) })
            SettingsSection.ABOUT -> AboutSettings()
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
