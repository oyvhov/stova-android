package app.stova.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.*
import app.stova.profiles.*

internal val themeLabels = listOf(R.string.theme_forest,R.string.theme_midnight,R.string.theme_cinema,
    R.string.theme_plum,R.string.theme_christmas,R.string.theme_halloween)
internal val accentLabels = listOf(R.string.appearance_lime,R.string.appearance_ocean,R.string.appearance_iris,
    R.string.appearance_coral,R.string.appearance_gold,R.string.appearance_mint,R.string.theme_rose,
    R.string.theme_pearl,R.string.theme_holly,R.string.theme_pumpkin)

@Composable
internal fun ThemeSwatches(selected: VisualTheme) {
    val outline = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(108.dp,22.dp).clearAndSetSemantics { }) {
        app.stova.profiles.PLAIN_MOODS.forEachIndexed { index, mood ->
            val x=index*18*density
            listOf(mood.background,mood.surface,mood.raised).forEachIndexed { stripe,color ->
                drawRect(color,Offset(x,stripe*6*density+2*density),Size(13*density,6*density))
            }
            if(mood==selected) drawRect(outline,Offset(x,2*density),Size(13*density,18*density),style=androidx.compose.ui.graphics.drawscope.Stroke(density))
        }
    }
}
@Composable
internal fun AccentSwatches(selected: Accent) {
    val outline = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(118.dp,22.dp).clearAndSetSemantics { }) {
        Accent.entries.forEachIndexed { index, accent ->
            val center=Offset((index*12+5)*density,size.height/2)
            drawCircle(accent.color,4*density,center)
            if(accent==selected) drawCircle(outline,5.5f*density,center,style=androidx.compose.ui.graphics.drawscope.Stroke(density))
        }
    }
}
