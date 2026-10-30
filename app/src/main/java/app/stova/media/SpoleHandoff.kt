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
    /**
     * Spole approves the Quick Connect code Stova shows, as the account the user picks there: Stova gets its
     * own token and none crosses between the apps. Started for a result, so Spole can see who asks.
     */
    const val APPROVE_ACTION = "app.reelstack.action.APPROVE_SIGN_IN"
    const val EXTRA_SERVER_URL = "app.reelstack.extra.SERVER_URL"
    const val EXTRA_SERVER_ID = "app.reelstack.extra.SERVER_ID"
    const val EXTRA_CODE = "app.reelstack.extra.CODE"
    const val EXTRA_FOR_CHILD = "app.reelstack.extra.FOR_CHILD"
    const val EXTRA_ERROR = "app.reelstack.extra.ERROR"
    fun approveIntent(pkg: String, server: app.stova.media.net.ServerInfo, code: String, forChild: Boolean): Intent =
        Intent(APPROVE_ACTION).setPackage(pkg).putExtra(EXTRA_SERVER_URL, server.baseUrl).putExtra(EXTRA_SERVER_ID, server.id)
            .putExtra(EXTRA_CODE, code).putExtra(EXTRA_FOR_CHILD, forChild)
    fun approval(resultCode: Int, data: Intent?): SpoleApproval = when {
        resultCode == android.app.Activity.RESULT_OK -> SpoleApproval.APPROVED
        data?.getStringExtra(EXTRA_ERROR) == "not_signed_in" -> SpoleApproval.NOT_SIGNED_IN
        data?.getStringExtra(EXTRA_ERROR) == "failed" -> SpoleApproval.FAILED
        else -> SpoleApproval.DECLINED
    }
    /** One library view. A Spole without this link has no filter for the host, so it falls back to Spole's home. */
    fun libraryUri(kind: ServerKind, userId: String, libraryId: String): Uri =
        Uri.Builder().scheme("spole").authority("library")
            .appendQueryParameter("kind", if (kind == ServerKind.JELLYFIN) "jellyfin" else "emby")
            .appendQueryParameter("user", userId).appendQueryParameter("library", libraryId).build()
}
enum class SpoleApproval { APPROVED, NOT_SIGNED_IN, DECLINED, FAILED }
/** Asks Spole to approve [code] on [server]; [done] hears the answer when the user comes back from Spole. */
fun interface SpoleApprover {
    fun approve(server: app.stova.media.net.ServerInfo, code: app.stova.media.net.QuickConnectCode, done: (SpoleApproval) -> Unit)
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
    /** The installed Spole that can approve a sign-in for Stova, or null: older ones cannot. */
    fun approvalPackage(): String? = installedPackages().firstOrNull { pkg ->
        context.packageManager.queryIntentActivities(Intent(SpoleContract.APPROVE_ACTION).setPackage(pkg), 0).isNotEmpty()
    }
    fun open(account: MediaAccount, item: MediaItem, action: SpoleAction): HandoffResult {
        if (!SpoleContract.ID_PATTERN.matches(account.userId) || !SpoleContract.ID_PATTERN.matches(item.id) || item.accountId != account.id) return HandoffResult.InvalidIds
        return send(SpoleContract.uri(account.kind, account.userId, item.id, action))
    }

    /** Opens Spole on one of [account]'s libraries, for a favourite library tile. */
    fun openLibrary(account: MediaAccount, libraryId: String): HandoffResult {
        if (!SpoleContract.ID_PATTERN.matches(account.userId) || !SpoleContract.ID_PATTERN.matches(libraryId)) return HandoffResult.InvalidIds
        return send(SpoleContract.libraryUri(account.kind, account.userId, libraryId))
    }

    private fun send(link: Uri): HandoffResult {
        val packages = installedPackages().ifEmpty { return HandoffResult.SpoleMissing }
        // Every installed Spole gets the link in turn: a release may be older than a debug build.
        for (pkg in packages) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, link).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return HandoffResult.Opened
            } catch (_: ActivityNotFoundException) { }
        }
        // Spole is here but cannot open the link yet: open it anyway rather than claim it is missing.
        val pkg = packages.first()
        val launch = context.packageManager.getLeanbackLaunchIntentForPackage(pkg) ?: context.packageManager.getLaunchIntentForPackage(pkg)
            ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setPackage(pkg)
        return try { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); HandoffResult.OpenedWithoutLink }
        catch (_: ActivityNotFoundException) { HandoffResult.SpoleMissing }
    }
}
