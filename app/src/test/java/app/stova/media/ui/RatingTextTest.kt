package app.stova.media.ui
import app.stova.media.model.*
import app.stova.media.net.*
import okhttp3.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import java.util.Locale
class RatingTextTest {
    @Test fun nynorskUsesComma() = assertEquals("7,8", communityText(7.84, Locale.forLanguageTag("nn")))
    @Test fun englishUsesDot() = assertEquals("7.8", communityText(7.84, Locale.ENGLISH))
    @Test fun criticPercentHasNumber() = assertTrue(criticText(91, Locale.forLanguageTag("nn")).startsWith("91"))
}
