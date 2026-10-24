# Plan 4

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
