package app.stova.home
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.media.model.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
@RunWith(AndroidJUnit4::class) @Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class HeroPanelTest {
 @get:Rule val compose=createComposeRule()
 @Test fun showsTitleMetaAndSourceWhenNoLogo() {
  val a=MediaAccount("a","p",ServerKind.JELLYFIN,"s","Demoarkiv","https://demo.invalid","u","Demo")
  compose.setContent { StovaTheme { HeroPanel(FocusedTarget.Media(MediaItem("m","a",ItemType.MOVIE,"Kystvakta",year=2024,runtimeMinutes=112,ratings=Ratings(official="12")),a)) } }
  compose.onNodeWithText("Kystvakta").assertIsDisplayed()
  compose.onNodeWithText("2024 · 1 t 52 min").assertIsDisplayed()
  compose.onNodeWithText("2024 · 1 t 52 min · 12").assertDoesNotExist()
  compose.onAllNodesWithText("12").assertCountEquals(1)
  compose.onNodeWithText("12").assertIsDisplayed()
  compose.onNodeWithText("Demoarkiv").assertIsDisplayed()
 }
}
