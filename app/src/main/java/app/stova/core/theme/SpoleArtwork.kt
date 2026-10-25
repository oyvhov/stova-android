package app.stova.core.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import app.stova.R
import androidx.compose.ui.graphics.toArgb

/** Spole's brand colour stays with the other colour constants, never in app tile code. */
private const val SpoleLime = 0xFFD5F675.toInt()

fun spoleArtwork(context: Context, outline: Boolean, width: Int, height: Int): Bitmap {
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    val drawable = context.getDrawable(if (outline) R.drawable.ic_spole_arcticons else R.drawable.ic_spole_brand)!!.mutate()
    if (!outline) {
        canvas.drawColor(StovaColors.Ink.toArgb())
        drawable.setTint(SpoleLime)
    }
    val side = minOf(width, height)
    drawable.setBounds((width-side)/2, (height-side)/2, (width+side)/2, (height+side)/2)
    drawable.draw(canvas)
    return bitmap
}
