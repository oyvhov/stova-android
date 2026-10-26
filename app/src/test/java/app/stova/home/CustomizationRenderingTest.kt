package app.stova.home

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.core.ui.focusOutline
import app.stova.media.model.*
import app.stova.media.ui.MediaCard
import app.stova.profiles.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CustomizationRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fourRadiiReachEveryMaterialShape() {
        var prefs by mutableStateOf(AppearancePrefs())
        var radii = emptyList<Float>()
        compose.setContent { StovaTheme(appearance = prefs) {
            val density = LocalDensity.current
            radii = listOf(MaterialTheme.shapes.small, MaterialTheme.shapes.medium, MaterialTheme.shapes.large).map {
                with(density) { it.topStart.toPx(androidx.compose.ui.geometry.Size(500f,500f), density).toDp().value }
            }
        } }
        CornerRadius.entries.forEach { radius ->
            compose.runOnIdle { prefs = prefs.copy(cornerRadius = radius) }
            compose.waitForIdle()
            assertEquals(listOf(radius.dp.toFloat(), radius.dp.toFloat(), radius.dp.toFloat()), radii)
        }
    }

    @Test fun rowFormatAndSizeOverrideOnlyTheSelectedRow() {
        compose.setContent { StovaTheme(appearance = AppearancePrefs(cardSize = ArtworkSize.MEDIUM, showTitles = false)) {
            Column {
                CompositionLocalProvider(LocalRowStyle provides RowStyle(RowFormat.ICON, ArtworkSize.SMALL)) {
                    MediaCard(MediaItem("a","a",ItemType.MOVIE,"Icon"), true, {}, {}, {}, Modifier.testTag("custom"))
                }
                MediaCard(MediaItem("b","a",ItemType.MOVIE,"Wide"), false, {}, {}, {}, Modifier.testTag("default"))
            }
        } }
        compose.onNodeWithTag("custom").assertWidthIsEqualTo(112.5.dp).assertHeightIsEqualTo(112.5.dp)
        compose.onNodeWithTag("default").assertWidthIsEqualTo(248.dp).assertHeightIsEqualTo(139.5.dp)
    }

    @Test fun lightUpDimsOnlyUnfocusedSiblingsAndRingZoomScalesArtwork() {
        val source = MutableInteractionSource()
        var prefs by mutableStateOf(AppearancePrefs(focusEffect = FocusEffect.RING_ZOOM, seasonChoice = SeasonChoice.NONE))
        var rowFocused by mutableStateOf(true)
        lateinit var root: android.view.View
        compose.setContent { StovaTheme(appearance = prefs) {
            root = LocalView.current.rootView
            CompositionLocalProvider(LocalRowHasFocus provides rowFocused) {
                Box(Modifier.size(160.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Box(Modifier.size(100.dp).focusOutline(source, MaterialTheme.shapes.medium, zoom = true)
                        .background(MaterialTheme.colorScheme.onSurface).testTag("art"))
                }
            }
        } }
        val focus = FocusInteraction.Focus()
        TestScope(UnconfinedTestDispatcher()).launch { source.emit(focus) }
        compose.waitForIdle(); compose.mainClock.advanceTimeBy(250)
        assertEquals(210f, compose.onNodeWithTag("art").fetchSemanticsNode().boundsInRoot.width, 1f)
        compose.runOnIdle { prefs = prefs.copy(reduceMotion = true) }
        compose.onNodeWithTag("art").assertWidthIsEqualTo(100.dp)
        TestScope(UnconfinedTestDispatcher()).launch { source.emit(FocusInteraction.Unfocus(focus)) }
        compose.runOnIdle { prefs = prefs.copy(focusEffect = FocusEffect.LIGHT_UP) }
        fun pixel(): Int {
            compose.waitForIdle()
            val center = compose.onNodeWithTag("art").fetchSemanticsNode().boundsInRoot.center
            val bitmap = android.graphics.Bitmap.createBitmap(root.width, root.height, android.graphics.Bitmap.Config.ARGB_8888)
            compose.runOnUiThread { root.draw(android.graphics.Canvas(bitmap)) }
            return bitmap.getPixel(center.x.toInt(), center.y.toInt())
        }
        val dim = pixel()
        compose.runOnIdle { rowFocused = false }
        val normal = pixel()
        assertNotEquals(normal, dim)
    }

    @Test fun progressCanBeThinThickOrHidden() {
        var prefs by mutableStateOf(AppearancePrefs())
        compose.setContent { StovaTheme(appearance = prefs) {
            MediaCard(MediaItem("a","a",ItemType.MOVIE,"Film",progress=0.5f), false, {}, {}, {})
        } }
        compose.onNodeWithTag("card-progress", useUnmergedTree = true).assertHeightIsEqualTo(4.dp)
        compose.runOnIdle { prefs = prefs.copy(progressStyle = ProgressStyle.THIN) }
        compose.onNodeWithTag("card-progress", useUnmergedTree = true).assertHeightIsEqualTo(2.dp)
        compose.runOnIdle { prefs = prefs.copy(progressStyle = ProgressStyle.HIDDEN) }
        compose.onNodeWithTag("card-progress", useUnmergedTree = true).assertDoesNotExist()
    }
}
