package app.stova.background

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import coil3.ImageLoader
import coil3.Uri
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class BackdropLayerTest {
    @get:Rule val compose = createComposeRule()
    private val gates = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private fun gate(url: String) = gates.getOrPut(url) { CompletableDeferred() }
    private fun loader(context: android.content.Context, memory: Boolean = true) = ImageLoader.Builder(context).components {
        add(Fetcher.Factory<Uri> { data, _, _ ->
            Fetcher { gate(data.toString()).await(); ImageFetchResult(Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).asImage(), false, DataSource.MEMORY) }
        })
    }.apply { if (!memory) memoryCachePolicy(coil3.request.CachePolicy.DISABLED) }.build()
    private fun shown(url: String) = compose.onAllNodesWithTag("backdrop:$url").fetchSemanticsNodes().isNotEmpty()

    @Test fun stricterFilterRemovesDecodedAndOutgoingArtBeforeAnotherImageLoads() {
        val a="https://demo.invalid/adult";val b="https://demo.invalid/safe"
        val account=app.stova.media.model.MediaAccount("a","p",app.stova.media.model.ServerKind.JELLYFIN,"s","S","https://demo.invalid","u","U")
        fun entry(url:String,age:String)=app.stova.media.SpotlightEntry(app.stova.media.model.MediaItem(url,"a",app.stova.media.model.ItemType.MOVIE,url,
            ratings=app.stova.media.model.Ratings(official=age),art=app.stova.media.model.ItemArt(backdrop=url)),account,false)
        var prefs by mutableStateOf(app.stova.profiles.AppearancePrefs(backgroundFilter=app.stova.profiles.BackgroundFilter(maxAge=null)))
        var current by mutableStateOf(entry(a,"18"))
        var composedAge:Int?=-1
        gate(a).complete(Unit)
        compose.setContent {
            val context=LocalContext.current;val images=remember {loader(context)}
            val state=rememberProfileBackdropState("p",prefs)
            SideEffect {composedAge=prefs.backgroundFilter.maxAge}
            StovaTheme(appearance=prefs) {ProfileBackdrop("p",prefs,current,null,state,images)}
        }
        compose.waitUntil(5_000) {shown(a)}
        compose.mainClock.autoAdvance=false
        compose.runOnIdle {
            prefs=prefs.copy(backgroundFilter=app.stova.profiles.BackgroundFilter());current=entry(b,"12")
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        }
        // Deliver the snapshot and composition without advancing through the ambient fade.
        compose.mainClock.advanceTimeByFrame();compose.mainClock.advanceTimeByFrame();compose.waitForIdle()
        org.junit.Assert.assertEquals("new preferences must be composed before checking removed art",15,composedAge)
        compose.onNodeWithTag("backdrop-target:$a").assertDoesNotExist()
        gate(b).completeExceptionally(java.io.IOException("unavailable"))
        compose.mainClock.autoAdvance=true;compose.waitForIdle()
        compose.onNodeWithTag("backdrop-target:$a").assertDoesNotExist()
    }

    // Review finding 7: every return to Home faded the backdrop in from the bare background again.
    @Test fun comingBackToHomeShowsTheBackdropAtOnce() {
        val a = "https://demo.invalid/a"
        var onHome by mutableStateOf(true)
        gate(a).complete(Unit)
        compose.setContent {
            val context = LocalContext.current
            val images = remember { loader(context, memory = false) }
            val state = rememberBackdropState()
            StovaTheme(reduceMotion = true) { if (onHome) BackdropLayer(a, imageLoader = images, state = state) }
        }
        compose.waitUntil(5_000) { shown(a) }
        compose.runOnIdle { onHome = false }
        compose.waitForIdle()
        // A second load of the same picture would now wait for ever: what shows must not depend on one.
        gates[a] = kotlinx.coroutines.CompletableDeferred()
        compose.runOnIdle { onHome = true }
        compose.waitForIdle()
        compose.onNodeWithTag("backdrop-target:$a").assertExists()
    }

    @Test fun keepsTheShownBackdropUntilTheNextHasLoaded() {
        val a = "https://demo.invalid/a"; val b = "https://demo.invalid/b"
        var url by mutableStateOf(a)
        compose.setContent {
            val context = LocalContext.current
            val images = remember { loader(context) }
            StovaTheme(reduceMotion = true) { BackdropLayer(url, imageLoader = images) }
        }
        gate(a).complete(Unit)
        compose.waitUntil(5_000) { shown(a) }
        compose.runOnIdle { url = b }
        compose.waitForIdle()
        // No dip to the bare background while the next title loads.
        compose.onNodeWithTag("backdrop:$a").assertExists()
        compose.onNodeWithTag("backdrop:$b").assertDoesNotExist()
        gate(b).complete(Unit)
        compose.waitUntil(5_000) { shown(b) && !shown(a) }
    }
}
