package app.stova.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Android's own settings (network, picture, sound, inputs). A launcher that replaces the system home
 * must always lead there; the TV settings app is the fallback when the generic action is missing.
 */
fun openSystemSettings(context: Context): Boolean {
    val candidates = listOf(
        Intent(Settings.ACTION_SETTINGS),
        Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.MainSettings"),
    )
    for (intent in candidates) {
        try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return true }
        catch (_: ActivityNotFoundException) { }
        catch (_: SecurityException) { }
    }
    return false
}
