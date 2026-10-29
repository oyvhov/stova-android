package app.stova.ha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.stova.core.storage.StovaJson
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaTheme
import app.stova.ha.cards.*
import app.stova.ha.ui.*
import app.stova.profiles.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.*

/** Isolated visual fixture: no container, credentials, disk writes or real service calls. */
open class ControlPreviewActivity : ComponentActivity() {
    protected open val previewFontScale:Float get()=1f
    override fun attachBaseContext(base:android.content.Context) {
        val config=android.content.res.Configuration(base.resources.configuration)
        config.setLocales(android.os.LocaleList(java.util.Locale.forLanguageTag("nn")))
        config.fontScale=previewFontScale
        super.attachBaseContext(base.createConfigurationContext(config))
    }
    override fun onNewIntent(intent:android.content.Intent) {super.onNewIntent(intent);setIntent(intent);recreate()}
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        check(android.os.Build.HARDWARE in setOf("ranchu","goldfish"))
        val screen=intent.getStringExtra("screen") ?: "dashboard"
        val socket=PreviewConnection()
        setContent {
            StovaTheme(reduceMotion=intent.getBooleanExtra("reduceMotion",false)) {
                val entities by socket.entities.collectAsState()
                val cards=remember {entities.values.map {CardConfig(it.id,cardTypesFor(it).first(),it.id)}}
                var dialog by remember {mutableStateOf(cards.find {it.entityId.substringBefore('.')==screen}?.let {controlPanel(it.type,it.entityId)})}
                var picker by remember {mutableStateOf(screen=="rooms")}
                val registry=HaRegistry(entities.keys.associateWith {HaEntityInfo(room=if(it.startsWith("climate")) "Kjøkken" else "Stova")})
                val profile=Profile("preview","Førehandsvising",AvatarColor.entries.first())
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    if(picker) Box(Modifier.fillMaxSize().padding(48.dp)) {
                        RoomSetupContent(entityCatalog(entities.values.toList(),registry),emptySet(),false,false,
                            onAdd={picker=false},onBrowse={picker=false},onClose={picker=false})
                    } else SmartHomeContent(profile,if(screen=="empty") emptyList() else cards,entities,HaStatus.CONNECTED,
                        onAdd={picker=true},onPerform={call,id->socket.apply(call,id)},onDialog={dialog=it},registry=registry,
                        connection=socket,overlayOpen=dialog!=null)
                    CardDialogHost(dialog,entities,socket,null,screen=="readonly",{call,id->socket.apply(call,id)},
                        onDismiss={dialog=null},room=dialog?.entityId?.let {registry.entities[it]?.room})
                }
            }
        }
    }
}

class LargeControlPreviewActivity : ControlPreviewActivity() {
    override val previewFontScale:Float get()=2f
}

private class PreviewConnection : HaConnection {
    private fun entity(id:String,state:String,attrs:String)=HaEntity(id,state,StovaJson.parseToJsonElement(attrs).jsonObject,0,0)
    override val status=MutableStateFlow(HaStatus.CONNECTED)
    override val entities=MutableStateFlow(listOf(
        entity("light.reading","off","""{"friendly_name":"Leselampa","supported_color_modes":["color_temp","hs"],"min_color_temp_kelvin":2000,"max_color_temp_kelvin":6500}"""),
        entity("climate.kitchen","heat","""{"friendly_name":"Varmepumpa","current_temperature":23.5,"temperature":21,"min_temp":10,"max_temp":30,"hvac_modes":["heat","cool","auto","off"],"preset_modes":["none","quiet","powerful","eco"],"preset_mode":"none"}"""),
        entity("media_player.living","playing","""{"friendly_name":"Høgtalarane i stova","volume_level":0.28,"supported_features":18485,"source":"TV","source_list":["TV","Radio","Musikk"]}"""),
        entity("weather.home","partlycloudy","""{"friendly_name":"Vêret heime","temperature":12.5}"""),
        entity("sensor.energy","0.84","""{"friendly_name":"Straumpris","unit_of_measurement":"kr/kWh","today":[0.7,0.6,0.5,0.6,0.7,0.8,1.1,1.3,1.5,1.2,1.0,0.9,0.84,0.8,0.9,1.1,1.4,1.5,1.4,1.1,0.9,0.8,0.7,0.6]}"""),
    ).associateBy {it.id})
    fun apply(call:ServiceCall,id:String) {entities.value=entities.value.mapValues {(key,value)->if(key==id) optimisticEntity(value,call) else value}}
    override fun watch(entityIds:Set<String>) {}
    override suspend fun callService(domain:String,service:String,entityId:String,data:JsonObject):Boolean {apply(ServiceCall(domain,service,data),entityId);return true}
    override suspend fun history(entityId:String,hours:Int)=listOf(0L to 0.7,1L to 0.9,2L to 0.84)
    override suspend fun states()=entities.value.values.toList()
    override suspend fun forecast(entityId:String)=List(5) {ForecastDay(1793278800000L+it*86_400_000L,if(it%2==0) "partlycloudy" else "rainy",12.0-it,6.0-it)}
    override suspend fun hourlyForecast(entityId:String)=List(24) {ForecastDay(1793278800000L+it*3_600_000L,"partlycloudy",12.0-it%5)}
}
