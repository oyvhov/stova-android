package app.stova.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import app.stova.core.theme.StovaLayout

/** One rhythm for every row heading: 22 dp above, 10 dp below, aligned with the TV safe edge. */
@Composable
fun RowHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        LocalRowStyle.current.heading(title),
        style = LocalRowStyle.current.headingTextStyle(),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = modifier.padding(
            start = StovaLayout.TvSafeEdge,
            end = StovaLayout.TvSafeEdge,
            top = app.stova.core.theme.LocalAppearance.current.rowSpacing.top.dp,
            bottom = StovaLayout.SectionBottom,
        ),
    )
}

@Composable
fun app.stova.profiles.RowStyle.headingTextStyle() = when (headingSize) {
    app.stova.profiles.HeadingSize.SMALL -> MaterialTheme.typography.titleMedium
    app.stova.profiles.HeadingSize.NORMAL -> MaterialTheme.typography.titleLarge
    app.stova.profiles.HeadingSize.LARGE -> MaterialTheme.typography.headlineSmall
}
