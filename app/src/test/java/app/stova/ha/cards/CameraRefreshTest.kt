package app.stova.ha.cards

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.security.InMemorySecretStore
import app.stova.core.theme.StovaTheme
import app.stova.ha.*
import coil3.ImageLoader
import coil3.Uri
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class CameraRefreshTest {
    @get:Rule val compose=createComposeRule()
    private val requests=CopyOnWriteArrayList<String>()
    private val entity=HaEntity("camera.test","idle",JsonObject(emptyMap()),0,0)
    @Composable private fun source():HaImageSource {
        val context=LocalContext.current
        return remember {
            val auth=HaAuth(OkHttpClient(),InMemorySecretStore(),{HaAddress("http://127.0.0.1:8123","ws://127.0.0.1:8123/api/websocket")})
            runBlocking {auth.useLongLivedToken("fictional-test-token")}
            val loader=ImageLoader.Builder(context).components {
                add(Fetcher.Factory<Uri> {data,_,_->Fetcher {
                    requests+=data.toString()
                    ImageFetchResult(Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888).asImage(),false,DataSource.MEMORY)
                }})
            }.build()
            HaImageSource("http://127.0.0.1:8123",auth,loader)
        }
    }
    @Test fun cardRefreshesAtTenSecondsAndStopsOffscreen() {
        var visible by mutableStateOf(true)
        compose.setContent {StovaTheme {CameraCard(entity,source(),visible)}}
        compose.waitUntil(5_000) {requests.size==1}
        compose.mainClock.advanceTimeBy(9_000);compose.waitForIdle();assertEquals(1,requests.size)
        compose.mainClock.advanceTimeBy(1_100)
        compose.waitUntil(5_000) {requests.size==2}
        assertNotEquals(requests[0],requests[1])
        compose.runOnIdle {visible=false}
        compose.mainClock.advanceTimeBy(20_000);compose.waitForIdle();assertEquals(2,requests.size)
    }
    @Test fun fullScreenRefreshesAtTwoSecondsAndStopsWhenClosed() {
        var dialog by mutableStateOf<CardDialog?>(CardDialog.Camera(entity.id))
        compose.setContent {StovaTheme {CardDialogHost(dialog,mapOf(entity.id to entity),FakeHaConnection(),source(),false,{_,_->},{dialog=null})}}
        compose.waitUntil(5_000) {requests.size==1}
        compose.mainClock.advanceTimeBy(2_100)
        compose.waitUntil(5_000) {requests.size==2}
        compose.runOnIdle {dialog=null}
        compose.mainClock.advanceTimeBy(6_000);compose.waitForIdle();assertEquals(2,requests.size)
    }
}
