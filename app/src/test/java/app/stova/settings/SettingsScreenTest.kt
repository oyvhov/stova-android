package app.stova.settings

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.AppContainer
import app.stova.MainActivity
import app.stova.StovaApplication
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.AvatarColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class SettingsScreenTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val container: AppContainer get() = (rule.activity.application as StovaApplication).container

    private fun profile(name: String) = runBlocking { container.profiles.file.first().profiles.first { it.name == name } }

    private fun openSettings() {
        rule.waitForIdle()
        rule.onAllNodesWithText("Innstillingar")[0].performClick()
        rule.waitForIdle()
    }

    @Test fun reduceMotionPersists() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        rule.onNodeWithText("Utsjånad").performClick()
        rule.onNodeWithText("Reduser rørsle").performScrollTo().performClick()
        rule.waitUntil(5_000) { profile("A").appearance.reduceMotion }
    }

    @Test fun appearanceChoicesPersistOnlyForTheActiveProfile() {
        runBlocking {
            container.profiles.create("B", AvatarColor.OCEAN, false)
            container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id)
        }
        openSettings()
        rule.onNodeWithText("Utsjånad").performClick()
        rule.onNodeWithText("Kortstorleik").performScrollTo().performClick()
        rule.waitUntil(5_000) { profile("A").appearance.cardSize == app.stova.profiles.ArtworkSize.LARGE }
        assertEquals(app.stova.profiles.ArtworkSize.MEDIUM, profile("B").appearance.cardSize)
    }

    @Test fun adultCanEditKidAppearanceWithoutChangingAdult() {
        runBlocking {
            container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id)
            container.profiles.create("K", AvatarColor.ROSE, true)
        }
        openSettings()
        rule.onNodeWithText("K").performClick()
        rule.onAllNodesWithText("Utsjånad")[1].performScrollTo().performClick()
        rule.onNodeWithText("Kortstorleik").performScrollTo().performClick()
        rule.waitUntil(5_000) { profile("K").appearance.cardSize == app.stova.profiles.ArtworkSize.LARGE }
        assertEquals(app.stova.profiles.ArtworkSize.MEDIUM, profile("A").appearance.cardSize)
        rule.onNodeWithText("Tilpass kvar rad").performScrollTo().performClick()
        rule.onAllNodesWithText("Kortformat for rada")[0].performScrollTo().performClick()
        rule.waitUntil(5_000) { profile("K").home.rowFormats[app.stova.home.HomeRowKey.APPS]?.format == app.stova.profiles.RowFormat.LARGE_WIDE }
        assertEquals(emptyMap<app.stova.home.HomeRowKey, app.stova.profiles.RowStyle>(), profile("A").home.rowFormats)
    }

    @Test fun kidSeesOnlySwitchProfile() {
        runBlocking {
            container.profiles.create("A", AvatarColor.LIME, false)
            container.session.enter(container.profiles.create("K", AvatarColor.ROSE, true).id)
        }
        openSettings()
        rule.onNodeWithText("Utsjånad").assertDoesNotExist()
        assertEquals(1, rule.onAllNodesWithText("Byt profil").fetchSemanticsNodes().size) // the one button
    }

    @Test fun allowAppForKid() {
        val pm = shadowOf(rule.activity.packageManager)
        val component = ComponentName("demo.banan", "demo.banan.Main")
        pm.addActivityIfNotPresent(component).nonLocalizedLabel = "Banan"
        pm.addIntentFilterForActivity(component, IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) })
        runBlocking {
            container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id)
            container.profiles.create("K", AvatarColor.ROSE, true)
        }
        openSettings()
        rule.onNodeWithText("K").performClick()
        rule.onNodeWithText("Tillatne appar").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Banan").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Banan").performClick()
        rule.waitUntil(5_000) { "demo.banan/demo.banan.Main" in profile("K").apps.kidAllowed }
    }

    // Review Important 6: a new PIN must be typed twice before it is saved.
    @Test fun newPinNeedsConfirmation() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        rule.onNodeWithText("Set PIN").performClick()
        "2468".forEach { rule.onNodeWithText(it.toString()).performClick() }
        rule.onNodeWithText("OK").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Stadfest ny PIN").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(null, profile("A").pin)
        "2468".forEach { rule.onNodeWithText(it.toString()).performClick() }
        rule.onNodeWithText("OK").performClick()
        rule.waitUntil(5_000) { profile("A").pin != null }
    }

    @Test fun changingPinRequiresCurrentPin() {
        runBlocking {
            val a = container.profiles.create("A", AvatarColor.LIME, false)
            container.profiles.setPin(a.id, "1357")
            container.session.enter(a.id)
        }
        openSettings()
        rule.onNodeWithText("Endre PIN").performClick()
        rule.onNodeWithText("Skriv PIN for A").assertExists()
    }
}
