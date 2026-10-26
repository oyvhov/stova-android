package app.stova.media

import app.stova.media.model.*
import app.stova.profiles.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class BackgroundEligibilityTest {
    private fun film(rating:String?)=MediaItem("film","account",ItemType.MOVIE,"Film",ratings=Ratings(official=rating))
    @Test fun defaultsExcludeAdultAndUnknownRatingsIncludingOldJson() {
        val filter=Json.decodeFromString(AppearancePrefs.serializer(),"{}").backgroundFilter
        assertEquals(15,filter.maxAge)
        for(rating in listOf("18","NO-18","18 år","TV-MA","NC-17","R",null,"NR","Unrated","film 12","2024","12 / 18"))
            assertFalse("Must hide $rating",acceptsBackground(film(rating),filter))
        for(rating in listOf("A","NO15","NO-15","15 år","FSK 12","US:PG-13","TV-14","12A","TV-Y7-FV"))
            assertTrue("Must accept $rating",acceptsBackground(film(rating),filter))
        assertFalse(acceptsBackground(film("FSK-16"),filter))
        assertTrue(acceptsBackground(film(null),filter.copy(maxAge=null)))
    }
    @Test fun libraryMembershipIsProvenAndAccountScopedWithExplicitEmptySelection() {
        val item=film("12").copy(backgroundLibraryIds=setOf("family"))
        val filter=BackgroundFilter(setOf(BackgroundLibraryRef("account","family")))
        assertTrue(acceptsBackground(item,filter))
        assertFalse(acceptsBackground(item.copy(accountId="another"),filter))
        assertFalse(acceptsBackground(item.copy(backgroundLibraryIds=emptySet()),filter))
        assertFalse(acceptsBackground(item,filter.copy(libraries=emptySet())))
        assertTrue(acceptsBackground(item.copy(backgroundLibraryIds=emptySet()),filter.copy(libraries=null)))
    }

    @Test fun overlappingLibrariesRetainBothMembershipsAndOldProfilesNeverPassTheDisplayGuard() {
        val account=MediaAccount("account","p",ServerKind.JELLYFIN,"s","S","https://demo.invalid","u","U")
        fun entry(lib:String)=SpotlightEntry(film("12").copy(backgroundLibraryIds=setOf(lib)),account,false)
        val merged=mergeBackgroundMembership(listOf(entry("a"),entry("b"))).single()
        assertEquals(setOf("a","b"),merged.item.backgroundLibraryIds)
        assertNotNull(backgroundForProfile(merged,"p",BackgroundFilter(setOf(BackgroundLibraryRef("account","b")))))
        assertNull(backgroundForProfile(merged,"another-profile",BackgroundFilter()))
    }
}
