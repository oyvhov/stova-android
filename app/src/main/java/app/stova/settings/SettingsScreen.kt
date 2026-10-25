package app.stova.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.stova.AppContainer
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.StovaShapes
import app.stova.core.ui.StovaButton
import app.stova.core.ui.focusOutline
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
    Row(Modifier.fillMaxSize().padding(StovaLayout.TvSafeEdge), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Column(Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            SettingsSection.entries.forEach { entry ->
                SectionTab(stringResource(entry.label), entry == section) { section = entry }
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            when (section) {
                SettingsSection.PROFILE -> ProfileSettings(container, profile)
                SettingsSection.ACCOUNTS -> AccountsSettings(profile, container)
                SettingsSection.HOME -> HomeRowSettings(container, profile, home)
                SettingsSection.SMART_HOME -> app.stova.ha.ui.HaSettings(container)
                SettingsSection.HOME_BUTTON -> HomeButtonSettings(container)
                SettingsSection.APPEARANCE -> AppearanceSettings(container, profile)
                SettingsSection.ABOUT -> AboutSettings()
            }
        }
    }
}

@Composable
private fun SectionTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.large
    Box(
        Modifier.fillMaxWidth()
            .heightIn(min = 56.dp)
            .focusOutline(source, shape)
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .selectable(selected, source, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** A kid's settings are one door: back to the profile picker. */
@Composable
private fun KidSettings(onSwitchProfile: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        StovaButton(stringResource(R.string.nav_profile), onSwitchProfile, primary = true)
    }
}
