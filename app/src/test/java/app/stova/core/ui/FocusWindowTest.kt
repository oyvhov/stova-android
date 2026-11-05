package app.stova.core.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.profiles.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
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
class FocusWindowTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val source=MutableInteractionSource()
    private fun focus() {
        TestScope(UnconfinedTestDispatcher()).launch {source.emit(FocusInteraction.Focus())}
        compose.waitForIdle();compose.mainClock.advanceTimeBy(350)
    }
    private fun pixels():IntArray=compose.runOnIdle {
        val view=compose.activity.window.decorView
        val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        view.draw(Canvas(b))
        IntArray(b.width*b.height).also {b.getPixels(it,0,b.width,0,0,b.width,b.height);b.recycle()}
    }
    @Test fun inactiveWindowsHideRetainedFocusAndRestoreItWhenActiveAgain() {
        var active by mutableStateOf(true)
        compose.setContent {StovaTheme(reduceMotion=true,appearance=AppearancePrefs(focusEffect=FocusEffect.OUTLINE)) {
            val platform=LocalWindowInfo.current
            val window=remember(platform) {object:WindowInfo by platform {override val isWindowFocused get()=active}}
            CompositionLocalProvider(LocalWindowInfo provides window) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(48.dp)) {
                    Box(Modifier.size(120.dp).focusOutline(source,MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface))
                }
            }
        }}
        val plain=pixels();focus();val focused=pixels()
        assertFalse(plain.contentEquals(focused))
        compose.runOnIdle {active=false}
        assertArrayEquals("The covered window must lose its focus decoration",plain,pixels())
        compose.runOnIdle {active=true}
        assertArrayEquals(focused,pixels())
    }
    @Test fun theDefaultAlsoBrightensArtworkWithoutAFrame() {
        compose.setContent {StovaTheme(reduceMotion=true) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(48.dp)) {
                Box(Modifier.size(120.dp).focusOutline(source,MaterialTheme.shapes.large,zoom=true)
                    .background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.large))
            }
        }}
        val before=pixels();focus()
        assertFalse("Default artwork focus needs a light cue as well as movement",before.contentEquals(pixels()))
    }
}
