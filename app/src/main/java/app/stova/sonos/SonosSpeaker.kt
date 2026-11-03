package app.stova.sonos

/** The physical forms Stova can draw truthfully. Several models share a silhouette. */
enum class SonosShape { ONE, ERA_100, ERA_300, FIVE, BEAM, ARC, RAY, PLAYBAR, PLAYBASE, MOVE, ROAM, SUB, SUB_MINI, COMPONENT, UNKNOWN }

/** [label] is the model HA reports, without the brand; null when the registry has none. */
data class SonosSpeaker(val shape: SonosShape, val label: String?)

private val generation = Regex("""\(?\s*(gen(eration)?\s*\d+|\d+(st|nd|rd|th)\s*gen(eration)?)\s*\)?""", RegexOption.IGNORE_CASE)
private val brand = Regex("""^sonos\s+""", RegexOption.IGNORE_CASE)

/** Exact names after generations and punctuation are removed: "Arcade" is not an Arc. */
private val shapes = mapOf(
    "one" to SonosShape.ONE, "onesl" to SonosShape.ONE, "play1" to SonosShape.ONE,
    "era100" to SonosShape.ERA_100, "era100sl" to SonosShape.ERA_100, "era100pro" to SonosShape.ERA_100,
    "era300" to SonosShape.ERA_300,
    "five" to SonosShape.FIVE, "play5" to SonosShape.FIVE, "play3" to SonosShape.FIVE,
    "beam" to SonosShape.BEAM, "arc" to SonosShape.ARC, "arcultra" to SonosShape.ARC, "ray" to SonosShape.RAY,
    "playbar" to SonosShape.PLAYBAR, "playbase" to SonosShape.PLAYBASE,
    "move" to SonosShape.MOVE, "move2" to SonosShape.MOVE,
    "roam" to SonosShape.ROAM, "roamsl" to SonosShape.ROAM, "roam2" to SonosShape.ROAM,
    "sub" to SonosShape.SUB, "sub4" to SonosShape.SUB, "submini" to SonosShape.SUB_MINI,
    "amp" to SonosShape.COMPONENT, "connectamp" to SonosShape.COMPONENT, "connect" to SonosShape.COMPONENT, "port" to SonosShape.COMPONENT,
    "zp80" to SonosShape.COMPONENT, "zp90" to SonosShape.COMPONENT, "zp100" to SonosShape.COMPONENT, "zp120" to SonosShape.COMPONENT,
)

fun sonosSpeaker(model: String?, manufacturer: String?): SonosSpeaker {
    val label = model?.trim()?.replace(Regex("""\s+"""), " ")?.replace(brand, "")?.takeIf(String::isNotBlank)
        ?: return SonosSpeaker(SonosShape.UNKNOWN, null)
    val sonos = manufacturer.isNullOrBlank() || manufacturer.contains("sonos", ignoreCase = true)
    val key = label.replace(generation, "").lowercase().filter(Char::isLetterOrDigit)
    return SonosSpeaker(shapes[key]?.takeIf { sonos } ?: SonosShape.UNKNOWN, label)
}

/** The speaker only moves while music really plays: never for TV, pause, disconnection, a hidden page or reduced motion. */
fun sonosSpeakerActive(player: SonosRoom?, resumed: Boolean, still: Boolean) =
    resumed && !still && player != null && player.available && player.playing && !player.tvAudio

/** [playing] lights the speaker steadily; [active] also lets it breathe. */
data class SonosRoomVisual(val speaker: SonosSpeaker, val playing: Boolean, val active: Boolean)

/** A room keeps its own native speaker; the group's music, perhaps through Music Assistant, decides whether it moves. */
fun sonosRoomVisual(room: SonosRoom, state: SonosState, resumed: Boolean, still: Boolean): SonosRoomVisual {
    val native = coordinator(room, state.rooms)
    val player = if (!room.available || room.tvAudio || native?.tvAudio == true) null else sonosPlaybackRoom(room, state)
    return SonosRoomVisual(room.speaker, sonosSpeakerActive(player, resumed = true, still = false), sonosSpeakerActive(player, resumed, still))
}
