package app.stova.core.ui

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockTest {
    @Test fun formats24h() = assertEquals(
        "21:04",
        clockText(Instant.parse("2026-10-24T19:04:00Z").toEpochMilli(), ZoneId.of("Europe/Oslo"), Locale.forLanguageTag("nn")),
    )
}
