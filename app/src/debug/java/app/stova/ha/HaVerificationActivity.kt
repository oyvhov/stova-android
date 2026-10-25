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
import androidx.lifecycle.lifecycleScope
import app.stova.StovaApplication
import app.stova.core.storage.StovaJson
import app.stova.core.theme.StovaTheme
import app.stova.ha.cards.*
import app.stova.ha.ui.*
import app.stova.profiles.Profile
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*

/** Read-only HA probe and dashboard fixture, shipped only in debug and only usable on an emulator.
 * It never writes to physical devices, exposes credentials, replaces profiles or bypasses a PIN.
 * All physical-device controls in verification go through the normal Stova UI.
 */
class HaVerificationActivity:ComponentActivity() {
    private val container get()=(application as StovaApplication).container
    override fun onStart() {super.onStart();container.haSocket.start()}
    // The probe is not the connection lifecycle owner. A second probe must not schedule a
    // disconnect after its successor has started; normal use remains owned by MainActivity.
    override fun onNewIntent(intent:android.content.Intent) {super.onNewIntent(intent);setIntent(intent);recreate()}
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        check(android.os.Build.HARDWARE in setOf("ranchu","goldfish"))
        var profile by mutableStateOf<Profile?>(null)
        val mode=intent.getStringExtra("mode") ?: "inspect"
        setContent {StovaTheme {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                profile?.let {p -> if(mode=="picker") Box(Modifier.fillMaxSize().padding(48.dp)) {AddCardFlow(container) {finish()}}
                    else SmartHomeScreen(container,p)
                }
            }
        }}
        lifecycleScope.launch {
            try {
                val file=container.profiles.current()
                val selected=file.profiles.singleOrNull {it.id==file.lastProfileId} ?: error("No active profile")
                check(!selected.kid && selected.pin==null) {"Verification requires an unprotected adult profile"}
                container.session.enter(selected.id)
                profile=selected
                withTimeout(20_000) {container.haSocket.status.first {it==HaStatus.CONNECTED}}
                val states=container.haSocket.states()
                val registry=container.haSocket.registry() ?: HaRegistry()
                val catalog=entityCatalog(states,registry)
                val safeAttributes=setOf("brightness","temperature","current_temperature","min_temp","max_temp","device_class","unit_of_measurement","supported_features","volume_level","color_temp_kelvin","supported_color_modes")
                val report=buildJsonObject {
                    put("captured_at",System.currentTimeMillis())
                    put("status",container.haSocket.status.value.name)
                    put("total_states",states.size)
                    put("registry_entities",registry.entities.size)
                    putJsonArray("entities") {catalog.forEach {item -> add(buildJsonObject {
                        put("id",item.entity.id);put("name",item.name);put("room",item.room);put("hidden",item.hidden)
                        put("state",item.entity.state);put("types",JsonArray(cardTypesFor(item.entity).map {JsonPrimitive(it.name)}))
                        put("attributes",JsonObject(item.entity.attributes.filterKeys {it in safeAttributes}))
                    })}}
                }
                withContext(Dispatchers.IO) {File(filesDir,"ha-verification.json").writeText(report.toString())}
                if(mode=="seed") {
                    val ids=intent.getStringExtra("entities").orEmpty().split(',').filter(String::isNotBlank).toSet()
                    val favorites=intent.getStringExtra("favorites").orEmpty().split(',').toSet()
                    val home=intent.getStringExtra("home").orEmpty().split(',').filter(String::isNotBlank).toSet()
                    check(ids.isNotEmpty() && ids.size<=24)
                    check(home.size<=3)
                    val current=container.profiles.current().profiles.single {it.id==selected.id}
                    val additional=ids.mapNotNull {id->catalog.find {it.entity.id==id}}.filter {item->current.dashboard.none {c->c.entityId==item.entity.id}}
                    val newCards=additional.map {item ->
                        val type=cardTypesFor(item.entity).first()
                        CardConfig("live-${item.entity.id}",type,item.entity.id,
                            size=if(type in setOf(CardType.WEATHER,CardType.ENERGY_PRICE,CardType.MEDIA_PLAYER)) CardSize.M else if(type==CardType.CAMERA) CardSize.L else CardSize.S,
                            favorite=item.entity.id in favorites,onHome=item.entity.id in home)
                    }
                    container.profiles.update(selected.id) {it.copy(dashboard=it.dashboard+newCards)}
                    profile=container.profiles.current().profiles.single {it.id==selected.id}
                }
            } catch(e:CancellationException) {throw e} catch(e:Exception) {
                // Report just the failure class, never HA URLs, response data or token contents.
                withContext(Dispatchers.IO) {File(filesDir,"ha-verification-error.txt").writeText(e.javaClass.simpleName)}
            }
        }
    }
}
