# Utsjånad v2 – verifikasjon, 25. oktober 2026

Arbeid: `C:/Launcher/.worktrees/appearance`, grein `appearance-v2`, frå `91725b0`.
Bestilling: `docs/briefs/2026-10-25-utsjanad-v2.md`. Referansebiletet er lesen og samanlikna visuelt.

## Sluttkontroll

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'
```

BUILD SUCCESSFUL. 318/318 testar, 0 feil, 0 hoppa over. AppScreensTest: 19/19.
DesignRulesTest: 4/4. Lint: 0 feil, 23 åtvaringar, 1 hint.
APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Kontraktar og raud/grøn

| Del | Evidens |
|---|---|
| Hero og sjangrar | HeroPanelTest og ItemParserTest: raud ved manglande genres-API og gamle hero-forventningar; grøn med liten logo/tittel, inga standardomtale, metadatapillar og inga stjerne for score 39. Genres er med i ITEM_FIELDS og toler manglande JSON-felt. |
| Profilval | AppearancePrefsTest og AppearanceRenderingTest: raud ved manglande nye enum-/prefs-/tema-API; grøn med gamle profilverdiar, JSON-rundtur og faktisk teikna storleikar, titlar, appstil, plakat/brei kunst, hero, hjørne og aksentar. |
| Spotlight og klokke | HomeViewModelTest prøver 8/12/20 s og endring utan å laste biblioteket på nytt. ClockTest prøver 12/24 t og skjult; TopBarTest kontrollerer at klokkesloten forsvinn. |
| D-pad og profil | AppearanceSettingsTest: kvar av dei 14 radene blir styrt med høgre, venstre og OK. SettingsScreenTest verifiserer lagring berre på aktiv profil og uendra sperre for barn. |
| Fokus | FocusAppearanceTest bruker injisert FocusInteraction og transformerte grenser: 1,05 ved valfri forstørring, 1,0 med redusert rørsle eller berre ring. Hav-ringen blir òg kontrollert mot faktiske piksel i Heim. |
| Scrim | AppScreensTest var raud fordi dempenivåa var like og høgre halvdel for mørk. Grøn med ulik luminans for alle tre nivå, ≥4,5:1 for venstre tekst og toppfelt på lyst bilete og tydeleg høgre kunst. Pillar har opak Ink-flate. |
| Tette oppsett | Raud for stor plakat/tittel utan plass og for manglande kunst utan fallback. Grøn med reserverte radhøgder og plasshaldartittel ved både null og lastefeil. HomeLayoutTest prøver fast høgd for begge herostorleikane. |
| Omtale | Ekstra raud native-test fann usynleg omtale med lang tittel, sjangrar, alle metadata og store kort. Grøn med profilfast minstehøgd når omtale er på; TextLayoutResult stadfestar to heile linjer over radoverskrifta. |

Den native skjermharnessen set tastaturmodus før han krev TV-fokus. Det er testoppsett, ikkje ei produksjonsendring.
Lokalt vart `local.properties` laga med korrekt SDK-sti og escapet Windows-kolon; fila er ignorert.

## Leveransebilete

Alle er 1920 × 1080, kopierte uendra frå `app/build/screens/`, bytekontrollerte og opna for visuell kontroll:

- `docs/images/appearance-default.png`: standard, logo og appbanner utan titlar, kunst med ring.
- `docs/images/appearance-large-titles.png`: store kort, titlar på, stor hero; synlege titlar utan ring gjennom teksten.
- `docs/images/appearance-small-icons.png`: små kort og kvadratiske appikon.
- `docs/images/appearance-light.png`: lyst testkunstverk med Lys demping og lesbar tekst.
- `docs/images/appearance-ocean.png`: Hav på fokusring, framdrift og «Ny sesong».

Tillegg i harnessen: `app/build/screens/appearance-overview.png`, tre `appearance-contrast-*.png` og dobbel skriftstorleik. Omtalebiletet er òg visuelt kontrollert. Data og kunst er lokale/fiktive.

Ingen emulator eller Shield er brukt. Ekte fjernkontroll, medieserver og installasjon er ikkje prøvde i denne bestillinga. Ingen push eller publisering. Dei forbodne HA-/SmartHome-filene er urørte.
