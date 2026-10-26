package app.stova.ha.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.color
import app.stova.core.ui.*
import app.stova.ha.*
import app.stova.ha.cards.*
import app.stova.profiles.Profile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlin.math.ceil

/** Rendering works from HaSnapshot before the network is ready. */
@Composable fun SmartHomeContent(
    profile:Profile,cards:List<CardConfig>,entities:Map<String,HaEntity>,status:HaStatus,
    images:HaImageSource?=null,onAdd:()->Unit={},onPerform:(ServiceCall,String)->Unit={_,_->},
    onDialog:(CardDialog)->Unit={},onMove:(CardConfig,Int)->Unit={_,_->},onWide:(CardConfig)->Unit={},
    onHome:suspend (CardConfig)->Boolean={true},onRemove:(CardConfig)->Unit={},
    onConfigure:(CardConfig)->Unit={},registry:HaRegistry=HaRegistry(),pending:Set<String> = emptySet(),
    feedback:HaFeedback?=null,connection:HaConnection?=null,completed:Set<String> = emptySet(),
    initialFocusId:String?=null,onCardFocused:(String)->Unit={},overlayOpen:Boolean=false,
    onLayoutChange:(DashboardPrefs)->Unit={},
    backdrop:String?=null,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
    SmartHomeAtmosphere(backdrop,cards.firstOrNull {it.type==CardType.WEATHER}?.let {entities[it.entityId]?.state})
    val columns = dashboardColumns((maxWidth - StovaLayout.TvSafeEdge * 2 - 8.dp).value,
        profile.smartHome.density, LocalDensity.current.fontScale)
    val headings = profile.smartHome.grouping == DashboardGrouping.ROOM
    var layoutOpen by remember(profile.id) { mutableStateOf(false) }
    val covered = overlayOpen || layoutOpen
    var editing by remember(profile.id) {mutableStateOf(false)}
    var menuId by remember(profile.id) {mutableStateOf<String?>(null)}
    var restoreId by remember(profile.id) {mutableStateOf<String?>(null)}
    var initialFocusGiven by remember(profile.id) {mutableStateOf(false)}
    var focusedId by remember(profile.id) {mutableStateOf(initialFocusId)}
    var hintVisible by remember {mutableStateOf(false)}
    var lightsToTurnOff by remember {mutableStateOf<List<String>?>(null)}
    val summaryFocus=remember {FocusRequester()}
    val headerFocus=remember {FocusRequester()}
    var editHeld by remember {mutableStateOf(false)}
    var editBackDown by remember {mutableStateOf(false)}
    var editConsumed by remember {mutableStateOf(false)}
    var movingId by remember {mutableStateOf<String?>(null)}
    var holdJob by remember {mutableStateOf<kotlinx.coroutines.Job?>(null)}
    val timeout=androidx.compose.ui.platform.LocalViewConfiguration.current.longPressTimeoutMillis
    val focus=remember(profile.id) {mutableMapOf<String,FocusRequester>()}
    val list=rememberLazyListState()
    val scope=rememberCoroutineScope()
    val scrollMotion=app.stova.core.theme.stovaMotion
    var gridFocused by remember {mutableStateOf(false)}
    val visible by remember {derivedStateOf {list.layoutInfo.visibleItemsInfo.map {it.key}.toSet()}}
    val sections=remember(cards,registry,profile.smartHome.grouping) {dashboardSections(cards,registry,profile.smartHome.grouping)}
    val cells=remember(sections,columns,headings) {cardFocusCells(sections,columns,headings)}
    var navigation by remember {mutableStateOf<Job?>(null)}
    var pendingCard by remember {mutableStateOf<String?>(null)}
    var navigationVersion by remember {mutableIntStateOf(0)}
    fun listIndex(id:String):Int = cells.firstOrNull { it.id == id }?.revealIndex ?: 0
    val firstId=cards.find {it.id==initialFocusId}?.id ?: cells.firstOrNull()?.id
    fun requestCard(id:String) {
        val target=cells.firstOrNull {it.id==id} ?: return
        navigation?.cancel()
        navigationVersion++
        val version=navigationVersion
        pendingCard=id
        navigation=scope.launch {
            try {
                withFrameNanos { }
                list.smoothScrollToItem(listIndex(id),scrollMotion)
                // A later band may not be composed until the scroll has reached layout.
                repeat(8) {
                    if(runCatching {focus[id]?.requestFocus()}.getOrNull()==true) return@launch
                    withFrameNanos {}
                }
            } finally {
                if(version==navigationVersion) pendingCard=null
            }
        }
    }
    fun navigate(key:Key):Boolean {
        val direction=when(key) {
            Key.DirectionUp->CardFocusDirection.UP
            Key.DirectionDown->CardFocusDirection.DOWN
            Key.DirectionLeft->CardFocusDirection.LEFT
            Key.DirectionRight->CardFocusDirection.RIGHT
            else->return false
        }
        val id=pendingCard ?: focusedId ?: return true
        val next=cardFocusNeighbor(cells,id,direction)
        if(next!=null) requestCard(next)
        else if(direction==CardFocusDirection.UP) {
            navigation?.cancel()
            pendingCard=null
            runCatching {headerFocus.requestFocus(FocusDirection.Enter)}
        }
        return true
    }
    LaunchedEffect(cards,firstId) {if(!initialFocusGiven && firstId!=null) list.scrollToItem(listIndex(firstId))}
    // Capture before layout detaches an old band. Disk updates may arrive after the menu
    // has already closed and restored focus to the old layout.
    val layoutFocusTarget = if(gridFocused) focusedId else null
    val fontScale = LocalDensity.current.fontScale
    LaunchedEffect(cells,fontScale) {
        if(initialFocusGiven && !covered && menuId==null && layoutFocusTarget!=null) {
            val target=layoutFocusTarget.takeIf {id->cards.any {it.id==id}} ?: cells.firstOrNull()?.id
            if(target!=null) requestCard(target)
        }
    }
    LaunchedEffect(menuId,covered,cards,restoreId,columns,headings) {
        if(menuId==null && !covered) restoreId?.let {id ->
            val target=id.takeIf {cardId->cards.any {it.id==cardId}} ?: cards.firstOrNull()?.id
            if(target!=null) {
                list.scrollToItem(listIndex(target))
                withFrameNanos {}
                runCatching {focus[target]?.requestFocus()}
            }
            restoreId=null
        }
    }
    LaunchedEffect(focusedId,gridFocused,editing,covered,menuId) {
        hintVisible=gridFocused && !covered && menuId==null
        if(hintVisible) {delay(4_000);hintVisible=false}
    }
    DisposableEffect(Unit) {onDispose {holdJob?.cancel();navigation?.cancel()}}
    LaunchedEffect(covered,menuId) {if(covered || menuId!=null) {holdJob?.cancel();navigation?.cancel();editHeld=false;editConsumed=true}}
    BackHandler(enabled=editing) {editing=false;menuId=null;restoreId=focusedId}
    Box(Modifier.fillMaxSize().onPreviewKeyEvent {event ->
        if(event.key==Key.Back && (editBackDown || (editing && menuId==null && !covered))) {
            if(event.type==KeyEventType.KeyDown) {editBackDown=true;editing=false;holdJob?.cancel();editHeld=false;restoreId=focusedId}
            else editBackDown=false
            true
        } else if(!editing || (!gridFocused && !editHeld) || menuId!=null || covered) false
        else if(event.key in listOf(Key.DirectionCenter,Key.Enter,Key.NumPadEnter)) {
            if(event.type==KeyEventType.KeyDown && !editHeld) {
                editHeld=true;editConsumed=false;movingId=focusedId
                holdJob=scope.launch {delay(timeout);editConsumed=true
                    cards.find {it.id==movingId}?.let {card->restoreId=card.id;onDialog(controlPanel(card.type,card.entityId))}
                }
            } else if(event.type==KeyEventType.KeyUp) {
                holdJob?.cancel();editHeld=false
                if(!editConsumed) {menuId=movingId;restoreId=movingId}
            }
            true
        } else if(editHeld && event.key in listOf(Key.DirectionLeft,Key.DirectionRight,Key.DirectionUp,Key.DirectionDown)) {
            holdJob?.cancel();editConsumed=true
            if(event.type==KeyEventType.KeyDown) cards.find {it.id==movingId}?.let {card ->
                restoreId=card.id
                if(event.key==Key.DirectionLeft || event.key==Key.DirectionRight) onMove(card,if(event.key==Key.DirectionLeft) -1 else 1)
            }
            true
        } else false
    }) {
    Column(Modifier.fillMaxSize().padding(StovaLayout.TvSafeEdge),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth().focusRequester(headerFocus).focusRestorer()
            .onPreviewKeyEvent {event ->
                if(event.key==Key.DirectionDown && cells.isNotEmpty() && !covered && menuId==null) {
                    if(event.type==KeyEventType.KeyDown) requestCard(focusedId?.takeIf {id->cells.any {it.id==id}} ?: cells.first().id)
                    true
                } else {
                    if(event.type==KeyEventType.KeyDown) navigation?.cancel()
                    false
                }
            }.focusGroup(),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically) {
            // Title, summary and actions share one line and one centre; only the summary text yields to space.
            PageHeader(stringResource(R.string.nav_smart_home),StovaIcons.Home,Modifier.width(IntrinsicSize.Max),
                tint=app.stova.core.theme.AvatarColor.GOLD.color,
                iconModifier=Modifier.testTag("page-icon-smart-home"),metadata={HaStatusLabel(status)})
            DashboardSummary(cards,entities,canControl(profile),summaryFocus,Modifier.weight(1f)) {lightsToTurnOff=it}
            if(!profile.kid && status!=HaStatus.NOT_CONFIGURED) {
                StovaButton(stringResource(R.string.ha_layout), { restoreId=focusedId; layoutOpen=true },
                    icon=StovaIcons.Rows,compact=true,quiet=true)
                if(cards.isNotEmpty()) StovaButton(stringResource(if(editing) R.string.ha_done else R.string.ha_edit),
                    {editing=!editing;restoreId=focusedId ?: firstId},icon=if(editing) StovaIcons.Check else StovaIcons.Edit,compact=true,quiet=!editing)
                StovaButton(stringResource(R.string.ha_add_card),onAdd,icon=StovaIcons.Plus,compact=true)
            }
        }
        if(status==HaStatus.NOT_CONFIGURED) {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.large).padding(28.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                CategoryIcon(StovaIcons.Link,size=48.dp)
                Text(stringResource(R.string.ha_setup_instruction),style=MaterialTheme.typography.titleMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }
        androidx.compose.animation.Crossfade(feedback, animationSpec = app.stova.core.theme.stovaMotion.fade(), label = "ha-feedback") { shown ->
            if(shown!=null) Text(stringResource(if(shown==HaFeedback.FAILED) R.string.ha_v2_failed else R.string.ha_v2_done),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(cards.isEmpty()) {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.large).padding(28.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                CategoryIcon(StovaIcons.Rows,size=48.dp)
                Text(stringResource(R.string.ha_empty),style=MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.ha_v2_empty_hint),style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
        LazyColumn(Modifier.fillMaxSize().onFocusChanged {gridFocused=it.hasFocus}.onPreviewKeyEvent {event ->
            if(!covered && menuId==null && !editHeld && event.key in listOf(Key.DirectionUp,Key.DirectionDown,Key.DirectionLeft,Key.DirectionRight)) {
                if(event.type==KeyEventType.KeyDown) navigate(event.key)
                true
            } else false
        },state=list,verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(4.dp,4.dp,4.dp,28.dp)) {
            sections.forEachIndexed {index,section ->
                if (headings) item(key="section-$index") {
                    SectionHeading(if(section.favorites) stringResource(R.string.ha_v2_favorites) else section.room ?: stringResource(R.string.ha_v2_unassigned),
                        if(section.favorites) StovaIcons.Star else StovaIcons.Home,
                        detail=pluralStringResource(R.plurals.ha_v2_room_count,section.cards.size,section.cards.size),
                        iconModifier=Modifier.testTag(if(section.favorites) "section-icon-favorites" else "section-icon-room-$index"))
                }
                items(packCards(section.cards,columns),key={band->"band-${band.first().card.id}"}) {band ->
                    CardBand(band,columns) {card ->
                        val requester=remember(card.id) {focus.getOrPut(card.id) {FocusRequester()}}
                        val intoView=remember(card.id) {BringIntoViewRequester()}
                        LaunchedEffect(card.id) {
                            if(!initialFocusGiven && card.id==firstId) {
                                requester.requestFocus();initialFocusGiven=true
                            }
                        }
                        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            HaCard(card,entities[card.entityId],!canControl(profile),{onPerform(it,card.entityId)},{restoreId=card.id;onDialog(it)},images,
                                visible="band-${band.first().card.id}" in visible,
                                onEdit=if(editing) {{menuId=card.id;restoreId=card.id}} else null,
                                onMenu=if(!profile.kid) {{menuId=card.id;restoreId=card.id}} else null,
                                pending=card.entityId in pending,connection=connection,completed=card.entityId in completed,editing=editing && !profile.kid,inputSuspended=covered || menuId!=null,
                                room=if(!headings || section.favorites) registry.entities[card.entityId]?.room else null,
                                modifier=Modifier.weight(1f).bringIntoViewRequester(intoView).onFocusChanged {
                                    if(it.isFocused) {focusedId=card.id;onCardFocused(card.id);scope.launch {intoView.bringIntoView()}}
                                }.focusRequester(requester))
                        }
                    }
                }
            }
        }
        }
    }
    val hintCard=cards.find {it.id==focusedId}
    val motion=app.stova.core.theme.stovaMotion
    val hintAlpha by animateFloatAsState(if(hintVisible) 1f else 0f,motion.fade(motion.quick))
    if(hintCard!=null && hintAlpha>0f) Text(remoteHint(hintCard,entities[hintCard.entityId],!canControl(profile),profile.kid,editing),
        Modifier.align(Alignment.BottomCenter).padding(horizontal=StovaLayout.TvSafeEdge,vertical=12.dp).alpha(hintAlpha).testTag("remote-hint"),
        style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
    lightsToTurnOff?.let {ids ->
        fun dismiss() {lightsToTurnOff=null;runCatching {summaryFocus.requestFocus()}}
        StovaDialog(::dismiss) {
            val cancelFocus=remember {FocusRequester()}
            Column(Modifier.widthIn(max=480.dp).background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.large).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.ha_action_all_lights,ids.size),style=MaterialTheme.typography.titleLarge)
                StovaButton(stringResource(R.string.ha_cancel),::dismiss,Modifier.focusRequester(cancelFocus))
                StovaButton(stringResource(R.string.ha_action_yes_off),{
                    if(canControl(profile)) ids.filter {entities[it]?.state=="on" && it !in pending}.forEach {onPerform(ServiceCall("light","turn_off"),it)}
                    dismiss()
                },enabled=canControl(profile))
            }
            LaunchedEffect(Unit) {cancelFocus.requestFocus()}
        }
    }
    cards.find {it.id==menuId}?.takeUnless {profile.kid}?.let {card ->
        CardMenu(card,card.name ?: entities[card.entityId]?.let(::friendlyName) ?: card.entityId,
            onMove=onMove,onConfigure=onConfigure,onHome=onHome,onRemove=onRemove,
            onDismiss={menuId=null})
    }
    if(layoutOpen) DashboardLayoutDialog(profile.smartHome,onLayoutChange) {layoutOpen=false}
    }
}

@Composable private fun DashboardSummary(cards:List<CardConfig>,entities:Map<String,HaEntity>,canControl:Boolean,focus:FocusRequester,modifier:Modifier=Modifier,onLights:(List<String>)->Unit) {
    val locale=LocalConfiguration.current.locales[0]
    val selected=cards.mapNotNull {entities[it.entityId]}.distinctBy {it.id}
    val parts=mutableListOf<String>()
    val lights=selected.filter {it.id.startsWith("light.") && it.state=="on"}.map {it.id}
    Row(modifier,horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
        if(selected.any {it.id.startsWith("light.")}) {
            val label=pluralStringResource(R.plurals.ha_v2_lights_on,lights.size,lights.size)
            if(canControl && lights.isNotEmpty()) StovaButton(label,{onLights(lights)},Modifier.focusRequester(focus),icon=StovaIcons.Light,compact=true,quiet=true)
            else Text(label,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        selected.firstNotNullOfOrNull {if(it.id.startsWith("climate.")) it.attributes.number("current_temperature") else null}?.let {parts+=stringResource(R.string.ha_v2_inside,formatNumber(it,locale))}
        cards.firstOrNull {it.type==CardType.ENERGY_PRICE}?.let {entities[it.entityId]}?.let {e ->
            (stateText(e) as? StateText.Number)?.let {parts+=stringResource(R.string.ha_v2_price_now,formatNumber(it.value,locale),it.unit.orEmpty())}
        }
        if(parts.isNotEmpty() || selected.isEmpty()) Text(if(parts.isEmpty()) stringResource(R.string.ha_v2_overview) else parts.joinToString(" · "),
            Modifier.weight(1f,fill=false),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

/** A real two-dimensional layout: L spans two rows, while neighbours remain reachable. */
@Composable private fun CardBand(band:List<PlacedCard>,columns:Int,content:@Composable (CardConfig)->Unit) {
    Layout(content={band.forEach {placed -> key(placed.card.id) {content(placed.card)}}}) {measurables,constraints ->
        val gap=16.dp.roundToPx();val cell=(constraints.maxWidth-(columns-1)*gap)/columns
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
