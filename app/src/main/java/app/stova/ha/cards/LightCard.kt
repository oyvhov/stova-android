package app.stova.ha.cards
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.ha.HaEntity
@Composable fun LightCard(entity:HaEntity) {
    Text(stringResource(if(entity.state=="on") R.string.ha_on else R.string.ha_off),style=MaterialTheme.typography.headlineMedium,color=if(entity.state=="on") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
}
