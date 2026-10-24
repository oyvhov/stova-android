package app.stova

import app.stova.apps.InstalledApp
import app.stova.apps.reconcileKeys
import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import app.stova.homebutton.LaunchGuard
import app.stova.profiles.PinCheck
import app.stova.profiles.ProfileRepository
import app.stova.profiles.ProfilesFile
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Tests for the findings of the Plan 1 whole-branch review. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewFixesTest {
    @get:Rule val tmp = TemporaryFolder()

    private class Counting(private val delegate: CoroutineDispatcher) : CoroutineDispatcher() {
        val dispatches = AtomicInteger()
        override fun dispatch(context: CoroutineContext, block: Runnable) { dispatches.incrementAndGet(); delegate.dispatch(context, block) }
    }

    private fun repo(cpu: CoroutineDispatcher = Dispatchers.Unconfined) = ProfileRepository(
        JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher()),
        cpu = cpu,
    )

    // Important 1: the service stands down after repeated launches that never came alive.
    @Test fun launchGuardStandsDownAfterThreeDeadLaunches() {
        assertFalse(LaunchGuard.standDown(listOf(10L, 20L), lastAlive = 0, now = 30))
        assertTrue(LaunchGuard.standDown(listOf(10L, 20L, 30L), lastAlive = 0, now = 40))
        assertFalse(LaunchGuard.standDown(listOf(10L, 20L, 30L), lastAlive = 35, now = 40))
        assertFalse(LaunchGuard.standDown(listOf(10L, 20L, 30L), lastAlive = 0, now = 30 + LaunchGuard.WINDOW_MS + 11))
    }

    // Important 4: PBKDF2 runs on the CPU dispatcher, never on the caller's (Main) thread.
    @Test fun pinHashingRunsOnTheCpuDispatcher() = runTest {
        val cpu = Counting(Dispatchers.Default)
        val r = repo(cpu)
        val id = r.create("A", AvatarColor.LIME, false).id
        r.setPin(id, "1357"); val afterSet = cpu.dispatches.get()
        r.verifyPin(id, "1357")
        assertTrue(afterSet > 0); assertTrue(cpu.dispatches.get() > afterSet)
    }

    // Important 5: any adult's PIN lets a kid out, with its own lockout counter.
    @Test fun kidExitAcceptsAnyAdultPin() = runTest {
        val r = repo()
        val a = r.create("A", AvatarColor.LIME, false).id; val b = r.create("B", AvatarColor.OCEAN, false).id
        r.setPin(a, "1357"); r.setPin(b, "2468")
        assertEquals(PinCheck.Rejected(0), r.verifyAnyAdultPin("0000"))
        assertEquals(PinCheck.Accepted, r.verifyAnyAdultPin("2468"))
        assertEquals(PinCheck.Accepted, r.verifyPin(a, "1357")) // A's own counter was never touched
    }

    // Important 7: a missing key is remapped, kept or dropped depending on what really happened.
    @Test fun reconcileRemapsRenamedKeepsUnavailableDropsUninstalled() {
        val renamed = InstalledApp("tv.app/.NewMain", "tv.app", ".NewMain", "TV", true)
        val result = reconcileKeys(
            keys = listOf("tv.app/.OldMain", "usb.app/.Main", "gone.app/.Main", "tv.app/.NewMain"),
            scanned = listOf(renamed),
            installed = { it == "usb.app" || it == "tv.app" },
        )
        assertEquals("tv.app/.NewMain", result["tv.app/.OldMain"])
        assertEquals("usb.app/.Main", result["usb.app/.Main"])
        assertEquals(null, result["gone.app/.Main"]); assertTrue("gone.app/.Main" in result)
        assertEquals("tv.app/.NewMain", result["tv.app/.NewMain"])
    }
}
