package app.stova.settings

import kotlinx.serialization.Serializable

/** Settings shared by every profile in the household. */
@Serializable
data class HouseholdSettings(
    val homeButtonOverride: Boolean = true,
    val deviceId: String = "",
    val haBaseUrl: String? = null,
    val cinema: app.stova.ha.CinemaPrefs = app.stova.ha.CinemaPrefs(),
)
