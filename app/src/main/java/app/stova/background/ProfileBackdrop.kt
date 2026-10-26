package app.stova.background

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import app.stova.media.SpotlightEntry
import app.stova.media.backgroundForProfile
import app.stova.profiles.AppearancePrefs
import coil3.ImageLoader
import coil3.SingletonImageLoader

/** Keep decoded art on ordinary page returns, but never across content restrictions or profiles. */
@Composable fun rememberProfileBackdropState(profileId:String,prefs:AppearancePrefs) =
    remember(profileId,prefs.backgroundFilter,prefs.backgroundSource,prefs.fixedBackground,prefs.spotlightSource) {BackdropState()}

@Composable fun ProfileBackdrop(profileId:String,prefs:AppearancePrefs,spotlight:SpotlightEntry?,fixed:SpotlightEntry?,
    state:BackdropState,imageLoader:ImageLoader=SingletonImageLoader.get(LocalContext.current)) {
    val allowedSpotlight=backgroundForProfile(spotlight,profileId,prefs.backgroundFilter)?.takeIf {app.stova.media.acceptsSpotlight(it.item,prefs.spotlightSource)}
    val allowedFixed=backgroundForProfile(fixed,profileId,prefs.backgroundFilter)?.takeIf {
        it.item.id==prefs.fixedBackground?.itemId && it.account.id==prefs.fixedBackground?.accountId
    }
    // Replacing this subtree also removes Crossfade's outgoing image in the same composition.
    key(profileId,prefs.backgroundFilter,prefs.backgroundSource,prefs.fixedBackground,prefs.spotlightSource) {
        BackdropLayer(backgroundUrl(prefs,allowedSpotlight?.item?.art?.backdrop,allowedFixed?.item?.art?.backdrop),
            state=state,imageLoader=imageLoader)
    }
}
