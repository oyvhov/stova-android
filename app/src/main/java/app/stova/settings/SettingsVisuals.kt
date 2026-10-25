package app.stova.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.stova.R
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.color
import app.stova.core.ui.StovaIcons

internal val SettingsSection.icon: ImageVector get() = when (this) {
    SettingsSection.HOME -> StovaIcons.Rows
    SettingsSection.APPEARANCE -> StovaIcons.Palette
    SettingsSection.ACCOUNTS -> StovaIcons.Link
    SettingsSection.SMART_HOME -> StovaIcons.Home
    SettingsSection.PROFILE -> StovaIcons.User
    SettingsSection.HOME_BUTTON -> StovaIcons.Toggle
    SettingsSection.ABOUT -> StovaIcons.Info
}
internal val SettingsSection.tone: Color get() = when (this) {
    SettingsSection.HOME -> AvatarColor.OCEAN.color
    SettingsSection.APPEARANCE -> AvatarColor.IRIS.color
    SettingsSection.ACCOUNTS -> AvatarColor.MINT.color
    SettingsSection.SMART_HOME -> AvatarColor.GOLD.color
    SettingsSection.HOME_BUTTON -> AvatarColor.CORAL.color
    else -> AvatarColor.PEARL.color
}
internal val SettingsSection.pageTitle: Int get() = when (this) {
    SettingsSection.HOME -> R.string.settings_title_home
    SettingsSection.PROFILE -> R.string.settings_title_profile
    SettingsSection.ACCOUNTS -> R.string.settings_title_accounts
    SettingsSection.SMART_HOME -> R.string.settings_title_smart_home
    SettingsSection.HOME_BUTTON -> R.string.settings_title_home_button
    SettingsSection.ABOUT -> R.string.settings_title_about
    SettingsSection.APPEARANCE -> R.string.settings_appearance
}
internal val SettingsSection.hint: Int get() = when (this) {
    SettingsSection.HOME -> R.string.settings_hint_home
    SettingsSection.PROFILE -> R.string.settings_hint_profile
    SettingsSection.ACCOUNTS -> R.string.settings_hint_accounts
    SettingsSection.SMART_HOME -> R.string.settings_hint_smart_home
    SettingsSection.HOME_BUTTON -> R.string.settings_hint_home_button
    SettingsSection.ABOUT -> R.string.settings_hint_about
    SettingsSection.APPEARANCE -> R.string.settings_appearance_intro
}
internal val AppearanceCategory.icon: ImageVector get() = when (this) {
    AppearanceCategory.THEME -> StovaIcons.Palette
    AppearanceCategory.HERO -> StovaIcons.Home
    AppearanceCategory.CARDS -> StovaIcons.Rows
    AppearanceCategory.BACKGROUND -> StovaIcons.Image
    AppearanceCategory.FOCUS -> StovaIcons.Focus
    AppearanceCategory.TOP_BAR -> StovaIcons.Clock
    AppearanceCategory.ICONS -> StovaIcons.Apps
    AppearanceCategory.PROFILE -> StovaIcons.User
}
internal val AppearanceCategory.tone: Color get() = when (this) {
    AppearanceCategory.THEME -> AvatarColor.IRIS.color
    AppearanceCategory.HERO -> AvatarColor.OCEAN.color
    AppearanceCategory.CARDS -> AvatarColor.MINT.color
    AppearanceCategory.FOCUS -> AvatarColor.GOLD.color
    AppearanceCategory.TOP_BAR -> AvatarColor.CORAL.color
    AppearanceCategory.ICONS -> AvatarColor.ROSE.color
    else -> AvatarColor.PEARL.color
}
