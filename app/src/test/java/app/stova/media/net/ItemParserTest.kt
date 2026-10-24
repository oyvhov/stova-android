package app.stova.media.net
import app.stova.media.model.*
import org.junit.Test
import org.junit.Assert.*
class ItemParserTest {
    private val acc = MediaAccount("a1", "p", ServerKind.JELLYFIN, "srv", "Heim", "http://jf:8096", "u1", "oyvind")
    private fun res(n: String) = javaClass.getResource("/media/$n")!!.readText()
    @Test fun episodeInheritsSeriesArtAndKeepsOwnStill() {
        val e = ItemParser.parseList(res("jellyfin_resume.json"), acc).single()
        assertEquals(ItemType.EPISODE, e.type); assertEquals("Kystvakta", e.seriesName); assertEquals(1, e.season); assertEquals(2, e.episode)
        assertEquals(48, e.runtimeMinutes); assertEquals(0.405f, e.progress!!, 0.0001f)
        assertEquals("http://jf:8096/Items/s1/Images/Backdrop/0?maxWidth=1920&quality=80&tag=b1", e.art.backdrop)
        assertEquals("http://jf:8096/Items/s1/Images/Logo?maxWidth=600&tag=l1", e.art.logo)
        assertEquals("http://jf:8096/Items/s1/Images/Primary?maxHeight=450&quality=90&tag=sp1", e.art.poster)
        assertEquals("http://jf:8096/Items/e1/Images/Primary?maxWidth=640&tag=p1", e.art.wide)
    }
    @Test fun movieRatingsAndBareArray() {
        val items = ItemParser.parseList(res("jellyfin_latest_movies.json"), acc)
        assertEquals(1, items.size)
        val m = items.single(); assertEquals(Ratings(7.84, 91, "12"), m.ratings); assertEquals(2024, m.year); assertNull(m.progress)
    }
    @Test fun embyEpisodeWithoutPrimaryUsesThumb() {
        val e = ItemParser.parseList(res("emby_nextup.json"), acc.copy(kind = ServerKind.EMBY)).single()
        assertTrue(e.art.wide!!.contains("/Images/Thumb?maxWidth=640"))
    }
}
