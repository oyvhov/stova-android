package app.stova.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import app.stova.R
import app.stova.core.ui.StovaIcons

enum class Destination(@StringRes val label: Int, val icon: () -> ImageVector) {
    HOME(R.string.nav_home, { StovaIcons.Home }),
    APPS(R.string.nav_apps, { StovaIcons.Apps }),
    SETTINGS(R.string.nav_settings, { StovaIcons.Settings }),
}
