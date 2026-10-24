package app.stova.core.security

import org.junit.Assert.assertEquals
import org.junit.Test

class PinPolicyTest {
    @Test fun lockoutTable() = assertEquals(
        listOf(0L, 0, 0, 0, 5_000, 15_000, 30_000, 60_000, 120_000, 300_000, 300_000),
        (0..10).map(PinPolicy::lockoutMillis),
    )
}
