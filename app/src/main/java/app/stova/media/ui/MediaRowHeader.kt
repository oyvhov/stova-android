package app.stova.media.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.home.HomeRowUi
import app.stova.media.MediaRowKind
@Composable fun MediaRowHeader(row:HomeRowUi.Media) {
 Row(Modifier.padding(start=StovaLayout.TvSafeEdge,end=StovaLayout.TvSafeEdge,top=app.stova.core.theme.LocalAppearance.current.rowSpacing.top.dp,bottom=StovaLayout.SectionBottom),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
  Text(stringResource(when(row.kind){MediaRowKind.CONTINUE->R.string.row_continue;MediaRowKind.LATEST_MOVIES->R.string.row_latest_movies;MediaRowKind.LATEST_SERIES->R.string.row_latest_series;MediaRowKind.HALLOWEEN->R.string.theme_halloween;MediaRowKind.CHRISTMAS->R.string.theme_christmas}),style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.onSurface)
  Text(row.account.serverName,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  if(row.offline) Text(stringResource(R.string.offline),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}
