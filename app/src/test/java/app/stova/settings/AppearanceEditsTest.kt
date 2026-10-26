package app.stova.settings

import app.stova.profiles.AppearancePrefs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppearanceEditsTest {
    @Test fun reopeningDuringAnAtomicWriteKeepsThePendingValueWhenEditingAnotherSlider() = runTest {
        val gate = CompletableDeferred<Unit>()
        val profile = app.stova.profiles.Profile("one", "One", app.stova.core.theme.AvatarColor.LIME)
        var saved = profile.appearance
        val store = AppearanceEditsStore(this) { _, prefs -> gate.await(); saved = prefs }
        val first = store.open(profile)
        first.change(first.draft.copy(appTileBackgroundOpacity = 40))
        first.flush(); runCurrent()
        val reopened = store.open(profile) // The file still contains the older value.
        reopened.change(reopened.draft.copy(cornerRadiusDp = 17))
        reopened.flush(); runCurrent()
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(40, saved.appTileBackgroundOpacity)
        assertEquals(17, saved.cornerRadiusDp)
    }
    @Test fun rapidChangesAndImmediateExitSaveOnlyTheLastValue() = runTest {
        val written = mutableListOf<AppearancePrefs>()
        val edits = AppearanceEdits(AppearancePrefs(), this) { written += it }
        repeat(30) { edits.change(edits.draft.copy(appTileBackgroundOpacity = it)) }
        assertEquals(29, edits.draft.appTileBackgroundOpacity)
        edits.sync(AppearancePrefs())
        assertEquals(29, edits.draft.appTileBackgroundOpacity)
        assertTrue(written.isEmpty())
        edits.flush() // Disposal happens before the normal 180 ms timer.
        runCurrent()
        assertEquals(listOf(29), written.map { it.appTileBackgroundOpacity })
        assertFalse(edits.pending)
        advanceUntilIdle()
        assertEquals(1, written.size)
    }
    @Test fun changesDuringAnAtomicWriteNeverCancelItAndOnlyKeepTheLatestNextValue() = runTest {
        val gate = CompletableDeferred<Unit>()
        val written = mutableListOf<Int?>()
        val edits = AppearanceEdits(AppearancePrefs(), this) {
            if (written.isEmpty()) gate.await()
            written += it.appTileBackgroundOpacity
        }
        edits.change(edits.draft.copy(appTileBackgroundOpacity = 20))
        advanceTimeBy(181); runCurrent()
        edits.change(edits.draft.copy(appTileBackgroundOpacity = 30))
        edits.change(edits.draft.copy(appTileBackgroundOpacity = 40))
        edits.flush(); runCurrent()
        assertTrue(edits.pending); assertTrue(written.isEmpty())
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(listOf(20, 40), written)
        assertFalse(edits.pending)
    }
    @Test fun resetOrCopyWaitsForThePendingEditBeforeReplacingIt() = runTest {
        val values = mutableListOf<Int?>()
        val edits = AppearanceEdits(AppearancePrefs(), this) { values += it.appTileBackgroundOpacity }
        edits.change(edits.draft.copy(appTileBackgroundOpacity = 35))
        launch { edits.awaitSaved(); values += null }
        advanceUntilIdle()
        assertEquals(listOf(35, null), values)
    }
}
