package app.stova.core.theme

import androidx.compose.ui.graphics.luminance
import app.stova.profiles.Accent
import org.junit.Assert.*
import org.junit.Test

class ThemePaletteTest {
    @Test fun allTenAccentsHaveSevenToOneAgainstEveryThemeBackground() {
        assertEquals(10, Accent.entries.size)
        assertEquals(6, VisualTheme.entries.size)
        for (theme in VisualTheme.entries) for (accent in Accent.entries) {
            val contrast = (accent.color.luminance() + .05f) / (theme.background.luminance() + .05f)
            assertTrue("$theme $accent contrast $contrast", contrast >= 7f)
        }
    }
    @Test fun christmasIsATrueRedOverDeepBerrySurfacesNotCoral() {
        val red = Accent.HOLLY.color
        assertTrue("$red", red.red > .95f && red.red - maxOf(red.green, red.blue) > .5f && kotlin.math.abs(red.green - red.blue) < .05f)
        val berry = Accent.HOLLY.container
        assertTrue("$berry", berry.red > .35f && berry.green < .12f && berry.blue < .15f)
        assertEquals(berry, stovaColorScheme(VisualTheme.NOEL, Accent.HOLLY).primaryContainer)
    }
    @Test fun everyMaterialSurfaceAndNeutralComesFromTheChosenMood() {
        for (theme in VisualTheme.entries) {
            val scheme = stovaColorScheme(theme, Accent.OCEAN)
            assertEquals(theme.background, scheme.background)
            assertEquals(theme.surface, scheme.surface)
            assertEquals(theme.raised, scheme.surfaceContainerHigh)
            assertEquals(theme.muted, scheme.onSurfaceVariant)
            assertEquals(theme.outline, scheme.outline)
            assertEquals(theme.divider, scheme.outlineVariant)
            assertEquals(Accent.OCEAN.color, scheme.primary)
        }
    }
}
