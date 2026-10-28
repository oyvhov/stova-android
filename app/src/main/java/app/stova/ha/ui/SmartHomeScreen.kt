package app.stova.ha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.AppContainer
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.*
import app.stova.ha.*
import app.stova.ha.cards.*
import app.stova.profiles.Profile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable fun SmartHomeScreen(container:AppContainer,profile:Profile,initialCardId:String?=null,backdrop:String?=null) {
    androidx.lifecycle.compose.LifecycleResumeEffect(container.haRepository) {
        container.haRepository.showingDashboard(true)
        onPauseOrDispose { container.haRepository.showingDashboard(false) }
    }
    val cards by container.haRepository.cards.collectAsStateWithLifecycle()
    val entities by container.haRepository.entities.collectAsStateWithLifecycle()
    val registry by container.haRepository.registry.collectAsStateWithLifecycle()
    val pending by container.haRepository.pending.collectAsStateWithLifecycle()
    val feedback by container.haRepository.feedback.collectAsStateWithLifecycle()
    val completed by container.haRepository.completed.collectAsStateWithLifecycle()
    val status by container.haSocket.status.collectAsStateWithLifecycle()
    val household by container.household.data.collectAsStateWithLifecycle(initialValue=null)
    val scope=rememberCoroutineScope()
    var adding by remember(profile.id) {mutableStateOf(false)}
    var dialog by remember(profile.id) {mutableStateOf<CardDialog?>(null)}
    val images=household?.haBaseUrl?.let {HaImageSource(it,container.haAuth,container.haImages)}
    LaunchedEffect(feedback) {if(feedback!=null) {delay(if(feedback==HaFeedback.FAILED) 5_000 else 2_000);container.haRepository.dismissFeedback()}}
    val firstFocus=remember(profile.id,initialCardId) {initialCardId ?: container.haRepository.lastFocused(profile.id)}
    SmartHomeContent(profile,cards,entities,if(household?.haBaseUrl==null) HaStatus.NOT_CONFIGURED else status,
        images=images,onAdd={adding=true},backdrop=backdrop,
        onLayoutChange={prefs->scope.launch {container.profiles.update(profile.id) {it.copy(smartHome=prefs)}}},
        onPerform={call,id->scope.launch {container.haRepository.perform(call,id,profile)}},onDialog={dialog=it},
        onMove={card,delta->scope.launch {container.haRepository.move(card.id,delta)}},
        onHome={card->container.haRepository.setOnHome(card.id,!card.onHome)},
        onRemove={card->scope.launch {container.haRepository.remove(card.id)}},
        onConfigure={card->scope.launch {container.haRepository.configure(card.id,card.size,card.name,card.icon,card.favorite,card.hiddenModes)}},
        onChipStyle={card,style->container.haRepository.setChipStyle(card.id,style)},
        registry=registry,pending=pending,feedback=feedback,connection=container.haSocket,completed=completed,overlayOpen=dialog!=null || adding,
        initialFocusId=firstFocus,onCardFocused={container.haRepository.rememberFocus(profile.id,it)})
    if(adding && !profile.kid) StovaDialog({adding=false}) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(StovaLayout.TvSafeEdge)) {AddCardFlow(container) {adding=false}}
    }
    CardDialogHost(dialog,entities,container.haSocket,images,!canControl(profile),{call,id->scope.launch {container.haRepository.perform(call,id,profile)}},{dialog=null},room=dialog?.let {registry.entities[it.entityId]?.room},
        name=cards.find {it.entityId==dialog?.entityId}?.name,send={call,id->container.haRepository.perform(call,id,profile)})
}
