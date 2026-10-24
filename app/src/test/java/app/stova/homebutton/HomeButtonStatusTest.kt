package app.stova.homebutton

import android.content.ComponentName
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class HomeButtonStatusTest {
    private val me = ComponentName("app.stova", "app.stova.homebutton.HomeButtonService")

    @Test fun detectsEnabledAmongOthers() =
        assertTrue(HomeButtonStatus.isEnabled("x.y/.A:app.stova/app.stova.homebutton.HomeButtonService", me))

    @Test fun detectsShortForm() = assertTrue(HomeButtonStatus.isEnabled("app.stova/.homebutton.HomeButtonService", me))

    @Test fun missingWhenAbsent() {
        assertFalse(HomeButtonStatus.isEnabled(null, me)); assertFalse(HomeButtonStatus.isEnabled("x.y/.A", me))
    }

    @Test fun appendsWithoutDuplicates() {
        assertEquals("x.y/.A:app.stova/app.stova.homebutton.HomeButtonService", HomeButtonStatus.withComponent("x.y/.A", me))
        assertEquals(
            "app.stova/app.stova.homebutton.HomeButtonService",
            HomeButtonStatus.withComponent("app.stova/app.stova.homebutton.HomeButtonService", me),
        )
        assertEquals("app.stova/app.stova.homebutton.HomeButtonService", HomeButtonStatus.withComponent(null, me))
    }
}
