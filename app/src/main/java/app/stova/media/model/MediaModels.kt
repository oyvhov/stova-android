package app.stova.media.model
import kotlinx.serialization.Serializable
@Serializable enum class ServerKind { JELLYFIN, EMBY }
@Serializable data class MediaAccount(val id: String, val profileId: String, val kind: ServerKind, val serverId: String, val serverName: String, val baseUrl: String, val userId: String, val userName: String, val needsSignIn: Boolean = false,
    /** The server's tag for the user's own picture; null when they have none. */
    val imageTag: String? = null)
@Serializable enum class ItemType { MOVIE, SERIES, SEASON, EPISODE, OTHER }
@Serializable data class Ratings(val community: Double? = null, val criticPercent: Int? = null, val official: String? = null)
@Serializable data class ItemArt(val backdrop: String? = null, val logo: String? = null, val poster: String? = null, val wide: String? = null)
@Serializable data class MediaItem(val id: String, val accountId: String, val type: ItemType, val title: String, val seriesName: String? = null, val seriesId: String? = null, val season: Int? = null, val episode: Int? = null, val year: Int? = null, val runtimeMinutes: Int? = null, val overview: String? = null, val ratings: Ratings = Ratings(), val progress: Float? = null, val art: ItemArt = ItemArt(), val premiered: String? = null, val genres: List<String> = emptyList(), val favorite: Boolean = false,
    /** Proven by a recursive ParentId query, never inferred from an immediate parent folder. */
    val backgroundLibraryIds: Set<String> = emptySet())
