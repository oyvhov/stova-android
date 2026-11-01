package app.stova.ha

import app.stova.core.storage.JsonFileStore
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/** Cinema mode, set up under Smarthus: the chosen lights dim while Spole plays. Off until someone turns it on. */
@Serializable
data class CinemaPrefs(val enabled: Boolean = false, val lights: List<String> = emptyList(), val level: Int = 10) {
    companion object {
        /** Percent during the film; 0 turns the lights off. */
        val LEVELS = listOf(0, 5, 10, 20, 30, 50)
    }
}

/** How a light was before the film: its brightness (0–255, null when it has none), and whether cinema mode turned it off. */
@Serializable
data class DimmedLight(val brightness: Int? = null, val turnedOff: Boolean = false)

/** The lights cinema mode has changed and not yet put back, kept on disk so a restart cannot leave the room dark. */
@Serializable
data class CinemaState(val lights: Map<String, DimmedLight> = emptyMap())

internal data class LightCall(val entityId: String, val service: String, val data: JsonObject = JsonObject(emptyMap()))

/**
 * The calls that bring [lights] down to [level] percent, and how each light was. Only lights that are on
 * and brighter than the level are touched, so a lamp already low stays as it is. A lamp that cannot dim
 * goes off, and at 0 % every light that is on does.
 */
internal fun cinemaDim(lights: List<HaEntity>, level: Int): Pair<List<LightCall>, Map<String, DimmedLight>> {
    val target = (level * 255 / 100.0).roundToInt()
    val calls = mutableListOf<LightCall>()
    val was = mutableMapOf<String, DimmedLight>()
    lights.filter { it.state == "on" }.forEach { light ->
        val brightness = light.brightness
        if (level == 0 || !light.dimmable) {
            calls += LightCall(light.id, "turn_off", light.fade())
            was[light.id] = DimmedLight(brightness, turnedOff = true)
        } else if (brightness != null && brightness > target) {
            calls += LightCall(light.id, "turn_on", light.fade { put("brightness_pct", level) })
            was[light.id] = DimmedLight(brightness)
        }
    }
    return calls to was
}

/**
 * The calls that put the lights back. A light someone switched by hand during the film stays as they left
 * it: a dimmed light that is now off, or one cinema mode turned off that is now on.
 */
internal fun cinemaRestore(lights: List<HaEntity>, was: Map<String, DimmedLight>): List<LightCall> = lights.mapNotNull { light ->
    val before = was[light.id] ?: return@mapNotNull null
    if (light.state != if (before.turnedOff) "off" else "on") return@mapNotNull null
    LightCall(light.id, "turn_on", light.fade { before.brightness?.let { put("brightness", it) } })
}

private val HaEntity.brightness: Int? get() = (attributes["brightness"] as? JsonPrimitive)?.doubleOrNull?.roundToInt()

private val HaEntity.dimmable: Boolean get() {
    val modes = (attributes["supported_color_modes"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    return modes?.any { it != "onoff" } ?: (brightness != null)
}

/** A two-second fade where the light has one (Home Assistant's transition feature, 32). */
private fun HaEntity.fade(extra: JsonObjectBuilder.() -> Unit = {}) = buildJsonObject {
    extra()
    val features = (attributes["supported_features"] as? JsonPrimitive)?.intOrNull ?: 0
    if (features and 32 != 0) put("transition", 2)
}

/**
 * Dims the chosen lights when Spole starts playing and puts them back when it pauses or stops, or when
 * Stova comes to the front. [prefs] is null when Home Assistant is not set up. Nothing is asked of Home
 * Assistant while cinema mode is off, or when there is nothing to put back. [withConnection] runs the block
 * once connected and says whether it could.
 */
class CinemaMode(
    private val prefs: suspend () -> CinemaPrefs?,
    private val store: JsonFileStore<CinemaState>,
    private val withConnection: suspend (suspend (HaConnection) -> Unit) -> Boolean,
) {
    private val mutex = Mutex()

    /** What Spole says: `playing`, `paused` or `stopped`. */
    suspend fun onSpole(state: String?) {
        when (state) {
            "playing" -> playing()
            "paused", "stopped" -> restore()
        }
    }

    suspend fun playing() = mutex.withLock {
        val settings = prefs()?.takeIf { it.enabled && it.lights.isNotEmpty() } ?: return@withLock
        attempt { ha ->
            val (calls, was) = cinemaDim(ha.states().filter { it.id in settings.lights }, settings.level)
            // Written before the calls, so the room comes back even if Stova stops halfway. A light already
            // remembered keeps what it was before the film, not the dim level of a pause that was missed.
            store.update { state -> state.copy(lights = was + state.lights) }
            calls.forEach { ha.callService("light", it.service, it.entityId, it.data) }
        }
    }

    suspend fun restore() = mutex.withLock {
        val was = store.current().lights
        if (was.isEmpty()) return@withLock
        // Home Assistant was disconnected from Stova since: there is nothing left to put the lights back through.
        if (prefs() == null) { store.update { CinemaState() }; return@withLock }
        val done = attempt { ha ->
            cinemaRestore(ha.states().filter { it.id in was }, was).forEach { ha.callService("light", it.service, it.entityId, it.data) }
        }
        if (done) store.update { CinemaState() }
    }

    private suspend fun attempt(block: suspend (HaConnection) -> Unit): Boolean = try {
        withConnection(block)
    } catch (e: CancellationException) { throw e } catch (_: Exception) { false }
}

/**
 * Spole plays while Stova is in the background, where the socket rests. Cinema mode wakes it for the calls
 * and lets it rest again afterwards, unless Stova has come to the front meanwhile.
 */
suspend fun withCinemaConnection(socket: HaSocket, visible: () -> Boolean, block: suspend (HaConnection) -> Unit): Boolean {
    socket.start()
    socket.hold()
    try {
        withTimeoutOrNull(10_000) { socket.status.first { it == HaStatus.CONNECTED } } ?: return false
        block(socket)
        return true
    } finally {
        socket.release()
        if (!visible()) socket.stop()
    }
}
