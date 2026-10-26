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

    @Test fun defaultFocusGentlyRaisesArtworkAndMenuControls() {
        val source = MutableInteractionSource()
        compose.setContent { StovaTheme {
            androidx.compose.foundation.layout.Column(Modifier.size(200.dp)) {
                Box(Modifier.size(100.dp).focusOutline(source, MaterialTheme.shapes.medium, zoom = true).testTag("art"))
                Box(Modifier.size(100.dp).focusOutline(source, MaterialTheme.shapes.medium).testTag("menu"))
            }
        } }
        val before = compose.onNodeWithTag("art").fetchSemanticsNode().boundsInRoot
        TestScope(kotlinx.coroutines.test.UnconfinedTestDispatcher()).launch { source.emit(FocusInteraction.Focus()) }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(300)
        val art = compose.onNodeWithTag("art").fetchSemanticsNode().boundsInRoot
        val menu = compose.onNodeWithTag("menu").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("artwork grows clearly, by about 6 %", art.width > before.width * 1.05f && art.width < before.width * 1.07f)
        org.junit.Assert.assertTrue("menus use the same calm treatment", menu.width > before.width && menu.width < art.width)
        org.junit.Assert.assertTrue("focus lifts the artwork", art.top < before.top)
    }

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
        compose.runOnIdle { prefs = prefs.copy(reduceMotion = false, focusEffect = FocusEffect.OUTLINE) }
        org.junit.Assert.assertEquals(200f, compose.onNodeWithTag("art").fetchSemanticsNode().boundsInRoot.width, 1f)
    }
}
