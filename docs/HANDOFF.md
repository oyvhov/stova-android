# Spotlight og innlogging på emulatoren
2026-10-25: Spotlight og innlogging er gjort i `polish` på `polish-cache` og snøggspola inn i `plan-2-media`. Siste commit er `cc217b8`.
- **Brukarønske:** ingen episodekunst eller episodenummer, ingen tenestelogo, og ein vakker hero som roterer frå biblioteket.
- **Hero:**
  - Heroen er ein spotlight (spec §5.1, endra): filmar og seriar med backdrop, dei med logo først, maks 30, nytt utval kvar 2. time.
  - Seriar der sesong 2 eller seinare kom dei siste 45 dagane, står først med «Ny sesong». Serieomtale, score og logo blir henta med éin `Ids=`-førespurnad.
  - Kvar tittel står i 12 s. Rotasjonen tikkar berre når Heim er synleg (`WhileSubscribed`).
  - Bakgrunnen tonar inn først når biletet er dekoda (900 ms). Logoar får transparent kant klipt bort.
  - Heroen har fast høgd, så radene flyttar seg ikkje.
- **Kort og rader:**
  - Episodekort bruker seriekunst. Fokusringen rammar berre kunstverket.
  - Fokusert rad smett til toppen, så forrige rad aldri stikk fram.
  - Startfokus følgjer medierader som kjem seint, til sjåaren har trykt ein tast.
- **Verifisering:**
  - `testDebugUnitTest lintDebug assembleDebug` → 291/291 bestått, 16 hoppa over (berre skjermbilettestar), lint 0 feil.
  - På emulatoren (`Stova_TV`, `emulator-5580`): Jellyfin «Midttunet» er innlogga via Quick Connect, og spotlight og rader er kontrollerte med skjermbilete i `.superpowers/spot-0*.png`.
- **Spole:**
  - `feat/spole-launcher-link` i `C:/JellyBin/spole-launcher-link` er fletta med Spole beta18: `d24a8dc`, pluss `b850ea0` for CHANGELOG-rekkjefølgja.
  - Ein subagent porterte lenkja til `DetailsCoordinator`/`KidsLibraryCoordinator`. Resultat: 1 281 testar, 1 280 bestått, 1 hoppa over (nettverkstest), lint 0 feil.
  - Debug-APK-en (beta18) er installert på emulatoren og ventar på Quick Connect-godkjenning.
- **Står att:**
  - Brukaren godkjenner Spole-koden.
  - Overleveringa Stova → Spole skal testast på emulatoren.
  - Emby og HA krev at brukaren skriv inn eigne opplysningar.
  - Ingen push.

# Cache og finpuss
2026-10-25: Hero-lesbarheit er fullført med raud/grøn TDD: heile tekstkolonna er maks 560 dp ved 960 dp skjerm; flat Ink-scrim har 100 % ved venstre kant, 85 % ved 35 %, 80 % ved 60 %, 70 % ved 62,5 % og 0 % ved 65 %. Ekstra fargestopp held slutten av omtalen lesbar; tre stopp åleine gav for låg kontrast.
Verifisering: `testDebugUnitTest --tests app.stova.AppScreensTest '-Pstova.screens=true'` → 12/12; deretter `testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'` → 265/265, ingen hoppa over, DesignRulesTest 3/3, lint 0 feil (20 åtvaringar, 1 hint), debug-APK OK. Alle kall via `pwsh -NoProfile -File scripts/gradle.ps1`.
Oppdaterte/lagra og visuelt kontrollerte bilete: `docs/images/cache-home-bright-backdrop.png` og `docs/images/cache-home-dark-backdrop.png` (1920 × 1080); kjelder `app/build/screens/13-home-bright-backdrop.png` og `09-home-media-hero.png`. Ingen påbyrja steg står att; ekte Shield er ikkje verifisert. Berre `polish`/`polish-cache` er endra; ingen push.

2026-10-25: Alle fire bestilte punkt er fullførte med raud/grøn TDD i `polish` på `polish-cache`; ingen påbyrja steg står att.
HA-cache: `a6856d9` (berre aktivt kortutval og nødvendige attributt/tidsstempel, aldri token, maks éi skriving per 5 s, tomme ferske svar ryddar cachen). Førehandslasting: `cd53232` (3 første titlar per rad, unike URL-ar, maks 12, Coil 1920 × 1080).
Toppscrim: `1928c0c` (flat Ink-gradient øvst i 30 %). Smarthus-status er sentrert i denne committen, verifisert ved skriftstorleik 1 og 2.
Sluttkontroll: `testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'` → 264/264 bestått, 0 hoppa over, DesignRulesTest 3/3; lint 0 feil (20 åtvaringar, 1 hint), debug-APK OK. Separat `testDebugUnitTest --tests app.stova.AppScreensTest '-Pstova.screens=true'` → 12/12. Alle Gradle-kall via `pwsh -NoProfile -File scripts/gradle.ps1`.
Nye skjermbilete: `docs/images/cache-home-bright-backdrop.png` og `docs/images/cache-smarthome-aligned.png`, begge 1920 × 1080 og visuelt kontrollerte; kjelder i `app/build/screens/13-home-bright-backdrop.png` og `14-smarthome-heading-aligned.png`. Ekte HA/Shield er ikkje prøvd; ingen spørsmål og ingen push.

# Status
2026-10-25: Alle 17 utsette Plan 1-småfunn er behandla i `C:/Launcher/.worktrees/minors`, greina `review-minors`.
15 funn er retta med raud/grøn TDD og éin commit per funn; 2 er hoppa over med grunn under «Småfunn».
Sluttkontroll: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'` → 252/252 bestått, ingen hoppa over; DesignRulesTest 3/3; lint 0 feil (20 åtvaringar, 1 hint); debug-APK OK.
Ingen påbyrja steg står att. Ekte fjernkontroll/Shield er ikkje verifisert; ingen spørsmål, ingen push og ingen arbeid i andre worktrear.
Skjermbiletet `app/build/screens/minor-pin-font-2.png` er generert og visuelt kontrollert; «Slett» held éi linje.
2026-10-25: Plan 5 oppgåve 2 og 3, og dokumentasjonsdelen av oppgåve 5 steg 1, er fullførte i `C:/Launcher/.worktrees/plan-5` på `plan-5-perf`.
Baseline Profile/benchmark: `8409d65`; måleskript: `7da6b7c`; oppsettdokument, README, tre emulatorbilete og målerapport er med i denne dokumentasjonscommitten.
Sluttkontroll `testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'`: 233/233 bestått, 0 hoppa over, DesignRulesTest 3/3, lint 0 feil (22 åtvaringar, 1 hint), debug-APK OK. Sjølvtest 24/24; profil og benchmark kvar 1/1 relevant test bestått.
Emulator (debug-signert benchmarkRelease): kald 313,50 ms, varm 98 ms, hakking 3,66 %, PSS 67,30 MiB; startupbenchmark TTID-median 232,454 ms. `emulator-5582` er slått av med `emu kill`.
Ingen påbyrja arbeid står att innanfor bestillinga. Brukaren må gjere signering/passord, Shield-målingar og oppsettkontroll, oppdateringstest og produksjonsrelease/hash; oppgåve 1 og 4 og resten av 5 er urørte. Versjonen er framleis 0.1.0.
Avvik står under Plan 5 nedanfor og i `docs/verification/plan-5-emulator.md`. Ingen spørsmål, ingen tag, ingen push eller publisering.

## Før Plan 5

2026-10-25: Samanslåingsoppgåve 1–3 er fullførte på `plan-2-media` i `C:/Launcher/.worktrees/plan-2`; Plan 2 og Plan 4 er bevarte.
Merge: `a568e8b`. Aldersgrense med raud/grøn TDD: `3865293`; HeroPanelTest er òg oppdatert til éi alderspille.
Sluttkontroll: `testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'` → 232/232 bestått, ingen hoppa over, DesignRulesTest 3/3, lint 0 feil (22 åtvaringar, 1 hint), debug-APK OK.
Eigen AppScreensTest-køyring: 10/10 bestått; nytt bilete med hero, media og HA-status ligg i `docs/images/home-media-ha.png` og er visuelt kontrollert.
Ingen påbyrja steg står att. Ekte medieserverar, Home Assistant, Shield og emulator er ikkje verifiserte; oppgåve 10 i Plan 2 og 4 er framleis urørt. Ingen spørsmål og ingen push.

## Samanslåing

2026-10-25: HA-greina `8216d9b` er fletta inn i `a568e8b`; etterfølgjande fletting av Plan 1 gav «Already up to date». Dei krevjande konfliktane i `AppContainer`, `HomeScreen` og `StovaApp` er løyste med både hero/backdrop/mediarader og HA-tilkopling/statusbrikker/Smarthus-navigasjon bevarte. Hushaldsmodellen har både `deviceId` og `haBaseUrl`, innstillingane har Kontoar og Smarthus, og avhengigheiter og INTERNET-løyve er utan duplikat. `3865293` fjernar dobbel aldersgrense med raud/grøn TDD; UI-testen krev òg éi alderspille. [Heim med media og HA-status](images/home-media-ha.png) bruker falske data og lokal kunst og er visuelt kontrollert. Sluttkontroll: 232/232 testar, AppScreensTest 10/10, DesignRulesTest 3/3, lint 0 feil og debug-APK OK. Ingen push.

## Før samanslåinga (Plan 2)
2026-10-25: Plan 2, oppgåve 1–9 er ferdige og committa etter raud/grøn TDD; alle stega er haka av.
Ingen påbyrja oppgåver står att. Oppgåve 10 er medvite ikkje køyrd etter brukarønske.
Sluttkontroll: 147/147 køyrde einingstestar (10 valfrie skjermtestar hoppa over), DesignRulesTest 3/3, lint 0 feil, assembleDebug OK.
Eigen AppScreensTest-køyring: 9/9 bestått, ti PNG-filer i app/build/screens; dei tre nye mediebileta er visuelt kontrollerte.
Ekte Jellyfin-/Emby-serverar, Shield og emulator er ikkje verifiserte. Ingen spørsmål; ingen push. Arbeidet er gjort i plan-2-worktreen.

## Plan 2
- Berre denne worktreen er lesen og endra, slik brukaren bad om. Spole-kjelder utanfor mappa er derfor ikkje kopierte; tenestelogoane er lokale, enkle vektorvariantar med originalfargane. Eksakt samsvar med Spole er ikkje verifisert.
- Tomatmerket og TMDB-merket er lokale, dempa variantar; Spole sine originalfiler er ikkje lesne utanfor worktreen.
- Heroen har 45 % som minimum og kan vekse ved stor skrift for å unngå klipping; omtalen fell bort ved skriftstorleik ≥ 1,5.
- Watch Next avviser òg intent-selectorar og fjernar eksterne flagg før opning i eigarpakken.
- Ekstra regresjonar etter oppgåve 9: tokenbyte i biletlaster, Emby-header ved vidarekopling, korrekt tenestetype ved LAN-svar utan produktnamn, avvist løyve, blokkert TV-provider og D-pad/OK/Meny på mediekort.
- UI-testane bruker den smale AddAccountContent-grensa med ekte REST mot MockWebServer og eit lokalt kontolager; produksjonsflyten brukar same kode.
- Quick Connect-nettverkstesten bruker reell coroutine-klokke: virtuell timeout kan gå ut før MockWebServer-tråden får svart.


### Commit-oversikt

| Oppgåve | Commit |
|---|---|
| 1 Modell og parser | `f829d7d` |
| 2 REST og fallback | `487b0ca` |
| 3 Kontoar og pålogging | `39b6ffa` |
| 4 Konto-UI | `71d947a` |
| 5 Rader og øyeblikksbilete | `c1c31f1` |
| 6 Kunst, kort og score | `26aa6ea` |
| 7 Hero og bakgrunn | `4a5627a` |
| 8 Spole-opning | `2ac689e` |
| Brukarønskte skjermbilete | `acc5ae6` |
| 9 Watch Next | `de4eb16` |
| Ekstra regresjonar og rettingar | `6d4e8dc` |

### Verifikasjon

- `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug`: PASS. 157 registrerte testar, 147 bestått, 10 valfrie skjermtestar hoppa over; ingen feil. Lint: 0 feil, 22 åtvaringar, 1 hint.
- `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests app.stova.AppScreensTest '-Pstova.screens=true'`: 9/9 PASS. PowerShell treng hermeteikn rundt eigenskapen med punktum.
- Nye bilete: `app/build/screens/09-home-media-hero.png`, `10-home-media-posters.png`, `11-home-media-font-2.png`, alle 1920 × 1080. Falsk profil «Demo», falsk konto på `demo.invalid`, lokalt teikna kunst; ingen ekte tenestekontoar eller namn.
- APK: `app/build/outputs/apk/debug/app-debug.apk`. Ingen installasjon eller manuell sjekk mot ekte serverar (oppgåve 10).
- Raud fase for kvar oppgåve var manglande produksjonsgrensesnitt i dei nye testane. Quick Connect-testen avdekte òg virtuell timeout mot ekte MockWebServer-tråd; redirect-regresjonen feila med uønskt Emby-header før rettinga.
## Plan 4

- 2026-10-25: Fletting av `plan-1-fundament` (`72624eb`) inn i `plan-4-home-assistant` er fullført i merge-commit `bdff9f2`; begge foreldra er bevarte.
- Verifisering: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug` → grønt (164 bestått, 10 valfrie skjermbilettestar hoppa over); same kommando med `'-Pstova.screens=true'` → 174/174 bestått, 0 hoppa over, inkludert 3/3 `DesignRulesTest`. Lint: 0 feil, 21 åtvaringar og 1 hint; debug-APK bygd.
- Ingen påbyrja steg står att. Plan 4 oppgåve 10 er framleis urørt; ekte HA, emulator og Shield er ikkje verifiserte etter flettinga. Ingen spørsmål til brukaren.
- Dei nygenererte `app/build/screens/ha-dashboard.png`, `ha-double-font-header.png` og `ha-double-font-sensor.png` er visuelt kontrollerte. Ingen push eller arbeid i andre worktrear.

## Fletting

Konfliktane låg i `AppContainer`, `MainActivity`, `HomeScreen` og `StovaApp`. `AppContainer` tek no både den tidleg installerte kræsjvakta og HA sitt `SecretStore`; kallestadene bruker namngjevne argument der desse blir sende inn. `MainActivity` beheld HA-start/stopp saman med `onNewIntent`, `homeRequests` og `LaunchGuard`. `HomeScreen` beheld statusbrikkene medan null-rader viser lasting utan tomtilstand. `StovaApp` kombinerer Smarthus-knappen og sida med Home-navigasjon og valfri vaksen-PIN ved profilbyte. Dei andre Plan 1-rettingane vart automatisk fletta inn. Ein ny integrasjonstest stadfestar at Home lukkar Smarthus, som framleis kan opnast att.

## Før fletting

- 2026-10-25: Oppgåve 1–9, alle steg til og med commit, er fullførte på `plan-4-home-assistant` i `C:/Launcher/.worktrees/plan-4`. Ingen påbyrja steg står att.
- Oppgåve 10 er medvite urørt etter brukarønske. Ekte Home Assistant, emulator og Shield er ikkje verifiserte; ingen spørsmål til brukaren.
- Sluttkontroll: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'` → 159/159 bestått, 0 hoppa over, inkludert alle 3 `DesignRulesTest`-testane; lint 0 feil (21 åtvaringar, 1 hint).
- Debug-APK er bygd i `app/build/outputs/apk/debug/app-debug.apk`. Smarthus er ein vilkårsstyrt ikonknapp i topplinja og ei fullskjermside som Tilbake lukkar. Ingen push eller endringar i andre worktrear.
- Visuelt kontrollert: `app/build/screens/ha-dashboard.png`, `ha-double-font-sensor.png` og `ha-double-font-header.png`. Topplinja held navigasjonen synleg ved skriftstorleik 2. Ledger med TDD/commit-evidens: `.superpowers/sdd/2026-10-24-plan-4-home-assistant.md/progress.md` (ignorert).

## Avvik

- Oppgåve 2 og 7: Plan 2 er ikkje implementert på denne greina. OkHttp/MockWebServer 4.12.0 og Coil 3.4.0 er difor lagde til her. Samordne desse avhengigheitene ved seinare fletting.
- Oppgåve 2: Tilbakekalling bruker `POST /auth/token` med `action=revoke`, ikkje `/auth/revoke`, etter den offisielle dokumentasjonen: https://contributors.home-assistant.io/core/internals/auth-api/.
- Oppgåve 3: `kidSmartHomeControl` er lagt til før oppgåve 6 fordi innstillingsbrytaren treng feltet. INTERNET og cleartext-støtte er òg lagde til her; LAN-regelen blir handheva før HA-adressa blir brukt. OAuth-utveksling kan ta ei validert adresse før hushaldsinnstillinga blir lagra.
- Oppgåve 5: `HaConnection` er eit lite transportgrensesnitt som `HaSocket` implementerer. Kort/UI og falske tilkoplingar bruker same kontrakt. All skriving til socket-tilstandane går gjennom éin kanal på IO-scope.
- Oppgåve 7: `StovaIcons.kt` fanst og er utvida. Korta bruker minstehøgd 160 dp slik at større skrift ikkje blir klipt. `Graph(today)` skil dagens straumpriskurve frå 24 t-sensorhistorikk. Kamerabilete bruker same-origin Bearer-header og ingen disk-/minnebuffer; oppdateringar stoppar med livssyklus/synlegheit.
- Oppgåve 8: UI-testane bruker `SmartHomeContent` og ei `HaConnection`-basert legg-til-flyt. Ein eigen integrasjonstest dekkjer ekte AppContainer, vilkåret for ikonknappen og Tilbake. Rutenettet er avgrensa til 752 dp for tre kort på 240 dp med 16 dp mellomrom (breie kort 496 dp).
- Oppgåve 9: `ChipContent` har eit valfritt strengressursfelt slik at På/Av kjem frå omsette Android-ressursar. HomeScreen sender brikkene til topplinjesloten. Brikkene har avgrensa breidd og ellipse for å halde klokke og ikon synlege; ein test med native font-rendering og skriftstorleik 2 fangar denne regresjonen.

## Småfunn

- OK mistar fokus etter PIN-forsøk: hoppa over som alt handtert av noverande Compose-versjon. `rejectedPinReturnsFocusToDigits` viser at D-pad-fokus går til «1» når OK blir deaktivert, både medan eit ekte suspendert svar ventar og etter avvist PIN. Ingen produksjonsendring.
- Langt OK-trykk kan velje første appmenyval: hoppa over etter brukarønske; krev ekte fjernkontroll/Shield.

Alle nummer nedanfor følgjer rekkjefølgja til dei 17 opphavlege `Final: minor (deferred)`-linjene. Kvar retting har ein test som feila før rettinga og bestod etter; testnamna står i `docs/verification/plan-1-ledger.md`.

| Funn | Commit | Retting |
|---|---|---|
| 1 | `264a6f3` | Vaksen-PIN medan profilfila lastar |
| 2 | `f511d9c` | Avgrensa PIN-ventetid ved klokkehopp bakover |
| 5 | `9ab6687` | Fast layout for fokusetikettar i topplinja |
| 6 | `11f5c46` | Klokka blir oppdatert ved resume |
| 7 | `82b57c0` | Profilendringar utan ny pakkeskanning |
| 8 | `7940989` | Fokus følgjer flytta heimerad |
| 9 | `9a67bb3` | Nye forsøk på manglande appkunst og oppfrisking ved pakkeoppdatering |
| 10 | `ad3ab5b` | Respekter avslått tilgjengeteneste |
| 11 | `f7a9c60` | JSON-lesing toler I/O-feil, òg ved korrupt kopi |
| 12 | `12f0f7d` | Same vaksen slepp dobbel PIN ved eitt profilbyte |
| 13 | `f520fe4` | PIN-tastatur med «Slett» på éi linje ved skriftstorleik 2,0 |
| 14 | `371417d` | Tydeleg feil ved tilgang til launcher-container i feil prosess |
| 15 | `593810f` | Home-overstyring av ved uleseleg kræsjvaktfil |
| 16 | `9a7fef9` | Varm JSON-lesar når Home-tenesta blir kopla til |
| 17 | `a438d7c` | «siffer skrivne» i fleirtal, korrekt eintal og engelsk |

Funn 3 er dokumentert med regresjonstest i `c0e3d9f`; testen var grøn før produksjonsendringar, også medan PIN-svaret var suspendert. Funn 4 er medvite urørt og krev fysisk fjernkontroll.

Funn 10: Android fortel ikkje om ei avslått teneste vart slått av av systemet eller av brukaren. Automatisk aktivering ved oppstart er difor fjerna, saman med den unyttige adb-grant-oppskrifta i UI-et; brukaren slår tenesta på i Android-innstillingane. Spesifikasjonen er oppdatert med denne avgjerda.

Sluttverifikasjon: 252/252 bestått (inkludert skjermtestane), DesignRulesTest 3/3, lint 0 feil / 20 åtvaringar / 1 hint, assembleDebug OK. Ingen emulator eller Shield er brukt. Arbeidstreet er reint etter overleveringscommit.
## Plan 5

- 2026-10-25: Arbeidet er avgrensa til `C:/Launcher/.worktrees/plan-5`, grein `plan-5-perf`. Brukarønsket går framfor standardregelen om port 5580: berre `emulator-5582` er brukt. Oppgåve 1 og 4 blir hoppa over; ingen produksjonsrelease, signeringsnøkkel eller tag blir laga.
- Oppgåve 2: Det bestilte API 34-TV-biletet `x86_64` finst ikkje i SDK-registeret (`sdkmanager` feila, og `--list` stadfesta det). AVD `Stova_TV34` er derfor laga med `system-images;android-34;android-tv;x86`, i `C:/Launcher/.avd`, på port 5582 med `-no-snapshot-save`. Også dette er eit produksjonsbilete utan root; API 34 kan generere profilen utan root.
- Oppgåve 2: Benchmark/Baseline Profile-plugin 1.4.1 og ProfileInstaller 1.4.1 er brukte med compileSdk 36. Pluginen krev AGP-kompatibilitetsflagget `android.newDsl=false`; innebygd Kotlin er bevart. Ein eventuell AGP 10-oppgradering må oppgradere profilpluginen og fjerne flagget.
- Oppgåve 2: Eit emulatoravgrensa demoinnsteg ligg berre i debug, nonMinifiedRelease og benchmarkRelease. Produksjonsrelease får det ikkje. Éin fiktiv profil og Jellyfin-konto, lokale bilete og bufra rader gjer køyringa repeterbar utan verkelege tenester. Generatoren rullar med D-pad og opnar topplinjehandlingane med ein selector som blir slått opp ved aktivering; UiObject2 gav stale-node-feil under fokus-animasjon.
- Oppgåve 2: `HomeReadyTest` gjekk frå AssertionError utan markør til 1/1 bestått. Profilgenerering og startupbenchmark er begge grøne (éin relevant test per køyring; den andre regelen blir medvite hoppa over). Profilen har 21 708 linjer, 334 med `Lapp/stova/home/`, og ingen reglar for demoinnsteget. Startupbenchmark: 10 iterasjonar, median `timeToInitialDisplayMs` 232,454 ms (min 206,374; maks 329,551). Verktøyvariantane er debug-signerte av profilpluginen; `assembleRelease` er ikkje køyrd.
- Oppgåve 3: `Measure-Stova.ps1 -SelfTest` er utvikla raud/grøn og bestod 24/24 kontrollar. Skriptet krev eksplisitt serie, avviser alle andre emulatorportar, ventar på `home-ready`, gir feil ved manglande data og kode 1 ved brote budsjett. Første integrasjonskøyring avdekte 32-bits overflyt i Android-shellen sin nanosekundaritmetikk; PowerShell si monotone `Stopwatch` styrer no dei 30 sekunda. D-pad-sekvensen har eigne grensetestar.
- Oppgåve 3: Målt på R8-minifisert, ikkje-debuggbar `benchmarkRelease` med Android Debug-sertifikat (ingen produksjonsrelease eller Stova-nøkkel): kaldstartmedian 313,50 ms (10 prøver), varm Home 98 ms, hakking 3,66 %, PSS 67,30 MiB. 147 tastetrykk over 30,14 s. Alle fire radene OK, avslutningskode 0. Dette er funksjonskontroll på emulator; Shield-budsjetta er ikkje godkjende. PSS bruker KiB/1024. Varmmålet er `am start -W` si TotalTime.

- Oppgåve 5 steg 1: `docs/SHIELD_SETUP.md` og `README.md` er skrivne på nynorsk. Tre bilete frå emulator-5582 med demodata ligg i `docs/images/home.png`, `apps.png` og `smarthome.png` og er visuelt kontrollerte; ingen personlege namn, adresser eller token er synlege. Målerapport og repeterbare kommandoar ligg i `docs/verification/plan-5-emulator.md`. Dei seks lokale dokumentasjonslenkjene verkar.
- Oppgåve 5: Berre dokumentasjon og bilete er fullførte etter bestillinga. Versjonsauke, signering, oppdatering over eldre signert installasjon, fysisk Shield-oppsett, produksjonsrelease/storleik/hash og tag er ikkje gjorde. Ingen `assembleRelease`, push eller publisering. Emulatoren vart slått av med `adb -s emulator-5582 emu kill` (OK).
- Sluttkontroll: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'` → BUILD SUCCESSFUL, 233/233 bestått, 0 hoppa over, DesignRulesTest 3/3; lint 0 feil, 22 åtvaringar og 1 hint. Debug-APK bygd. Ingen nye kodeendringar etter denne kontrollen.
