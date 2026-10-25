package app.stova.media.ui

import org.junit.Assert.*
import org.junit.Test

class LogoTrimTest {
    private val clear = 0x00000000
    private val ink = 0xFFFFFFFF.toInt()
    private fun canvas(w: Int, h: Int, vararg opaque: Pair<Int, Int>, colour: Int = ink) =
        IntArray(w * h) { i -> if ((i % w) to (i / w) in opaque) colour else clear }

    @Test fun findsTheBoxAroundTheVisibleLogo() =
        assertArrayEquals(intArrayOf(2, 1, 4, 3), opaqueBounds(canvas(6, 4, 2 to 1, 3 to 2), 6, 4))

    @Test fun aFullyTransparentImageHasNoBox() = assertNull(opaqueBounds(canvas(6, 4), 6, 4))

    @Test fun aFaintHaloDoesNotWidenTheBox() {
        val pixels = canvas(6, 4, 2 to 1, 3 to 2)
        pixels[0] = 0x08FFFFFF
        assertArrayEquals(intArrayOf(2, 1, 4, 3), opaqueBounds(pixels, 6, 4))
    }
}
