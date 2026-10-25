package app.stova.home

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.*
import app.stova.core.theme.*
import app.stova.media.MediaRowKind
import app.stova.media.model.*
import app.stova.navigation.TopBar
import app.stova.profiles.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class HomeFocusNavigationTest {
    @get:Rule val compose=createComposeRule()
    private fun render(apps:Boolean, middleRows:Int=0, emptyApps:Boolean=false, reduceMotion:Boolean=true, startOnApps:Boolean=false) {
        val account=MediaAccount("a","p",ServerKind.JELLYFIN,"s","Demo","https://demo.invalid","u","U")
        val media=HomeRowUi.Media(HomeRowKey.latestMovies("a"),account,MediaRowKind.LATEST_MOVIES,
            listOf(MediaItem("f","a",ItemType.MOVIE,"Film")),false)
        val installed=if(emptyApps) emptyList() else listOf(InstalledApp("demo/.Main","demo",".Main","Min app",true))
        val rows=(if(apps) listOf(HomeRowUi.Apps(HomeRowKey.APPS,installed)) else emptyList())+
            List(middleRows) {media.copy(key=HomeRowKey("middle-$it"))}+media
        compose.setContent { StovaTheme(reduceMotion=reduceMotion) {
            val context=LocalContext.current
            HomeContent(rows,null,if(startOnApps) HomeRowKey.APPS else media.key,AppArtCache(context),PackageManagerAppCatalog(context,emptySet()),{},{},{},{},
                topBar={TopBar(Profile("p","A",AvatarColor.LIME),{},{},{},onSmartHome={},onSystemSettings={})})
        } }
    }

    @Test fun downFromEveryTopIconReachesAppsAndUpReturnsToThatIcon() {
        render(apps=true)
        for(label in listOf("Smarthus","Appar","Innstillingar","Android-innstillingar","Byt profil")) {
            compose.onNodeWithText(label).performSemanticsAction(SemanticsActions.RequestFocus)
            compose.onNodeWithText(label).performKeyInput { pressKey(Key.DirectionDown) }
            compose.onNode(hasContentDescription("Min app") and hasClickAction()).assertIsFocused()
            compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
            compose.onNodeWithText(label).assertIsFocused()
        }
    }

    @Test fun withoutAppsDownReachesTheFirstMediaRowAndBottomDoesNotJumpSideways() {
        render(apps=false)
        compose.onNodeWithText("Byt profil").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithContentDescription("Film").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown);pressKey(Key.DirectionRight) }
        compose.onNodeWithContentDescription("Film").assertIsFocused()
    }

    @Test fun aNewHeaderDirectionCancelsPendingRowEntry() {
        render(apps=true,middleRows=20)
        compose.mainClock.autoAdvance=false
        compose.onNodeWithText("Smarthus").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onRoot().performKeyInput { keyDown(Key.DirectionDown);keyUp(Key.DirectionDown);keyDown(Key.DirectionRight);keyUp(Key.DirectionRight) }
        compose.mainClock.advanceTimeBy(250)
        compose.onNodeWithText("Appar").assertIsFocused()
        compose.mainClock.autoAdvance=true
    }

    @Test fun downSkipsAnEmptyAppsRow() {
        render(apps=true,emptyApps=true)
        compose.onNodeWithText("Appar").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithContentDescription("Film").assertIsFocused()
    }

    @Test fun movingFromAppsToMediaHasIntermediateFramesInsteadOfJumping() {
        render(apps=true, reduceMotion=false, startOnApps=true)
        val app = compose.onNode(hasContentDescription("Min app") and hasClickAction())
        app.assertIsFocused()
        val start = app.fetchSemanticsNode().positionInRoot.y
        compose.mainClock.autoAdvance=false
        app.performKeyInput { pressKey(Key.DirectionDown) }
        compose.mainClock.advanceTimeBy(64)
        val early = app.fetchSemanticsNode().positionInRoot.y
        org.junit.Assert.assertTrue("The app row must still be travelling after 64 ms", early > start - 100f)
        compose.mainClock.advanceTimeBy(160)
        val middle = app.fetchSemanticsNode().positionInRoot.y
        org.junit.Assert.assertTrue("The scroll must make visible progress", middle < early)
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithContentDescription("Film").assertIsFocused()
        compose.mainClock.autoAdvance=true
    }

    @Test fun upDuringTheScrollReversesFromTheCurrentPositionAndReturnsToApps() {
        render(apps=true, reduceMotion=false, startOnApps=true)
        val app = compose.onNode(hasContentDescription("Min app") and hasClickAction())
        val start = app.fetchSemanticsNode().positionInRoot.y
        compose.mainClock.autoAdvance=false
        app.performKeyInput { pressKey(Key.DirectionDown) }
        compose.mainClock.advanceTimeBy(128)
        compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        compose.mainClock.advanceTimeBy(1_000)
        app.assertIsFocused()
        org.junit.Assert.assertEquals(start, app.fetchSemanticsNode().positionInRoot.y, 2f)
        compose.mainClock.autoAdvance=true
    }
}
