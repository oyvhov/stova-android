package app.stova.ha.cards

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class PricePresentationTest {
    private val zone=ZoneId.of("Europe/Oslo")
    @Test fun quarterHoursBecomeTwentyFourWeightedHourlyAverages() {
        val start=LocalDate.of(2026,10,24).atStartOfDay(zone).toInstant().toEpochMilli()
        val raw=List(96) {i->PricePoint((i%4+1)*10.0,start+i*900_000L,start+(i+1)*900_000L)}
        val hourly=hourlyPrices(raw,zone)
        assertEquals(24,hourly.size)
        assertTrue(hourly.all {it.value==25.0 && it.end-it.start==3_600_000L})
    }
    @Test fun unequalIntervalsAreWeightedAndMissingHoursAreNotInvented() {
        val start=LocalDate.of(2026,10,24).atStartOfDay(zone).toInstant().toEpochMilli()
        val points=listOf(PricePoint(10.0,start,start+900_000),PricePoint(30.0,start+900_000,start+3_600_000),PricePoint(99.0,start+7_200_000,start+10_800_000))
        assertEquals(listOf(25.0,99.0),hourlyPrices(points,zone).map {it.value})
    }
    @Test fun winterTimeKeepsBothTwoOclockHours() {
        val start=LocalDate.of(2026,10,25).atStartOfDay(zone).toInstant().toEpochMilli()
        val points=List(100) {i->PricePoint(i.toDouble(),start+i*900_000L,start+(i+1)*900_000L)}
        val hours=hourlyPrices(points,zone)
        assertEquals(25,hours.size)
        assertEquals(2,hours.count {Instant.ofEpochMilli(it.start).atZone(zone).hour==2})
    }
    @Test fun priceLevelsUseDailyRangeIncludingNegativeAndFlatDays() {
        assertEquals(PriceLevel.LOW,priceLevel(-20.0,-20.0,100.0))
        assertEquals(PriceLevel.MEDIUM,priceLevel(40.0,-20.0,100.0))
        assertEquals(PriceLevel.HIGH,priceLevel(100.0,-20.0,100.0))
        assertEquals(PriceLevel.MEDIUM,priceLevel(30.0,30.0,30.0))
    }
}
