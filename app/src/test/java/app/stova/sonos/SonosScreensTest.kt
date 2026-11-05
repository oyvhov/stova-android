package app.stova.sonos

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.*
import app.stova.core.theme.StovaTheme
import app.stova.profiles.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SonosScreensTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Before fun enabled() = assumeScreensEnabled()
    private fun render(scale: Float = 1f, group: Boolean = false, empty: Boolean = false) {
        val rooms = if (empty) emptyList() else sonosPreviewRooms()
        compose.setContent { StovaTheme(reduceMotion = true) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale)) {
                SonosContent(SonosState(rooms, loading = false), rooms.firstOrNull()?.id, {}, {}, {}, {}, {},
                    artwork = { _, modifier -> SonosPreviewArtwork(modifier) }, onBrowseMusicAssistant = {})
                if (group) SonosGroupDialog(rooms.first(), rooms, false, false, {}, {})
            }
        } }
        if (group) compose.saveScreen("sonos-group", org.robolectric.shadows.ShadowDialog.getLatestDialog().window!!.decorView)
        else compose.saveScreen(if (empty) "sonos-empty" else if (scale > 1f) "sonos-large" else "sonos-playing")
    }
    @Test fun groupedRooms() {
        val ids = listOf("media_player.room0", "media_player.room1")
        val members = kotlinx.serialization.json.JsonArray(ids.map { kotlinx.serialization.json.JsonPrimitive(it) })
        val rooms = sonosPreviewRooms().map { room -> if (room.id !in ids) room else room.copy(entity = room.entity.copy(
            attributes = kotlinx.serialization.json.JsonObject(room.entity.attributes + ("group_members" to members)))) }
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosContent(SonosState(rooms, loading = false), rooms[1].id, {}, {}, {}, {}, {},
                artwork = { _, modifier -> SonosPreviewArtwork(modifier) })
        } }
        compose.saveScreen("sonos-grouped")
    }
    /** Every drawn form, lit as if playing, so the shapes can be judged side by side. */
    @Test fun speakerGallery() {
        val models = listOf("One SL", "Era 100", "Era 300", "Five", "Move 2", "Roam", "Beam (Gen 2)", "Arc Ultra",
            "Ray", "Playbar", "Playbase", "Sub (Gen 3)", "Sub Mini", "Amp", null)
        compose.setContent { StovaTheme(reduceMotion = true) {
            androidx.compose.foundation.layout.FlowRow(androidx.compose.ui.Modifier.fillMaxSize()
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                .padding(48.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                models.forEachIndexed { index, model ->
                    val speaker = sonosSpeaker(model, "Sonos")
                    androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.width(150.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        SonosSpeakerArt(speaker, SonosRoomVisual(speaker, playing = index % 4 != 3, active = false),
                            androidx.compose.ui.Modifier.fillMaxWidth().height(104.dp), "gallery-$index", waves = true)
                        androidx.compose.material3.Text(speaker.label ?: "–", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                    }
                }
            }
        } }
        compose.saveScreen("sonos-speakers")
    }
    /** Transparent logos sit whole on the plate; sleeves fill; a missing picture keeps the quiet mark. */
    @Test fun library() {
        val fake = app.stova.ha.FakeHaConnection()
        val room = sonosPreviewRooms().first()
        fake.entities.value = mapOf(room.id to room.entity)
        fake.registryValue = app.stova.ha.HaRegistry(mapOf(room.id to app.stova.ha.HaEntityInfo(platform = "sonos")))
        val connection = object : app.stova.ha.HaConnection by fake {
            override suspend fun browseMedia(entityId: String, contentType: String?, contentId: String?) =
                app.stova.core.storage.StovaJson.parseToJsonElement("""{"title":"Sonos","media_content_id":"root","media_content_type":"library","children":[
                {"title":"Favorites","media_class":"directory","media_content_id":"f","media_content_type":"favorites","can_expand":true,"thumbnail":"logo:rows"},
                {"title":"Radio","media_class":"app","media_content_id":"r","media_content_type":"app","can_expand":true,"thumbnail":"logo:play"},
                {"title":"Emby/Jellyfin","media_class":"directory","media_content_id":"e","media_content_type":"app","can_expand":true,"thumbnail":"https://brands.home-assistant.io/_/emby/logo.png"},
                {"title":"A Quiet Kind of Blue","media_class":"album","media_content_id":"a","media_content_type":"album","can_play":true,"thumbnail":"album"},
                {"title":"Line-in","media_class":"directory","media_content_id":"l","media_content_type":"app","can_expand":true}]}""").let {
                    (it as kotlinx.serialization.json.JsonObject) }
        }
        val repository = SonosRepository(connection) { true }
        kotlinx.coroutines.runBlocking { repository.observe().first { it.rooms.isNotEmpty() } }
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosLibrary(repository, room, false, false, {}, { path, modifier, fit, onLoad ->
                LaunchedEffect(path) { onLoad() }
                if (!fit) SonosPreviewArtwork(modifier)
                else androidx.compose.material3.Icon(when (path) { "logo:rows" -> app.stova.core.ui.StovaIcons.Rows; "logo:play" -> app.stova.core.ui.StovaIcons.Play
                    else -> app.stova.core.ui.StovaIcons.Link }, null, modifier, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
            }) {}
        } }
        compose.saveScreen("sonos-library")
    }
    @Test fun roomsHidden() {
        val rooms = sonosPreviewRooms()
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosContent(SonosState(rooms, loading = false), rooms.first().id, {}, {}, {}, {}, {},
                artwork = { _, modifier -> SonosPreviewArtwork(modifier) }, onBrowseMusicAssistant = {}, roomsOpen = false)
        } }
        compose.saveScreen("sonos-rooms-hidden")
    }
    /** The Sonos favourite among other favourites: music, paused with a failed picture, and TV audio. */
    @Test fun favouriteCards() {
        val rooms = sonosPreviewRooms()
        fun state(room: SonosRoom) = SonosState(listOf(room), loading = false)
        val playing = sonosCard(SonosPrefs(enabled = true), state(rooms[0]))
        val paused = sonosCard(SonosPrefs(enabled = true), state(rooms[2]))
        val tv = sonosCard(SonosPrefs(enabled = true), state(rooms[0].copy(entity = rooms[0].entity.copy(attributes =
            kotlinx.serialization.json.JsonObject(rooms[0].entity.attributes + ("source" to kotlinx.serialization.json.JsonPrimitive("TV")))))))
        compose.setContent { StovaTheme(reduceMotion = true) {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.fillMaxSize()
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background).padding(48.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                androidx.compose.material3.Text("Favorittappar", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp)) {
                    listOf(playing to true, paused to false, tv to true).forEach { (card, loads) ->
                        androidx.compose.runtime.CompositionLocalProvider(LocalSonosCard provides SonosCardSource(card) { path, modifier, onLoad, onError ->
                            LaunchedEffect(path) { if (loads) onLoad() else onError() }
                            if (loads) SonosPreviewArtwork(modifier)
                        }) {
                            app.stova.apps.ShortcutTile(app.stova.apps.FavoriteTile.Sonos("Sonos"), {}, {})
                        }
                    }
                    app.stova.apps.ShortcutTile(app.stova.apps.FavoriteTile.Sonos("Sonos"), {}, {})
                }
            }
        } }
        compose.saveScreen("sonos-card")
    }
    @Test fun nowPlaying() = render()
    private fun seasonal(choice: SeasonChoice, name: String) {
        val rooms = sonosPreviewRooms()
        compose.setContent { StovaTheme(reduceMotion = true,
            appearance = choice.applyTo(AppearancePrefs(seasonalDecor = SeasonalDecor.CALM))) {
            SonosContent(SonosState(rooms, loading = false), rooms.first().id, {}, {}, {}, {}, {},
                artwork = { _, modifier -> SonosPreviewArtwork(modifier) })
        } }
        compose.saveScreen(name)
    }
    /** Full screen as it is lit: art, no art, TV audio, seasons and large type. */
    private fun immersive(name: String, room: SonosRoom = sonosPreviewRooms().first(), choice: SeasonChoice = SeasonChoice.NONE,
        scale: Float = 1f, backgroundArt: Boolean = true) {
        compose.setContent { StovaTheme(reduceMotion = true, appearance = choice.applyTo(AppearancePrefs(seasonalDecor = SeasonalDecor.CALM))) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale)) {
                SonosContent(SonosState(listOf(room), loading = false), room.id, {}, {}, {}, {}, {}, backgroundArt = backgroundArt,
                    artwork = { _, modifier -> SonosPreviewArtwork(modifier) }, immersive = true)
            }
        } }
        compose.saveScreen(name)
    }
    private fun attrs(room: SonosRoom, vararg extra: Pair<String, kotlinx.serialization.json.JsonElement>, drop: String? = null) = room.copy(entity =
        room.entity.copy(attributes = kotlinx.serialization.json.JsonObject(room.entity.attributes.filterKeys { it != drop } + extra)))
    @Test fun immersiveArt() = immersive("sonos-fullscreen")
    @Test fun immersiveUprightSpeaker() = immersive("sonos-fullscreen-era", sonosPreviewRooms()[1])
    @Test fun immersiveLongRoomName() = immersive("sonos-fullscreen-long-room",
        sonosPreviewRooms()[1].copy(name = "Sonos Kjøkken og spisestove"), scale = 2f)
    @Test fun immersiveVolumePopup() {
        immersive("sonos-fullscreen")
        compose.onNodeWithTag("sonos-volume-button").performClick()
        val popup = compose.runOnIdle {
            android.view.inspector.WindowInspector.getGlobalWindowViews().last { it !== compose.activity.window.decorView }
        }
        compose.saveScreen("sonos-fullscreen-volume", popup)
    }
    @Test fun favoritesOnPlayer() {
        val rooms = sonosPreviewRooms()
        val shelf = listOf("Kveldsmiks", "Favorittar", "Fredag", "Rolig jazz", "Heime", "På tur").mapIndexed { index, title ->
            SonosMedia("favorite:$index", "favorite_item_id", title, "/shelf/$index", playable = true, mediaClass = "playlist")
        }
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosContent(SonosState(rooms, loading = false), rooms.first().id, {}, {}, {}, {}, {},
                artwork = { _, modifier -> SonosPreviewArtwork(modifier) }, shelf = shelf,
                shelfThumbnail = { _, modifier, _, loaded -> SonosPreviewArtwork(modifier); SideEffect { loaded() } })
        } }
        compose.saveScreen("sonos-player-favorites")
    }
    @Test fun immersiveNoArt() = immersive("sonos-fullscreen-noart", attrs(sonosPreviewRooms()[1], drop = "entity_picture"))
    @Test fun immersiveTv() = immersive("sonos-fullscreen-tv", attrs(sonosPreviewRooms().first(),
        "source" to kotlinx.serialization.json.JsonPrimitive("TV")))
    @Test fun immersiveThemeOnly() = immersive("sonos-fullscreen-themeonly", backgroundArt = false)
    @Test fun immersiveChristmas() = immersive("sonos-fullscreen-christmas", choice = SeasonChoice.CHRISTMAS)
    @Test fun immersiveHalloween() = immersive("sonos-fullscreen-halloween", choice = SeasonChoice.HALLOWEEN)
    @Test fun immersiveLargeFont() = immersive("sonos-fullscreen-large", scale = 2f)
    @Test fun christmas() = seasonal(SeasonChoice.CHRISTMAS, "sonos-christmas")
    @Test fun halloween() = seasonal(SeasonChoice.HALLOWEEN, "sonos-halloween")
    @Test fun largeFont() = render(2f)
    @Test fun groupRooms() = render(group = true)
    @Test fun inlineGroupRooms() {
        val rooms = sonosPreviewRooms()
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosContent(SonosState(rooms, loading = false), rooms.first().id, {}, {}, {}, {}, {},
                artwork = { _, modifier -> SonosPreviewArtwork(modifier) }, onApplyGroup = { _, _ -> })
        } }
        compose.onNodeWithTag("sonos-room-${rooms.first().id}").performSemanticsAction(SemanticsActions.OnLongClick)
        compose.onNodeWithTag("sonos-room-${rooms[1].id}").performClick()
        compose.saveScreen("sonos-group-inline")
    }
    @Test fun emptyRooms() = render(empty = true)
    @Test fun homeWidget() {
        val room = sonosPreviewRooms().first()
        compose.setContent { StovaTheme(reduceMotion = true) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val demoApp = app.stova.apps.InstalledApp("demo/.Main", "demo", ".Main", "Demo", true)
            Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
                SonosPreviewArtwork(Modifier.fillMaxSize().graphicsLayer { alpha = .38f })
                app.stova.home.HomeContent(listOf(app.stova.home.HomeRowUi.Apps(app.stova.home.HomeRowKey.APPS, listOf(demoApp))),
                    null, app.stova.home.HomeRowKey.APPS, app.stova.apps.AppArtCache(ctx),
                    app.stova.apps.PackageManagerAppCatalog(ctx, emptySet()), {}, {}, {}, {},
                    aboveApps = { modifier ->
                        SonosNowPlaying(room, room, false, false, {}, {}, modifier, artwork = { _, art -> SonosPreviewArtwork(art) })
                    })
            }
        } }
        compose.saveScreen("sonos-home-widget")
    }
}
