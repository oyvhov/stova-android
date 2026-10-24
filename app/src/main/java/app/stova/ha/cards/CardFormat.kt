package app.stova.ha.cards

import app.stova.ha.HaEntity
import java.text.NumberFormat
import java.util.Locale
import kotlinx.serialization.json.*

sealed interface StateText {
    data class Number(val value: Double, val unit: String?) : StateText
    data class OnOff(val on: Boolean) : StateText
    data class Raw(val text: String) : StateText
    data object Unavailable : StateText
}
fun stateText(entity: HaEntity?): StateText = when {
    entity == null || entity.state in listOf("unavailable","unknown") -> StateText.Unavailable
    entity.state in listOf("on","off") -> StateText.OnOff(entity.state == "on")
    else -> entity.state.toDoubleOrNull()?.takeIf { it.isFinite() }?.let { StateText.Number(it, entity.attributes["unit_of_measurement"]?.jsonPrimitive?.contentOrNull) } ?: StateText.Raw(entity.state)
}
fun formatNumber(value: Double, locale: Locale): String = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1; minimumFractionDigits = 0; isGroupingUsed = false }.format(value)
fun priceCurve(entity: HaEntity): List<Double> {
    val raw = entity.attributes["raw_today"] as? JsonArray
    return if (raw != null) raw.mapNotNull { (it as? JsonObject)?.number("value") }
    else (entity.attributes["today"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.doubleOrNull?.takeIf(Double::isFinite) }
}
fun friendlyName(entity: HaEntity): String = entity.attributes["friendly_name"]?.jsonPrimitive?.contentOrNull ?: entity.id
