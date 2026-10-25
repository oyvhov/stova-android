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
    val dashboard: List<app.stova.ha.cards.CardConfig> = emptyList(),
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
    val iconOverrides: Map<String, app.stova.apps.IconOverride> = emptyMap(),
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
data class AppearancePrefs(
    val iconPack: String? = null,
    val iconColor: IconColor = IconColor.ORIGINAL,
    val iconFill: IconFill = IconFill.CENTER,
    val reduceMotion: Boolean = false,
    val strongFocus: Boolean = false,
    val cardSize: CardSize = CardSize.MEDIUM,
    val showTitles: Boolean = false,
    val appStyle: AppStyle = AppStyle.BANNER,
    val movieStyle: MovieStyle = MovieStyle.POSTER,
    val heroSize: HeroSize = HeroSize.COMPACT,
    val showOverview: Boolean = false,
    val showGenres: Boolean = true,
    val spotlightTime: SpotlightTime = SpotlightTime.NORMAL,
    val backdropDimming: BackdropDimming = BackdropDimming.MEDIUM,
    val corners: Corners = Corners.ROUND,
    val focusEffect: FocusEffect = FocusEffect.RING,
    val accent: Accent = Accent.LIME,
    val clock: ClockMode = ClockMode.H24,
    val rowSpacing: RowSpacing = RowSpacing.NORMAL,
    val visualTheme: app.stova.core.theme.VisualTheme = app.stova.core.theme.VisualTheme.FOREST,
    val seasonChoice: SeasonChoice = SeasonChoice.CALENDAR,
    val seasonalDecor: SeasonalDecor = SeasonalDecor.FULL,
    @kotlinx.serialization.Transient val seasonOverlay: SeasonOverlay? = null,
)

@Serializable enum class CardSize(val width: Int) { SMALL(200), MEDIUM(248), LARGE(296) }
@Serializable enum class AppStyle { BANNER, ICON }
@Serializable enum class IconColor { ORIGINAL, ACCENT }
@Serializable enum class IconFill { CENTER, CROP }
@Serializable enum class MovieStyle { POSTER, WIDE }
@Serializable enum class HeroSize(val fraction: Float) { COMPACT(0.40f), LARGE(0.52f) }
@Serializable enum class SpotlightTime(val millis: Long) { FAST(8_000), NORMAL(12_000), SLOW(20_000) }
@Serializable enum class BackdropDimming { LIGHT, MEDIUM, DARK }
@Serializable enum class Corners { ROUND, SHARP }
@Serializable enum class FocusEffect { RING, ZOOM }
@Serializable enum class Accent { LIME, OCEAN, IRIS, CORAL, GOLD, MINT, ROSE, PEARL, HOLLY, PUMPKIN }
@Serializable enum class ClockMode { H24, H12, HIDDEN }
@Serializable enum class RowSpacing(val top: Int) { TIGHT(12), NORMAL(22), AIRY(34) }

@Serializable
data class PinFailures(val count: Int = 0, val lastFailureAt: Long = 0)

@Serializable
data class ProfilesFile(
    val profiles: List<Profile> = emptyList(),
    val lastProfileId: String? = null,
    val startInLastProfile: Boolean = true,
    val pinFailures: Map<String, PinFailures> = emptyMap(),
)
