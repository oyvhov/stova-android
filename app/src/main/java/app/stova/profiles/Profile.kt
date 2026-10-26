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
    val smartHome: app.stova.ha.cards.DashboardPrefs = app.stova.ha.cards.DashboardPrefs(),
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
    val rowFormats: Map<HomeRowKey, RowStyle> = emptyMap(),
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
    val cardSize: ArtworkSize = ArtworkSize.COMPACT,
    val showTitles: Boolean = true,
    val appStyle: AppStyle = AppStyle.BANNER,
    val appTileBackground: Boolean = true,
    val movieStyle: MovieStyle = MovieStyle.POSTER,
    val heroSize: HeroSize = HeroSize.COMPACT,
    val showOverview: Boolean = false,
    val showGenres: Boolean = true,
    val spotlightTime: SpotlightTime = SpotlightTime.NORMAL,
    val backdropDimming: BackdropDimming = BackdropDimming.MEDIUM,
    val corners: Corners = Corners.ROUND,
    val focusEffect: FocusEffect = FocusEffect.LIFT,
    val accent: Accent = Accent.LIME,
    val clock: ClockMode = ClockMode.H24,
    val rowSpacing: RowSpacing = RowSpacing.NORMAL,
    val visualTheme: app.stova.core.theme.VisualTheme = app.stova.core.theme.VisualTheme.FOREST,
    val seasonChoice: SeasonChoice = SeasonChoice.CALENDAR,
    val seasonalDecor: SeasonalDecor = SeasonalDecor.FULL,
    // Legacy fields above remain readable. New fields win when both versions are present.
    val cornerRadius: CornerRadius = if (corners == Corners.SHARP) CornerRadius.SHARP else CornerRadius.ROUND,
    val ringWidth: RingWidth = if (strongFocus) RingWidth.THICK else RingWidth.NORMAL,
    val cardSpacing: CardSpacing = CardSpacing.NORMAL,
    val progressStyle: ProgressStyle = ProgressStyle.THICK,
    val backgroundSource: BackgroundSource = BackgroundSource.SPOTLIGHT,
    val fixedBackground: BackgroundTitle? = null,
    val spotlightSource: SpotlightSource = SpotlightSource.LIBRARY,
    val backgroundFilter: BackgroundFilter = BackgroundFilter(),
    val heroTitle: HeroTitle = HeroTitle.LOGO,
    val backgroundMotion: Boolean = false,
    val showDate: Boolean = false,
    val showHaChips: Boolean = true,
    val showCountdown: Boolean = true,
    val showAppsButton: Boolean = true,
    val showSmartHomeButton: Boolean = true,
    val showProfile: Boolean = true,
    val showHeroYear: Boolean = true,
    val showHeroAge: Boolean = true,
    val showHeroRatings: Boolean = true,
    val showNewSeason: Boolean = true,
    val clockSize: ClockSize = ClockSize.NORMAL,
    val homeRows: HomeRows = HomeRows.ONE,
    val homeRowInset: HomeRowInset = HomeRowInset.LOW,
    // Null inherits the older preset, preserving existing profiles exactly.
    val appTileBackgroundOpacity: Int? = null,
    val backdropDimmingPercent: Int? = null,
    val cornerRadiusDp: Int? = null,
    val cardSpacingDp: Int? = null,
    val spotlightSeconds: Int? = null,
    /** The mood and accent in use before a chosen Jul or Halloween; they come back when it ends. */
    val moodBeforeSeason: app.stova.core.theme.VisualTheme? = null,
    val accentBeforeSeason: Accent? = null,
    @kotlinx.serialization.Transient val seasonOverlay: SeasonOverlay? = null,
)

@Serializable enum class ArtworkSize(val width: Int) {
    TINY(152), SMALL(200), COMPACT(224), MEDIUM(248), LARGE(296), EXTRA_LARGE(344)
}
@Serializable enum class AppStyle { BANNER, ICON }
@Serializable enum class IconColor { ORIGINAL, ACCENT }
@Serializable enum class IconFill { CENTER, CROP }
@Serializable enum class MovieStyle { POSTER, WIDE }
@Serializable enum class HeroSize(val fraction: Float) { OFF(0f), COMPACT(0.40f), LARGE(0.52f) }
@Serializable enum class SpotlightTime(val millis: Long) { FAST(8_000), NORMAL(12_000), SLOW(20_000) }
@Serializable enum class BackdropDimming { LIGHT, MEDIUM, DARK }
@Serializable enum class Corners { ROUND, SHARP }
/** RING was the old default. Read it as LIFT; OUTLINE is the new explicit opt-in to a ring. */
@Serializable enum class FocusEffect { RING, ZOOM, RING_ZOOM, LIGHT_UP, LIFT, OUTLINE }
fun FocusEffect.resolved(): FocusEffect = if (this == FocusEffect.RING) FocusEffect.LIFT else this
@Serializable enum class Accent { LIME, OCEAN, IRIS, CORAL, GOLD, MINT, ROSE, PEARL, HOLLY, PUMPKIN }
@Serializable enum class ClockMode { H24, H12, HIDDEN }
@Serializable enum class RowSpacing(val top: Int) { TIGHT(12), NORMAL(22), AIRY(34) }

@Serializable enum class CornerRadius(val dp: Int) { SHARP(4), SOFT(12), ROUND(20), EXTRA_ROUND(28) }
@Serializable enum class RingWidth(val dp: Int) { THIN(2), NORMAL(3), THICK(4) }
@Serializable enum class CardSpacing(val dp: Int) { TIGHT(8), NORMAL(16), AIRY(24) }
@Serializable enum class ProgressStyle(val dp: Int) { THIN(2), THICK(4), HIDDEN(0) }
@Serializable enum class BackgroundSource { SPOTLIGHT, FIXED, THEME }
@Serializable enum class SpotlightSource { LIBRARY, MOVIES, SERIES, FAVORITES }
@Serializable enum class HeroTitle { LOGO, TITLE }
@Serializable enum class ClockSize { NORMAL, LARGE }
@Serializable enum class HomeRows { ONE, TWO, SCROLLING }
@Serializable enum class HomeRowInset(val dp: Int) { LOW(32), BALANCED(48), HIGH(72) }
@Serializable enum class RowFormat { POSTER, WIDE, LARGE_WIDE, SMALL_WIDE, ICON }

/** Null format/size inherit the row's original format and the global artwork size. */
@Serializable data class RowStyle(
    val format: RowFormat? = null,
    val size: ArtworkSize? = null,
    val showTitle: Boolean = true,
    val title: String? = null,
    val headingSize: HeadingSize = HeadingSize.NORMAL,
    val showSource: Boolean = true,
    /** Names below this row's cards; null follows the profile's choice. */
    val cardTitles: Boolean? = null,
) {
    fun heading(default: String): String = title?.trim()?.takeIf { it.isNotEmpty() } ?: default
    fun withHeading(value: String): RowStyle = copy(title = value.replace(Regex("\\s+"), " ").trim().take(80).takeIf { it.isNotEmpty() })
}
@Serializable enum class HeadingSize { SMALL, NORMAL, LARGE }

/** IDs, rather than authenticated URLs: a removed account cannot leave its artwork behind. */
@Serializable data class BackgroundTitle(val accountId: String, val itemId: String, val title: String)

@Serializable
data class PinFailures(val count: Int = 0, val lastFailureAt: Long = 0)

@Serializable
data class ProfilesFile(
    val profiles: List<Profile> = emptyList(),
    val lastProfileId: String? = null,
    val startInLastProfile: Boolean = true,
    val pinFailures: Map<String, PinFailures> = emptyMap(),
)
