package app.stova.core.ui

import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.assumeScreensEnabled
import app.stova.saveScreen
import app.stova.core.theme.StovaTheme
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
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SoftFocusScreensTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()

    @Test fun legacyProfilesAndReducedMotionUseAVisibleSurfaceWithoutAnAccentFrame() {
        assumeScreensEnabled()
        val source=MutableInteractionSource()
        var prefs by mutableStateOf(AppearancePrefs(focusEffect=FocusEffect.RING))
        var accent=0
        compose.setContent { StovaTheme(appearance=prefs) {
            accent=MaterialTheme.colorScheme.primary.toArgb()
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(48.dp),
                verticalArrangement=Arrangement.spacedBy(24.dp)) {
                Box(Modifier.size(248.dp,140.dp).focusOutline(source,MaterialTheme.shapes.medium,zoom=true)
                    .background(MaterialTheme.colorScheme.surfaceVariant,MaterialTheme.shapes.medium).padding(24.dp)) { Text("Artwork") }
                Box(Modifier.size(400.dp,56.dp).focusOutline(source,MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.medium).padding(16.dp)) { Text("Menu choice") }
            }
        } }
        val before=BitmapFactory.decodeFile(compose.saveScreen("focus-calm-unfocused").path)
        TestScope(UnconfinedTestDispatcher()).launch { source.emit(FocusInteraction.Focus()) }
        compose.waitForIdle();compose.mainClock.advanceTimeBy(300)
        val focused=BitmapFactory.decodeFile(compose.saveScreen("focus-calm-focused").path)
        compose.runOnIdle { prefs=prefs.copy(reduceMotion=true) }
        val reduced=BitmapFactory.decodeFile(compose.saveScreen("focus-calm-reduced-motion").path)
        fun pixels(bitmap:android.graphics.Bitmap)=IntArray(bitmap.width*bitmap.height).also { bitmap.getPixels(it,0,bitmap.width,0,0,bitmap.width,bitmap.height) }
        val initial=pixels(before)
        for(bitmap in listOf(focused,reduced)) {
            val actual=pixels(bitmap)
            assertTrue("focused surface remains visible",actual.indices.count { actual[it]!=initial[it] }>100)
            assertEquals("no solid accent frame",0,actual.count { it==accent })
        }
    }
}
