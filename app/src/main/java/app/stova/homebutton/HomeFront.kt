package app.stova.homebutton

import java.io.File

/**
 * Whether Stova was on screen when it was last seen. The launcher marks it on resume and clears it on
 * pause, so a process that died on screen (an update, a crash) leaves it set. The Home service reads
 * it when it connects: an update kills Stova and brings up the stock home while the service is
 * restarting, so the service misses that window and, with the stock home already in front, Home
 * changes nothing until Stova is opened by hand (the loft's Google TV, 2026-11-01).
 */
object HomeFront {
    private const val FILE = "front"

    fun mark(filesDir: File, front: Boolean) {
        val file = File(filesDir, FILE)
        if (front) file.writeText("1") else file.delete()
    }

    fun wasInFront(filesDir: File): Boolean = File(filesDir, FILE).exists()
}
