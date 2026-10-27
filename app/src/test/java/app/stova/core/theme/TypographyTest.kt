package app.stova.core.theme

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.R
import app.stova.TV_QUALIFIERS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class TypographyTest {
    private val type = stovaTypography()

    @Test fun titlesAndTheClockUseTheDisplayFaceAndReadingTextStaysPlain() {
        for (style in listOf(type.displaySmall, type.headlineLarge, type.headlineMedium, type.headlineSmall, type.titleLarge))
            assertEquals(StovaFonts.Display, style.fontFamily)
        for (style in listOf(type.titleMedium, type.titleSmall, type.bodyLarge, type.bodyMedium, type.bodySmall,
            type.labelLarge, type.labelMedium, type.labelSmall))
            assertEquals(FontFamily.SansSerif, style.fontFamily)
    }

    @Test fun everyWeightTheTitlesAndClockAskForIsSetOnTheVariableAxis() {
        // A weight without its own entry would be faked by the renderer instead of drawn by the font.
        val fonts = (StovaFonts.Display as FontListFontFamily).fonts
        val weights = fonts.map { it.weight }.toSet()
        assertTrue(weights.containsAll(listOf(FontWeight.Thin, FontWeight.Light, FontWeight.Normal,
            FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)))
        for (font in fonts) assertEquals(R.font.outfit, (font as ResourceFont).resId)
    }

    @Test fun theFontShipsWithTheApp() {
        val face: Typeface? = ResourcesCompat.getFont(ApplicationProvider.getApplicationContext(), R.font.outfit)
        assertNotNull(face)
        val licence = ApplicationProvider.getApplicationContext<android.app.Application>().assets
            .open("licenses/outfit-OFL.txt").bufferedReader().readText()
        assertTrue(licence.contains("SIL Open Font License"))
    }
}
