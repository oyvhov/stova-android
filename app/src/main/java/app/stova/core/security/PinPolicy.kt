package app.stova.core.security

/** How long a profile stays locked after a run of wrong PINs (spec §4). */
object PinPolicy {
    fun lockoutMillis(failures: Int): Long = when {
        failures <= 3 -> 0
        failures == 4 -> 5_000
        failures == 5 -> 15_000
        failures == 6 -> 30_000
        failures == 7 -> 60_000
        failures == 8 -> 120_000
        else -> 300_000
    }
}
