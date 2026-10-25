package app.stova.ha.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.AppContainer
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.*
import app.stova.ha.*
import app.stova.ha.cards.*
import app.stova.profiles.Profile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil

@Composable fun SmartHomeScreen(container:AppContainer,profile:Profile) {
    val cards by container.haRepository.cards.collectAsStateWithLifecycle()
    val entities by container.haRepository.entities.collectAsStateWithLifecycle()
    val registry by container.haRepository.registry.collectAsStateWithLifecycle()
    val pending by container.haRepository.pending.collectAsStateWithLifecycle()
    val feedback by container.haRepository.feedback.collectAsStateWithLifecycle()
    val status by container.haSocket.status.collectAsStateWithLifecycle()
    val household by container.household.data.collectAsStateWithLifecycle(initialValue=null)
    val scope=rememberCoroutineScope()
    var adding by remember(profile.id) {mutableStateOf(false)}
    var dialog by remember(profile.id) {mutableStateOf<CardDialog?>(null)}
    val images=household?.haBaseUrl?.let {HaImageSource(it,container.haAuth,container.haImages)}
    LaunchedEffect(feedback) {if(feedback!=null) {delay(if(feedback==HaFeedback.FAILED) 5_000 else 2_000);container.haRepository.dismissFeedback()}}
    SmartHomeContent(profile,cards,entities,if(household?.haBaseUrl==null) HaStatus.NOT_CONFIGURED else status,
        images=images,onAdd={adding=true},
        onPerform={call,id->scope.launch {container.haRepository.perform(call,id,profile)}},onDialog={dialog=it},
        onMove={card,delta->scope.launch {container.haRepository.move(card.id,delta)}},
        onHome={card->container.haRepository.setOnHome(card.id,!card.onHome)},
        onRemove={card->scope.launch {container.haRepository.remove(card.id)}},
        onConfigure={card->scope.launch {container.haRepository.configure(card.id,card.size,card.name,card.icon,card.favorite)}},
        registry=registry,pending=pending,feedback=feedback,connection=container.haSocket)
    if(adding && !profile.kid) Dialog({adding=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(StovaLayout.TvSafeEdge)) {AddCardFlow(container) {adding=false}}
    }
    CardDialogHost(dialog,entities,container.haSocket,images,!canControl(profile),{call,id->scope.launch {container.haRepository.perform(call,id,profile)}},{dialog=null})
}

/** Rendering works from HaSnapshot before the network is ready. */
@Composable fun SmartHomeContent(
    profile:Profile,cards:List<CardConfig>,entities:Map<String,HaEntity>,status:HaStatus,
    images:HaImageSource?=null,onAdd:()->Unit={},onPerform:(ServiceCall,String)->Unit={_,_->},
    onDialog:(CardDialog)->Unit={},onMove:(CardConfig,Int)->Unit={_,_->},onWide:(CardConfig)->Unit={},
    onHome:suspend (CardConfig)->Boolean={true},onRemove:(CardConfig)->Unit={},
    onConfigure:(CardConfig)->Unit={},registry:HaRegistry=HaRegistry(),pending:Set<String> = emptySet(),
    feedback:HaFeedback?=null,connection:HaConnection?=null,
) {
    var editing by remember(profile.id) {mutableStateOf(false)}
    var menuId by remember(profile.id) {mutableStateOf<String?>(null)}
    var restoreId by remember(profile.id) {mutableStateOf<String?>(null)}
    var initialFocusGiven by remember(profile.id) {mutableStateOf(false)}
    val focus=remember(profile.id) {mutableMapOf<String,FocusRequester>()}
    val list=rememberLazyListState()
    val scope=rememberCoroutineScope()
    var gridFocused by remember {mutableStateOf(false)}
    val visible by remember {derivedStateOf {list.layoutInfo.visibleItemsInfo.map {it.key}.toSet()}}
    val sections=remember(cards,registry) {dashboardSections(cards,registry)}
    LaunchedEffect(menuId) {if(menuId==null) restoreId?.let {runCatching {focus[it]?.requestFocus()};restoreId=null}}
    BackHandler(enabled=editing) {editing=false;menuId=null}
    Column(Modifier.fillMaxSize().padding(StovaLayout.TvSafeEdge),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(stringResource(R.string.nav_smart_home),style=MaterialTheme.typography.headlineMedium)
                    HaStatusLabel(status)
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {DashboardSummary(cards,entities)}
                    if(cards.isNotEmpty() && gridFocused && LocalConfiguration.current.fontScale<1.5f) {
                        Text(stringResource(if(canControl(profile)) R.string.ha_v2_remote_hint else R.string.ha_locked),
                            style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if(!profile.kid && status!=HaStatus.NOT_CONFIGURED) {
                if(cards.isNotEmpty()) StovaButton(stringResource(if(editing) R.string.ha_done else R.string.ha_edit),{editing=!editing})
                StovaButton(stringResource(R.string.ha_add_card),onAdd,icon=StovaIcons.Plus)
            }
        }
        if(status==HaStatus.NOT_CONFIGURED) {Text(stringResource(R.string.ha_setup_instruction));return@Column}
        if(feedback!=null) Text(stringResource(if(feedback==HaFeedback.FAILED) R.string.ha_v2_failed else R.string.ha_v2_done),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(cards.isEmpty()) {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.large).padding(32.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(StovaIcons.Home,null,Modifier.size(40.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.ha_empty),style=MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.ha_v2_empty_hint),style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
        LazyColumn(Modifier.fillMaxSize().onFocusChanged {gridFocused=it.hasFocus},state=list,verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(4.dp,4.dp,4.dp,28.dp)) {
            sections.forEachIndexed {index,section ->
                item(key="section-$index") {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Text(if(section.favorites) stringResource(R.string.ha_v2_favorites) else section.room ?: stringResource(R.string.ha_v2_unassigned),style=MaterialTheme.typography.titleMedium)
                        Text(pluralStringResource(R.plurals.ha_v2_room_count,section.cards.size,section.cards.size),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(packCards(section.cards),key={band->"band-${band.first().card.id}"}) {band ->
                    CardBand(band) {card ->
                        val requester=remember(card.id) {focus.getOrPut(card.id) {FocusRequester()}}
                        val intoView=remember(card.id) {BringIntoViewRequester()}
                        LaunchedEffect(card.id) {
                            if(!initialFocusGiven && card.id==cards.firstOrNull()?.id) {
                                requester.requestFocus();initialFocusGiven=true
                            }
                        }
                        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            HaCard(card,entities[card.entityId],!canControl(profile),{onPerform(it,card.entityId)},onDialog,images,
                                visible="band-${band.first().card.id}" in visible,
                                onEdit=if(editing) {{menuId=card.id;restoreId=card.id}} else null,
                                onMenu=if(!profile.kid) {{menuId=card.id;restoreId=card.id}} else null,
                                pending=card.entityId in pending,connection=connection,
                                room=if(section.favorites) registry.entities[card.entityId]?.room else null,
                                modifier=Modifier.weight(1f).bringIntoViewRequester(intoView).onFocusChanged {
                                    if(it.isFocused) scope.launch {intoView.bringIntoView()}
                                }.focusRequester(requester))
                            if(editing && !profile.kid) StovaButton(stringResource(R.string.ha_card_menu,card.name ?: entities[card.entityId]?.let(::friendlyName) ?: card.entityId),{menuId=card.id;restoreId=card.id})
                        }
                    }
                }
            }
        }
        }
    }
    cards.find {it.id==menuId}?.takeUnless {profile.kid}?.let {card ->
        CardMenu(card,card.name ?: entities[card.entityId]?.let(::friendlyName) ?: card.entityId,onMove,onConfigure,onHome,onRemove) {menuId=null}
    }
}

@Composable private fun DashboardSummary(cards:List<CardConfig>,entities:Map<String,HaEntity>) {
    val locale=LocalConfiguration.current.locales[0]
    val selected=cards.mapNotNull {entities[it.entityId]}.distinctBy {it.id}
    val parts=mutableListOf<String>()
    val lights=selected.count {it.id.startsWith("light.") && it.state=="on"}
    if(selected.any {it.id.startsWith("light.")}) parts+=pluralStringResource(R.plurals.ha_v2_lights_on,lights,lights)
    selected.firstNotNullOfOrNull {if(it.id.startsWith("climate.")) it.attributes.number("current_temperature") else null}?.let {parts+=stringResource(R.string.ha_v2_inside,formatNumber(it,locale))}
    cards.firstOrNull {it.type==CardType.ENERGY_PRICE}?.let {entities[it.entityId]}?.let {e ->
        (stateText(e) as? StateText.Number)?.let {parts+=stringResource(R.string.ha_v2_price_now,formatNumber(it.value,locale),it.unit.orEmpty())}
    }
    Text(if(parts.isEmpty()) stringResource(R.string.ha_v2_overview) else parts.joinToString(" · "),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A real two-dimensional layout: L spans two rows, while neighbours remain reachable. */
@Composable private fun CardBand(band:List<PlacedCard>,content:@Composable (CardConfig)->Unit) {
    Layout(content={band.forEach {placed -> key(placed.card.id) {content(placed.card)}}}) {measurables,constraints ->
        val gap=16.dp.roundToPx();val cell=(constraints.maxWidth-2*gap)/3
        val base=152.dp.roundToPx()
        // Derive row heights from text before measuring: equal edges, including at font scale 2.
        val unit=measurables.mapIndexed {i,m ->
            val size=band[i].card.size
            ceil((m.maxIntrinsicHeight(cell*size.columns+gap*(size.columns-1))-gap*(size.rows-1)).toDouble()/size.rows).toInt().coerceAtLeast(base)
        }.maxOrNull() ?: base
        val placeables=measurables.mapIndexed {i,m ->
            val size=band[i].card.size
            m.measure(Constraints.fixed(cell*size.columns+gap*(size.columns-1),unit*size.rows+gap*(size.rows-1)))
        }
        val rows=band.maxOf {it.row+it.card.size.rows}
        layout(constraints.maxWidth,unit*rows+gap*(rows-1)) {
            placeables.forEachIndexed {i,p->p.placeRelative(band[i].column*(cell+gap),band[i].row*(unit+gap))}
        }
    }
}

@Composable private fun CardMenu(card:CardConfig,label:String,onMove:(CardConfig,Int)->Unit,onConfigure:(CardConfig)->Unit,
    onHome:suspend (CardConfig)->Boolean,onRemove:(CardConfig)->Unit,onDismiss:()->Unit) {
    var page by remember(card.id) {mutableStateOf("main")}
    var name by remember(card.id) {mutableStateOf(card.name ?: label)}
    var limit by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    fun save(updated:CardConfig) {onConfigure(updated);onDismiss()}
    Dialog(onDismiss) {
        Column(Modifier.widthIn(max=560.dp).background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.large).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(label,style=MaterialTheme.typography.titleLarge)
            LazyColumn(Modifier.heightIn(max=320.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                when(page) {
                    "name" -> item {
                        RemoteTextField(name,{name=it},stringResource(R.string.ha_v2_name))
                        StovaButton(stringResource(R.string.ha_v2_save),{save(card.copy(name=name))})
                    }
                    "size" -> items(CardSize.entries) {size -> StovaButton(stringResource(when(size) {CardSize.S->R.string.ha_v2_size_s;CardSize.M->R.string.ha_v2_size_m;CardSize.L->R.string.ha_v2_size_l}),{save(card.copy(size=size))},primary=card.size==size)}
                    "icon" -> items(listOf(null,"light","sun","home","climate","sensor","lock")) {icon ->
                        val type=when(icon) {"light"->CardType.LIGHT;"sun"->CardType.WEATHER;"home"->CardType.SCENE;"climate"->CardType.CLIMATE;"lock"->CardType.LOCK;else->CardType.SENSOR}
                        StovaButton(stringResource(when(icon) {null->R.string.ha_v2_icon_default;"sun"->R.string.ha_v2_sun_icon;"home"->R.string.nav_home;else->cardTypeLabel(type)}),{save(card.copy(icon=icon))},icon=cardIcon(type,override=icon))
                    }
                    else -> {
                        item {StovaButton(stringResource(R.string.ha_v2_size),{page="size"})}
                        item {StovaButton(stringResource(if(card.onHome) R.string.ha_hide_home else R.string.ha_show_home),{scope.launch {if(onHome(card)) onDismiss() else limit=true}})}
                        if(limit) item {Text(stringResource(R.string.ha_home_limit),color=MaterialTheme.colorScheme.onSurfaceVariant)}
                        item {StovaButton(stringResource(R.string.ha_move_left),{onMove(card,-1);onDismiss()})}
                        item {StovaButton(stringResource(R.string.ha_move_right),{onMove(card,1);onDismiss()})}
                        item {StovaButton(stringResource(R.string.ha_v2_move_up),{onMove(card,-3);onDismiss()})}
                        item {StovaButton(stringResource(R.string.ha_v2_move_down),{onMove(card,3);onDismiss()})}
                        item {StovaButton(stringResource(R.string.ha_v2_rename),{page="name"})}
                        item {StovaButton(stringResource(R.string.ha_v2_icon),{page="icon"})}
                        item {StovaButton(stringResource(if(card.favorite) R.string.ha_v2_favorite_remove else R.string.ha_v2_favorite_add),{save(card.copy(favorite=!card.favorite))})}
                        item {StovaButton(stringResource(R.string.ha_remove),{onRemove(card);onDismiss()})}
                    }
                }
            }
            StovaButton(stringResource(R.string.ha_close),onDismiss)
        }
    }
}
