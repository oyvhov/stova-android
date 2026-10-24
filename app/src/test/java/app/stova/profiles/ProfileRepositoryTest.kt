package app.stova.profiles

import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private var now = 1_000_000L

    private fun repo() = ProfileRepository(
        JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher()),
        { now },
    )

    @Test(expected = IllegalArgumentException::class) fun firstProfileCannotBeKid() = runTest {
        repo().create("Maya", AvatarColor.ROSE, kid = true); Unit
    }

    @Test(expected = IllegalStateException::class) fun lastAdultCannotBeDeleted() = runTest {
        val r = repo(); val a = r.create("Øyvind", AvatarColor.LIME, false); r.create("Maya", AvatarColor.ROSE, true); r.delete(a.id)
    }

    @Test(expected = IllegalArgumentException::class) fun setPinRejectsInvalid() = runTest {
        val r = repo(); r.setPin(r.create("A", AvatarColor.LIME, false).id, "12")
    }

    @Test fun wrongPinsFollowPolicyAndRightPinResets() = runTest {
        val r = repo(); val id = r.create("A", AvatarColor.LIME, false).id; r.setPin(id, "1357")
        repeat(3) { assertEquals(PinCheck.Rejected(0), r.verifyPin(id, "0000")) }
        assertEquals(PinCheck.Rejected(5_000), r.verifyPin(id, "0000"))
        assertEquals(PinCheck.LockedOut(5_000), r.verifyPin(id, "1357"))
        now += 5_000
        assertEquals(PinCheck.Accepted, r.verifyPin(id, "1357"))
        assertEquals(PinCheck.Rejected(0), r.verifyPin(id, "0000"))
    }

    @Test fun lockoutSurvivesRestart() = runTest {
        val r = repo(); val id = r.create("A", AvatarColor.LIME, false).id; r.setPin(id, "1357")
        repeat(4) { r.verifyPin(id, "0000") }
        assertTrue(repo().verifyPin(id, "1357") is PinCheck.LockedOut)
    }

    @Test fun profileWithoutPinIsAlwaysAccepted() = runTest {
        val r = repo(); val id = r.create("A", AvatarColor.LIME, false).id
        assertEquals(PinCheck.Accepted, r.verifyPin(id, "9999"))
    }
}
