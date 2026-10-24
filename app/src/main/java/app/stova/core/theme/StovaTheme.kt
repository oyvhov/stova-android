package app.stova.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalReduceMotion = staticCompositionLocalOf { false }
val LocalStrongFocus = staticCompositionLocalOf { false }

/**
 * Every Material role is assigned, so no default purple leaks into a control. The values match
 * Spole's `ReelstackColors` with the FOREST mood and the lime accent.
 */
private val StovaColorScheme = darkColorScheme(
    primary = StovaColors.Primary,
    onPrimary = StovaColors.Ink,
    primaryContainer = StovaColors.PrimaryContainer,
    onPrimaryContainer = StovaColors.PrimarySoft,
    secondary = StovaColors.PrimarySoft,
    onSecondary = StovaColors.Ink,
    secondaryContainer = StovaColors.Raised,
    onSecondaryContainer = StovaColors.PrimarySoft,
    tertiary = StovaColors.Success,
    onTertiary = StovaColors.Ink,
    tertiaryContainer = StovaColors.Raised,
    onTertiaryContainer = StovaColors.Success,
    background = StovaColors.Ink,
    onBackground = StovaColors.Text,
    surface = StovaColors.Surface,
    onSurface = StovaColors.Text,
    surfaceVariant = StovaColors.Raised,
    onSurfaceVariant = StovaColors.Muted,
    surfaceDim = StovaColors.Ink,
    surfaceBright = StovaColors.Raised,
    surfaceContainerLowest = StovaColors.Ink,
    surfaceContainerLow = StovaColors.Surface,
    surfaceContainer = StovaColors.Surface,
    surfaceContainerHigh = StovaColors.Raised,
    surfaceContainerHighest = StovaColors.Raised,
    outline = StovaColors.Outline,
    outlineVariant = StovaColors.Divider,
    inverseSurface = StovaColors.Text,
    inverseOnSurface = StovaColors.Ink,
    inversePrimary = StovaColors.PrimaryContainer,
    error = StovaColors.Warning,
    onError = StovaColors.Ink,
    errorContainer = StovaColors.Raised,
    onErrorContainer = StovaColors.Warning,
)

private val StovaMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(StovaShapes.ExtraSmall),
    small = RoundedCornerShape(StovaShapes.Small),
    medium = RoundedCornerShape(StovaShapes.Medium),
    large = RoundedCornerShape(StovaShapes.Large),
    extraLarge = RoundedCornerShape(StovaShapes.ExtraLarge),
)

private val StovaTypography = stovaTypography()

@Composable
fun StovaTheme(
    reduceMotion: Boolean = false,
    strongFocus: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = StovaColorScheme,
        shapes = StovaMaterialShapes,
        typography = StovaTypography,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides StovaColors.Text,
            LocalReduceMotion provides reduceMotion,
            LocalStrongFocus provides strongFocus,
            content = content,
        )
    }
}
