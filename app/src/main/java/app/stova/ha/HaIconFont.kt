package app.stova.ha

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import app.stova.R
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Home Assistant's own icons, the Material Design Icons it draws with (Apache 2.0, licence in
 * `assets/licenses`). The font loads only when a tile draws an icon, and the name list in
 * `assets/mdi/names.txt` is read only for the names asked for; only those are kept. The icon
 * picker alone reads it whole, while it is open.
 */
object HaIconFont {
    val family by lazy { FontFamily(Font(R.font.material_design_icons)) }
    private val known = ConcurrentHashMap<String, Int>()

    /** What Home Assistant shows for a scene or script that has no icon of its own. */
    fun defaultFor(entityId: String) = if (entityId.substringBefore('.') == "script") "mdi:script-text" else "mdi:palette"

    /** The glyph for an `mdi:` icon, or null when it is not one Stova has. [names] opens the sorted name list. */
    fun codepoint(icon: String, names: () -> InputStream): Int? {
        if (!icon.startsWith("mdi:")) return null
        val name = icon.removePrefix("mdi:").takeIf(String::isNotBlank) ?: return null
        known[name]?.let { return it.takeIf { code -> code >= 0 } }
        val code = names().bufferedReader().useLines { lookup(it, name) }
        known[name] = code ?: -1
        return code
    }

    /** Every icon in the list, for the picker. */
    fun all(names: () -> InputStream): List<HaIconEntry> = names().bufferedReader().useLines { lines ->
        lines.filterNot { it.startsWith('#') }.mapNotNull { line ->
            line.substringAfter(' ').trim().toIntOrNull(16)?.let { HaIconEntry(line.substringBefore(' '), it) }
        }.toList()
    }

    /** The list is sorted, so the search stops at the first name past [name]. */
    internal fun lookup(lines: Sequence<String>, name: String): Int? {
        for (line in lines) {
            if (line.startsWith('#')) continue
            val entry = line.substringBefore(' ')
            if (entry == name) return line.substringAfter(' ').trim().toIntOrNull(16)
            if (entry > name) return null
        }
        return null
    }
}

/** A Home Assistant icon such as `mdi:movie-open`, or [fallback] when there is none Stova knows. */
@Composable
fun HaIcon(icon: String?, fallback: ImageVector, size: Dp, tint: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val code by produceState<Int?>(null, icon) {
        value = icon?.let { withContext(Dispatchers.IO) { runCatching { HaIconFont.codepoint(it) { context.assets.open("mdi/names.txt") } }.getOrNull() } }
    }
    val glyph = code
    if (glyph == null) Icon(fallback, contentDescription = null, modifier = modifier.size(size), tint = tint)
    else HaGlyph(glyph, size, tint, modifier.testTag("ha-icon"))
}

/** One icon as Home Assistant names it, such as `cat`, with its glyph in the font. */
data class HaIconEntry(val name: String, val code: Int) {
    val icon get() = "mdi:$name"
}

@Composable
internal fun HaGlyph(code: Int, size: Dp, tint: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val fontSize = with(LocalDensity.current) { size.toSp() }
        Text(String(Character.toChars(code)), color = tint, style = TextStyle(fontFamily = HaIconFont.family, fontSize = fontSize,
            lineHeight = fontSize, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)))
    }
}
