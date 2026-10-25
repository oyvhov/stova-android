package app.stova.media.net
import app.stova.media.model.*
import org.junit.Test
import org.junit.Assert.*
class ItemParserTest {
    private val acc = MediaAccount("a1", "p", ServerKind.JELLYFIN, "srv", "Heim", "http://jf:8096", "u1", "oyvind")
    private fun res(n: String) = javaClass.getResource("/media/$n")!!.readText()
    @Test fun episodeInheritsSeriesArtAndCardUsesSeriesArt() {
        val e = ItemParser.parseList(res("jellyfin_resume.json"), acc).single()
        assertEquals(ItemType.EPISODE, e.type); assertEquals("Kystvakta", e.seriesName); assertEquals(1, e.season); assertEquals(2, e.episode)
        assertEquals(48, e.runtimeMinutes); assertEquals(0.405f, e.progress!!, 0.0001f)
        assertEquals("http://jf:8096/Items/s1/Images/Backdrop/0?maxWidth=1920&quality=80&tag=b1", e.art.backdrop)
        assertEquals("http://jf:8096/Items/s1/Images/Logo?maxWidth=600&tag=l1", e.art.logo)
        assertEquals("http://jf:8096/Items/s1/Images/Primary?maxHeight=450&quality=90&tag=sp1", e.art.poster)
        // No episode stills on cards: the series art, here its backdrop, since the series has no thumb.
        assertEquals("http://jf:8096/Items/s1/Images/Backdrop/0?maxWidth=640&tag=b1", e.art.wide)
    }
    @Test fun movieRatingsAndBareArray() {
        val items = ItemParser.parseList(res("jellyfin_latest_movies.json"), acc)
        assertEquals(1, items.size)
        val m = items.single(); assertEquals(Ratings(7.84, 91, "12"), m.ratings); assertEquals(2024, m.year); assertNull(m.progress)
    }
    @Test fun episodePrefersSeriesThumbOverBackdrop() {
        val json = """[{"Id":"e1","Type":"Episode","Name":"Pilot","SeriesId":"s1","ImageTags":{"Primary":"p1"},"ParentThumbItemId":"s1","ParentThumbImageTag":"t9","ParentBackdropItemId":"s1","ParentBackdropImageTags":["b1"]}]"""
        assertEquals("http://jf:8096/Items/s1/Images/Thumb?maxWidth=640&tag=t9", ItemParser.parseList(json, acc).single().art.wide)
    }
    @Test fun seasonKeepsItsNumberPremiereAndSeriesArt() {
        val json = """[{"Id":"se3","Type":"Season","Name":"Sesong 3","SeriesName":"Kystvakta","SeriesId":"s1","IndexNumber":3,"PremiereDate":"2026-10-10T00:00:00.0000000Z","ParentBackdropItemId":"s1","ParentBackdropImageTags":["b1"],"ParentLogoItemId":"s1","ParentLogoImageTag":"l1"}]"""
        val s = ItemParser.parseList(json, acc).single()
        assertEquals(ItemType.SEASON, s.type); assertEquals(3, s.season); assertNull(s.episode); assertEquals("2026-10-10", s.premiered)
        assertEquals("http://jf:8096/Items/s1/Images/Backdrop/0?maxWidth=1920&quality=80&tag=b1", s.art.backdrop)
        assertEquals("http://jf:8096/Items/s1/Images/Logo?maxWidth=600&tag=l1", s.art.logo)
    }
    @Test fun embyEpisodeWithoutSeriesArtFallsBackToItsThumb() {
        val e = ItemParser.parseList(res("emby_nextup.json"), acc.copy(kind = ServerKind.EMBY)).single()
        assertTrue(e.art.wide!!.contains("/Images/Thumb?maxWidth=640"))
    }
}
