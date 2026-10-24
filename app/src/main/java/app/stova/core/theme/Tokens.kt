package app.stova.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

/**
 * Spole's matte FOREST surfaces with everything on top of them muted further: a sage-lime accent
 * instead of Spole's bright lime, softer text and desaturated status colours. Surfaces are
 * separated by fill, never by glass or a line. The accent is for selection, progress and direct
 * actions only — never for counts, labels or status. Text keeps at least 6:1 against Surface.
 */
object StovaColors {
    val Ink = Color(0xFF101211)
    val Surface = Color(0xFF191C19)
    val Raised = Color(0xFF2E332E)
    val Text = Color(0xFFE9E9E2)
    val Muted = Color(0xFF9AA299)
    val Primary = Color(0xFFBAC79C)
    val PrimaryContainer = Color(0xFF30372A)
    val PrimarySoft = Color(0xFFD2D9C2)
    val Success = Color(0xFF84BE9C)
    val Caution = Color(0xFFD8B97E)
    val Warning = Color(0xFFD48E8E)
    val Outline = Color(0xFF646E63)
    val Divider = Color(0xFF3D443C)
}

/** Five steps, and every corner in the app is one of them. */
object StovaShapes {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 24.dp
}

object StovaLayout {
    /** About 5 % of a 1080p panel; sets that still overscan keep the last row on screen. */
    val TvSafeEdge = 48.dp
    val Gutter = 24.dp
    val RailCollapsed = 80.dp
    val RailExpanded = 200.dp
    val PosterWidth = 132.dp
    val PosterHeight = 198.dp
    val WideWidth = 248.dp
    val WideHeight = 139.5.dp
    val RowGap = 16.dp
    val SectionTop = 22.dp
    val SectionBottom = 10.dp
    const val TransitionMillis = 180
    const val FocusFadeMillis = 110
}

/** Spole's eight accent hues, desaturated to sit with the muted palette, as profile avatar colours. */
@Serializable
enum class AvatarColor(val argb: Long) {
    LIME(0xFFBAC79C),
    OCEAN(0xFF9DB9CC),
    IRIS(0xFFB4ACD3),
    CORAL(0xFFD3AEA3),
    GOLD(0xFFD4BE8F),
    MINT(0xFF97C4B2),
    ROSE(0xFFCFAFC0),
    PEARL(0xFFD3D6DC),
}

val AvatarColor.color: Color get() = Color(argb)
