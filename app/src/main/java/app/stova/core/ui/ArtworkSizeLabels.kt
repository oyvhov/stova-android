package app.stova.core.ui

import androidx.annotation.StringRes
import app.stova.R
import app.stova.profiles.ArtworkSize

/** Shared by the global selector and each row's override; heading sizes are independent. */
@get:StringRes
val ArtworkSize.labelRes: Int get() = when (this) {
    ArtworkSize.TINY -> R.string.appearance_extra_small
    ArtworkSize.SMALL -> R.string.appearance_small
    ArtworkSize.COMPACT -> R.string.appearance_compact
    ArtworkSize.MEDIUM -> R.string.appearance_medium
    ArtworkSize.LARGE -> R.string.appearance_large
    ArtworkSize.EXTRA_LARGE -> R.string.appearance_extra_large
}
