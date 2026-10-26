package app.stova.ha.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.*
import app.stova.core.storage.StovaJson
import app.stova.core.theme.*
import app.stova.ha.*
import app.stova.ha.cards.*
import app.stova.profiles.Profile
import kotlinx.serialization.json.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HaScreensTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Before fun enabled()=assumeScreensEnabled()
    private fun entity(id:String,state:String,attrs:String)=HaEntity(id,state,StovaJson.parseToJsonElement(attrs).jsonObject,0,0)
    @Test fun mixedFootprints() {
        val entities=listOf(entity("weather.home","rainy","""{"friendly_name":"Vêret ved huset","temperature":8.2,"temperature_unit":"°C"}"""),
            entity("media_player.home","playing","""{"friendly_name":"Musikk i stova","media_title":"Ein roleg kveld","media_artist":"Speleliste"}"""),
            entity("sensor.temperature","21.7","""{"friendly_name":"Temperaturen i vinterhagen","unit_of_measurement":"°C"}"""),
            entity("light.reading","on","""{"friendly_name":"Leselampe","brightness":120}"""))
        val cards=entities.mapIndexed {i,e->CardConfig("$i",cardTypesFor(e).first(),e.id,size=listOf(CardSize.L,CardSize.M,CardSize.S,CardSize.S)[i])}
        compose.setContent {StovaTheme {Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            SmartHomeContent(Profile("a","A",AvatarColor.LIME),cards,entities.associateBy {it.id},HaStatus.CONNECTED)
        }}}
        compose.saveScreen("ha-mixed-footprints")
    }
    @Test fun visualLayoutChoices() {
        compose.setContent {StovaTheme {DashboardLayoutDialog(DashboardPrefs(),{},{})}}
        compose.waitForIdle()
        compose.saveScreen("ha-layout-choices",org.robolectric.shadows.ShadowDialog.getLatestDialog().window!!.decorView)
    }
    @Test fun dashboard() {
        val entities=listOf(
            entity("light.stove","on","""{"friendly_name":"Stovelys"}"""),
            entity("weather.home","rainy","""{"friendly_name":"Vêret ute","temperature":4.1,"temperature_unit":"°C"}"""),
            entity("sensor.price","135","""{"friendly_name":"Straumpris","unit_of_measurement":"øre","today":[80,60,70,100,135,90]}"""),
            entity("sensor.ute","4.06","""{"friendly_name":"Utetemperatur","unit_of_measurement":"°C"}"""),
            entity("climate.stove","heat","""{"friendly_name":"Stovetemperatur","temperature":22,"current_temperature":21.5}"""),
            entity("switch.lamp","off","""{"friendly_name":"Leselampe"}"""))
        val cards=entities.mapIndexed {i,e->CardConfig("$i",cardTypesFor(e).first(),e.id)}
        compose.setContent {StovaTheme {Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            SmartHomeContent(Profile("a","A",AvatarColor.LIME),cards,entities.associateBy {it.id},HaStatus.CONNECTED)
        }}}
        compose.saveScreen("ha-dashboard")
    }
    @Test fun doubleFontSensor() {
        compose.setContent {StovaTheme {CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,2f)) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),contentAlignment=Alignment.Center) {
                HaCard(CardConfig("1",CardType.SENSOR,"sensor.ute"),entity("sensor.ute","4.06","""{"friendly_name":"Utetemperatur","unit_of_measurement":"°C"}"""),false,{},{})
            }
        }}}
        compose.saveScreen("ha-double-font-sensor")
    }
    @Test fun emptyDashboard() {
        compose.setContent {StovaTheme {Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            SmartHomeContent(Profile("a","A",AvatarColor.LIME),emptyList(),emptyMap(),HaStatus.CONNECTED)
        }}}
        compose.saveScreen("ha-empty-dashboard")
    }
    @Test fun doubleFontHomeHeader() {
        compose.setContent {StovaTheme {CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,2f)) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                app.stova.navigation.TopBar(Profile("a","A",AvatarColor.LIME),{},{},{},onSmartHome={},
                    leading={HomeStatusChips(List(3) {ChipContent(CardType.MEDIA_PLAYER,"Ein lang medietittel nr $it")})})
            }
        }}}
        compose.saveScreen("ha-double-font-header")
    }
    @Test fun doubleFontPolishedDashboard() {
        val entities=listOf(
            entity("light.reading","on","""{"friendly_name":"Leselys","brightness":222,"color_temp_kelvin":2700}"""),
            entity("climate.home","heat","""{"friendly_name":"Varmepumpe","temperature":23.5,"current_temperature":25}"""),
            entity("sensor.temp","21.5","""{"friendly_name":"Innetemperatur","unit_of_measurement":"°C"}"""))
        compose.setContent {StovaTheme {CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,2f)) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                SmartHomeContent(Profile("a","A",AvatarColor.LIME),entities.mapIndexed {i,e->CardConfig("$i",cardTypesFor(e).first(),e.id)},entities.associateBy {it.id},HaStatus.CONNECTED)
            }
        }}}
        compose.saveScreen("ha-polish-double-font-dashboard")
    }
}
