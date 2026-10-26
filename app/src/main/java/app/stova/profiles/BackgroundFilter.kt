package app.stova.profiles

import kotlinx.serialization.Serializable

@Serializable data class BackgroundLibraryRef(val accountId: String, val libraryId: String)

/** null libraries = all; an empty set deliberately permits none. Unknown ages fail closed. */
@Serializable data class BackgroundFilter(
    val libraries: Set<BackgroundLibraryRef>? = null,
    val maxAge: Int? = 15,
)
