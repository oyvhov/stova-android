package app.stova.background

import app.stova.profiles.*

fun backgroundUrl(p: AppearancePrefs, spotlight: String?, fixed: String?): String? = when (p.backgroundSource) {
    BackgroundSource.SPOTLIGHT -> spotlight
    BackgroundSource.FIXED -> fixed
    BackgroundSource.THEME -> null
}

fun backgroundCanMove(p: AppearancePrefs, visible: Boolean): Boolean =
    visible && p.backgroundMotion && !p.reduceMotion && p.heroSize != HeroSize.OFF && p.backgroundSource != BackgroundSource.THEME
