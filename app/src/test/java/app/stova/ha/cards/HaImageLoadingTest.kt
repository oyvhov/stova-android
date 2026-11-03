package app.stova.ha.cards

import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.security.InMemorySecretStore
import app.stova.core.security.SecretStore
import app.stova.ha.HaAuth
import coil3.ImageLoader
import coil3.Uri
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class HaImageLoadingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun waitingForCredentialsIsNotAnImageFailureAndTheImageCanStillLoad() {
        val credentials = CompletableDeferred<String?>()
        var errors = 0
        var loaded = false
        compose.setContent {
            val context = LocalContext.current
            val source = remember {
                val secrets = object : SecretStore by InMemorySecretStore() {
                    override suspend fun read(key: String): String? = credentials.await()
                }
                val loader = ImageLoader.Builder(context).components {
                    add(Fetcher.Factory<Uri> { _, _, _ -> Fetcher {
                        ImageFetchResult(Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).asImage(), false, DataSource.MEMORY)
                    } })
                }.build()
                HaImageSource("http://127.0.0.1:8123", HaAuth(okhttp3.OkHttpClient(), secrets, { null }), loader)
            }
            // A failed cover is removed just as it is by the Sonos favourite card.
            var failed by remember { mutableStateOf(false) }
            if (!failed) HaImage("/api/album.png", source, Modifier.size(100.dp),
                onLoad = { loaded = true }, onError = { errors++; failed = true })
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals("No request has failed while credentials are pending", 0, errors) }
        credentials.complete("""{"access":"fictional-test-token","expiresAt":9223372036854775807}""")
        compose.waitUntil(5_000) { loaded }
        compose.runOnIdle { assertEquals(0, errors) }
    }
}
