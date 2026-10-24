package app.stova.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

/**
 * Spole's FOREST palette, unchanged. Matte surfaces separated by fill, never by glass or a line.
 * Lime is for selection, progress and direct actions only — never for counts, labels or status.
 */
object StovaColors {
    val Ink = Color(0xFF101211)
    val Surface = Color(0xFF191C19)
    val Raised = Color(0xFF2E332E)
    val Text = Color(0xFFF3F3EC)
    val Muted = Color(0xFFA4ADA3)
    val Primary = Color(0xFFD5F478)
    val PrimaryContainer = Color(0xFF344024)
    val PrimarySoft = Color(0xFFDCE9BD)
    val Success = Color(0xFF56D993)
    val Caution = Color(0xFFFFC66D)
    val Warning = Color(0xFFFF7A7D)
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

/** The eight accent palettes Spole offers, reused as profile avatar colours. */
@Serializable
enum class AvatarColor(val argb: Long) {
    LIME(0xFFD5F478),
    OCEAN(0xFF96D5FF),
    IRIS(0xFFCAB8FF),
    CORAL(0xFFFFB5A0),
    GOLD(0xFFFFD478),
    MINT(0xFF86E2C2),
    ROSE(0xFFF4ABD0),
    PEARL(0xFFE3E7EF),
}

val AvatarColor.color: Color get() = Color(argb)
