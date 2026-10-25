package app.stova.settings

import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.AppContainer
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.stovaMotion
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
    val motion = stovaMotion
    Row(Modifier.fillMaxSize().padding(StovaLayout.TvSafeEdge), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        val context = androidx.compose.ui.platform.LocalContext.current
        SettingsNavigation(section, { section = it }) { app.stova.navigation.openSystemSettings(context) }
        // The list follows focus, so the content crossfades quickly and never asks for focus itself;
        // the section on its way out cannot take it either. Each section starts at the top.
        Crossfade(section, Modifier.weight(1f), animationSpec = motion.fade(motion.quick), label = "settings-section") { shown ->
            val leaving = shown != section
            Column(
                Modifier.then(if (leaving) Modifier.focusProperties { canFocus = false } else Modifier)
                    .then(if (shown == SettingsSection.APPEARANCE) Modifier.fillMaxSize() else Modifier.verticalScroll(rememberScrollState())),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                when (shown) {
                    SettingsSection.PROFILE -> ProfileSettings(container, profile)
                    SettingsSection.ACCOUNTS -> AccountsSettings(profile, container)
                    SettingsSection.HOME -> HomeRowSettings(container, profile, home)
                    SettingsSection.SMART_HOME -> app.stova.ha.ui.HaSettings(container)
                    SettingsSection.HOME_BUTTON -> HomeButtonSettings(container)
                    SettingsSection.APPEARANCE -> AppearanceSettings(container, profile, pinnedPreview = true, onEditRows = { section = SettingsSection.HOME })
                    SettingsSection.ABOUT -> AboutSettings()
                }
            }
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
