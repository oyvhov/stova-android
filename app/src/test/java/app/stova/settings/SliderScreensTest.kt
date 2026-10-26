package app.stova.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.*
import app.stova.apps.*
import app.stova.core.theme.StovaTheme
import app.stova.profiles.*
import org.junit.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SliderScreensTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @get:Rule val tmp=TemporaryFolder()
    @Before fun enabled()=assumeScreensEnabled()
    @Test fun iconSliderAndActualPreview()=icons(1f)
    @Test fun iconSliderWithLargeText()=icons(2f)
    private fun icons(scale:Float) {
        val fake=FakeIconPackSource().apply {packs=listOf(IconPack(ProjectivyIcons.PACKAGE,"Projectivy Icon Pack",119));filter=""}
        val art=AppArtCache(compose.activity,iconPacks=IconPackRepository(fake,tmp.root))
        val app=InstalledApp("app.reelstack/app.reelstack.MainActivity","app.reelstack","app.reelstack.MainActivity","Spole",true)
        var prefs by mutableStateOf(AppearancePrefs(iconPack=ProjectivyIcons.PACKAGE,appTileBackgroundOpacity=43))
        compose.setContent {
            val density=LocalDensity.current
            val input=LocalInputModeManager.current
            SideEffect {input.requestInputMode(InputMode.Keyboard)}
            CompositionLocalProvider(LocalDensity provides Density(density.density,scale)) {StovaTheme(appearance=prefs) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(48.dp).verticalScroll(rememberScrollState())) {
                    AppIconPreview(prefs,listOf(app),art)
                    IconPackSettingsContent(prefs,fake.packs) {prefs=it}
                }
            }}
        }
        val slider=compose.onNodeWithText("Bakgrunn på appkort").performScrollTo()
        slider.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        slider.performKeyInput {pressKey(Key.Enter)}
        slider.assertIsDisplayed()
        compose.saveScreen("appearance-slider-icons-$scale")
    }
}
