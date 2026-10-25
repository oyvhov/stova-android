package app.stova.settings

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import app.stova.core.theme.StovaLayout

/** Two explicit focus areas: arrows never choose a neighbouring section by screen geometry. */
@Composable
internal fun SettingsLayout(
    selected: SettingsSection,
    onSelect: (SettingsSection) -> Unit,
    onSystemSettings: () -> Unit,
    content: @Composable ColumnScope.(SettingsSection, (SettingsSection) -> Unit) -> Unit,
) {
    val tabs = remember { SettingsSection.entries.associateWith { FocusRequester() } }
    val pane = remember { FocusRequester() }
    var enterSection by remember { mutableStateOf<SettingsSection?>(null) }
    fun select(section: SettingsSection) { enterSection = null; onSelect(section) }
    fun enter(section: SettingsSection) {
        // Cross-links can remove the currently focused control. Park focus on the stable
        // destination tab first; otherwise Compose falls back to the first tab (Profile),
        // whose onFocused selects Profile before the new pane has attached.
        tabs.getValue(section).requestFocus()
        onSelect(section)
        enterSection = section
    }

    LaunchedEffect(Unit) { tabs.getValue(selected).requestFocus() }
    LaunchedEffect(enterSection, selected) {
        if (enterSection != selected) return@LaunchedEffect
        // A quick Right may arrive in the same frame that selected the new section.
        repeat(8) {
            withFrameNanos { }
            if (enterSection != selected) return@LaunchedEffect
            if (runCatching { pane.requestFocus(FocusDirection.Enter) }.getOrDefault(false)) {
                enterSection = null
                return@LaunchedEffect
            }
        }
        enterSection = null // About has no controls: keep focus on its tab.
    }

    Row(Modifier.fillMaxSize().padding(StovaLayout.TvSafeEdge), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        SettingsNavigation(selected, ::select, onSystemSettings, tabs, ::enter,
            onCancelEntry = { enterSection = null })
        // Keep only the selected pane in the focus tree. An outgoing animated pane must not
        // compete for Up/Down while the user is already selecting the next section.
        key(selected) {
            Column(
                Modifier.weight(1f).fillMaxHeight()
                    .focusRequester(pane)
                    .focusProperties {
                        onExit = {
                            when (requestedFocusDirection) {
                                FocusDirection.Left -> tabs.getValue(selected).requestFocus()
                                FocusDirection.Up, FocusDirection.Down -> cancelFocusChange()
                                else -> Unit
                            }
                        }
                    }
                    .focusGroup()
                    .then(if (selected == SettingsSection.APPEARANCE) Modifier else Modifier.verticalScroll(rememberScrollState())),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) { content(selected, ::enter) }
        }
    }
}
