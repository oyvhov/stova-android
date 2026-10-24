package app.stova

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class ApplicationBoundaryTest {
    @Test fun uninitializedContainerExplainsHomeServiceProcessBoundary() {
        val error = runCatching { StovaApplication().container }.exceptionOrNull()
        assertTrue(error.toString(), error is IllegalStateException)
        assertTrue(error?.message.orEmpty(), error?.message.orEmpty().contains(":homebutton"))
        assertTrue(error?.message.orEmpty(), error?.message.orEmpty().contains("process-local"))
    }
}
