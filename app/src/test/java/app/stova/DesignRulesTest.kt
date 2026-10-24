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

    @Test fun scansRealSources() {
        // Guards the two rules above against passing because the working directory moved.
        assert(files().any()) { "no Kotlin sources under ${src.absolutePath}" }
    }
}
