package app.stova.discover

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowContentResolver

/**
 * Spole's launcher provider answering only Discover's rows: one popular film, and [seerr] for what the
 * person may do (`none`, `browse` or `personal`). Every other question gets no answer.
 */
class SeerrSpole(@Volatile var seerr: String) : ContentProvider() {
    override fun onCreate() = true
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor? =
        if (uri.path != "/discover") null
        else MatrixCursor(arrayOf("row", "type", "tmdb", "title", "year", "status")).apply {
            if (seerr != "none") addRow(arrayOf<Any?>("trending", "movie", 949, "Heat", 1995, 5))
            setExtras(Bundle().apply { putString("seerr", seerr) })
        }

    companion object {
        /** Spole installed as `app.reelstack`, with this provider answering [seerr]. */
        fun install(context: Context, seerr: String): SeerrSpole = SeerrSpole(seerr).also {
            shadowOf(context.packageManager).installPackage(PackageInfo().apply { packageName = "app.reelstack" })
            ShadowContentResolver.registerProviderInternal("app.reelstack.launcher", it)
        }
    }
}
