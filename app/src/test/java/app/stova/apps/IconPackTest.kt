package app.stova.apps

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class IconPackTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun packageChangesDoNotScanUntilPacksAreNeeded() = runTest {
        val source = FakeIconPackSource()
        val repo = IconPackRepository(source, tmp.root)
        repo.packagesChanged()
        assertEquals(0, source.scans)
        repo.installed()
        assertEquals(1, source.scans)
        repo.packagesChanged()
        repo.installed()
        assertEquals(2, source.scans)
    }

    @Test fun discoversEverySupportedActionOnceAndDeclaresVisibility() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = org.robolectric.Shadows.shadowOf(context.packageManager)
        PackageManagerIconPackSource.ACTIONS.forEachIndexed { i, action ->
            val pkg = "pack.$i"
            val component = android.content.ComponentName(pkg, "$pkg.Main")
            pm.addActivityIfNotPresent(component)
            context.packageManager.getPackageInfo(pkg, 0).setLongVersionCode(7)
            context.packageManager.getApplicationInfo(pkg, 0).nonLocalizedLabel = "Pack $i"
            pm.addIntentFilterForActivity(component, android.content.IntentFilter(action))
        }
        pm.addIntentFilterForActivity(android.content.ComponentName("pack.0", "pack.0.Main"), android.content.IntentFilter(PackageManagerIconPackSource.ACTIONS.last()))
        assertEquals(5, PackageManagerIconPackSource(context).installed().size)
        val manifest = java.io.File("src/main/AndroidManifest.xml").readText()
        PackageManagerIconPackSource.ACTIONS.forEach { assertTrue("visibility for $it", manifest.contains("android:name=\"$it\"")) }
    }

    @Test fun realArcticonsExcerptAndShortActivityNames() {
        val xml = javaClass.getResource("/icons/arcticons-appfilter.xml")!!.readText()
        val index = AppFilter.parse(xml)
        assertEquals("jellyfin", index["org.jellyfin.mobile/org.jellyfin.mobile.MainActivity"])
        assertEquals("vlc", index["org.videolan.vlc/org.videolan.vlc.StartActivity"])
        assertEquals("short", AppFilter.parse("""<item component="ComponentInfo{demo/.Main}" drawable="short"/>""")["demo/demo.Main"])
    }

    @Test fun damagedEntriesDoNotLoseTheRestOfThePack() {
        val index = AppFilter.parse("""<resources>
          <!-- <item component="ComponentInfo{comment/.Main}" drawable="wrong"/> -->
          <item component="bad" drawable="bad"/>
          <item component="ComponentInfo{one/.Main}" drawable="one"/>
          <item component="ComponentInfo{bad/.Main}" drawable="../bad"/>
          <item component="ComponentInfo{broken/.Main}" drawable=broken/>
          <item component="ComponentInfo{two/.Main}" drawable="two"/>
          <item component="ComponentInfo{missing/.Main}"/>
        </resources>""")
        assertEquals(mapOf("one/one.Main" to "one", "two/two.Main" to "two"), index)
    }

    @Test fun versionedIndexSurvivesRestartAndRefreshesAfterUpgrade() = runTest {
        val source = FakeIconPackSource()
        val repo = IconPackRepository(source, tmp.root)
        val pack = source.packs.single()
        assertEquals("jellyfin", repo.index(pack).components["org.jellyfin.mobile/org.jellyfin.mobile.MainActivity"])
        assertEquals(1, source.reads)
        IconPackRepository(source, tmp.root).index(pack)
        assertEquals("restart reads disk", 1, source.reads)
        source.packs = listOf(pack.copy(versionCode = 2))
        source.filter = """<item component="ComponentInfo{org.jellyfin.mobile/.MainActivity}" drawable="new_icon"/>"""
        assertEquals("new_icon", repo.index(source.packs.single()).components.values.single())
        assertEquals(2, source.reads)
    }

    @Test fun drawableCatalogIncludesUnmappedIconsAndDeduplicates() = runTest {
        val source = FakeIconPackSource().apply {
            catalog = """<resources><item drawable="extra"/><item drawable="jellyfin"/><item drawable="../bad"/></resources>"""
        }
        val index = IconPackRepository(source, tmp.root).index(source.packs.single())
        assertEquals(listOf("extra", "jellyfin"), index.drawables)
    }

    @Test fun corruptDiskIndexIsRebuilt() = runTest {
        val source = FakeIconPackSource()
        IconPackRepository(source, tmp.root).index(source.packs.single())
        tmp.root.listFiles()!!.single { it.extension == "json" }.writeText("broken")
        assertFalse(IconPackRepository(source, tmp.root).index(source.packs.single()).components.isEmpty())
        assertEquals(2, source.reads)
    }
}

class FakeIconPackSource : IconPackSource {
    var packs = listOf(IconPack("com.donnnno.arcticons", "Arcticons", 1))
    var filter = """<item component="ComponentInfo{org.jellyfin.mobile/.MainActivity}" drawable="jellyfin"/>"""
    var catalog: String? = null
    var reads = 0
    var scans = 0
    var missingDrawables: Set<String> = emptySet()
    val requested = mutableListOf<Pair<String, Int>>()
    override fun installed(): List<IconPack> { scans++; return packs }
    override fun appFilter(pack: IconPack): String { reads++; return filter }
    override fun drawableCatalog(pack: IconPack) = catalog
    override fun drawable(pack: IconPack, name: String, density: Int): Drawable? {
        requested += name to density
        if (name in missingDrawables) return null
        return ApplicationProvider.getApplicationContext<Context>().getDrawable(app.stova.R.drawable.ic_stova_foreground)
    }
}
