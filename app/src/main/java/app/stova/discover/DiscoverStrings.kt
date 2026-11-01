package app.stova.discover

import androidx.annotation.StringRes
import app.stova.R

/** What Seerr's status says about a title; null for a title Seerr knows nothing of, which says nothing. */
@StringRes
fun seerrStatusLabel(status: SeerrStatus): Int? = when (status) {
    SeerrStatus.UNKNOWN -> null
    SeerrStatus.PENDING -> R.string.seerr_status_pending
    SeerrStatus.PROCESSING -> R.string.seerr_status_processing
    SeerrStatus.PARTLY -> R.string.seerr_status_partly
    SeerrStatus.AVAILABLE -> R.string.seerr_status_available
    SeerrStatus.BLOCKED -> R.string.seerr_status_blocked
}

/** How far the person's own request has come. */
@StringRes
fun requestStageLabel(stage: RequestStage): Int = when (stage) {
    RequestStage.REQUESTED -> R.string.request_stage_requested
    RequestStage.DOWNLOADING -> R.string.request_stage_downloading
    RequestStage.IMPORTING -> R.string.request_stage_importing
    RequestStage.AVAILABLE -> R.string.request_stage_available
    RequestStage.DECLINED -> R.string.request_stage_declined
    RequestStage.FAILED -> R.string.request_stage_failed
    RequestStage.UNKNOWN -> R.string.request_stage_unknown
    RequestStage.WATCHING -> R.string.request_stage_watching
}
