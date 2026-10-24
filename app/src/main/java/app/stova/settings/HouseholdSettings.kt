package app.stova.settings

import kotlinx.serialization.Serializable

/** Settings for the whole household, not one profile. Plans 2 and 4 add fields here. */
@Serializable
data class HouseholdSettings(val homeButtonOverride: Boolean = true, val haBaseUrl: String? = null)
