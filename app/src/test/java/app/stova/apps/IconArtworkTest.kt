package app.stova.apps

import android.content.Context
import android.util.DisplayMetrics
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import app.stova.profiles.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class IconArtworkTest {
    @get:Rule val tmp = TemporaryFolder()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val jellyfin = InstalledApp("org.jellyfin.mobile/.MainActivity", "org.jellyfin.mobile", ".MainActivity", "Jellyfin", true)
    private val spole = InstalledApp("app.reelstack/.MainActivity", "app.reelstack", ".MainActivity", "Spole", true)

    @Test fun projectivyExportKeepsTheOriginalProductionPathsInBothFormats() {
        val svg=java.io.File("../docs/icons/spole-projectivy.svg").readText()
        val vector=java.io.File("src/main/res/drawable/ic_spole_projectivy.xml").readText()
        val paths=Regex(""" d="([^"]+)"""").findAll(svg).map { it.groupValues[1] }.toList()
        assertEquals(6,paths.size)
        assertEquals(paths,Regex("""android:pathData="([^"]+)"""").findAll(vector).map { it.groupValues[1] }.toList())
        assertTrue(svg.contains("translate(42 43) scale(0.59 0.59)"))
        assertTrue(svg.contains("translate(118 76) scale(1 1)"))
        assertFalse("no background path",svg.contains("M0,0 H320 V180"))
    }

    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun projectivySpoleSupplementIsAWideTransparentWordmarkForBothPackages() = runTest {
        val source=FakeIconPackSource().apply { packs=listOf(IconPack("dev.projectivy.blueprint.app","Projectivy Icon Pack",119)) }
        val art=AppArtCache(context,iconPacks=IconPackRepository(source,tmp.root))
        val prefs=AppearancePrefs(iconPack=source.packs.single().packageName)
        assertTrue(ProjectivyIcons.SPOLE in art.iconPacks.index(source.packs.single()).drawables)
        for(pkg in listOf("app.reelstack","app.reelstack.debug")) {
            val image=art.artwork(spole.copy(packageName=pkg),prefs)!!
            assertEquals(16f/9f,image.bitmap.width.toFloat()/image.bitmap.height,0.01f)
            val pixels=IntArray(image.bitmap.width*image.bitmap.height)
            image.bitmap.readPixels(pixels)
            assertEquals("no baked background",0,android.graphics.Color.alpha(pixels[0]))
            assertTrue(image.packIcon)
            assertFalse("brand and white lettering retain their own colours",image.monochrome)
        }
        val manual=art.artwork(spole,AppearancePrefs(),IconOverride(ProjectivyIcons.PACKAGE,ProjectivyIcons.SPOLE))!!
        assertEquals(16f/9f,manual.bitmap.width.toFloat()/manual.bitmap.height,0.01f)
    }

    @Test fun widePackLogosKeepTheirAspectRatio() = runTest {
        val fake=FakeIconPackSource()
        val source=object:IconPackSource by fake {
            override fun drawable(pack:IconPack,name:String,density:Int)=android.graphics.drawable.BitmapDrawable(context.resources,
                android.graphics.Bitmap.createBitmap(320,180,android.graphics.Bitmap.Config.ARGB_8888))
        }
        val image=AppArtCache(context,iconPacks=IconPackRepository(source,tmp.root)).packIcon(fake.packs.single(),"wide")!!
        assertEquals(16f/9f,image.width.toFloat()/image.height,0.01f)
    }

    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun submissionSvgAndAndroidVectorMatchAndFitTheArcticonsRectangleIncludingStroke() {
        val svg = java.io.File("../docs/icons/spole-arcticons.svg").readText()
        val vector = java.io.File("src/main/res/drawable/ic_spole_arcticons.xml").readText()
        assertTrue(svg.contains("viewBox=\"0 0 48 48\""))
        assertTrue(svg.contains("stroke-width=\"1\""))
        assertTrue(svg.contains("fill=\"none\""))
        assertTrue(svg.contains("stroke-linecap=\"round\""))
        assertTrue(svg.contains("stroke-linejoin=\"round\""))
        val path = Regex(""" d="([^"]+)"""").find(svg)!!.groupValues[1]
        assertEquals(path, Regex("""android:pathData="([^"]+)"""").find(vector)!!.groupValues[1])
        val bounds = android.graphics.RectF()
        androidx.core.graphics.PathParser.createPathFromPathData(path).computeBounds(bounds, true)
        assertTrue("31 px wide including stroke: $bounds", bounds.width() + 1 <= 31f)
        assertTrue("39 px tall including stroke: $bounds", bounds.height() + 1 <= 39.001f)
    }

    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun otherMonochromePacksUseAccentAndTheLineSupplementWhileColourPacksKeepTheirColours() = runTest {
        val fake = FakeIconPackSource().apply { packs = listOf(IconPack("other.pack", "Other", 1)) }
        var colourful = false
        val source = object : IconPackSource by fake {
            override fun drawable(pack: IconPack, name: String, density: Int): android.graphics.drawable.Drawable {
                val bitmap = android.graphics.Bitmap.createBitmap(32, 32, android.graphics.Bitmap.Config.ARGB_8888)
                for (y in 4..27) for (x in 8..23) bitmap.setPixel(x, y, if (!colourful) android.graphics.Color.WHITE else if (x < 16) android.graphics.Color.RED else android.graphics.Color.BLUE)
                return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
            }
        }
        val art = AppArtCache(context, iconPacks = IconPackRepository(source, tmp.root))
        val prefs = AppearancePrefs(iconPack = "other.pack", iconColor = IconColor.ACCENT)
        assertTrue(art.artwork(jellyfin, prefs)!!.monochrome)
        assertTrue(art.artwork(spole, prefs)!!.monochrome)
        colourful = true
        fake.packs = fake.packs.map { it.copy(versionCode = 2) }
        art.iconPacks.refresh()
        assertFalse(art.artwork(jellyfin, prefs)!!.monochrome)
        assertFalse(art.artwork(spole, prefs)!!.monochrome)
    }

    @Test fun packMatchWinsAndDecodesOnceAtTvDensity() = runTest {
        val source = FakeIconPackSource()
        val art = AppArtCache(context, iconPacks = IconPackRepository(source, tmp.root))
        val prefs = AppearancePrefs(iconPack = source.packs.single().packageName)
        val first = art.artwork(jellyfin, prefs)
        assertTrue(first!!.packIcon)
        assertSame(first.bitmap, art.artwork(jellyfin, prefs)!!.bitmap)
        assertEquals(listOf("jellyfin" to DisplayMetrics.DENSITY_XXHIGH), source.requested)
    }

    @Test fun spoleSupplementMatchesThePackStyleForReleaseAndDebug() = runTest {
        val source = FakeIconPackSource()
        val art = AppArtCache(context, iconPacks = IconPackRepository(source, tmp.root))
        val line = art.artwork(spole, AppearancePrefs(iconPack = source.packs.single().packageName))!!
        assertTrue(line.packIcon)
        source.packs = listOf(IconPack("color.pack", "Color", 1))
        art.iconPacks.refresh()
        val color = art.artwork(spole.copy(packageName = "app.reelstack.debug"), AppearancePrefs(iconPack = "color.pack"))!!
        assertTrue(color.packIcon)
        assertNotSame(line.bitmap, color.bitmap)
        assertNotNull(art.artwork(spole, AppearancePrefs()))
    }

    @Test fun missingMappingDrawableAndUninstalledPackUseOwnBanner() = runTest {
        val component = android.content.ComponentName(context.packageName, "demo.Main")
        org.robolectric.Shadows.shadowOf(context.packageManager).addActivityIfNotPresent(component).apply {
            applicationInfo = context.applicationInfo; banner = app.stova.R.drawable.tv_banner
        }
        val app = InstalledApp(component.flattenToString(), component.packageName, component.className, "Demo", true)
        val source = FakeIconPackSource()
        val art = AppArtCache(context, iconPacks = IconPackRepository(source, tmp.root))
        val own = art.artwork(app, AppearancePrefs())!!
        assertFalse(own.packIcon)
        assertSame(own.bitmap, art.artwork(app, AppearancePrefs(iconPack = "missing.pack"))!!.bitmap)
        assertSame(own.bitmap, art.artwork(app, AppearancePrefs(iconPack = source.packs.single().packageName))!!.bitmap)
        source.filter = """<item component="ComponentInfo{${app.key}}" drawable="missing"/>"""
        source.missingDrawables = setOf("missing")
        source.packs = source.packs.map { it.copy(versionCode = 2) }
        art.iconPacks.refresh()
        assertSame(own.bitmap, art.artwork(app, AppearancePrefs(iconPack = source.packs.single().packageName))!!.bitmap)
    }

    @Test fun settingsAndPerAppOverridesSurviveRestartAndStayInTheirProfile() = runTest {
        fun repository() = ProfileRepository(JsonFileStore(tmp.root.resolve("profiles.json"), ProfilesFile.serializer(), ProfilesFile()))
        val repo = repository()
        val a = repo.create("A", AvatarColor.LIME, false)
        val b = repo.create("B", AvatarColor.OCEAN, false)
        val custom = IconOverride("com.donnnno.arcticons", "kodi")
        repo.update(a.id) { it.copy(appearance = it.appearance.copy(iconPack = custom.packageName, iconColor = IconColor.ACCENT, iconFill = IconFill.CROP), apps = it.apps.copy(iconOverrides = mapOf(jellyfin.key to custom))) }
        val loaded = repository().current().profiles
        assertEquals(custom, loaded.first { it.id == a.id }.apps.iconOverrides[jellyfin.key])
        assertEquals(IconColor.ACCENT, loaded.first { it.id == a.id }.appearance.iconColor)
        assertEquals(IconFill.CROP, loaded.first { it.id == a.id }.appearance.iconFill)
        assertTrue(loaded.first { it.id == b.id }.apps.iconOverrides.isEmpty())
        assertNull(loaded.first { it.id == b.id }.appearance.iconPack)
    }

    @Test fun manualIconOverridesAutomaticMappingEvenWithoutGlobalPack() = runTest {
        val source = FakeIconPackSource()
        val art = AppArtCache(context, iconPacks = IconPackRepository(source, tmp.root))
        assertTrue(art.artwork(jellyfin, AppearancePrefs(), IconOverride(source.packs.single().packageName, "kodi"))!!.packIcon)
        assertEquals("kodi", source.requested.single().first)
    }
}
