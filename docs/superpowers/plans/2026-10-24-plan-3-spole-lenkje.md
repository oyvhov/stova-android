# Plan 3 – `spole://open` i Spole · implementasjonsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spole opnar detaljsida eller spelaren for ein Jellyfin-/Emby-tittel når Stova (eller ein annan app) sender `spole://open?kind=…&user=…&item=…&action=details|play`, og respekterer Spole sine reglar for profilar og barnemodus.

**Architecture:** Ein rein parser (`LauncherLink`) og ein rein avgjerdsfunksjon (`resolveLauncherTarget`) blir testa på JVM. Ein liten eksportert aktivitet utan UI (`LauncherOpenActivity`) brukar dei, startar `JellyfinPlayerActivity` for `play`, og sender `details` vidare til `MainActivity` som ventande handling, på same måte som `pendingSetupLink`. `ReelstackViewModel` får `openRemoteDetails`, som hentar tittelen med den eksisterande `ServiceClients.details(connection, itemId)` når han ikkje alt ligg i tilstanden.

**Tech Stack:** Spole sin eksisterande stack (Kotlin, Compose, Robolectric, JUnit). Ingen nye avhengigheiter.

**Spec:** `C:\Launcher\docs\superpowers\specs\2026-10-24-stova-launcher-design.md` §7

**Repo:** `C:\JellyBin\reelstack-android` (ikkje Stova). Les `AGENTS.md` og `docs/AI_INSTRUCTIONS.md` der før du startar.

## Global Constraints

- Spole sine reglar gjeld: nynorsk i UI og dokumentasjon, ingen hardkoda tekst (lint), bevar eksisterande endringar i arbeidsmappa, og ingen destruktive git-kommandoar.
- Strengar finst i `values` (engelsk), `values-b+nn` og `values-b+nb`.
- Instrumenterte testar køyrer berre på emulator-5562. emulator-5560 og -5564 får berre `install -r` av signert bygg og manuelle sjekkar, aldri testpakka.
- Ingen release eller versjonsauke utan at brukaren ber om det (`docs/RELEASE_WORKFLOW.md`).
- Kontrakt (låst, Stova er avhengig av han):
  - skjema `spole`, vert `open`
  - parameterar `kind` ∈ {`jellyfin`, `emby`}, `user` og `item` som begge passar `[A-Za-z0-9_-]{1,128}`, og `action` ∈ {`details`, `play`}
  - ukjende parameterar blir ignorerte, og alt anna blir avvist utan å opne noko

## Review Focus

1. **Lenkja kjem medan Spole er i barnemodus og peikar på den vaksne kontoen.** Forventa: ingenting av det vaksne innhaldet blir opna, og meldinga «Spole er i barnemodus. Gå ut av barnemodus i Spole først.» blir vist. Test i oppgåve 2.
2. **Ein vilkårleg app sender lenkja med manipulerte id-ar (`../`, `%2F`, 500 teikn).** Forventa: avvist i parseren, ingen nettverkskall. Test i oppgåve 1.
3. **Tittelen ligg ikkje i minnet (kaldstart frå Stova).** Forventa: detaljsida opnar etter at tittelen er henta, med skeleton medan han lastar. Ved feil: Spole sin vanlege feilmelding. Test i oppgåve 3.
4. **Spole er ikkje innlogga i det heile.** Forventa: oppsettet/heimesida opnar med meldinga «Denne kontoen er ikkje logga inn i Spole.». Test i oppgåve 2.
5. **Lenkja kjem medan spelaren går.** Forventa: `play` startar den nye tittelen, og `details` legg detaljsida over heimesida. Ingen dobbel spelar. Manuell sjekk i oppgåve 4.

---

### Task 1: Parser for lenkja

**Files:**
- Create: `app/src/main/java/app/reelstack/launcher/LauncherLink.kt`
- Test: `app/src/test/java/app/reelstack/launcher/LauncherLinkTest.kt`

**Interfaces:**
- Produces: `data class LauncherLink(val kind: ServiceKind, val userId: String, val itemId: String, val action: LauncherAction)`, `enum class LauncherAction { DETAILS, PLAY }`, `object LauncherLinks { fun parse(raw: String?): LauncherLink? }`. Parsar med `java.net.URI` og URL-dekoda query, så testen er rein JVM.

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
class LauncherLinkTest {
    private val u = "0f1e2d3c4b5a69788796a5b4c3d2e1f0"; private val i = "9a8b7c6d5e4f30211203f4e5d6c7b8a9"
    @Test fun parsesPlay() = assertEquals(LauncherLink(ServiceKind.JELLYFIN, u, i, LauncherAction.PLAY),
        LauncherLinks.parse("spole://open?kind=jellyfin&user=$u&item=$i&action=play"))
    @Test fun parsesEmbyDetailsAndIgnoresExtra() = assertEquals(LauncherAction.DETAILS,
        LauncherLinks.parse("spole://open?kind=emby&user=12&item=345&action=details&x=1")!!.action)
    @Test fun rejectsBadInput() = listOf(
        null, "", "spole://setup?x=1", "https://open?kind=jellyfin&user=$u&item=$i&action=play",
        "spole://open?kind=seerr&user=$u&item=$i&action=play", "spole://open?kind=jellyfin&user=$u&item=..%2Fx&action=play",
        "spole://open?kind=jellyfin&user=$u&item=${"a".repeat(129)}&action=play", "spole://open?kind=jellyfin&user=$u&item=$i&action=delete",
        "spole://open?kind=jellyfin&item=$i&action=play",
    ).forEach { assertNull(it, LauncherLinks.parse(it)) }
}
```

- [ ] **Step 2: Køyr og sjå feil**

Run (PowerShell i `C:\JellyBin\reelstack-android`, same miljø som `docs/AI_INSTRUCTIONS.md` §3): `$env:TEMP='C:/JellyBin/.gradle-tmp'; $env:TMP=$env:TEMP; ./gradlew.bat --gradle-user-home C:/JellyBin/.gradle-home testDebugUnitTest --tests "app.reelstack.launcher.LauncherLinkTest"`
Expected: FAIL.

- [ ] **Step 3: Implementer `LauncherLinks.parse`**

- [ ] **Step 4: Køyr testen**

Run: same som Step 2
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/reelstack/launcher/LauncherLink.kt app/src/test/java/app/reelstack/launcher/LauncherLinkTest.kt
git commit -m "feat(launcher): parse spole://open launcher links"
```

---

### Task 2: Avgjerd: opne, byte til barneprofil, eller avvise

**Files:**
- Create: `app/src/main/java/app/reelstack/launcher/LauncherTarget.kt`
- Test: `app/src/test/java/app/reelstack/launcher/LauncherTargetTest.kt`

**Interfaces:**
- Consumes: `LauncherLink`, `ServiceConnection` (`data/model/Models.kt:49`), `UserProfile` (`data/model/UserProfile.kt`, `id` = barnekontoen sin id, `""` = hovudkontoen).
- Produces:
  - `sealed interface LauncherTarget { data class Open(val connection: ServiceConnection) : LauncherTarget; data class SwitchToKid(val profile: UserProfile) : LauncherTarget; data object BlockedInKidMode : LauncherTarget; data object NotSignedIn : LauncherTarget }`
  - `fun resolveLauncherTarget(link: LauncherLink, connections: List<ServiceConnection>, activeProfileId: String, kidProfiles: List<UserProfile>): LauncherTarget`
- Reglar. Hovudtilkoplinga `main` er den første med `kind == link.kind` og ikkje-tomt `token`.
  - Utan `main` → `NotSignedIn`.
  - Hovudmodus (`activeProfileId` er tom):
    - `link.userId == main.userId` → `Open(main)`.
    - `link.userId` er id-en til ein barneprofil → `SwitchToKid(profil)`. Å gå inn i barnemodus krev ikkje PIN i Spole.
    - Elles → `NotSignedIn`.
  - Barnemodus:
    - `link.userId == activeProfileId` → `Open(main)`. Same tilkopling, Spole bruker barnekontoen i barnemodus.
    - `link.userId == main.userId`, eller id-en til ein annan barneprofil → `BlockedInKidMode`.
    - Elles → `NotSignedIn`.
  - **Før implementasjon:** stadfest i `ConnectionRepository`/`ProfileViewModel` at barnemodus spelar via `main` med barnekontoen. Om Spole har ei eiga tilkopling per barneprofil, bruk henne i `Open` og oppdater testen.

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
class LauncherTargetTest {
    private val main = ServiceConnection(ServiceKind.JELLYFIN, "Heim", "http://jf", token = "t", userId = "adult")
    private val kid = UserProfile("kid1", "Maya", isKid = true); private val kid2 = UserProfile("kid2", "Jonas", isKid = true)
    private fun link(user: String, kind: ServiceKind = ServiceKind.JELLYFIN) = LauncherLink(kind, user, "item", LauncherAction.DETAILS)
    @Test fun adultInMainModeOpens() = assertEquals(LauncherTarget.Open(main), resolveLauncherTarget(link("adult"), listOf(main), "", listOf(kid)))
    @Test fun kidLinkInMainModeSwitches() = assertEquals(LauncherTarget.SwitchToKid(kid), resolveLauncherTarget(link("kid1"), listOf(main), "", listOf(kid)))
    @Test fun adultLinkInKidModeBlocked() = assertEquals(LauncherTarget.BlockedInKidMode, resolveLauncherTarget(link("adult"), listOf(main), "kid1", listOf(kid, kid2)))
    @Test fun otherKidInKidModeBlocked() = assertEquals(LauncherTarget.BlockedInKidMode, resolveLauncherTarget(link("kid2"), listOf(main), "kid1", listOf(kid, kid2)))
    @Test fun activeKidOpens() = assertEquals(LauncherTarget.Open(main), resolveLauncherTarget(link("kid1"), listOf(main), "kid1", listOf(kid)))
    @Test fun unknownUserNotSignedIn() = assertEquals(LauncherTarget.NotSignedIn, resolveLauncherTarget(link("x"), listOf(main), "", emptyList()))
    @Test fun noConnectionForKind() = assertEquals(LauncherTarget.NotSignedIn, resolveLauncherTarget(link("adult", ServiceKind.EMBY), listOf(main), "", emptyList()))
}
```

- [ ] **Step 2: Køyr og sjå feil**

Run: same kommando som oppgåve 1 med `--tests "app.reelstack.launcher.LauncherTargetTest"`
Expected: FAIL.

- [ ] **Step 3: Implementer `resolveLauncherTarget`**

- [ ] **Step 4: Køyr testen**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/reelstack/launcher/LauncherTarget.kt app/src/test/java/app/reelstack/launcher/LauncherTargetTest.kt
git commit -m "feat(launcher): resolve launcher links against profiles and kid mode"
```

---

### Task 3: Aktivitet, ventande handling og `openRemoteDetails`

**Files:**
- Create: `app/src/main/java/app/reelstack/launcher/LauncherOpenActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml` (aktivitet med `exported="true"`, `excludeFromRecents="true"`, `noHistory="true"`, tema utan vindauge, og intent-filter `VIEW` + `DEFAULT` + `<data android:scheme="spole" android:host="open"/>`, utan `BROWSABLE`)
- Modify: `app/src/main/java/app/reelstack/MainActivity.kt` (extras `launcher_kind`, `launcher_item`, `launcher_switch_profile`, `launcher_message` handsama i `onCreate`/`onNewIntent` på same måte som `pendingSetupLink`)
- Modify: `app/src/main/java/app/reelstack/ui/ReelstackViewModel.kt` (ny `fun openRemoteDetails(kind: ServiceKind, itemId: String)`)
- Modify: `app/src/main/res/values*/strings.xml`
- Test: `app/src/test/java/app/reelstack/launcher/LauncherOpenActivityTest.kt`, og ein ViewModel-test etter mønsteret i dei eksisterande ViewModel-testane i `app/src/test`

**Interfaces:**
- Consumes: `LauncherLinks.parse`, `resolveLauncherTarget`, `container.connectionRepository` (`list()`, `activeProfileId`), kjende barneprofilar (same kjelde som profilmenyen), `JellyfinPlayerActivity.open(context, itemId, source = kind, kidsMode = …)`, `ServiceClients.details(connection, itemId)`, `ReelstackViewModel.selectProfile(profile)`.
- Produces:
  - `LauncherOpenActivity.onCreate`: parse, avgjer, utfør, `finish()`.
    - `Open` + PLAY → `JellyfinPlayerActivity.open`.
    - `Open` + DETAILS → `MainActivity` med `launcher_kind`/`launcher_item` og `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP`.
    - `SwitchToKid` → `MainActivity` med `launcher_switch_profile = profile.id`, deretter same handling.
    - `BlockedInKidMode` / `NotSignedIn` → `MainActivity` med `launcher_message`.
    - Ugyldig lenkje → berre `finish()`.
  - `ReelstackViewModel.openRemoteDetails(kind, itemId)`: finst tittelen i tilstanden → `openLibraryDetails(itemId)`. Elles blir `AppSheet.TitleDetails` opna med skeleton. `details(connection, itemId)` blir henta på IO, og `contentDetails` fyllt på same måte som `openLibraryDetails` gjer.
  - Strengar: `launcher_not_signed_in` («Denne kontoen er ikkje logga inn i Spole.»), `launcher_kid_mode` («Spole er i barnemodus. Gå ut av barnemodus i Spole først.»), med bokmål og engelsk.

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
@RunWith(AndroidJUnit4::class)
class LauncherOpenActivityTest {
    // Før kvar test: sett opp container med main Jellyfin-tilkopling (userId "adult") via same testhjelparar som eksisterande Robolectric-testar.
    @Test fun playStartsPlayer() {
        val a = Robolectric.buildActivity(LauncherOpenActivity::class.java, Intent(Intent.ACTION_VIEW, Uri.parse("spole://open?kind=jellyfin&user=adult&item=abc&action=play"))).setup().get()
        val started = shadowOf(a).nextStartedActivity
        assertEquals(JellyfinPlayerActivity::class.java.name, started.component!!.className); assertEquals("abc", started.getStringExtra(JellyfinPlayerActivity.ITEM_ID))
        assertTrue(a.isFinishing)
    }
    @Test fun detailsForwardsToMain() { /* action=details → nextStartedActivity er MainActivity med launcher_item == "abc" */ }
    @Test fun invalidLinkStartsNothing() { /* item=../x → nextStartedActivity == null, isFinishing */ }
    @Test fun unknownUserShowsMessage() { /* user=x → MainActivity med launcher_message == getString(R.string.launcher_not_signed_in) */ }
}
// ViewModel: openRemoteDetailsFetchesWhenNotInState: fake details() returnerer tittel "Kystvakta" → uiState.activeSheet == TitleDetails("abc") og contentDetails.title == "Kystvakta".
```
(Om `ITEM_ID` er `private`, gjer han `internal` og nemn det i commit-meldinga.)

- [ ] **Step 2: Køyr og sjå feil**

Run: same kommando med `--tests "app.reelstack.launcher.*"` og ViewModel-testen
Expected: FAIL.

- [ ] **Step 3: Implementer aktiviteten, manifestet, handsaminga i `MainActivity`, `openRemoteDetails` og strengane**

- [ ] **Step 4: Køyr heile testpakka og lint**

Run: `./gradlew.bat --gradle-user-home C:/JellyBin/.gradle-home testDebugUnitTest assembleDebug lintDebug`
Expected: alle testar bestått, 0 lint-feil.

- [ ] **Step 5: Commit**

```bash
git add -A app/src docs
git commit -m "feat(launcher): open details or playback from spole://open links"
```

---

### Task 4: Dokumentasjon og verifisering

**Files:**
- Create: `docs/LAUNCHER_LINK.md` (kontrakt, reglar og døme på nynorsk)
- Modify: `CHANGELOG.md` (under «Ikkje publisert»), `docs/APP_GUIDE.md` (kort avsnitt om Stova)

- [ ] **Step 1: Skriv dokumentasjonen**

- [ ] **Step 2: Instrumenterte testar på 5562 (om Spole sin praksis krev det for nye aktivitetar)**

Run: `ANDROID_SERIAL=emulator-5562` + `connectedDebugAndroidTest` slik `docs/AI_INSTRUCTIONS.md` skildrar.

- [ ] **Step 3: Manuell sjekk på TV-review-eininga (5564, berre `install -r`, ingen testpakke)**

```powershell
adb -s emulator-5564 shell am start -a android.intent.action.VIEW -d "spole://open?kind=jellyfin&user=<din-userId>&item=<ein-filmId>&action=details"
adb -s emulator-5564 shell am start -a android.intent.action.VIEW -d "spole://open?kind=jellyfin&user=<din-userId>&item=<ein-filmId>&action=play"
```
Sjekk: detaljsida opnar, spelaren startar på resume-posisjonen, og ei lenkje medan spelaren går byter tittel. I barnemodus blir den vaksne lenkja avvist med melding. Skriv resultatet i `docs/VERIFICATION_LAUNCHER_LINK.md`.

- [ ] **Step 4: Commit**

```bash
git add docs CHANGELOG.md
git commit -m "docs: launcher link contract and verification"
```
