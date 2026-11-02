package app.stova.discover

import app.stova.R
import org.junit.Assert.assertEquals
import org.junit.Test

/** What the person is told when Spole has answered a request or a withdrawal. */
class DiscoverActionsTest {
    @Test fun resultsMapToMessages() {
        val messages = mapOf(
            DiscoverActionResult.DONE to R.string.discover_sent,
            DiscoverActionResult.ALREADY to R.string.discover_already,
            DiscoverActionResult.NOT_PERSONAL to R.string.discover_not_personal,
            DiscoverActionResult.FORBIDDEN to R.string.discover_forbidden,
            DiscoverActionResult.FAILED to R.string.discover_failed_action,
            DiscoverActionResult.UNKNOWN to R.string.discover_unknown_profile,
            DiscoverActionResult.NOT_SUPPORTED to R.string.discover_update_spole,
        )
        assertEquals(DiscoverActionResult.entries.toSet(), messages.keys)
        for ((result, message) in messages) assertEquals(result.name, message, discoverActionMessage(result))
    }
}
