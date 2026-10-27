package app.stova.ha.cards

import app.stova.ha.HaEntity
import app.stova.ha.HaRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import app.stova.ha.text

@Serializable enum class CardType { LIGHT, TOGGLE, CLIMATE, SENSOR, WEATHER, ENERGY_PRICE, MEDIA_PLAYER, SCENE, CAMERA, COVER, LOCK }
@Serializable enum class CardSize(val columns: Int, val rows: Int) { S(1,1), M(2,1), L(2,2) }
@Serializable data class CardConfig(
    val id: String, val type: CardType, val entityId: String,
    // Read legacy JSON and keep constructor compatibility. All new layout and edits use size.
    val wide: Boolean = false, val onHome: Boolean = false,
    val size: CardSize = if (wide) CardSize.M else CardSize.S,
    val name: String? = null, val icon: String? = null, val favorite: Boolean = false,
    /** Climate modes and presets left out of this card's panel, by Home Assistant name. */
    val hiddenModes: Set<String> = emptySet(),
)
data class DashboardSection(val room: String?, val favorites: Boolean, val cards: List<CardConfig>)
@Serializable enum class DashboardGrouping { NONE, ROOM }
@Serializable enum class DashboardDensity(val minimumWidth: Int) { COMPACT(152), BALANCED(200), AIRY(252) }
@Serializable data class DashboardPrefs(
    val grouping: DashboardGrouping = DashboardGrouping.NONE,
    val density: DashboardDensity = DashboardDensity.BALANCED,
)

fun dashboardColumns(width: Float, density: DashboardDensity, fontScale: Float = 1f): Int =
    ((width + 16) / (density.minimumWidth * maxOf(1f, fontScale * 0.8f) + 16)).toInt().coerceIn(2, 6)

fun dashboardSections(cards: List<CardConfig>, registry: HaRegistry, grouping: DashboardGrouping = DashboardGrouping.NONE): List<DashboardSection> = buildList {
    if (grouping == DashboardGrouping.NONE) {
        if (cards.isNotEmpty()) add(DashboardSection(null, false, cards))
        return@buildList
    }
    cards.filter { it.favorite }.takeIf { it.isNotEmpty() }?.let { add(DashboardSection(null,true,it)) }
    cards.filterNot { it.favorite }.groupBy { registry.entities[it.entityId]?.room }
        .forEach { (room, list) -> add(DashboardSection(room,false,list)) }
}

fun cardTypesFor(entity: HaEntity): List<CardType> = when (entity.id.substringBefore('.')) {
    "light" -> listOf(CardType.LIGHT)
    "switch", "input_boolean", "fan" -> listOf(CardType.TOGGLE)
    "climate" -> listOf(CardType.CLIMATE)
    "sensor" -> if (isEnergyPrice(entity)) listOf(CardType.ENERGY_PRICE, CardType.SENSOR) else listOf(CardType.SENSOR)
    "binary_sensor", "alarm_control_panel" -> listOf(CardType.SENSOR)
    "weather" -> listOf(CardType.WEATHER)
    "media_player" -> listOf(CardType.MEDIA_PLAYER)
    "scene", "script" -> listOf(CardType.SCENE)
    "camera" -> listOf(CardType.CAMERA)
    "cover" -> listOf(CardType.COVER)
    "lock" -> listOf(CardType.LOCK)
    else -> emptyList()
}

fun isEnergyPrice(entity:HaEntity):Boolean {
    val values=(entity.attributes["raw_today"] ?: entity.attributes["today"]) as? JsonArray
    val curve=values?.any { (it as? JsonPrimitive)?.doubleOrNull!=null || (it as? JsonObject)?.number("value")!=null }==true
    val unit=entity.attributes.text("unit_of_measurement").orEmpty().lowercase()
    return curve || (unit.contains("/kwh") && unit!="kwh/kwh") ||
        (unit in setOf("øre","ore","öre","nok","kr","eur") && (entity.id.contains("price") || entity.id.contains("pris")))
}
