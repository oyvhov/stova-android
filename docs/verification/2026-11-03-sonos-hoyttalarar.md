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

## Ikkje verifisert etter første runde

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

## Runde 3: bibliotekbilete (funn frå Loftet)

Funn (`.superpowers/sonos-visuals-2026-11-03/loftet-library-first.png`): HA-biblioteket
crop-fylte transparente logoar og mappeikon, og Sonos-reserveikonet skein gjennom dei.

- `SonosMedia` les `media_class`. `logoArtwork`: HA-merkelogoar (`brands.home-assistant.io`,
  `/api/brands/`) og alt som ikkje er bilete-klasser (album, artist, spelelister, spor, podkast,
  episode, film, serie, sesong, video, musikk) blir viste heile (`ContentScale.Fit`) med luft på
  ei roleg flate. Album- og spelelistekunst fyller framleis ruta.
- `SonosLibraryTile`: reserveteiknet blir berre vist til biletet har lasta (`HaImage.onLoad`), og
  blir verande ved manglande eller feila bilete, så ingen rute blir tom. Ingen HA-kjelder er skjulte.
- Avspelingssida (`SonosCover`) og Heim er urørte: albumkunst, feil- og tomreserve som før.

Testar først (raude ved kompilering før endringa): `logosAndFolderIconsAreFittedWholeButAlbumArtFillsItsSquare`
og `aLoadedPictureHidesTheFallbackButAMissingOrFailedOneKeepsIt` i `SonosLibraryTest`, og
skjermtesten `SonosScreensTest.library` (`docs/images/sonos-library.png`, kontrollert).
`testDebugUnitTest --tests 'app.stova.sonos.*' --tests 'app.stova.DesignRulesTest' --tests
'app.stova.core.theme.ThemePaletteTest' -Pstova.screens=true`: **58 testar, 0 feil, 0 hoppa over**.
Ikkje løyst: ein svart transparent logo (til dømes «Favorites» på Loftet) har låg kontrast mot den
mørke flata; det krev ei eiga lys plate eller fargejustering og er ikkje gjort no.

## Runde 4: Loftet med verkelege data (Codex, 3. november)

Eigaren bad uttrykkeleg om designgjennomgang med Claude og installasjon/test på Loftet.
Claude Code gjorde design og kode i `db81866`, `0e0bfe7` og `944aae0`; Codex kontrollerte
bileta, bad om rettingar og tok einingstesten. Berre éin kode-/byggagent arbeidde om gongen.

Sluttkontroll: `pwsh -NoProfile -File scripts/gradle.ps1 lintDebug assembleTv` grønt,
lint **0 feil, 84 åtvaringar, 1 hint**. Full suite før siste avgrensa rettingar: 1097 grøne;
siste modell-/synlegheitsrunde 62 grøne; siste biblioteksrunde 58 grøne, med DesignRules og
ThemePalette i begge. Ingen ny full suite etter biblioteksrettinga.

Installerte med `scripts/Install-Stova.ps1 -Serial 192.168.10.125:5555 -Tv` (install -r).
Loftet Tv er Google TV Streamer; eksisterande Øyvind-profil, kontoar og HA-tilkopling vart
bevarte. Sonos vart aktivert, med standardval for widget/toppikon/sidekunst. Heim-kunst og
Music Assistant er framleis av. Vald Sonos-spelar er Lydplanke. Ingen demodata eller clear,
avinstallering, force-stop eller endring av systemlauncheren. Shield vart ikkje brukt.

Kontrollert på faktiske skjermbilete:
- HA rapporterer Play:3 for Sonos Kjøkken, Playbar for Sonos Lydplanke, Era 100 for
  Sonos Platespelar (fråkopla) og Play:1 for Sonos Storestova. Figurane og etikettane stemmer
  med denne modellkoplinga. Platespelar får dempa figur og deaktiverte kontrollar.
- Eksisterande gruppe Kjøkken/Storestova får eigne medlemsfigurar. Gruppedialogen viser
  modell og varsel om flytting frå anna gruppe; lukka utan å bruke endringar.
- Omslag, bakgrunn, romval, volumvising og D-pad-rulling fungerer. Langt OK med `hold:1500`
  på valt rom starta inga musikk. Volumet på Lydplanke vart verande 9 %.
- Bibliotek → Favorites → Playlists viser dei verkelege favorittane med omslag, mellom
  anna «2021 Sommar». Val opnar «Spel» / «Legg til i køen» / «Avbryt»; lukka utan avspeling.
  Tilbake gjennom alle nivå fungerer. Dei engelske kategorinamna kjem frå HA.
- Biblioteklogoar er etter rettinga heile og utan dobbelt reserveikon. Svart Sonos-logo og
  mørk tekst i nokre tenestelogoar har framleis låg kontrast på den mørke flata.
- Heim-widget viser omslag, rom og «På pause»; tittelknappen opnar Sonos. Toppikonet er synleg.
- Heim-knapp-tenesta var både enabled og bound etter siste installasjon; crash-bufferen var
  tom. Loftet vart sett tilbake på Øyvind sin Heim og lagt i søvn (`mWakefulness=Asleep`).

Bilete ligg lokalt (ikkje i Git) under `.superpowers/sonos-visuals-2026-11-03/`:
`final-sonos.png`, `loftet-group.png`, `loftet-offline.png`, `final-library.png`,
`final-playlists.png`, `final-favorite-choice.png` og `final-restored-home.png`.

Ikkje prøvd på anlegget: musikkstart/pause/volum/gruppering, Music Assistant, aktiv TV-lyd
og animasjonen under avspeling. Alle tilgjengelege rom var pausa; ingen tenestehandlingar
vart sende. Spørsmålet om ein kort, låg test på Lydplanke vart ikkje svara på i denne økta.
Animasjonsvilkår og TV-sperre er kontrollerte i automatiske testar, ikkje under ekte lyd.
