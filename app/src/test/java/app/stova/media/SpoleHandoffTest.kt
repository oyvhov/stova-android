package app.stova.media
import android.app.Application
import android.content.pm.PackageInfo
import app.stova.media.model.*
import app.stova.TV_QUALIFIERS
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
@RunWith(AndroidJUnit4::class) @Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class SpoleHandoffTest {
 private val context: android.content.Context = androidx.test.core.app.ApplicationProvider.getApplicationContext()
    private val acc = MediaAccount("a", "p", ServerKind.JELLYFIN, "s", "S", "http://jf", "0f1e2d3c4b5a69788796a5b4c3d2e1f0", "n")
    private val item = MediaItem("9a8b7c6d5e4f30211203f4e5d6c7b8a9", "a", ItemType.MOVIE, "M")
    @Test fun uriFormat() = assertEquals(
        "spole://open?kind=jellyfin&user=0f1e2d3c4b5a69788796a5b4c3d2e1f0&item=9a8b7c6d5e4f30211203f4e5d6c7b8a9&action=play",
        SpoleContract.uri(ServerKind.JELLYFIN, acc.userId, item.id, SpoleAction.PLAY).toString())
    @Test fun missingSpole() = assertEquals(HandoffResult.SpoleMissing, SpoleHandoff(context).open(acc, item, SpoleAction.DETAILS))
    @Test fun opensReleaseSpoleWhenInstalled() {
        shadowOf(context.packageManager).installPackage(PackageInfo().apply { packageName = "app.reelstack" })
        assertEquals(HandoffResult.Opened, SpoleHandoff(context).open(acc, item, SpoleAction.DETAILS))
        val started = shadowOf(context as Application).nextStartedActivity
        assertEquals("app.reelstack", started.`package`); assertEquals("details", started.data!!.getQueryParameter("action"))
    }
    @Test fun rejectsBadIds() = assertEquals(HandoffResult.InvalidIds, SpoleHandoff(context).open(acc, item.copy(id = "../x"), SpoleAction.PLAY))
}
