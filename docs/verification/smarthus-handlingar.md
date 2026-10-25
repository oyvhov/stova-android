# Smarthus – fjernkontrollhandlingar, 25.10.2026

Heile bestillinga i `docs/briefs/2026-10-25-smarthus-handlingar.md` er fullført i
`C:/Launcher/.worktrees/ha-v2`, grein `ha-v2`. Ingen push eller publisering.

## Sluttkontroll

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'
```

**BUILD SUCCESSFUL: 381/381 testar, 0 feil, 0 hoppa over.** 165 HA-testar,
`DesignRulesTest` 4/4. Dette er 33 fleire testar enn før bestillinga.
Lint har 0 feil, 32 åtvaringar og 1 hint. Dei ekstra åtvaringane gjeld mellom anna
eldre strengar som ikkje lenger er brukte når dei gamle justeringsknappane og
standardhintet er erstatta; strengar utanfor Smarthus v2-blokka er bevarte.
APK: `app/build/outputs/apk/debug/app-debug.apk`.

Rapportar: `app/build/test-results/testDebugUnitTest/` og
`app/build/reports/lint-results-debug.html`. Full lokal logg:
`.superpowers/ha-actions-final-check.log`. TDD-ledger:
`.superpowers/sdd/2026-10-25-smarthus-handlingar.md/progress.md`.

## Raud/grøn og dekte kontraktar

- `RemoteActionsTest`: 4 av 5 feila før implementeringa; alle er grøne.
  Faktiske tastetrykk flyttar mellom to lyskort utan tenestekall. Kort OK,
  halde OK og Meny er ulike handlingar. Media sine transporttastar og lesepanel
  for barneprofil er dekte. Dei gamle testane som kravde piljustering, er
  oppdaterte til den nye, bindande modellen.
- `ControlPanelTest`: 11 av dei første 12 feila før panela vart laga; no 13/13.
  Lys, klima, media, vifte, støtta funksjonar, knapp-/glidebrytarfokus,
  lesemodus, sensorhistorikk og eigne stadfestingar for opplåsing, garasje og
  portposisjon er dekte. Avbryt har startfokus i stadfestingar.
- `PanelStateTest`: den manglande tilstandsmodellen var raud; 4/4 grøne for
  nøyaktig 300 ms frå siste endring, optimistisk verdi, tilbakeføring ved feil,
  1/5/10-prosentsteg og serialisering utan å avbryte eit allereie sendt kall.
- `RemoteDashboardTest`: ny kontrakt var først raud; 8/8 grøne for kontekstuell
  hint og firesekundsgrensa, trygg lysoppsummering, Rediger, fleire flytt av same
  kort med OK halde, Tilbake, barn, Heim-brikke, fokus utanfor første synsfelt,
  retur frå panel og første OK etter eit halde OK.
- `HaRepositoryTest`: 10/10 grøne. Nye testar forbyr alarm/utilgjengelege
  einingar og tilbakekalla barneløyve, bind scenekvittering til rett kort i to
  sekund og ventar på faktisk `current_position` før optimistisk posisjon blir
  fjerna. Dei to siste regresjonane for prosentavrunding og posisjonskvittering
  vart prøvde raude (2/2), deretter grøne.

§5.2 i spesifikasjonen inneheld dei nye tast- og korttabellane og reglane for
panel, stadfesting, barn, optimisme, hint, Rediger og fokusminne.

## Ekte HA: berre emulator-5580

Oppdatering med `adb -s emulator-5580 install -r` bevarte innloggingar og profilar.
Alle fysiske endringar gjekk gjennom Stova sine vanlege kort/panel; den eksisterande
`HaVerificationActivity` vart berre brukt til nye, lesande `get_states`-rapportar.
Ingen direkte HA-styring frå verifikasjonsskript eller debug-probe.

| Eining | Før | Prøve gjennom Stova | Ferskt HA-svar etter |
|---|---|---|---|
| `light.ph_lampe` | På, 222/255, 2732 K | Panel 87 → 88 → 87 %, faktisk 224/255 ved 88 %; kort OK av/på | På, 222/255, 2732 K |
| `light.skulpturlampen` | På, 84/255 | Pilnavigasjon; ingen handling | På, 84/255 |
| `light.kjokken` | På, 153/255, 2732 K | Pilnavigasjon; gruppestatus følgde PH av/på | På, 153/255, 2732 K |
| `climate.varmepumpe` | Varme, mål 23,5 °C | Fokusert panelglidebrytar 24,0 → 23,5 °C | Varme, mål 23,5 °C |
| `media_player.kjokken1` | Pause, volum 0 | Panel, transportknappar, volum og støtta kjelder inspiserte | Pause, volum 0 |

- Ekte halde OK med `input keyevent --longpress KEYCODE_DPAD_CENTER` opna lys-
  og mediepanel. Meny opna kortval. Tilbake gav same kort, og neste korte OK
  på PH-kortet verka med ein gong.
- Piler navigerte mellom lyskorta utan å justere lysstyrke. Justering i panelet
  viste den optimistiske verdien og vart stadfesta av motteken HA-cache.
- «Slå av alle» vart opna og avbroten i røynda; sjølve Ja-grena er testa med
  falske einingar. Alarm, opplåsing og garasje/port vart aldri styrte i røynda.
- Rediger vart opna, og OK opna kortval. Eit kort vart flytta høgre og tilbake
  venstre via kortval. Fleire flytt medan OK er halde er testa med tastetrykk
  mot det vanlege skjerminnhaldet og ei oppdatert kortliste i Compose.
- Heim si temperaturbrikke vart vald med D-pad og OK. Smarthus opna med
  temperaturkortet fokusert utan noko tenestekall frå Heim.
- Dei ti korta, alle kortinnstillingane, rekkjefølgja og dei tre Heim-brikkene
  vart samanlikna med profilen før prøva: identiske etter tilbakeføringa.

Lokale kvitteringar utan token ligg i `.superpowers/ha-actions-states-before.json`,
`ha-actions-states-final.json`, `ha-actions-light-raised.json`,
`ha-actions-climate-raised.json` og `ha-actions-profiles-final.json`.
Alle fem relevante state-/attributtsamanlikningar og heile dashboard-samanlikninga
var sanne. Fysisk Shield er ikkje prøvd. Garasje/lås, barn, feil og scene-kvittering
har kontrollerte testar; det er ikkje gjort risikofylte eller unødige fysiske prøver.

Eit anna bygg vart installert på den delte emulatoren under prøva. Avviket vart
oppdaga både visuelt og med APK-hash. Byggjet frå dette worktreet vart installert
på nytt, mediebiletet teke på nytt, og installert SHA-256 samanlikna med den lokale
APK-en: identiske (`.superpowers/ha-actions-apk-check.json`). Ingen filer eller
arbeid i andre worktrear er endra.

## Skjermbilete

Alle ti bileta er ekte `adb exec-out screencap -p` frå emulator-5580, 1920 × 1080,
med brukaren sine HA-data. Alle er opna og visuelt kontrollerte. Emulatoren brukar
engelsk systemspråk; nynorsk og engelsk ligg i ressursblokka «Smarthus v2».

| Fil | Innhald |
|---|---|
| [ha-v2-handling-light.png](../images/ha-v2-handling-light.png) | Lyspanel: lysstyrke, av/på, kvittemperatur og fargeval |
| [ha-v2-handling-media.png](../images/ha-v2-handling-media.png) | Mediepanel med transport, volum og kjelder |
| [ha-v2-handling-climate.png](../images/ha-v2-handling-climate.png) | Klimapanel med temperatur, modus og førehandsinnstillingar |
| [ha-v2-handling-climate-restored.png](../images/ha-v2-handling-climate-restored.png) | Optimistisk retur til 23,5 °C og ventemerke, seinare stadfesta av HA |
| [ha-v2-handling-edit.png](../images/ha-v2-handling-edit.png) | Rediger med rammer, fokus og eige hint |
| [ha-v2-handling-hint.png](../images/ha-v2-handling-hint.png) | Kontekstuell lyshint i botnmargen |
| [ha-v2-handling-card-options.png](../images/ha-v2-handling-card-options.png) | Kortval, skilt frå kontrollpanelet |
| [ha-v2-handling-all-lights-confirm.png](../images/ha-v2-handling-all-lights-confirm.png) | Stadfesting før alle lys |
| [ha-v2-handling-home-chip-focus.png](../images/ha-v2-handling-home-chip-focus.png) | Rett kort fokusert etter OK på Heim-brikke |
| [ha-v2-handling-dashboard.png](../images/ha-v2-handling-dashboard.png) | Oversikt med klikkbar lysoppsummering |

## Avgrensingar

Berre HA-kode, HA-testar, Smarthus v2-strengblokka og den nødvendige
HA-rutinga i `navigation/StovaApp.kt` er endra. `home/`, hero, mediekorta på Heim,
`core/theme`, utsjånadsinnstillingar og ikonpakka er urørte. HA sin eksisterande
`MediaPlayerCard.kt` er òg urørt; kortverten gir han berre visingsrolla og legg
transportkontrollane i panelet. Ingen systemlauncher er slått av.
