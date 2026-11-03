# Sonos: teikna høgtalarar — 3. november 2026

Plan: [`2026-11-03-sonos-hoyttalarar.md`](../superpowers/plans/2026-11-03-sonos-hoyttalarar.md).

## Automatisk kontroll

Testane vart skrivne først og feila (manglande `model`/`manufacturer`, `SonosSpeakerActive`,
`sonosRoomVisual`). Nye testar: `SonosSpeakerTest` (modellar, generasjonar og alias, ukjend/
anna produsent, modell frå registeret og ikkje namnet, aktivitet ved spel/pause/TV/fråkopla/
ikkje RESUMED/redusert rørsle, MA held native modell, gruppemedlem viser eigen modell),
`HaCatalogTest.sonosEntitiesCarryTheirDeviceModelOthersStayLean` og tre UI-testar i
`SonosContentTest` (modell/høgtalar i romlista, rørsle berre når musikk spelar, gruppedialog).

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'
```

**1097 testar, 0 feil, 0 hoppa over**, lint 0 feil, debug-APK bygd (ikkje installert).
`DesignRulesTest` og `ThemePaletteTest` grøne.

## Skjermkontroll (Robolectric, native grafikk)

`docs/images/sonos-speakers.png` (alle former), `sonos-playing.png`, `sonos-grouped.png`,
`sonos-group.png` og `sonos-large.png`. Retta etter første bilete: toppflata såg ut som eit
laust lokk, det ukjende motivet likna ein One, lydplanka fekk for høg scene, og ei gruppe
viste same høgtalar to gonger og skauv volumet under kanten.

## Ikkje verifisert

Animasjonen er berre kontrollert via semantikk/testar, ikkje sett i rørsle på ei eining.
Ingen eining, HA eller Sonos er brukt. Kva modellstrengar eigaren sitt HA faktisk rapporterer,
og korleis teikningane ser ut på Loftet, står att for Codex.

## Runde 2 (etter Codex-gjennomgang)

- Sub Mini var feil: ei brei vassrett stripe. Retta til ståande sylinder (Ø230 × 305 mm,
  aspekt .754) med flat topp og høgt, smalt senterhol, kontrollert mot Sonos sitt faktaark
  (`assets.sonos.com/media-kits/hardware/sub-mini/subminifactsheet.pdf`) og diagramma i
  brukarrettleiaren (spegla hos Richer Sounds). Era 300 stemmer med faktaarket (H160 × B260,
  timeglas). Sub (Gen 3) er justert til 402 × 389 mm etter `sonos.com/en-us/guides/sub`; rettleiaren stadfestar
  den gjennomgåande «acoustic slot», men ikkje retninga i teksten, så den vassrette slissen er ikkje kontrollert.
  Alle motiv er stiliserte illustrasjonar: korta lydplanker og samanslåtte modellvariantar
  er ikkje nøyaktige 3D-modellar.
- Synlegheit: `SonosSpeakerArt` bruker `Modifier.onVisibilityChanged` (klippa layoutgrenser
  mot rulleflate/vindauge, kalla når layout flyttar seg, ikkje per animasjonsramme). Klokka
  går berre med synleg areal. Ny test `speakerStopsWhenScrolledOutOfSightAndResumesOnReturn`
  feila med kontrollen fjerna (aktiv vart verande `true` etter utrulling), og er grøn med han.
  Ny test `speakerIsStillUnlessTheScreenIsResumed` (STARTED → RESUMED → STARTED).
  Redusert rørsle var alt dekt.
- Første forsøk med `onGloballyPositioned` fekk ikkje tilbakekall då høgtalaren vart rulla inn
  att; byta til `onVisibilityChanged`.

Kontroll: `testDebugUnitTest --tests 'app.stova.sonos.*' --tests 'app.stova.ha.HaCatalogTest'
--tests 'app.stova.DesignRulesTest' --tests 'app.stova.core.theme.ThemePaletteTest'
-Pstova.screens=true`: **62 testar, 0 feil, 0 hoppa over**. Full suite og lint er ikkje køyrde
på nytt i denne runden. Galleriet er rendra på nytt og kontrollert (`docs/images/sonos-speakers.png`).
