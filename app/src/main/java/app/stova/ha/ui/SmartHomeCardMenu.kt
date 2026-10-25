package app.stova.ha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.*
import app.stova.ha.*
import app.stova.ha.cards.*
import kotlinx.coroutines.launch

@Composable internal fun CardMenu(card:CardConfig,label:String,onMove:(CardConfig,Int)->Unit,onConfigure:(CardConfig)->Unit,
    onHome:suspend (CardConfig)->Boolean,onRemove:(CardConfig)->Unit,onDismiss:()->Unit) {
    var page by remember(card.id) {mutableStateOf("main")}
    var name by remember(card.id) {mutableStateOf(card.name ?: label)}
    var limit by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    fun save(updated:CardConfig) {onConfigure(updated);onDismiss()}
    StovaDialog(onDismiss) {
        Column(Modifier.widthIn(max=560.dp).background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.large).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            PageHeader(label,cardIcon(card.type,override=card.icon))
            LazyColumn(Modifier.heightIn(max=320.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                when(page) {
                    "name" -> item {
                        RemoteTextField(name,{name=it},stringResource(R.string.ha_v2_name))
                        StovaButton(stringResource(R.string.ha_v2_save),{save(card.copy(name=name))})
                    }
                    "size" -> items(CardSize.entries) {size -> StovaButton(stringResource(when(size) {CardSize.S->R.string.ha_v2_size_s;CardSize.M->R.string.ha_v2_size_m;CardSize.L->R.string.ha_v2_size_l}),{save(card.copy(size=size))},primary=card.size==size)}
                    "icon" -> items(listOf(null,"light","sun","home","climate","sensor","lock")) {icon ->
                        val type=when(icon) {null->card.type;"light"->CardType.LIGHT;"sun"->CardType.WEATHER;"home"->CardType.SCENE;"climate"->CardType.CLIMATE;"lock"->CardType.LOCK;else->CardType.SENSOR}
                        StovaButton(stringResource(when(icon) {null->R.string.ha_v2_icon_default;"sun"->R.string.ha_v2_sun_icon;"home"->R.string.nav_home;else->cardTypeLabel(type)}),{save(card.copy(icon=icon))},icon=cardIcon(type,override=icon))
                    }
                    else -> {
                        item {StovaButton(stringResource(R.string.ha_v2_size),{page="size"},icon=StovaIcons.Rows,compact=true,quiet=true)}
                        item {StovaButton(stringResource(if(card.onHome) R.string.ha_hide_home else R.string.ha_show_home),{scope.launch {if(onHome(card)) onDismiss() else limit=true}},icon=StovaIcons.Home,compact=true,quiet=true)}
                        if(limit) item {Text(stringResource(R.string.ha_home_limit),color=MaterialTheme.colorScheme.onSurfaceVariant)}
                        item {StovaButton(stringResource(R.string.ha_move_left),{onMove(card,-1);onDismiss()})}
                        item {StovaButton(stringResource(R.string.ha_move_right),{onMove(card,1);onDismiss()})}
                        item {StovaButton(stringResource(R.string.ha_v2_move_up),{onMove(card,-3);onDismiss()})}
                        item {StovaButton(stringResource(R.string.ha_v2_move_down),{onMove(card,3);onDismiss()})}
                        item {StovaButton(stringResource(R.string.ha_v2_rename),{page="name"},icon=StovaIcons.Edit,compact=true,quiet=true)}
                        item {StovaButton(stringResource(R.string.ha_v2_icon),{page="icon"},icon=StovaIcons.Palette,compact=true,quiet=true)}
                        item {StovaButton(stringResource(if(card.favorite) R.string.ha_v2_favorite_remove else R.string.ha_v2_favorite_add),{save(card.copy(favorite=!card.favorite))},icon=StovaIcons.Star,compact=true,quiet=true)}
                        item {StovaButton(stringResource(R.string.ha_remove),{onRemove(card);onDismiss()})}
                    }
                }
            }
            StovaButton(stringResource(R.string.ha_close),onDismiss)
        }
    }
}
