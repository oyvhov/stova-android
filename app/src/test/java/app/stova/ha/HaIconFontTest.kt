package app.stova.ha

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HaIconFontTest {
    private val names = File("src/main/assets/mdi/names.txt")
    private var opened = 0
    private fun open() = names.inputStream().also { opened++ }

    // The icons Home Assistant shows, by the same names, from the bundled Material Design Icons.
    @Test fun findsHomeAssistantIconsByName() {
        assertEquals(0xF0FCE, HaIconFont.codepoint("mdi:movie-open", ::open))
        assertEquals(0xF03D8, HaIconFont.codepoint("mdi:palette", ::open))
        assertEquals(0xF0594, HaIconFont.codepoint("mdi:weather-night", ::open))
    }

    @Test fun unknownOrForeignIconsFallBack() {
        assertNull(HaIconFont.codepoint("mdi:no-such-icon-anywhere", ::open))
        assertNull(HaIconFont.codepoint("hass:lightbulb", ::open))
        assertNull(HaIconFont.codepoint("mdi:", ::open))
    }

    // Only the names asked for are read, once each; the list is never held whole.
    @Test fun eachNameIsLookedUpOnce() {
        opened = 0
        repeat(3) { HaIconFont.codepoint("mdi:script-text-play", ::open) }
        assertEquals(1, opened)
    }

    @Test fun theListIsSortedAsTheSearchExpects() {
        val entries = names.readLines().filterNot { it.startsWith("#") }.map { it.substringBefore(' ') }
        assertEquals(entries.sorted(), entries)
        assertEquals("script", HaIconFont.defaultFor("script.god_natt").substringAfter(':').substringBefore('-'))
        assertEquals("mdi:palette", HaIconFont.defaultFor("scene.kino"))
    }
}
