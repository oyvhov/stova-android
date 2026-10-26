package app.stova.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.input.key.Key
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.media.*
import app.stova.profiles.BackgroundLibraryRef
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class BackgroundLibraryPickerTest {
 @get:Rule val compose=createComposeRule()
 @Test fun dpadCanChooseOneLibraryAndEmptySelectionDoesNotMeanAll() {
  val family=BackgroundLibraryRef("a","family");val films=BackgroundLibraryRef("a","films")
  var saved:Set<BackgroundLibraryRef>?=null;var saves=0
  compose.setContent {StovaTheme {BackgroundLibraryPicker(null,{BackgroundLibraryCatalog(listOf(
   BackgroundLibrary(family,"Familie","Jellyfin"),BackgroundLibrary(films,"Filmar","Jellyfin")),hasAccounts=true)},
   {saved=it;saves++},{})}}
  compose.onNodeWithText("Fjern alle val").performClick()
  compose.onNodeWithText("Familie").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) {it()}
  compose.onNodeWithText("Familie").performKeyInput {pressKey(Key.DirectionCenter)}
  compose.onNodeWithText("Bruk utvalet").performClick()
  assertEquals(setOf(family),saved)
  compose.onNodeWithText("Fjern alle val").performClick()
  compose.onNodeWithText("Bruk utvalet").performClick()
  assertEquals(emptySet<BackgroundLibraryRef>(),saved);assertEquals(2,saves)
 }
 @Test fun unavailableSelectionsSurvivePartialFailureAndCancelDoesNotSave() {
  val missing=BackgroundLibraryRef("offline","family")
  var saved:Set<BackgroundLibraryRef>?=null;var cancelled=false
  compose.setContent {StovaTheme {BackgroundLibraryPicker(setOf(missing),{BackgroundLibraryCatalog(failedAccounts=listOf("Jellyfin"),hasAccounts=true)},
   {saved=it},{cancelled=true})}}
  compose.onNodeWithText("Prøv igjen").assertExists()
  compose.onNodeWithText("Bruk utvalet").performClick();assertEquals(setOf(missing),saved)
  compose.onNodeWithText("Fjern alle val").performClick()
  compose.onNodeWithText("Avbryt").performClick()
  assertTrue(cancelled);assertEquals(setOf(missing),saved)
 }
}
