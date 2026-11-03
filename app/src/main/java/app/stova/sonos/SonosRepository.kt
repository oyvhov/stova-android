package app.stova.sonos

import app.stova.ha.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.*

/** Only the visible Sonos page collects this repository. No discovery or polling while disabled. */
class SonosRepository(private val connection: HaConnection,
    private val musicAssistantLinks: suspend () -> Map<String, String> = { emptyMap() },
    private val enabled: suspend () -> Boolean) {
    private var rooms: List<SonosRoom> = emptyList()
    private var assistants: Map<String, SonosRoom> = emptyMap()
    private val actionMutex = Mutex()
    fun observe(): Flow<SonosState> = channelFlow {
        if (!enabled()) { rooms = emptyList(); assistants = emptyMap(); send(SonosState(loading = false)); return@channelFlow }
        connection.hold()
        try {
            connection.status.collectLatest { status ->
                if (status != HaStatus.CONNECTED) {
                    rooms = emptyList()
                    assistants = emptyMap()
                    send(SonosState(loading = status == HaStatus.CONNECTING, failed = status != HaStatus.CONNECTING))
                } else {
                    try {
                        val registry = connection.registry() ?: throw java.io.IOException("Registry unavailable")
                        val ids = registry.entities.filter { (id, info) -> id.startsWith("media_player.") && info.platform == "sonos" && !info.disabled }.keys
                        val links = musicAssistantLinks().filter { (native, ma) -> native in ids && ma.startsWith("media_player.") &&
                            registry.entities[ma]?.let { it.platform == "music_assistant" && !it.disabled } == true }
                        if (ids.isEmpty()) { rooms = emptyList(); send(SonosState(loading = false)) }
                        else {
                            // The subscription includes its own initial snapshot; no all-entity polling.
                            connection.observeEntities(ids + links.values).collect { entities ->
                                rooms = sonosRooms(entities.values.toList(), registry)
                                assistants = links.mapNotNull { (native, ma) -> entities[ma]?.let { native to SonosRoom(it,
                                    registry.entities[ma]?.name ?: it.attributes.text("friendly_name") ?: ma) } }.toMap()
                                send(SonosState(rooms, loading = false, musicAssistant = assistants))
                            }
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { rooms = emptyList(); assistants = emptyMap(); send(SonosState(loading = false, failed = true)) }
                }
            }
        } finally { connection.release() }
    }
    private suspend fun allowed() = enabled() && connection.status.value == HaStatus.CONNECTED
    private suspend fun currentAssistants(): Map<String, SonosRoom> {
        val links = musicAssistantLinks()
        return assistants.filter { (native, ma) -> links[native] == ma.id && rooms.any { it.id == native && it.available } }
    }
    internal suspend fun nextArtwork(roomId: String, expected: SonosQueueTarget): SonosQueuedArtwork? {
        if (!allowed()) return null
        val room = rooms.find { it.id == roomId } ?: return null
        if (sonosQueueTarget(room, SonosState(rooms, musicAssistant = currentAssistants())) != expected) return null
        return connection.serviceResponse("music_assistant", "get_queue", expected.player)?.let { nextSonosArtwork(expected, it) }
    }
    suspend fun perform(roomId: String, action: SonosAction): Boolean = guarded {
        val current = currentAssistants()
        val explicit = current.values.find { it.id == roomId && it.available }
        val room = rooms.find { it.id == roomId && it.available }
        val target = (explicit ?: room?.let {
            when (action) {
                is SonosAction.Volume, is SonosAction.Mute -> it
                is SonosAction.PlayMedia, is SonosAction.Source -> coordinator(it, rooms)
                else -> sonosPlaybackRoom(it, SonosState(rooms, musicAssistant = current))
            }
        })?.takeIf { it.available } ?: return@guarded false
        val data = buildJsonObject {
            when (action) {
                is SonosAction.Volume -> put("volume_level", action.percent.coerceIn(0, 100) / 100.0)
                is SonosAction.Mute -> put("is_volume_muted", action.muted)
                is SonosAction.Seek -> put("seek_position", action.seconds.coerceIn(0, target.duration))
                is SonosAction.Source -> put("source", action.name)
                is SonosAction.Shuffle -> put("shuffle", action.enabled)
                is SonosAction.Repeat -> put("repeat", action.mode)
                is SonosAction.PlayMedia -> {
                    put("media_content_id", action.media.id); put("media_content_type", action.media.type)
                    if (action.enqueue) put("enqueue", "add")
                }
                else -> Unit
            }
        }
        val (service, feature) = when (action) {
            SonosAction.PlayPause -> if (target.playing) "media_pause" to 1 else "media_play" to 16384
            SonosAction.Previous -> "media_previous_track" to 16
            SonosAction.Next -> "media_next_track" to 32
            is SonosAction.Volume -> "volume_set" to 4
            is SonosAction.Mute -> "volume_mute" to 8
            is SonosAction.Seek -> "media_seek" to 2
            is SonosAction.Source -> "select_source" to 2048
            is SonosAction.Shuffle -> "shuffle_set" to 32768
            is SonosAction.Repeat -> "repeat_set" to 262144
            is SonosAction.PlayMedia -> "play_media" to 512
        }
        if (!target.supports(feature)) return@guarded false
        if (action is SonosAction.Source && action.name !in target.sources) return@guarded false
        if (action is SonosAction.Repeat && action.mode !in setOf("off", "one", "all")) return@guarded false
        if (action is SonosAction.PlayMedia && (!action.media.playable || action.media.id.isBlank() || action.media.type.isBlank() || (action.enqueue && !target.supports(2097152)))) return@guarded false
        connection.callService("media_player", service, target.id, data)
    }
    suspend fun group(anchorId: String, selected: Set<String>, expectedMembers: Set<String>): Boolean = guarded {
        val anchor = rooms.find { it.id == anchorId && it.supports(524288) } ?: return@guarded false
        if (anchor.members.toSet() != expectedMembers) return@guarded false
        val plan = groupPlan(anchor, rooms, selected) ?: return@guarded false
        // HA may apply only part of a group change. Stop on failure; live events show actual membership.
        for (id in plan.leave) {
            if (!allowed() || !connection.callService("media_player", "unjoin", id)) return@guarded false
        }
        plan.join.isEmpty() || (allowed() && connection.callService("media_player", "join", plan.coordinator,
            buildJsonObject { putJsonArray("group_members") { plan.join.forEach { add(it) } } }))
    }
    suspend fun browse(roomId: String, media: SonosMedia? = null): SonosMedia? {
        if (!allowed()) return null
        val explicit = currentAssistants().values.find { it.id == roomId }
        val target = (explicit ?: rooms.find { it.id == roomId && it.available }?.let { coordinator(it, rooms) })
            ?.takeIf { it.available && it.supports(131072) } ?: return null
        return try { connection.browseMedia(target.id, media?.type, media?.id)?.let(::sonosMedia) }
        catch (e: CancellationException) { throw e } catch (_: Exception) { null }
    }
    private suspend fun guarded(block: suspend () -> Boolean): Boolean {
        if (!allowed() || !actionMutex.tryLock()) return false
        return try { block() } catch (e: CancellationException) { throw e } catch (_: Exception) { false }
        finally { actionMutex.unlock() }
    }
}
