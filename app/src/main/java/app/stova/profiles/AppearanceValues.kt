package app.stova.profiles

val AppearancePrefs.appBackgroundPercent: Int
    get() = (appTileBackgroundOpacity ?: if (appTileBackground) 100 else 0).coerceIn(0, 100)
val AppearancePrefs.dimmingPercent: Int
    get() = (backdropDimmingPercent ?: when (backdropDimming) {
        BackdropDimming.LIGHT -> 25; BackdropDimming.MEDIUM -> 50; BackdropDimming.DARK -> 75
    }).coerceIn(0, 100)
val AppearancePrefs.cornerDp: Int get() = (cornerRadiusDp ?: cornerRadius.dp).coerceIn(0, 32)
val AppearancePrefs.spacingDp: Int get() = (cardSpacingDp ?: cardSpacing.dp).coerceIn(0, 40)
val AppearancePrefs.spotlightMillis: Long get() = (spotlightSeconds?.coerceIn(5, 60)?.times(1000L) ?: spotlightTime.millis)
