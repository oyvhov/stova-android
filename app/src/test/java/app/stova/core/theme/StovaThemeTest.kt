package app.stova.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class StovaThemeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun schemeUsesMutedForestTokens() {
        lateinit var scheme: ColorScheme
        compose.setContent { StovaTheme { scheme = MaterialTheme.colorScheme } }
        // Spole's matte FOREST surfaces, with a muted sage-lime accent and softer text (user wish 25.10.2026).
        assertEquals(Color(0xFFBAC79C), scheme.primary)
        assertEquals(Color(0xFF30372A), scheme.primaryContainer)
        assertEquals(Color(0xFFE9E9E2), scheme.onBackground)
        assertEquals(Color(0xFF101211), scheme.background)
        assertEquals(Color(0xFF191C19), scheme.surface)
        assertEquals(Color(0xFF2E332E), scheme.surfaceVariant)
    }

    @Test fun avatarColoursAreMutedToo() {
        assertEquals(Color(0xFFBAC79C), AvatarColor.LIME.color)
        assertEquals(Color(0xFF9DB9CC), AvatarColor.OCEAN.color)
    }
}
