package app.stova.homebutton

import app.stova.core.storage.StovaJson
import app.stova.settings.HouseholdSettings
import java.io.File
import kotlinx.serialization.KSerializer

/**
 * Whether Home belongs to Stova right now, read from disk on every call. The Home service runs in
 * its own process (a crash in the UI must not take it down, or Android marks it crashed and stops
 * binding it), so it cannot share the UI's in-memory stores. Both files are tiny and the call only
 * happens on Home or when a stock launcher comes to the front.
 */
class HomeOverrideState(private val filesDir: File) {
    fun isOn(): Boolean {
        val household = read(File(filesDir, "household.json"), HouseholdSettings.serializer(), HouseholdSettings())
        val guard = read(
            File(filesDir, "crash_guard.json"), CrashGuardState.serializer(), CrashGuardState(),
            onFailure = CrashGuardState(tripped = true),
        )
        return household.homeButtonOverride && !guard.tripped
    }

    private fun <T : Any> read(file: File, serializer: KSerializer<T>, default: T, onFailure: T = default): T =
        runCatching { if (file.exists()) StovaJson.decodeFromString(serializer, file.readText()) else default }
            .getOrDefault(onFailure)
}
