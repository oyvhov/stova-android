# Status
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
