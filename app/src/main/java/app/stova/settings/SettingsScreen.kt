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
}

/** Sections on the left, the chosen one on the right, as in Spole's TV settings. */
@Composable
fun SettingsScreen(container: AppContainer, profile: Profile, home: HomeViewModel, onSwitchProfile: () -> Unit) {
    if (profile.kid) {
        KidSettings(onSwitchProfile)
        return
    }
    var section by rememberSaveable { mutableStateOf(SettingsSection.PROFILE) }
    val context = androidx.compose.ui.platform.LocalContext.current
    SettingsLayout(section, { section = it }, { app.stova.navigation.openSystemSettings(context) }) { shown, openSection ->
        when (shown) {
            SettingsSection.PROFILE -> ProfileSettings(container, profile)
            SettingsSection.ACCOUNTS -> AccountsSettings(profile, container)
            SettingsSection.HOME -> HomeRowSettings(container, profile, home, onEditAppearance = { openSection(SettingsSection.APPEARANCE) })
            SettingsSection.SMART_HOME -> app.stova.ha.ui.HaSettings(container)
            SettingsSection.HOME_BUTTON -> HomeButtonSettings(container)
            SettingsSection.APPEARANCE -> AppearanceSettings(container, profile, pinnedPreview = true, onEditRows = { openSection(SettingsSection.HOME) })
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
