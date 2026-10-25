package app.stova.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.stova.R

/** Localised defaults shared by Home's row editor and Settings. Custom text stays in RowStyle. */
@Composable
fun rowLabel(key: HomeRowKey): String = when (key) {
    HomeRowKey.APPS -> stringResource(R.string.row_favorite_apps)
    HomeRowKey.WATCH_NEXT -> stringResource(R.string.row_watch_next)
    else -> when (key.value.substringBefore(':')) {
        "halloween" -> stringResource(R.string.theme_halloween)
        "christmas" -> stringResource(R.string.theme_christmas)
        "continue" -> stringResource(R.string.row_continue)
        "latest_movies" -> stringResource(R.string.row_latest_movies)
        "latest_series" -> stringResource(R.string.row_latest_series)
        else -> key.value
    }
}
