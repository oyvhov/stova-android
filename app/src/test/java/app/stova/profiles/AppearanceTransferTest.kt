package app.stova.profiles

import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import app.stova.home.HomeRowKey
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppearanceTransferTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun repo() = ProfileRepository(JsonFileStore(tmp.root.resolve("profiles.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher()))

    @Test fun resetRestoresDefaultsWithoutChangingIdentitySecurityAppsOrDashboard() = runTest {
        val repo = repo()
        val a = repo.create("Demo",AvatarColor.OCEAN,false)
        val changed = repo.update(a.id) { it.copy(appearance=AppearancePrefs(heroSize=HeroSize.OFF,cornerRadius=CornerRadius.EXTRA_ROUND),
            home=HomePrefs(hiddenRows=setOf(HomeRowKey.APPS)), apps=AppPrefs(favorites=listOf("app"))) }
        repo.resetAppearance(a.id)
        val reset = repo.current().profiles.single()
        assertEquals(changed.copy(appearance=AppearancePrefs(),home=HomePrefs()),reset)
        assertEquals(reset,repo().current().profiles.single())
    }

    @Test fun copyIntoKidMapsRowsAndKeepsTargetSecurityAndApps() = runTest {
        val repo = repo()
        val a = repo.create("Vaksen",AvatarColor.LIME,false)
        val kid = repo.create("Barn",AvatarColor.ROSE,true)
        val src = HomeRowKey.latestMovies("source")
        val dst = HomeRowKey.latestMovies("target")
        val style = RowStyle(RowFormat.POSTER,ArtworkSize.LARGE,false,title="Filmhylla",headingSize=HeadingSize.LARGE,showSource=false)
        repo.update(a.id) { it.copy(appearance=AppearancePrefs(heroSize=HeroSize.OFF,focusEffect=FocusEffect.LIGHT_UP),
            home=HomePrefs(rowOrder=listOf(src,HomeRowKey.APPS),hiddenRows=setOf(src),rowFormats=mapOf(src to style))) }
        repo.copyAppearance(kid.id,a.id,mapOf(src to dst))
        val result = repo.current().profiles.first { it.id==kid.id }
        assertEquals(kid.copy(appearance=AppearancePrefs(heroSize=HeroSize.OFF,focusEffect=FocusEffect.LIGHT_UP),
            home=HomePrefs(rowOrder=listOf(dst,HomeRowKey.APPS),hiddenRows=setOf(dst),rowFormats=mapOf(dst to style))),result)
        assertTrue(result.kid)
        assertFalse(result.kidSmartHomeControl)
        assertEquals(result,repo().current().profiles.first { it.id==kid.id })
    }
}
