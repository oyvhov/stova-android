package app.stova.core.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvScrollEdgeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun contentStaysVisibleInsideTheSafeAreaAndFadesAtThePhysicalEdge() {
        lateinit var view: View
        compose.setContent {
            view = LocalView.current
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                TvScrollColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
                    Box(Modifier.fillMaxWidth().height(2000.dp).background(Color.White))
                }
            }
        }
        compose.runOnIdle {
            val pixels = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(pixels))
            fun brightnessAboveBottom(dp: Float): Int {
                val y = view.height - (dp * view.resources.displayMetrics.density).toInt()
                return android.graphics.Color.red(pixels.getPixel(view.width / 2, y))
            }
            assertEquals("A scrolling row still draws 16 dp above the screen edge", 255, brightnessAboveBottom(16f))
            assertTrue("The last 8 dp soften the row into the background", brightnessAboveBottom(4f) in 60..195)
            pixels.recycle()
        }
    }
}
