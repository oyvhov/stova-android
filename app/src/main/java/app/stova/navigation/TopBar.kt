package app.stova.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.LocalReduceMotion
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.StovaShapes
import app.stova.core.ui.Clock
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.focusOutline
import app.stova.profiles.AvatarBadge
import app.stova.profiles.Profile
import app.stova.profiles.season

/**
 * The only chrome on the home screen: clock, Apps, Settings and the profile, top right. The icons
 * name themselves only while focused, so the resting screen is content and nothing else.
 * [leading] is where the smart-home status chips go (Plan 4).
 */
@Composable
fun TopBar(
    profile: Profile,
    onApps: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
    onSmartHome: (() -> Unit)? = null,
    leading: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = StovaLayout.TvSafeEdge).padding(top = 28.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val appearance = app.stova.core.theme.LocalAppearance.current
        val rowScope = this
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { if (appearance.showHaChips) leading(rowScope) }
        if (appearance.seasonalDecor != app.stova.profiles.SeasonalDecor.OFF) {
            if (appearance.showCountdown && androidx.compose.ui.platform.LocalDensity.current.fontScale < 1.5f)
                app.stova.home.SeasonalCountdown(appearance.season())
            app.stova.home.SeasonalMark(appearance.season())
        }
        Clock(Modifier.padding(end = 16.dp))
        if(onSmartHome!=null && appearance.showSmartHomeButton) TopBarButton(StovaIcons.Home,stringResource(R.string.nav_smart_home),onSmartHome)
        if (appearance.showAppsButton) TopBarButton(StovaIcons.Apps, stringResource(R.string.nav_apps), onApps)
        TopBarButton(StovaIcons.Settings, stringResource(R.string.nav_settings), onSettings)
        if (appearance.showProfile) ProfileButton(profile, onProfile)
    }
}

@Composable
private fun TopBarButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = MaterialTheme.shapes.extraLarge
    val millis = if (LocalReduceMotion.current) 0 else 140
    val labelAlpha by animateFloatAsState(if (focused) 1f else 0f, tween(millis))
    Box(
        Modifier
            .size(48.dp)
            .focusOutline(source, shape)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { text = AnnotatedString(label) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        // Draw the label below its fixed slot; focus must not move the clock or any button.
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = 48.dp)
                .wrapContentWidth(unbounded = true).alpha(labelAlpha).clearAndSetSemantics { },
        )
    }
}

@Composable
private fun ProfileButton(profile: Profile, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val label = stringResource(R.string.nav_profile)
    Box(
        Modifier
            .size(48.dp)
            .focusOutline(source, CircleShape)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { text = AnnotatedString(label) },
        contentAlignment = Alignment.Center,
    ) {
        AvatarBadge(profile, 36.dp)
    }
}
