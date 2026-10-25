package app.stova.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import app.stova.profiles.AppearancePrefs
import app.stova.profiles.Corners
import app.stova.profiles.seasonal

val LocalSeasonDate = staticCompositionLocalOf { java.time.LocalDate.now() }

val LocalReduceMotion = staticCompositionLocalOf { false }
val LocalStrongFocus = staticCompositionLocalOf { false }
val LocalAppearance = staticCompositionLocalOf { AppearancePrefs() }

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

/** All surfaces, scrims and controls share the selected mood, including HA's Material roles. */
fun stovaColorScheme(theme: VisualTheme, accent: app.stova.profiles.Accent) = StovaColorScheme.copy(
    primary = accent.color, onPrimary = theme.background,
    primaryContainer = accent.container, onPrimaryContainer = accent.color,
    secondary = theme.mutedHigh, onSecondary = theme.background,
    secondaryContainer = theme.raised, onSecondaryContainer = theme.mutedHigh,
    background = theme.background, surface = theme.surface, surfaceVariant = theme.raised,
    onSurfaceVariant = theme.muted, surfaceDim = theme.background, surfaceBright = theme.raised,
    surfaceContainerLowest = theme.background, surfaceContainerLow = theme.surface,
    surfaceContainer = theme.surface, surfaceContainerHigh = theme.raised, surfaceContainerHighest = theme.raised,
    tertiaryContainer = theme.raised, errorContainer = theme.raised,
    onTertiary = theme.background, onError = theme.background,
    outline = theme.outline, outlineVariant = theme.divider, inverseOnSurface = theme.background,
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
    appearance: AppearancePrefs = AppearancePrefs(),
    date: java.time.LocalDate = LocalSeasonDate.current,
    content: @Composable () -> Unit,
) {
    val shown = appearance.seasonal(date)
    MaterialTheme(
        colorScheme = stovaColorScheme(shown.visualTheme, shown.accent),
        shapes = if (appearance.corners == Corners.SHARP) Shapes(
            RoundedCornerShape(0.dp), RoundedCornerShape(0.dp), RoundedCornerShape(0.dp),
            RoundedCornerShape(0.dp), RoundedCornerShape(0.dp)) else StovaMaterialShapes,
        typography = StovaTypography,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides StovaColors.Text,
            LocalReduceMotion provides (reduceMotion || appearance.reduceMotion),
            LocalStrongFocus provides (strongFocus || appearance.strongFocus),
            LocalAppearance provides shown,
            LocalSeasonDate provides date,
            content = content,
        )
    }
}
