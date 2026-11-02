package app.stova.discover

import app.stova.R
import org.junit.Assert.assertEquals
import org.junit.Test

/** The line under a request's card: its stage, and how far it has come only while it downloads. */
class DiscoverStringsTest {
    @Test fun captionsFollowTheStage() {
        val labels = mapOf(
            RequestStage.REQUESTED to R.string.request_stage_requested,
            RequestStage.DOWNLOADING to R.string.request_stage_downloading,
            RequestStage.IMPORTING to R.string.request_stage_importing,
            RequestStage.AVAILABLE to R.string.request_stage_available,
            RequestStage.DECLINED to R.string.request_stage_declined,
            RequestStage.FAILED to R.string.request_stage_failed,
            RequestStage.UNKNOWN to R.string.request_stage_unknown,
            RequestStage.WATCHING to R.string.request_stage_watching,
        )
        assertEquals(RequestStage.entries.toSet(), labels.keys)
        // A download says how far it has come; one whose percent Spole does not know says only that it downloads.
        assertEquals(R.string.request_stage_downloading to 42, requestCaption(RequestStage.DOWNLOADING, 42))
        assertEquals(R.string.request_stage_downloading to null, requestCaption(RequestStage.DOWNLOADING, null))
        // Every other stage is its label alone, whatever percent Spole sends with it.
        for ((stage, label) in labels - RequestStage.DOWNLOADING) assertEquals(label to null, requestCaption(stage, 42))
        // A percent out of range is held to 0–100.
        assertEquals(R.string.request_stage_downloading to 100, requestCaption(RequestStage.DOWNLOADING, 130))
        assertEquals(R.string.request_stage_downloading to 0, requestCaption(RequestStage.DOWNLOADING, -5))
    }
}
