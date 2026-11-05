package app.stova.discover

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.background.rememberBackdropState
import app.stova.core.theme.StovaTheme
import app.stova.core.ui.stepEntrance
import coil3.ImageLoader
import coil3.Uri
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(coil3.annotation.DelicateCoilApi::class)
class DiscoverArtworkTransitionTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @After fun resetImages() {coil3.SingletonImageLoader.reset()}
    private val title=DiscoverTitle(DiscoverType.MOVIE,1,"Ei lang og lys historie",2026,null,null,
        "https://art.invalid/white",SeerrStatus.UNKNOWN,null,emptyList())

    private fun brightness():Int=compose.runOnIdle {
        val view=compose.activity.window.decorView
        val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val pixel=bitmap.getPixel(bitmap.width*9/10,bitmap.height/3)
        bitmap.recycle()
        android.graphics.Color.red(pixel)
    }
    private fun artworkStaysVisible(details:Boolean) {
        var step by mutableIntStateOf(0)
        val loader=ImageLoader.Builder(compose.activity).components {
                add(Fetcher.Factory<Uri> {_,_,_->Fetcher {
                    val bitmap=Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    ImageFetchResult(bitmap.asImage(),false,DataSource.MEMORY)
                }})
            }.build()
        coil3.SingletonImageLoader.setUnsafe(loader)
        compose.setContent {
                StovaTheme {
                    val backdrop=rememberBackdropState()
                    val entrance=Modifier.stepEntrance(step,step)
                    if(details) DiscoverTitleContent(title,null,null,SeerrAccess.NONE,false,{},{},{},{},
                        modifier=entrance,backdrop=backdrop)
                    else DiscoverContent(DiscoverState(SeerrAccess.PERSONAL,listOf(DiscoverRow(DiscoverRowKind.TRENDING,listOf(title))),emptyList(),false,false),
                        {},{},{},{},modifier=entrance,backdrop=backdrop)
                }
        }
        compose.waitUntil(5_000) {compose.onAllNodesWithTag("backdrop:${title.backdrop}").fetchSemanticsNodes().isNotEmpty()}
        compose.waitForIdle()
        val before=brightness()
        val artLeft=compose.onNodeWithTag("backdrop:${title.backdrop}").fetchSemanticsNode().boundsInRoot.left
        assertTrue("The white backdrop must have loaded",before>35)
        compose.mainClock.autoAdvance=false
        for(next in listOf(1,0)) {
            compose.runOnIdle {step=next}
            repeat(12) {
                compose.mainClock.advanceTimeByFrame()
                assertEquals("Artwork must stay outside the controls' entrance transform",artLeft,
                    compose.onNodeWithTag("backdrop:${title.backdrop}").fetchSemanticsNode().boundsInRoot.left,.1f)
                assertTrue("Artwork disappeared on step $next, frame $it",brightness()>=before-8)
            }
        }
    }
    @Test fun titleEntranceAndReturnNeverFadeArtworkToBlack()=artworkStaysVisible(true)
    @Test fun rowsEntranceAndReturnNeverFadeArtworkToBlack()=artworkStaysVisible(false)
}
