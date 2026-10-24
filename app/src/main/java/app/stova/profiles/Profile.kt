package app.stova.profiles

import app.stova.core.security.PinHash
import app.stova.core.theme.AvatarColor
import app.stova.home.HomeRowKey
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val name: String,
    val avatar: AvatarColor,
    val pin: PinHash? = null,
    val kid: Boolean = false,
    val kidSmartHomeControl: Boolean = false,
    val apps: AppPrefs = AppPrefs(),
    val home: HomePrefs = HomePrefs(),
    val appearance: AppearancePrefs = AppearancePrefs(),
)

/** App keys are `"<package>/<activity>"`. */
@Serializable
data class AppPrefs(
    val favorites: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val kidAllowed: Set<String> = emptySet(),
)

@Serializable
data class HomePrefs(
    val startFocus: StartFocus = StartFocus.FIRST_MEDIA_ROW,
    val rowOrder: List<HomeRowKey> = emptyList(),
    val hiddenRows: Set<HomeRowKey> = emptySet(),
    val backdropRotation: Boolean = true,
)

@Serializable
enum class StartFocus { FIRST_MEDIA_ROW, FAVORITE_APPS }

@Serializable
data class AppearancePrefs(val reduceMotion: Boolean = false, val strongFocus: Boolean = false)

@Serializable
data class PinFailures(val count: Int = 0, val lastFailureAt: Long = 0)

@Serializable
data class ProfilesFile(
    val profiles: List<Profile> = emptyList(),
    val lastProfileId: String? = null,
    val startInLastProfile: Boolean = true,
    val pinFailures: Map<String, PinFailures> = emptyMap(),
)
