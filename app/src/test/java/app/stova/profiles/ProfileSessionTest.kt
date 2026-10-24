package app.stova.profiles

import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSessionTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun enterSetsActiveAndRemembers() = runTest(UnconfinedTestDispatcher()) {
        val repo = ProfileRepository(
            JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)),
        )
        val a = repo.create("A", AvatarColor.LIME, false)
        val s = ProfileSession(repo, backgroundScope); s.enter(a.id)
        assertEquals("A", s.active.value?.name); assertEquals(a.id, repo.file.first().lastProfileId)
        repo.update(a.id) { it.copy(name = "B") }; assertEquals("B", s.active.value?.name)
        s.leave(); assertNull(s.active.value)
        assertEquals(StartDecision.Pick, s.start.value)
    }
}
