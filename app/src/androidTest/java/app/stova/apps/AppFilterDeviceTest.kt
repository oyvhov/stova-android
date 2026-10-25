package app.stova.apps

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Android's ICU matcher copies its input on reset; a desktop JVM does not. */
@RunWith(AndroidJUnit4::class)
class AppFilterDeviceTest {
    @Test(timeout = 15_000)
    fun largePackIsIndexedWithoutResettingTheMatcherForEveryItem() {
        val count = 30_000
        val filter = buildString {
            append("<resources>")
            repeat(count) { append("<item component=\"ComponentInfo{pack.app$it/.Main}\" drawable=\"icon_$it\"/>") }
            append("</resources>")
        }
        val components = AppFilter.parse(filter)
        assertEquals(count, components.size)
        assertEquals("icon_29999", components["pack.app29999/pack.app29999.Main"])
        val catalog = buildString {
            append("<resources>")
            repeat(count) { append("<item drawable=\"icon_$it\"/>") }
            append("</resources>")
        }
        assertEquals(count, AppFilter.drawables(catalog).size)
    }
}
