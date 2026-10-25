package app.stova

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import java.io.File
import org.junit.Assume.assumeTrue

/** Screenshot tests run only with `-Pstova.screens=true`; they render, they do not assert. */
fun assumeScreensEnabled() = assumeTrue(System.getProperty("stova.screens") == "true")

/**
 * Writes the activity's window to `app/build/screens/<name>.png` at the device's full pixel size.
 * Draws the decor view straight into a bitmap: Compose's captureToImage waits for a frame callback
 * that Robolectric never delivers.
 */
fun AndroidComposeTestRule<*, out ComponentActivity>.saveScreen(name: String, windowView: android.view.View = activity.window.decorView): File {
    waitForIdle()
    val view = windowView
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    val out = File("build/screens/$name.png").apply { parentFile?.mkdirs() }
    out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return out
}
