package app.stova.apps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.core.ui.SheetHeader
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppMenuArtworkTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun compactHeaderKeepsBothEndsOfAWideAppLogo() {
        val logo=Bitmap.createBitmap(320,100,Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GREEN)
            Canvas(this).apply {
                drawRect(0f,0f,24f,100f,Paint().apply {color=Color.RED})
                drawRect(296f,0f,320f,100f,Paint().apply {color=Color.BLUE})
            }
        }.asImageBitmap()
        compose.setContent {StovaTheme(reduceMotion=true) {
            SheetHeader("Spole",compact=true,art={AppSheetImage(logo,null)})
        }}
        compose.waitForIdle()
        val view=compose.activity.window.decorView
        val image=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        compose.runOnUiThread {view.draw(Canvas(image))}
        var left=0;var right=0
        for(y in 0 until image.height) for(x in 0 until image.width) {
            when(image.getPixel(x,y)) {Color.RED->left++;Color.BLUE->right++}
        }
        assertTrue("The beginning of the app logo is cropped ($left pixels)",left>20)
        assertTrue("The end of the app logo is cropped ($right pixels)",right>20)
    }
}
