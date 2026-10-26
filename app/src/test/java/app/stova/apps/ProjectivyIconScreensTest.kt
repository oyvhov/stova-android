package app.stova.apps

import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.assumeScreensEnabled
import app.stova.saveScreen
import app.stova.core.theme.StovaTheme
import app.stova.profiles.AppearancePrefs
import app.stova.home.LocalRowHasFocus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectivyIconScreensTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @get:Rule val tmp=TemporaryFolder()

    @Test fun transparentLogoShowsTheWallpaperAndSolidCardsRemainAnOption() {
        assumeScreensEnabled()
        val fake=FakeIconPackSource().apply { packs=listOf(IconPack(ProjectivyIcons.PACKAGE,"Projectivy Icon Pack",119));filter="" }
        val art=AppArtCache(compose.activity,iconPacks=IconPackRepository(fake,tmp.root))
        val app=InstalledApp("app.reelstack/app.reelstack.MainActivity","app.reelstack","app.reelstack.MainActivity","Spole",true)
        var prefs by mutableStateOf(AppearancePrefs(iconPack=ProjectivyIcons.PACKAGE,appTileBackground=false))
        runBlocking { assertNotNull(art.artwork(app,prefs)) }
        var wallpaper=0;var surface=0
        compose.setContent { StovaTheme(appearance=prefs) {
            wallpaper=MaterialTheme.colorScheme.background.toArgb();surface=MaterialTheme.colorScheme.surfaceVariant.toArgb()
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(48.dp)) {
                CompositionLocalProvider(LocalRowHasFocus provides true) {
                    AppTile(app,art,{}, {},Modifier.testTag("spole"))
                }
            }
        } }
        val bounds=compose.onNodeWithTag("spole").fetchSemanticsNode().boundsInRoot
        val x=bounds.left.toInt()+20;val y=bounds.top.toInt()+20
        val transparent=BitmapFactory.decodeFile(compose.saveScreen("spole-projectivy-transparent").path)
        assertEquals("wallpaper is visible through the empty tile",wallpaper,transparent.getPixel(x,y))
        compose.runOnIdle { prefs=prefs.copy(appTileBackground=true) }
        val solid=BitmapFactory.decodeFile(compose.saveScreen("spole-projectivy-solid").path)
        assertEquals("card surface is opt-in",surface,solid.getPixel(x,y))
        assertEquals(bounds,compose.onNodeWithTag("spole").fetchSemanticsNode().boundsInRoot)
        compose.runOnIdle { prefs = prefs.copy(appTileBackgroundOpacity = 50) }
        val half = BitmapFactory.decodeFile(compose.saveScreen("spole-projectivy-half-background").path)
        val expected = androidx.core.graphics.ColorUtils.compositeColors(androidx.core.graphics.ColorUtils.setAlphaComponent(surface,128),wallpaper)
        // Skia rounds premultiplied channels; integer ColorUtils truncates. Allow one byte.
        for (shift in listOf(0,8,16)) assertEquals(((expected shr shift) and 255).toDouble(), ((half.getPixel(x,y) shr shift) and 255).toDouble(), 1.0)
        // A bright, opaque point in the logo must not fade along with its background.
        val bright = (bounds.top.toInt()+24 until bounds.top.toInt()+170).flatMap { py ->
            (bounds.left.toInt()+24 until bounds.right.toInt()-24).map { px -> px to py }
        }.maxBy { (px,py) -> val c=transparent.getPixel(px,py); ((c shr 16) and 255)+((c shr 8) and 255)+(c and 255) }
        assertEquals(transparent.getPixel(bright.first,bright.second),half.getPixel(bright.first,bright.second))
        compose.runOnIdle { prefs=prefs.copy(appTileBackgroundOpacity=0,reduceMotion=true) }
        val calm=BitmapFactory.decodeFile(compose.saveScreen("spole-projectivy-transparent-reduced-motion").path)
        assertEquals(wallpaper,calm.getPixel(x,y))
    }
}
