package app.stova.core.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import app.stova.R
import androidx.compose.ui.graphics.toArgb

/** Spole's brand colour stays with the other colour constants, never in app tile code. */
private const val SpoleLime = 0xFFD5F675.toInt()

/** Transparent 16:9 canvas, matching Projectivy's wordmark assets rather than a square app badge. */
fun projectivySpoleArtwork(context: Context, width: Int = 512, height: Int = 288): Bitmap {
    val bitmap = createBitmap(width, height)
    context.getDrawable(R.drawable.ic_spole_projectivy)!!.mutate().apply {
        setBounds(0, 0, width, height)
        draw(Canvas(bitmap))
    }
    return bitmap
}

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
