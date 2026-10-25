package app.stova.profiles

import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.*
import java.time.LocalDate
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SeasonPersistenceTest {
    @get:Rule val tmp = TemporaryFolder()
    @Test fun savingAnotherPreferenceDuringHalloweenDoesNotStoreTheCalendarColors() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val store = JsonFileStore(tmp.root.resolve("profiles.json"), ProfilesFile.serializer(), ProfilesFile(), io)
        val repo = ProfileRepository(store)
        val profile = repo.create("Demo", AvatarColor.LIME, false)
        val base = AppearancePrefs(visualTheme = VisualTheme.MIDNIGHT, accent = Accent.OCEAN)
        repo.update(profile.id) { it.copy(appearance = base) }
        repo.update(profile.id) { it.copy(appearance = it.appearance.seasonal(LocalDate.of(2026,10,25)).copy(showTitles = true)) }
        val cold = JsonFileStore(tmp.root.resolve("profiles.json"), ProfilesFile.serializer(), ProfilesFile(), io)
        assertEquals(base.copy(showTitles = true), cold.current().profiles.single().appearance)
        assertFalse(tmp.root.resolve("profiles.json").readText().contains("PUMPKIN"))
    }
}
