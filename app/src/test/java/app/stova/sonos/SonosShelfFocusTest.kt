package app.stova.sonos

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.ha.HaEntity
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class SonosShelfFocusTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val actions=mutableListOf<SonosAction>()
    private val media=List(14) { SonosMedia("fav:$it","favorite_item_id","Favoritt $it","/art/$it",playable=true) }
    private val room=SonosRoom(HaEntity("media_player.stova","playing",buildJsonObject {
        put("supported_features",1048575);put("volume_level",.3)
        put("media_title","Ei svært lang låt som skal få plass saman med alle kontrollane")
        put("media_artist","Artisten");put("media_duration",240)
    },0,0),"Stova")
    private fun show(roomsOpen:Boolean=true,restore:SonosMedia?=null,onRestored:()->Unit={}) {
        compose.setContent {StovaTheme(reduceMotion=true) {
            val input=LocalInputModeManager.current
            SideEffect {input.requestInputMode(InputMode.Keyboard)}
            SonosContent(SonosState(listOf(room),loading=false),room.id,{}, {actions+=it},{},{},{},
                roomsOpen=roomsOpen,onBrowseMusicAssistant={},shelf=media,shelfReturn=restore,onShelfRestored=onRestored,
                shelfThumbnail={path,modifier,_,loaded -> Box(modifier.testTag(path));SideEffect {loaded()}})
        }}
    }
    private fun down()=compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
    @Test fun downFromVolumeAlwaysEntersTheFirstFavourite() {
        show()
        compose.onNodeWithTag("slider-sonos-${room.id}").requestFocus()
        down()
        compose.onNodeWithText("Favoritt 0").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionUp)}
        compose.onNodeWithTag("slider-sonos-${room.id}").assertIsFocused()
        assertTrue(actions.isEmpty())
    }
    @Test fun returningFromAboveResetsAPreviouslyScrolledShelfToItsFirstFavourite() {
        show()
        compose.onNodeWithText("Favoritt 0").requestFocus()
        repeat(10) {compose.onRoot().performKeyInput {pressKey(Key.DirectionRight)}}
        compose.onNodeWithText("Favoritt 10").assertIsFocused()
        compose.onNodeWithTag("slider-sonos-${room.id}").requestFocus()
        down()
        compose.onNodeWithText("Favoritt 0").assertIsFocused().assertIsDisplayed()
        assertTrue(actions.isEmpty())
    }
    @Test fun explicitReturnStillRestoresTheOpeningFavourite() {
        var restored=0
        show(restore=media[10],onRestored={restored++})
        compose.onNodeWithText("Favoritt 10").assertIsFocused().assertIsDisplayed()
        assertEquals(1,restored)
    }
    private fun buttonsFit(roomsOpen:Boolean) {
        show(roomsOpen)
        val bottom=compose.onRoot().getUnclippedBoundsInRoot().bottom-24.dp
        for(text in listOf("Musikk og favorittar","Grupper rom","Fleire val","Music Assistant")) {
            val bounds=compose.onNodeWithText(text).getUnclippedBoundsInRoot()
            assertTrue("$text is below the viewport: $bounds / $bottom",bounds.bottom<=bottom)
        }
        compose.onNodeWithText("Favorittar").assertIsDisplayed()
        if (roomsOpen) assertTrue("The shelf stays beside the room list in the player",
            compose.onNodeWithTag("sonos-shelf").getUnclippedBoundsInRoot().left>=
                compose.onNodeWithTag("sonos-room-${room.id}").getUnclippedBoundsInRoot().right)
        assertTrue("The volume control must remain above the shelf",
            compose.onNodeWithTag("slider-sonos-${room.id}").getUnclippedBoundsInRoot().bottom<=
                compose.onNodeWithTag("sonos-shelf").getUnclippedBoundsInRoot().top)
    }
    @Test fun allFooterButtonsFitWithRoomsVisible()=buttonsFit(true)
    @Test fun allFooterButtonsFitWithRoomsHidden()=buttonsFit(false)
}
