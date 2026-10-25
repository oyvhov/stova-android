package app.stova.ha.cards

import java.time.*
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

enum class PriceLevel { LOW, MEDIUM, HIGH }
fun priceLevel(value:Double,min:Double,max:Double):PriceLevel = when {
    max<=min -> PriceLevel.MEDIUM
    (value-min)/(max-min)<1.0/3 -> PriceLevel.LOW
    (value-min)/(max-min)<2.0/3 -> PriceLevel.MEDIUM
    else -> PriceLevel.HIGH
}

/** Duration weighting also handles sparse data and the repeated hour at winter time. */
fun hourlyPrices(points:List<PricePoint>,zone:ZoneId):List<PricePoint> {
    val hours=sortedMapOf<Long,Pair<Double,Long>>()
    points.filter {it.value.isFinite() && it.end>it.start}.forEach {p ->
        var hour=Instant.ofEpochMilli(p.start).atZone(zone).truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli()
        while(hour<p.end) {
            val duration=minOf(p.end,hour+3_600_000)-maxOf(p.start,hour)
            val previous=hours[hour] ?: (0.0 to 0L)
            hours[hour]=(previous.first+p.value*duration) to (previous.second+duration)
            hour+=3_600_000
        }
    }
    return hours.map {(start,weighted)->PricePoint(weighted.first/weighted.second,start,start+3_600_000)}
}

fun priceWindow(point:PricePoint,zone:ZoneId,quarters:Boolean=false):String {
    val start=Instant.ofEpochMilli(point.start).atZone(zone)
    val end=Instant.ofEpochMilli(point.end).atZone(zone)
    val pattern=DateTimeFormatter.ofPattern(if(quarters) "HH:mm" else "HH")
    val endText=if(end.toLocalDate()>start.toLocalDate() && end.hour==0) (if(quarters) "24:00" else "24") else end.format(pattern)
    return "${start.format(pattern)}–$endText"
}
