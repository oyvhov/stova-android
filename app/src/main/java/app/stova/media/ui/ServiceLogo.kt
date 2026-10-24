package app.stova.media.ui
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.media.model.ServerKind
@Composable fun ServiceLogo(kind: ServerKind,size: Dp=20.dp) {
 Image(painterResource(if(kind==ServerKind.JELLYFIN) R.drawable.ic_jellyfin else R.drawable.ic_emby),contentDescription=stringResource(if(kind==ServerKind.JELLYFIN) R.string.service_jellyfin else R.string.service_emby),modifier=Modifier.size(size))
}
