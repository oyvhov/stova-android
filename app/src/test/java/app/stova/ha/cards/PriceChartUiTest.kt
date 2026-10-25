package app.stova.ha.cards

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.ha.*
import kotlinx.serialization.json.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class PriceChartUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun graphOffersBothResolutionsAndTimedExtremes() {
        val entity=HaEntity("sensor.price","74",buildJsonObject {
            put("unit_of_measurement","øre")
            put("today",JsonArray(List(96) {JsonPrimitive(it.toDouble())}))
        },0,0)
        compose.setContent {StovaTheme {CardDialogHost(CardDialog.Graph(entity.id,true),mapOf(entity.id to entity),FakeHaConnection(),null,false,{_,_->},{})}}
        compose.onNodeWithText("15 minutt").assertIsDisplayed().performClick()
        compose.onNodeWithText("Timesnitt").assertIsDisplayed()
        compose.onNodeWithText("Billigast 00:00–00:15").assertExists()
        compose.onNodeWithText("Timesnitt").performClick()
        compose.onNodeWithText("Billigast 00–01").assertExists()
        compose.onNodeWithText("Dyrast 23–24").assertExists()
    }
}
