package app.stova.ha.cards
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.*
import org.junit.Test
class SparklineTest {
    @Test fun normalises() = assertEquals(listOf(Offset(0f,100f),Offset(50f,0f),Offset(100f,50f)),sparklinePoints(listOf(0.0,10.0,5.0),100f,100f))
    @Test fun flatLineInMiddle() = assertEquals(listOf(Offset(0f,50f),Offset(100f,50f)),sparklinePoints(listOf(3.0,3.0),100f,100f))
    @Test fun noLineForSinglePoint() = assertTrue(sparklinePoints(listOf(3.0),100f,100f).isEmpty())
}
