package app.stova.ha

import app.stova.ha.cards.number
import kotlinx.serialization.json.*
import java.time.*

/** Tibber exposes day-ahead prices as a read-only service, not state attributes. */
fun tibberPricesRequest(id:Int,date:LocalDate=LocalDate.now()) = callService(id,"tibber","get_prices",null,buildJsonObject {
    put("start","${date} 00:00:00");put("end","${date.plusDays(1)} 23:59:59")
},returnResponse=true)

fun parseTibberPrices(result:JsonObject,entity:HaEntity,date:LocalDate=LocalDate.now(),zone:ZoneId=ZoneId.systemDefault()):JsonObject {
    val homes=(result["response"] as? JsonObject)?.get("prices") as? JsonObject ?: return JsonObject(emptyMap())
    val name=entity.attributes.text("app_nickname") ?: entity.attributes.text("home_name")
    val prices=(name?.let {homes[it]} ?: homes.values.singleOrNull()) as? JsonArray ?: return JsonObject(emptyMap())
    val factor=if(entity.attributes.text("unit_of_measurement")?.lowercase() in setOf("øre","ore","öre")) 100 else 1
    val points=prices.mapNotNull {raw ->
        val j=raw as? JsonObject ?: return@mapNotNull null
        val time=j.text("start_time")?.replaceFirst(' ','T')?.let {runCatching {haMillis(it)}.getOrNull()} ?: return@mapNotNull null
        val value=j.number("price") ?: return@mapNotNull null
        time to value*factor
    }.sortedBy {it.first}
    val step=points.zipWithNext {a,b->b.first-a.first}.filter {it in 1..3_600_000}.minOrNull() ?: 3_600_000
    fun day(target:LocalDate)=JsonArray(points.mapIndexedNotNull {i,(time,value) ->
        if(Instant.ofEpochMilli(time).atZone(zone).toLocalDate()!=target) return@mapIndexedNotNull null
        buildJsonObject {
            put("start",Instant.ofEpochMilli(time).toString())
            put("end",Instant.ofEpochMilli(minOf(points.getOrNull(i+1)?.first ?: time+step,time+step)).toString())
            put("value",value)
        }
    })
    return buildJsonObject {put("raw_today",day(date));put("raw_tomorrow",day(date.plusDays(1)));put("tomorrow_valid",day(date.plusDays(1)).isNotEmpty())}
}
