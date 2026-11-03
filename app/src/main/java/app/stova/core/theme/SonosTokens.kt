package app.stova.core.theme

import androidx.compose.ui.graphics.Color

/**
 * A matte graphite speaker finish for the Sonos drawings. It sits between the theme's ink and
 * raised surfaces, so the speakers stay quiet objects and never compete with album art.
 */
object SonosFinish {
    val Shell = Color(0xFF2C302E)
    val ShellTop = Color(0xFF454B47)
    val ShellShade = Color(0xFF1B1E1C)
    val Rim = Color(0xFF5C645E)
    val Grille = Color(0xFF181A19)
    val GrilleDot = Color(0xFF3E4440)
    val Opening = Color(0xFF0A0B0A)
    val Floor = Color(0xFF050605)
    /** Sonos' own status light is a soft white. */
    val Light = Color(0xFFF3F2EA)
    val LightIdle = Color(0xFF7F877F)
}
