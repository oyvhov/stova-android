package app.stova.home

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.R
import app.stova.TV_QUALIFIERS
import app.stova.apps.*
import app.stova.core.theme.StovaTheme
import app.stova.media.model.*
import app.stova.media.ui.MediaCard
import app.stova.profiles.*
import app.stova.saveScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SmallCardTextTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun smallestWideFallbacksKeepReadableNamesWithDoubleTextAndOptionalCaptions() {
        val context = compose.activity
        val component = ComponentName(context.packageName, "demo.Main")
        shadowOf(context.packageManager).addActivityIfNotPresent(component).apply {
            applicationInfo = context.applicationInfo
            banner = 0; applicationInfo.banner = 0
            icon = R.drawable.ic_stova_foreground
        }
        val app = InstalledApp(component.flattenToString(), component.packageName, component.className, "Appnamn som er langt", true)
        val art = AppArtCache(context)
        var captions by mutableStateOf(false)
        var fit by mutableStateOf(1f)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f),
                LocalRowStyle provides RowStyle(RowFormat.SMALL_WIDE, ArtworkSize.TINY), LocalArtworkScale provides fit) {
                StovaTheme(appearance = AppearancePrefs(showTitles = captions), reduceMotion = true) {
                    Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AppTile(app, art, {}, {}, Modifier.testTag("app"))
                        MediaCard(MediaItem("f", "a", ItemType.MOVIE, "Filmnamn som er langt"), false, {}, {}, {}, Modifier.testTag("film"))
                    }
                }
            }
        }
        // Wait for the actual icon, so the fixed icon + name fallback cannot escape this check.
        kotlinx.coroutines.runBlocking { assertNotNull(art.icon(app)) }
        compose.waitForIdle()
        for (scale in listOf(1f, .7f, .6f)) for (enabled in listOf(false, true)) {
            compose.runOnIdle { captions = enabled; fit = scale }
            if (System.getProperty("stova.screens") == "true") compose.saveScreen("tiny-cards-double-text-$scale-$enabled")
            for ((title, tag) in listOf(app.label to "app", "Filmnamn som er langt" to "film")) {
                val card = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
                val texts = compose.onAllNodesWithText(title, useUnmergedTree = true)
                assertTrue(texts.fetchSemanticsNodes().isNotEmpty())
                repeat(texts.fetchSemanticsNodes().size) { index ->
                    val text = texts[index].assertIsDisplayed()
                    val bounds = text.getUnclippedBoundsInRoot()
                    assertTrue(bounds.left >= card.left && bounds.right <= card.right)
                    assertTrue(bounds.top >= card.top && bounds.bottom <= card.bottom)
                    val layouts = mutableListOf<TextLayoutResult>()
                    text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    val layout = layouts.single()
                    assertTrue("$tag scale=$scale caption=$enabled: line bottom ${layout.getLineBottom(0)}, height ${layout.size.height}", layout.getLineBottom(0) <= layout.size.height + 1f)
                }
            }
        }
    }
}
