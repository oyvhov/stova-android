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
    val Ember = Color(0xFFFFA74F)
    val Snow = Color(0xFFE8F2FF)
    val Spider = Color(0xFFD8D1E9)
    val Divider = Color(0xFF3D443C)
    /** Christmas lights in turn: red, green, gold, blue and warm white, on a dark green wire. */
    val Bulbs = listOf(Color(0xFFFF5A52), Color(0xFF4FD27A), Color(0xFFFFC94D), Color(0xFF5AA9FF), Color(0xFFFFF0D2))
    val Wire = Color(0xFF22352A)
    val Fir = Color(0xFF1F4A33)
    /** Halloween's low mist. */
    val Mist = Color(0xFF9A86B8)
}

/** Critic status colours stay independent of the profile's action accent. */
object RatingColors {
    val Fresh = Color(0xFFFA320A)
    val Rotten = Color(0xFF8DB448)
}

/** Shared calm focus, sized to stay within the TV gutters and menu spacing. */
/** Focus must read from the sofa: the card grows clearly, its neighbours step back. No glow. */
object StovaFocus {
    const val ArtworkScale = 1.06f
    const val ControlScale = 1.02f
    const val SurfaceAlpha = 0.16f
    /** The rest of the row with focus, and the titles under those cards. */
    const val DimmedArtwork = 0.70f
    const val DimmedTitle = 0.55f
    val Lift = 2.dp
}

/** A title under a card: full strength on the focused card or while its row is idle, dimmed beside focus. */
fun cardTitleAlpha(focused: Boolean, rowHasFocus: Boolean): Float =
    if (focused || !rowHasFocus) 1f else StovaFocus.DimmedTitle

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
    /** Wide enough for "Smarthus og kontoar" on one line beside its badge. */
    val SettingsNavigationWidth = 240.dp
    /** A name and its one-line description. */
    val SettingsNavigationMinHeight = 60.dp
    val SettingsRowMinHeight = 44.dp
    val SettingsControlMinHeight = 48.dp
    val PageIconSize = 40.dp
    /** Home drawn small beside a look page's title: a third of the television's width. */
    val SettingsPreviewWidth = 304.dp
    val CategoryIconSize = 34.dp
    val RowSourceMaxWidth = 200.dp
    val RailCollapsed = 80.dp
    val RailExpanded = 200.dp
    val PosterWidth = 132.dp
    val PosterHeight = 198.dp
    val WideWidth = 248.dp
    val WideHeight = 139.5.dp
    val RowGap = 16.dp
    val SectionTop = 22.dp
    val SectionBottom = 10.dp
    /** The long-press panel from the right: about two fifths of a 960 dp wide screen. */
    val SheetWidth = 400.dp
    val SheetPadding = 28.dp
    /** Less than the 48 dp side edge: overscan eats far less height, and the artwork needs it. */
    val SheetTop = 32.dp
    val SheetGap = 6.dp
    val SheetRowMinHeight = 52.dp
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

/** Appearance accents share the same muted palette as profile avatars. */
val app.stova.profiles.Accent.color: Color get() = when (this) {
    // Jul: a true red at the edge of 7:1 on the darkest mood, not the old coral.
    app.stova.profiles.Accent.HOLLY -> Color(0xFFFF7575)
    app.stova.profiles.Accent.PUMPKIN -> Color(0xFFFFA74F)
    else -> AvatarColor.valueOf(name).color
}

val app.stova.profiles.Accent.container: Color get() = when (this) {
    app.stova.profiles.Accent.LIME -> StovaColors.PrimaryContainer
    // Deep berry, so chosen rows and chips read as Christmas red rather than brown.
    app.stova.profiles.Accent.HOLLY -> Color(0xFF6E1219)
    else -> androidx.compose.ui.graphics.lerp(StovaColors.Ink, color, 0.18f)
}
