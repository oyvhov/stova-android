package app.stova.home
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.media.model.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
@RunWith(AndroidJUnit4::class) @Config(sdk=[30],qualifiers=TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
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

 @Test fun entireTextColumnStaysWithin560DpOn960DpScreen() {
  val title="Fjordlys og den lange ferda gjennom fjordar og ukjende landskap"
  val overview="Ei ferd gjennom stille fjordar og opne landskap. Vegen vidare byr på nye møte og ukjende stader."
  val source="Demoarkiv med eit langt kjeldenamn som framleis må halde seg innanfor tekstkolonna"
  val account=MediaAccount("a","p",ServerKind.JELLYFIN,"s",source,"https://demo.invalid","u","Demo")
  val item=MediaItem("m","a",ItemType.MOVIE,title,year=2024,runtimeMinutes=112,overview=overview,ratings=Ratings(7.84,91,"12"))
  compose.setContent { StovaTheme {
   Box(Modifier.fillMaxSize()) { HeroPanel(FocusedTarget.Media(item,account),Modifier.fillMaxWidth()) }
  } }
  compose.onRoot().assertWidthIsEqualTo(960.dp)
  listOf(title,"2024 · 1 t 52 min","TMDB","7,8",app.stova.media.ui.criticText(91,java.util.Locale.forLanguageTag("nn")),"12",overview,source).forEach { text ->
   val node=compose.onNodeWithText(text).assertIsDisplayed()
   val bounds=node.getUnclippedBoundsInRoot()
   Assert.assertTrue("Hero text must be at most 560 dp wide: $text ($bounds)",bounds.right-bounds.left<=560.dp)
   Assert.assertTrue("Hero text must stay inside the 560 dp column: $text ($bounds)",bounds.left>=48.dp && bounds.right<=608.dp)
  }
 }
}
