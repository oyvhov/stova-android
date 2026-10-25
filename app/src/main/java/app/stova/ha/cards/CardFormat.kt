package app.stova.ha.cards

import app.stova.ha.HaEntity
import java.text.NumberFormat
import java.util.Locale
import kotlinx.serialization.json.*
import java.time.*

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

data class PricePoint(val value:Double,val start:Long,val end:Long)
fun pricePoints(entity:HaEntity,tomorrow:Boolean=false,date:LocalDate=LocalDate.now(),zone:ZoneId=ZoneId.systemDefault()):List<PricePoint> {
    if(tomorrow && (entity.attributes["tomorrow_valid"] as? JsonPrimitive)?.booleanOrNull==false) return emptyList()
    val key=if(tomorrow) "tomorrow" else "today"
    val raw=entity.attributes["raw_$key"] as? JsonArray
    val values=raw ?: entity.attributes[key] as? JsonArray ?: return emptyList()
    val day=if(tomorrow) date.plusDays(1) else date
    val begin=day.atStartOfDay(zone).toInstant().toEpochMilli()
    val duration=day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()-begin
    return values.take(200).mapIndexedNotNull {i,value ->
        val obj=value as? JsonObject
        val number=(obj?.get("value") as? JsonPrimitive)?.doubleOrNull ?: (value as? JsonPrimitive)?.doubleOrNull
        if(number==null || !number.isFinite()) return@mapIndexedNotNull null
        val start=obj?.get("start")?.jsonPrimitive?.contentOrNull?.let {runCatching {app.stova.ha.haMillis(it)}.getOrNull()} ?: (begin+duration*i/values.size)
        val end=obj?.get("end")?.jsonPrimitive?.contentOrNull?.let {runCatching {app.stova.ha.haMillis(it)}.getOrNull()} ?: (begin+duration*(i+1)/values.size)
        PricePoint(number,start,end)
    }
}
