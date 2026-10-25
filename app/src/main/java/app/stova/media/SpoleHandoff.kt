package app.stova.media

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import app.stova.media.model.MediaAccount
import app.stova.media.model.MediaItem
import app.stova.media.model.ServerKind

enum class SpoleAction(val wire: String) { DETAILS("details"), PLAY("play") }
object SpoleContract {
    const val PACKAGE = "app.reelstack"
    const val DEBUG_PACKAGE = "app.reelstack.debug"
    val ID_PATTERN = Regex("[A-Za-z0-9_-]{1,128}")
    fun uri(kind: ServerKind, userId: String, itemId: String, action: SpoleAction): Uri =
        Uri.Builder().scheme("spole").authority("open")
            .appendQueryParameter("kind", if (kind == ServerKind.JELLYFIN) "jellyfin" else "emby")
            .appendQueryParameter("user", userId).appendQueryParameter("item", itemId)
            .appendQueryParameter("action", action.wire).build()
}
sealed interface HandoffResult {
    data object Opened : HandoffResult
    /** Spole is installed but too old for spole:// links: it opened at its own home instead. */
    data object OpenedWithoutLink : HandoffResult
    data object SpoleMissing : HandoffResult
    data object InvalidIds : HandoffResult
}
class SpoleHandoff(private val context: Context) {
    @Suppress("DEPRECATION")
    fun installedPackages(): List<String> = listOf(SpoleContract.PACKAGE, SpoleContract.DEBUG_PACKAGE).filter { pkg ->
        try { context.packageManager.getPackageInfo(pkg, 0); true }
        catch (_: PackageManager.NameNotFoundException) { false }
    }
    fun open(account: MediaAccount, item: MediaItem, action: SpoleAction): HandoffResult {
        if (!SpoleContract.ID_PATTERN.matches(account.userId) || !SpoleContract.ID_PATTERN.matches(item.id) || item.accountId != account.id) return HandoffResult.InvalidIds
        val packages = installedPackages().ifEmpty { return HandoffResult.SpoleMissing }
        // Every installed Spole gets the link in turn: a release may be older than a debug build.
        for (pkg in packages) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, SpoleContract.uri(account.kind, account.userId, item.id, action))
                    .setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return HandoffResult.Opened
            } catch (_: ActivityNotFoundException) { }
        }
        // Spole is here but cannot open a title yet: open it anyway rather than claim it is missing.
        val pkg = packages.first()
        val launch = context.packageManager.getLeanbackLaunchIntentForPackage(pkg) ?: context.packageManager.getLaunchIntentForPackage(pkg)
            ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setPackage(pkg)
        return try { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); HandoffResult.OpenedWithoutLink }
        catch (_: ActivityNotFoundException) { HandoffResult.SpoleMissing }
    }
}
