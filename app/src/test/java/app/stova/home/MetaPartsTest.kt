@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package app.stova.home
import app.stova.home.*
import app.stova.media.model.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
class MetaPartsTest {
    @Test fun movie() = assertEquals(listOf(MetaPart.Year(2024), MetaPart.Runtime(112)),
        metaParts(MediaItem("m", "a", ItemType.MOVIE, "M", year = 2024, runtimeMinutes = 112, ratings = Ratings(official = "12"))))
    @Test fun episode() = assertEquals(listOf(MetaPart.EpisodeCode(2, 3), MetaPart.Runtime(48)),
        metaParts(MediaItem("e", "a", ItemType.EPISODE, "E", season = 2, episode = 3, runtimeMinutes = 48, year = 2024, ratings = Ratings(official = "12"))))
    @Test fun ageAloneDoesNotProduceMetadata() = assertEquals(emptyList<MetaPart>(),
        metaParts(MediaItem("m", "a", ItemType.MOVIE, "M", ratings = Ratings(official = "12"))))
}
