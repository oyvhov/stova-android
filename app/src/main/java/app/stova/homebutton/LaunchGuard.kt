package app.stova.homebutton

import java.io.File

/**
 * The Home service's own brake. It counts the times it opened Stova, and the launcher marks itself
 * alive on every resume. Three openings in five minutes that never reached a resume mean Stova
 * dies before it can record a crash (a failing start, an ANR, a native crash), so the service
 * stands down instead of relaunching it forever.
 */
object LaunchGuard {
    const val WINDOW_MS = 300_000L
    const val THRESHOLD = 3
    private const val ALIVE_FILE = "alive"

    fun standDown(launches: List<Long>, lastAlive: Long, now: Long): Boolean =
        launches.count { it > lastAlive && now - it < WINDOW_MS } >= THRESHOLD

    fun markAlive(filesDir: File, now: Long = System.currentTimeMillis()) {
        File(filesDir, ALIVE_FILE).writeText(now.toString())
    }

    fun lastAlive(filesDir: File): Long =
        runCatching { File(filesDir, ALIVE_FILE).readText().trim().toLong() }.getOrDefault(0L)
}
