# Smarthus v2 – finpuss, 25.10.2026

Bestillinga `docs/briefs/2026-10-25-smarthus-v2-finpuss.md` er fullført i
`C:\Launcher\.worktrees\ha-v2`, grein `ha-v2`. Ingen push eller publisering.

## Sluttkontroll

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'
```

**BUILD SUCCESSFUL: 348/348 testar, 0 feil, 0 hoppa over.**
132 testar under `app.stova.ha`; `DesignRulesTest` 4/4. Dette er 19 fleire testar enn
før finpussen. Lint: 0 feil, 25 åtvaringar og 1 hint. APK:
`app/build/outputs/apk/debug/app-debug.apk`.

Resultat: `app/build/test-results/testDebugUnitTest/`, lint:
`app/build/reports/lint-results-debug.html`. Lokal full logg:
`.superpowers/ha-v2-polish-final-check.log`. TDD-ledger:
`.superpowers/sdd/2026-10-25-smarthus-v2-finpuss.md/progress.md`.

## Resultat og raud/grøn-evidens

- `LightPresentationTest`: nye temperaturressursar/presentasjon mangla i raud fase;
  grønt for begge temperaturgrenser, 8/15/22 % glød, avslått lys, fargemodus og avmetting.
- `PricePresentationTest`: raud på manglande timesnitt/nivå/akse, deretter grøn for
  96 kvarter → 24 timar, vekting av ulike intervall, manglande timar, negative/jamne prisar,
  25-timars vinterdag og lokal plassering av 06/12/18.
- `HaPolishUiTest`: alle tre opphavlege testar feila på namn/verdi og 160/336 dp;
  grøne på namn før hovudverdi og 152/320 dp. Tillegg testar kompakt femdagarsvêr,
  klima med fokushint og albumkunst som fyller heile venstresida. Vêrtesten avdekte
  158,5 dp før finjustert avstand.
- `PriceChartUiTest`: raud fordi 15-minuttsvalet var skjult bak «Timesnitt»;
  grønt med to synlege val og korrekte klokkeslett for min/maks ved begge oppløysingar.
- `SmartHomeV2UiTest`: D-pad-rulling krev at heile fokusert kort ligg innanfor rutenettet;
  hintet forsvinn når fokus går til topplinja. Eksisterande kontroll-, kamera-,
  barneprofil- og feiltestar er grøne.

Korta har namn/ikon øvst, hovudverdi og detaljar nedst. Ingen dobbel På-status.
Lys har varm eller avmetta fargeglød; klima har temperatur, modusikon og ±-hint.
M/L-media har kunst heilt til venstrekanten, tittel/artist og framdrift til høgre.
Romtekst finst berre i Favorittar. Graf og minigraf deler nivåfarging og timesnitt.

Visuell iterasjon retta ei for høg vêrrad, eit hint som dekte grafen, for lita
albumkunst og ein trong prisdialog. Hintet står no roleg i overskriftsfeltet berre
med rutenettfokus ved vanleg skriftstorleik, utan å bruke kortplass. Dette er eit
medvite avvik frå den opphavlege plasseringa nedst. Ved dobbel skriftstorleik er
hintet skjult; korta får vekse etter teksten.

Berre HA-kode, HA-testar og strengblokka «Smarthus v2» er endra. Den einaste endra
temafila er den nye, uttrykkeleg tillatne `core/theme/HaTokens.kt`. Heim, hero,
mediekorta på Heim og utsjånadsfilene er urørte. Spesifikasjonen §5.2 er oppdatert.

## Ekte Home Assistant på emulator-5580

Berre `adb -s emulator-5580`, oppdatering med `install -r` og `app.stova.debug` er brukt.
Ingen sletting av data/innloggingar. Den vanlege MainActivity er prøvd etter siste
installasjon; media, tre Heim-brikker og alle ti HA-korta er bevarte. Smarthus er
opna frå den vanlege topplinja og står ope ved levering.

Tilbakeføring er kontrollert både i live-cache frå mottatte tilstandar og med eit
nytt `get_states` gjennom den eksisterande debug-proben; dette er ikkje berre
optimistisk UI.

| Eining | Før | Prøve | Stadfesta etter |
|---|---|---|---|
| `light.ph_lampe` | På, 222/255 | Av | På, 222/255 |
| `light.skulpturlampen` | På, 84/255 | Av | På, 84/255 |
| `climate.varmepumpe` | Mål 23,5 °C | 24,0 °C med D-pad | Mål 23,5 °C |

Kvitteringar utan token ligg lokalt i `.superpowers/ha-v2-polish-light-control.json`,
`ha-v2-polish-climate-control.json` og `ha-v2-polish-final-states.json`.
Alarm, lås og garasjeport er ikkje endra.

Ekte Tibber viser både timesnitt og 15-minuttsprisar. Denne datoen er ein vinterdag
med **25** timar; grafen bevarer den ekstra timen. Morgondagen er enno ikkje publisert,
og viser meldinga om manglande prisar. D-pad-fokus er kontrollert på S, M og L;
L-kameraet er heilt synleg med fokusringen. Kameraet gir framleis eit mørkt bilete
frå sjølve kjelda. Media viser ekte Sonos-kunst, tittel, artist og framdriftslinje;
avspelinga vart ikkje endra. Hushaldet har ingen fargelys som er endra i denne prøva;
RGB/HS og grensene for temperaturord er dekte av testar.

## Skjermbilete

Ni ekte emulatorbilete, alle 1920 × 1080, er tekne med `adb exec-out screencap -p`.
Originalane ligg i worktreet si `.superpowers/`. Det tiande biletet er tydeleg skilt
ut som Robolectric med kontrollerte data og dobbel skriftstorleik. Bileta er opna og
vurderte visuelt; ingen genererte illustrasjonar er brukte.

| Fil | Innhald |
|---|---|
| [ha-v2-polish-dashboard.png](../images/ha-v2-polish-dashboard.png) | Vanleg Smarthus med kompakt vêr, klima, pris og sensor |
| [ha-v2-polish-lights.png](../images/ha-v2-polish-lights.png) | Lysglød ved 87/32/60 %, namn først og heile fokusringen |
| [ha-v2-polish-energy.png](../images/ha-v2-polish-energy.png) | Timesnitt, nivåfargar, lokal tidsakse, no og min/maks |
| [ha-v2-polish-energy-quarters.png](../images/ha-v2-polish-energy-quarters.png) | 15-minuttsval og klokkeslett |
| [ha-v2-polish-energy-tomorrow.png](../images/ha-v2-polish-energy-tomorrow.png) | Meldinga når morgondagen ikkje er publisert |
| [ha-v2-polish-climate-restored.png](../images/ha-v2-polish-climate-restored.png) | Modus med ikon, fokushint og opphavleg måltemperatur |
| [ha-v2-polish-media.png](../images/ha-v2-polish-media.png) | Albumkunst fyller venstresida, artist, framdrift og fokus |
| [ha-v2-polish-large-focus.png](../images/ha-v2-polish-large-focus.png) | L-kort heilt innanfor synsfeltet |
| [ha-v2-polish-home.png](../images/ha-v2-polish-home.png) | Ekte Heim med dei tre bevarte HA-brikkene |
| [ha-v2-polish-font-2.png](../images/ha-v2-polish-font-2.png) | Robolectric, kontrollerte data, dobbel skriftstorleik |

Fysisk Shield er ikkje prøvd i denne bestillinga. Dei eldre release-/Shield-stega
er framleis utsette slik dei var før finpussen.
