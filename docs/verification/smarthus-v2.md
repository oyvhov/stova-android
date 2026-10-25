# Smarthus v2 – verifisering 25.10.2026

Fullført i `C:\Launcher\.worktrees\ha-v2`, grein `ha-v2`. Ingen push eller publisering.
Bestilling og tematillegg: `docs/briefs/2026-10-25-smarthus-v2.md`.

## Sluttkontroll

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'
```

**BUILD SUCCESSFUL. 329/329 testar, 0 feil, 0 hoppa over.**
113 testar ligg under `app.stova.ha`; `DesignRulesTest` er 4/4.
Lint: **0 feil, 25 åtvaringar, 1 hint**. Debug-APK:
`app/build/outputs/apk/debug/app-debug.apk`.
Testresultat: `app/build/test-results/testDebugUnitTest/`; lint:
`app/build/reports/lint-results-debug.html`. Lokal logg: `.superpowers/ha-v2-final-check.log`.

TDD-evidens og commitintervall ligg i den ignorerte ledgeren
`.superpowers/sdd/2026-10-25-smarthus-v2.md/progress.md`.

## Ekte Home Assistant gjennom Stova

Berre `adb -s emulator-5580` er brukt. APK-en er oppdatert med `install -r`;
ingen avinstallering, `pm clear`, sletting av innloggingar, endring av systemlauncheren
eller bruk av Spole-emulatorane. Scrcpy står ope.

- HA svarte med **1 627 tilstandar og 2 952 registeroppføringar**. Standardfilteret
  viste 695 støtta einingar. Rom og namn er henta frå dei tre registra.
- Eit varig dashbord med **ti ekte kort** er lagt til i den eksisterande vaksenprofilen:
  tre lys, klima, to sensorar, straumpris, vêr, mediaspelar og kamera.
  Fire er Favorittar. Vêr, straumpris og temperatur står på Heim.
- Søking etter «PH» saman med Lys-filteret viste PH-lampa med rom, aktuell tilstand
  og «Added». Deaktiverte einingar og oppkopling/ny oppkopling er i tillegg dekte av testar.
- Vêrkortet teikna noverande temperatur og fem dagars varsel frå `get_forecasts`.
- Straumpris i dette hushaldet er **Tibber**. Korta viste ekte 15-minuttsintervall,
  noverande intervall, min/maks og grafdialogen. Morgondagen var enno ikkje publisert;
  valet viste den rette meldinga. Nordpool-attributt, negative prisar og dei to ulike
  02-timane ved vintertid er dekte med JSON-/modelltestar.
- Sensor-sparkline, kamera i kort og fullskjerm, kortmeny via Meny og lang OK,
  og vêrkort M → S → M er prøvde. Lagringa stadfesta M etter tilbakeføring.
- Kameraet gav eit mørkt bilete frå sjølve kjelda. Ingen blur blir lagd på av Stova.
  Oppdateringsintervalla 10 s / 2 s og stopp når skjult/lukka er kontrollerte med to
  kameratestar som tel faktiske Coil-førespurnader til ein falsk bilethentar.
- Vanleg `MainActivity` er starta att etter APK-oppdatering: innloggingane, media,
  dashbordet og dei tre Heim-brikkene er bevarte. Smarthus er opna frå topplinja.

### Styring og tilbakeføring

Alle endringar nedanfor gjekk gjennom dei vanlege Stova-korta eller klima-dialogen.
Tilstandane vart stadfesta frå mottatte HA-snapshot, ikkje berre frå optimistisk UI.

| Eining | Før | Prøve | Etter |
|---|---|---|---|
| `light.ph_lampe` | på, lysstyrke 222/255 | av | på, 222/255 |
| `light.skulpturlampen` | på, lysstyrke 84/255 | av | på, 84/255 |
| `climate.varmepumpe` | mål 23,5 °C | mål 24,0 °C | mål 23,5 °C |

Kvitteringar utan token ligg lokalt i `.superpowers/ha-v2-light-control.json` og
`.superpowers/ha-v2-climate-control.json`. Ingen alarm, lås, garasjeport eller anna
tryggleiksutstyr er endra. Styring av lås/gardin, scenekall, barnemodus og feil med
tilbakeføring er dekte av falske tilkoplingar og einingstestar.

## Skjermbilete

Alle emulatorbileta er tekne med `adb exec-out screencap -p`, i 1920 × 1080, og visuelt
kontrollerte. Originalane ligg i **worktreet** si `.superpowers/ha-v2-*.png`; dette
følgjer den nyaste føringa om å arbeide berre her framfor den eldre absolutte stien
til hovudrepoet si `.superpowers` i bestillinga.

| Bilete | Innhald |
|---|---|
| [ha-v2-dashboard.png](../images/ha-v2-dashboard.png) | Favorittar, vêr, klima og synleg D-pad-fokus |
| [ha-v2-entities.png](../images/ha-v2-entities.png) | Rutenett med registerrom, namn og tilstand |
| [ha-v2-entity-search.png](../images/ha-v2-entity-search.png) | Søk, Lys-filter og eksisterande kort |
| [ha-v2-energy.png](../images/ha-v2-energy.png) | Ekte Tibber-prisar og graf |
| [ha-v2-lights.png](../images/ha-v2-lights.png) | Lys etter tilbakeføring |
| [ha-v2-climate.png](../images/ha-v2-climate.png) | Stadfesta måltemperatur etter tilbakeføring |
| [ha-v2-card-menu.png](../images/ha-v2-card-menu.png) | Kortmeny |
| [ha-v2-home.png](../images/ha-v2-home.png) | Tre ekte HA-brikker på vanleg Heim |

Robolectric sine font-2-bilete er òg genererte under `app/build/screens/`;
`ha-double-font-sensor.png` og `ha-double-font-header.png` er visuelt kontrollerte.

## Reproduserbar utviklingsprobe

`HaVerificationActivity` finst berre i debug, krev emulator og ein vaksenprofil utan
PIN, og brukar Stova si eksisterande innlogging. Han skriv ei avgrensa katalogfil
utan token eller bilete-URL-ar. Han har ingen tenestekall som styrer fysiske einingar.
`mode=seed` kan leggje til oppgitte einings-ID-ar i dashbordet; eksisterande profilar,
kort og innloggingar blir bevarte. Proben eig ikkje produksjonslivssyklusen til socketen.

```powershell
adb -s emulator-5580 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5580 shell am start -n app.stova.debug/app.stova.ha.HaVerificationActivity --es mode inspect
adb -s emulator-5580 shell am start -n app.stova.debug/app.stova.ha.HaVerificationActivity --es mode picker
adb -s emulator-5580 shell am start -n app.stova.debug/app.stova.MainActivity
```

Fysisk Shield, ein verkeleg Nordpool-installasjon og ekte tryggleiksstyring er ikkje
prøvde. Signering, release og ytelsesbudsjett for Shield høyrer framleis til Plan 5.

Protokollreferansar: [HA WebSocket](https://developers.home-assistant.io/docs/api/websocket/),
[vêrhandling](https://www.home-assistant.io/integrations/weather/) og
[Tibber-prisrespons](https://www.home-assistant.io/actions/tibber.get_prices/).
