package app.stova.navigation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class PageAnchorTest {
    @Test fun theAnchorUsesTheVisibleCenterInTheRoot() {
        assertEquals(TransformOrigin(.2f, .8f), pageOrigin(Rect(100f, 700f, 300f, 900f), IntSize(1000, 1000)))
        assertEquals(TransformOrigin(1f, 0f), pageOrigin(Rect(1200f, -40f, 1300f, -20f), IntSize(1000, 1000)))
    }

    @Test fun anUnmeasuredRootOpensFromTheCenter() {
        assertEquals(TransformOrigin.Center, pageOrigin(Rect(0f, 0f, 10f, 10f), IntSize.Zero))
    }
}
