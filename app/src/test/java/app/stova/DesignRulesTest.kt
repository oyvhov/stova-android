package app.stova

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/** The design rules the compiler cannot see: no glass, and no colours outside the theme. */
class DesignRulesTest {
    private val src = File("src/main/java")
    private fun files() = src.walkTopDown().filter { it.extension == "kt" }

    @Test fun noBlurOrRenderEffect() {
        val offenders = files().filter { f -> f.readText().let { ".blur(" in it || "RenderEffect" in it } }.toList()
        assertEquals(emptyList<File>(), offenders)
    }

    @Test fun noColorLiteralsOutsideTheme() {
        val offenders = files().filterNot { it.path.replace('\\', '/').contains("core/theme/") }
            .filter { Regex("""Color\(0x""").containsMatchIn(it.readText()) }.toList()
        assertEquals(emptyList<File>(), offenders)
    }

    // One entry and one exit for every dialog, menu and panel: they all go through StovaDialog.
    @Test fun everyDialogGoesThroughStovaDialog() {
        val raw = Regex("""androidx\.compose\.ui\.window\.Dialog\b|\bAlertDialog\(""")
        val offenders = files().filterNot { it.name == "StovaDialog.kt" }.filter { raw.containsMatchIn(it.readText()) }.toList()
        assertEquals(emptyList<File>(), offenders)
    }

    // Android 11 (the Shield) parses only "Z" in Instant.parse; Home Assistant sends "+00:00",
    // so every entity was silently dropped and "Add card" found nothing.
    @Test fun noInstantParseBecauseAndroid11RejectsOffsets() {
        val offenders = files().filter { "Instant.parse(" in it.readText() }.toList()
        assertEquals(emptyList<File>(), offenders)
    }

    @Test fun scansRealSources() {
        // Guards the two rules above against passing because the working directory moved.
        assert(files().any()) { "no Kotlin sources under ${src.absolutePath}" }
    }
}
