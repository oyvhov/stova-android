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
import app.stova.home.LocalRowStyle
import app.stova.home.headingTextStyle
@Composable fun MediaRowHeader(row:HomeRowUi.Media) {
 Row(Modifier.padding(start=StovaLayout.TvSafeEdge,end=StovaLayout.TvSafeEdge,top=app.stova.core.theme.LocalAppearance.current.rowSpacing.top.dp,bottom=StovaLayout.SectionBottom),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
  app.stova.home.SeasonalMark(row.kind.season)
  Text(LocalRowStyle.current.heading(stringResource(when(row.kind){MediaRowKind.CONTINUE->R.string.row_continue;MediaRowKind.LATEST_MOVIES->R.string.row_latest_movies;MediaRowKind.LATEST_SERIES->R.string.row_latest_series;MediaRowKind.HALLOWEEN->R.string.theme_halloween;MediaRowKind.CHRISTMAS->R.string.theme_christmas})),style=LocalRowStyle.current.headingTextStyle(),color=MaterialTheme.colorScheme.onSurface,
   modifier=Modifier.weight(1f,fill=false),maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
  if(LocalRowStyle.current.showSource) Text(row.account.serverName,Modifier.widthIn(max=StovaLayout.RowSourceMaxWidth),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,
   maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
  if(row.offline) Text(stringResource(R.string.offline),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
 }
}
