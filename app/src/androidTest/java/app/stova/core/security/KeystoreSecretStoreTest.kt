package app.stova.core.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreSecretStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun storesEncryptedAndDeletes() = runTest {
        val store = KeystoreSecretStore(context, alias = "stova.test.secrets")
        store.write("a", "klartekst-verdi")
        assertEquals("klartekst-verdi", store.read("a"))
        assertFalse(File(context.filesDir, "secrets.json").readText().contains("klartekst-verdi"))
        store.delete("a")
        assertNull(store.read("a"))
    }
}
