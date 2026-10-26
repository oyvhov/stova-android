package app.stova.ha.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.focusRequester
import app.stova.R
import app.stova.core.ui.*
import app.stova.ha.*
import app.stova.ha.cards.*
import kotlinx.coroutines.launch

/** Long OK on a card: the panel from the right, with size, move, name and icon as pages of their own. */
@Composable internal fun CardMenu(card:CardConfig,label:String,onMove:(CardConfig,Int)->Unit,onConfigure:(CardConfig)->Unit,
    onHome:suspend (CardConfig)->Boolean,onRemove:(CardConfig)->Unit,onDismiss:()->Unit) {
    var name by remember(card.id) {mutableStateOf(card.name ?: label)}
    var limit by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    fun save(updated:CardConfig) {onConfigure(updated);onDismiss()}
    val sizeLabel={size:CardSize -> when(size) {CardSize.S->R.string.ha_v2_size_s;CardSize.M->R.string.ha_v2_size_m;CardSize.L->R.string.ha_v2_size_l}}
    StovaSheet(onDismiss) {pages ->
        when(pages.current) {
            null -> {
                SheetHeader(label,stringResource(cardTypeLabel(card.type)),cardIcon(card.type,override=card.icon))
                SheetAction(stringResource(R.string.ha_v2_size),StovaIcons.Rows,{pages.open("size")},Modifier.focusRequester(pages.opener("size")),
                    value=stringResource(sizeLabel(card.size)).substringBefore(" "),opensPage=true)
                SheetAction(stringResource(if(card.onHome) R.string.ha_hide_home else R.string.ha_show_home),StovaIcons.Home,
                    {scope.launch {if(onHome(card)) onDismiss() else limit=true}})
                if(limit) Text(stringResource(R.string.ha_home_limit),Modifier.padding(horizontal=14.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)
                SheetAction(stringResource(R.string.ha_move),StovaIcons.Focus,{pages.open("move")},Modifier.focusRequester(pages.opener("move")),opensPage=true)
                SheetAction(stringResource(R.string.ha_v2_rename),StovaIcons.Edit,{pages.open("name")},Modifier.focusRequester(pages.opener("name")),opensPage=true)
                SheetAction(stringResource(R.string.ha_v2_icon),StovaIcons.Palette,{pages.open("icon")},Modifier.focusRequester(pages.opener("icon")),opensPage=true)
                SheetAction(stringResource(if(card.favorite) R.string.ha_v2_favorite_remove else R.string.ha_v2_favorite_add),StovaIcons.Star,
                    {save(card.copy(favorite=!card.favorite))})
                SheetAction(stringResource(R.string.ha_remove),StovaIcons.Hide,{onRemove(card);onDismiss()},danger=true)
            }
            "size" -> {
                SheetHeader(stringResource(R.string.ha_v2_size),label,StovaIcons.Rows)
                CardSize.entries.forEach {size -> LayoutChoice(stringResource(sizeLabel(size)),card.size==size,size.columns,size.rows) {save(card.copy(size=size))}}
            }
            "move" -> {
                SheetHeader(stringResource(R.string.ha_move),label,StovaIcons.Focus)
                SheetAction(stringResource(R.string.ha_move_earlier),StovaIcons.ChevronLeft,{onMove(card,-1);onDismiss()})
                SheetAction(stringResource(R.string.ha_move_later),StovaIcons.ChevronRight,{onMove(card,1);onDismiss()})
            }
            "name" -> {
                SheetHeader(stringResource(R.string.ha_v2_rename),label,StovaIcons.Edit)
                RemoteTextField(name,{name=it},stringResource(R.string.ha_v2_name))
                StovaButton(stringResource(R.string.ha_v2_save),{save(card.copy(name=name))},Modifier.fillMaxWidth(),primary=true)
            }
            "icon" -> {
                SheetHeader(stringResource(R.string.ha_v2_icon),label,StovaIcons.Palette)
                listOf(null,"light","sun","home","climate","sensor","lock").forEach {icon ->
                    val type=when(icon) {null->card.type;"light"->CardType.LIGHT;"sun"->CardType.WEATHER;"home"->CardType.SCENE;"climate"->CardType.CLIMATE;"lock"->CardType.LOCK;else->CardType.SENSOR}
                    SheetAction(stringResource(when(icon) {null->R.string.ha_v2_icon_default;"sun"->R.string.ha_v2_sun_icon;"home"->R.string.nav_home;else->cardTypeLabel(type)}),
                        cardIcon(type,override=icon),{save(card.copy(icon=icon))},chosen=card.icon==icon)
                }
            }
        }
    }
}
