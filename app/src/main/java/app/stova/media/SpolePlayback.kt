package app.stova.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.stova.StovaApplication
import app.stova.media.model.ServerKind

/**
 * What Spole says when a film or an episode stops, or the player goes to the background. Nothing in
 * it is secret: a forged one can at most move a progress bar on this TV until the server answers.
 */
data class SpolePlayback(val kind: ServerKind, val userId: String, val itemId: String,
    val positionMs: Long, val durationMs: Long, val ended: Boolean) {
    val progress: Float? get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else null
    /** Seen to the end, or past nine tenths, where Jellyfin and Emby count it as played. */
    val finished: Boolean get() = ended || (progress ?: 0f) >= 0.9f

    companion object {
        const val ACTION = "app.reelstack.action.PLAYBACK_PROGRESS"
        fun parse(kind: String?, user: String?, item: String?, positionMs: Long, durationMs: Long, ended: Boolean): SpolePlayback? {
            val server = when (kind) { "jellyfin" -> ServerKind.JELLYFIN; "emby" -> ServerKind.EMBY; else -> return null }
            if (user == null || item == null || !SpoleContract.ID_PATTERN.matches(user) || !SpoleContract.ID_PATTERN.matches(item)) return null
            if (positionMs < 0 || durationMs < 0) return null
            return SpolePlayback(server, user, item, positionMs, durationMs, ended)
        }
        fun from(intent: Intent): SpolePlayback? = parse(intent.getStringExtra("kind"), intent.getStringExtra("user"),
            intent.getStringExtra("item"), intent.getLongExtra("position_ms", -1), intent.getLongExtra("duration_ms", -1),
            intent.getBooleanExtra("ended", false))
    }
}

/** Spole's report reaches Home's rows; the launcher process is the only one with media state. */
class SpolePlaybackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SpolePlayback.ACTION) return
        val report = SpolePlayback.from(intent) ?: return
        val container = runCatching { (context.applicationContext as StovaApplication).container }.getOrNull() ?: return
        container.media.applyPlayback(report)
    }
}
