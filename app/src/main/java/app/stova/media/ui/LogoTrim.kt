package app.stova.media.ui
import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation

/** Alpha at or below this counts as empty canvas, so a faint halo does not widen the box. */
private const val CANVAS_ALPHA = 16

/**
 * The [left, top, right, bottom) box around the pixels you can see, or null when there are none.
 * Logos often sit centred on a wide transparent canvas; trimming it lets them line up with text.
 */
fun opaqueBounds(pixels: IntArray, width: Int, height: Int): IntArray? {
    var left = width; var top = height; var right = -1; var bottom = -1
    for (y in 0 until height) for (x in 0 until width) {
        if (pixels[y * width + x] ushr 24 > CANVAS_ALPHA) {
            if (x < left) left = x; if (x > right) right = x
            if (y < top) top = y; if (y > bottom) bottom = y
        }
    }
    return if (right < 0) null else intArrayOf(left, top, right + 1, bottom + 1)
}

class TrimTransparent : Transformation() {
    override val cacheKey = "trim-transparent"
    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val w = input.width; val h = input.height
        val pixels = IntArray(w * h).also { input.getPixels(it, 0, w, 0, 0, w, h) }
        val box = opaqueBounds(pixels, w, h) ?: return input
        if (box[0] == 0 && box[1] == 0 && box[2] == w && box[3] == h) return input
        return Bitmap.createBitmap(input, box[0], box[1], box[2] - box[0], box[3] - box[1])
    }
}
