package app.stova.ha.cards

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal object HaIcons {
    val Next by lazy {
        ImageVector.Builder("next",24.dp,24.dp,24f,24f).apply {
            path(stroke=SolidColor(Color.Black),strokeLineWidth=2f,strokeLineCap=StrokeCap.Round,strokeLineJoin=StrokeJoin.Round) {
                moveTo(5f,4f);lineTo(16f,12f);lineTo(5f,20f);close()
                moveTo(20f,4f);lineTo(20f,20f)
            }
        }.build()
    }
}
