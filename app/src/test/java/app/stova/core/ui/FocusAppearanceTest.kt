package app.stova.core.ui

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.profiles.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class FocusAppearanceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun zoomAffectsArtworkOnlyAndReduceMotionDisablesIt() {
        val source = MutableInteractionSource()
        var prefs by mutableStateOf(AppearancePrefs(focusEffect = FocusEffect.ZOOM))
        compose.setContent { StovaTheme(appearance = prefs) {
            Box(Modifier.size(160.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Box(Modifier.size(100.dp).focusOutline(source, MaterialTheme.shapes.medium, zoom = true).testTag("art"))
            }
        } }
        org.junit.Assert.assertEquals(200f, compose.onNodeWithTag("art").fetchSemanticsNode().boundsInRoot.width, 1f)
        TestScope(kotlinx.coroutines.test.UnconfinedTestDispatcher()).launch { source.emit(FocusInteraction.Focus()) }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(250)
        org.junit.Assert.assertEquals(210f, compose.onNodeWithTag("art").fetchSemanticsNode().boundsInRoot.width, 1f)
        compose.runOnIdle { prefs = prefs.copy(reduceMotion = true) }
        org.junit.Assert.assertEquals(200f, compose.onNodeWithTag("art").fetchSemanticsNode().boundsInRoot.width, 1f)
        compose.runOnIdle { prefs = prefs.copy(reduceMotion = false, focusEffect = FocusEffect.RING) }
        org.junit.Assert.assertEquals(200f, compose.onNodeWithTag("art").fetchSemanticsNode().boundsInRoot.width, 1f)
    }
}
