package app.stova.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.apps.AppArtCache
import app.stova.apps.AppCatalog
import app.stova.background.BackdropLayer
import app.stova.background.backgroundUrl
import app.stova.core.theme.LocalReduceMotion
import app.stova.core.theme.LocalStrongFocus
import app.stova.core.theme.StovaTheme
import app.stova.ha.ui.ChipContent
import app.stova.ha.ui.HomeStatusChips
import app.stova.home.HomeContent
import app.stova.home.HomeRowKey
import app.stova.home.HomeRowUi
import app.stova.home.LocalHomePreview
import app.stova.media.SpotlightEntry
import app.stova.navigation.TopBar
import app.stova.profiles.AppearancePrefs
import app.stova.profiles.Profile

/** Every Home layout is made for a 960 × 540 dp television. */
private val HomeWidth = 960.dp

/**
 * The real Home, drawn small with the unsaved look: its backdrop, top bar, hero and rows. It is a
 * picture, not a page: focus never enters it, and a screen reader hears one description.
 */
@Composable
internal fun HomeMiniature(
    prefs: AppearancePrefs,
    profile: Profile,
    rows: List<HomeRowUi>?,
    start: HomeRowKey?,
    entry: SpotlightEntry?,
    art: AppArtCache,
    catalog: AppCatalog,
    modifier: Modifier = Modifier,
    smartHome: Boolean = false,
    chips: List<ChipContent> = emptyList(),
) {
    val description = stringResource(R.string.home_preview_description)
    val outer = LocalDensity.current
    val shape = MaterialTheme.shapes.medium
    BoxWithConstraints(modifier.aspectRatio(16f / 9f).clip(shape)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
        .focusProperties { onEnter = { cancelFocusChange() } }.focusGroup()
        .clearAndSetSemantics { contentDescription = description; testTag = "home-preview" }) {
        // A density scaled with the frame lays Home out for the whole screen: the same layout, only smaller.
        val scale = maxWidth / HomeWidth
        CompositionLocalProvider(LocalDensity provides Density(outer.density * scale, outer.fontScale),
            LocalHomePreview provides true) {
            StovaTheme(LocalReduceMotion.current, LocalStrongFocus.current, prefs) {
                Box(Modifier.fillMaxSize()) {
                    BackdropLayer(backgroundUrl(prefs, entry?.item?.art?.backdrop, entry?.item?.art?.backdrop))
                    HomeContent(rows, null, start, art, catalog, {}, {}, {}, {},
                        topBar = {
                            TopBar(profile, {}, {}, onSmartHome = if (smartHome) ({}) else null,
                                leading = { HomeStatusChips(chips) })
                        },
                        spotlight = entry, appIconOverrides = profile.apps.iconOverrides, homePrefs = profile.home)
                }
            }
        }
    }
}
