package app.stova.media.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaColors
import app.stova.core.theme.StovaTheme
import app.stova.media.model.Ratings
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RatingRowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var view: android.view.View

    @Test fun norwegianAgeCodeShowsYearsWhileTextualRatingsKeepTheirMeaning() {
        val age = mutableIntStateOf(15)
        compose.setContent { StovaTheme { RatingRow(Ratings(official = if (age.intValue == 15) "NO-15" else "TV-MA")) } }
        compose.onNodeWithText("15 år").assertIsDisplayed()
        compose.runOnIdle { age.intValue = 0 }
        compose.onNodeWithText("TV-MA").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en-w960dp-h540dp-land-television-xhdpi")
    fun englishAgeUsesLocalizedYears() {
        val age = mutableIntStateOf(12)
        compose.setContent { StovaTheme { RatingRow(Ratings(official = age.intValue.toString())) } }
        compose.onNodeWithText("12 years").assertIsDisplayed()
        compose.runOnIdle { age.intValue = 1 }
        compose.onNodeWithText("1 year").assertIsDisplayed()
    }

    @Test fun metadataLetsTheBackdropShowThrough() {
        compose.setContent { StovaTheme {
            view = LocalView.current
            Box(Modifier.background(StovaColors.Raised)) { RatingRow(Ratings(7.8, 91, "12"), year = 2024) }
        } }
        val pixels = capture(compose.onNodeWithText("2024"))
        assertTrue("No opaque fact background behind the text", close(Color(pixels.getPixel(0, 0)), StovaColors.Raised))
    }

    @Test fun criticMarkChangesFromGreenRottenToRedFreshAtSixtyPercent() {
        val score = mutableIntStateOf(59)
        compose.setContent { StovaTheme {
            view = LocalView.current
            RatingRow(Ratings(criticPercent = score.intValue))
        } }
        assertInk("Rotten Tomatoes: roten", Color(0xFF8DB448))
        compose.runOnIdle { score.intValue = 60 }
        compose.onNodeWithContentDescription("Rotten Tomatoes: roten").assertDoesNotExist()
        assertInk("Rotten Tomatoes: fersk", Color(0xFFFA320A))
    }

    private fun assertInk(description: String, expected: Color) {
        val pixels = capture(compose.onNodeWithContentDescription(description).assertIsDisplayed())
        assertTrue("Visible status colour for $description", (0 until pixels.height).any { y ->
            (0 until pixels.width).any { x -> close(Color(pixels.getPixel(x, y)), expected) }
        })
    }

    // Robolectric does not deliver PixelCopy callbacks; draw the Compose host as Screens.kt does.
    private fun capture(node: SemanticsNodeInteraction): android.graphics.Bitmap {
        val bounds = node.fetchSemanticsNode().boundsInRoot
        val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
        compose.runOnIdle { view.draw(android.graphics.Canvas(bitmap)) }
        return android.graphics.Bitmap.createBitmap(bitmap, bounds.left.roundToInt(), bounds.top.roundToInt(),
            bounds.width.roundToInt(), bounds.height.roundToInt())
    }

    private fun close(actual: Color, expected: Color) =
        kotlin.math.abs(actual.red - expected.red) < 0.02f &&
        kotlin.math.abs(actual.green - expected.green) < 0.02f &&
        kotlin.math.abs(actual.blue - expected.blue) < 0.02f
}
