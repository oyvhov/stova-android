package app.stova.profiles

import kotlinx.serialization.Serializable

@Serializable data class BackgroundLibraryRef(val accountId: String, val libraryId: String)

/** null libraries = all; an empty set deliberately permits none. Unknown ages fail closed. */
@Serializable data class BackgroundFilter(
    val libraries: Set<BackgroundLibraryRef>? = null,
    val maxAge: Int? = 15,
)

/** Kids time for the hero: every day from [startHour] up to [endHour]; a window may run past midnight. */
@Serializable data class KidsHours(
    val enabled: Boolean = false,
    val startHour: Int = 6,
    val endHour: Int = 20,
    val libraries: Set<BackgroundLibraryRef> = emptySet(),
) {
    fun covers(hour: Int): Boolean = enabled &&
        if (startHour <= endHour) hour in startHour until endHour else hour >= startHour || hour < endHour
}

/**
 * Kids time shows only the kids libraries; the rest of the day leaves them out of the chosen ones.
 * With every library chosen there is nothing to subtract from, so the evening stays as it is.
 */
fun BackgroundFilter.atHour(kids: KidsHours, hour: Int): BackgroundFilter = when {
    !kids.enabled || kids.libraries.isEmpty() -> this
    kids.covers(hour) -> copy(libraries = kids.libraries)
    else -> libraries?.let { copy(libraries = it - kids.libraries) } ?: this
}
