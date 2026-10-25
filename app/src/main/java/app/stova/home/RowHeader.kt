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
        title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(
            start = StovaLayout.TvSafeEdge,
            end = StovaLayout.TvSafeEdge,
            top = app.stova.core.theme.LocalAppearance.current.rowSpacing.top.dp,
            bottom = StovaLayout.SectionBottom,
        ),
    )
}
