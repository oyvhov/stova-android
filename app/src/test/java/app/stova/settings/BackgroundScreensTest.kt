package app.stova.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.*
import app.stova.core.theme.StovaTheme
import app.stova.media.*
import app.stova.profiles.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackgroundScreensTest {
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 @Before fun enabled()=assumeScreensEnabled()
 @Test fun backgroundControls() {
  compose.setContent {StovaTheme {Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
   .padding(48.dp).verticalScroll(rememberScrollState())) {
   AppearanceSettingsContent(AppearancePrefs(),category=AppearanceCategory.BACKGROUND,includePreview=false) {}
  }}}
  compose.saveScreen("background-filter-controls")
 }
 @Test fun librarySelection()=picker(1f)
 @Test fun librarySelectionLargeText()=picker(2f)
 @Test fun ageSelection() {
  compose.setContent {StovaTheme {BackgroundAgeDialog(15,{},{})}}
  compose.waitForIdle()
  compose.saveScreen("background-age-selection",org.robolectric.shadows.ShadowDialog.getLatestDialog().window!!.decorView)
 }
 private fun picker(fontScale:Float) {
  val libraries=listOf("Familiefilmar","Filmar","Seriar","Dokumentarar").mapIndexed {i,name->
   BackgroundLibrary(BackgroundLibraryRef("a","$i"),name,"Jellyfin · Stova")}
  compose.setContent {CompositionLocalProvider(LocalDensity provides Density(2f,fontScale)) {StovaTheme {
   BackgroundLibraryPicker(setOf(libraries.first().ref),{BackgroundLibraryCatalog(libraries,hasAccounts=true)},{},{})
  }}}
  compose.waitForIdle()
  compose.saveScreen("background-libraries-$fontScale",org.robolectric.shadows.ShadowDialog.getLatestDialog().window!!.decorView)
 }
}
