package app.stova.core.theme

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/** Spole's moods. Forest retains Stova's quieter secondary text. */
@Serializable
enum class VisualTheme(
    private val inkArgb: Long, private val surfaceArgb: Long, private val raisedArgb: Long,
    private val mutedArgb: Long, private val mutedHighArgb: Long, private val outlineArgb: Long,
    private val outlineHighArgb: Long, private val dividerArgb: Long,
) {
    FOREST(0xFF101211, 0xFF191C19, 0xFF2E332E, 0xFF9AA299, 0xFFDBE0DD, 0xFF646E63, 0xFFAFB8B0, 0xFF3D443C),
    MIDNIGHT(0xFF0D111B, 0xFF151D2D, 0xFF26334B, 0xFFA3A6AE, 0xFFDCDDE0, 0xFF6B6E76, 0xFFB1B3B9, 0xFF3C3E44),
    CINEMA(0xFF000000, 0xFF101010, 0xFF292929, 0xFFA8A8A8, 0xFFDEDEDE, 0xFF696969, 0xFFB5B5B5, 0xFF404040),
    PLUM(0xFF17111B, 0xFF231A29, 0xFF392D41, 0xFFA9A3AE, 0xFFDEDCE0, 0xFF716B76, 0xFFB6B1B9, 0xFF413C44),
    NOEL(0xFF0C1210, 0xFF141D18, 0xFF24332C, 0xFF9FAEA6, 0xFFD9E2DD, 0xFF5F6F67, 0xFFAAB9B1, 0xFF37463E),
    HALLOWEEN(0xFF120D16, 0xFF1B1421, 0xFF2E2337, 0xFFA79FAE, 0xFFDDD9E1, 0xFF6E6577, 0xFFB4ADBA, 0xFF423849);
    val background get() = Color(inkArgb)
    val surface get() = Color(surfaceArgb)
    val raised get() = Color(raisedArgb)
    val muted get() = Color(mutedArgb)
    val mutedHigh get() = Color(mutedHighArgb)
    val outline get() = Color(outlineArgb)
    val outlineHigh get() = Color(outlineHighArgb)
    val divider get() = Color(dividerArgb)
}
