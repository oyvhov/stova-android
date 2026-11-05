# Samla UI-retting, flyttemodus og romikon

2026-11-05. Gjennomført etter `2026-11-05-ui-apple-google.md` og eigaren sine
tillegg om avkutta logo, romlister og ikon frå Home Assistant.
Berre Shield `192.168.10.182:5555`; ingen emulator eller Loftet.

Siste TV-APK er installert med `install -r`, og SHA-256 er samanlikna med
`base.apk` på Shield:
`5b48e64bc026ccb3ecd28a16055e22fdee2be4971c9d6f4c865f6e423fb9f8a6`.

## Resultat

- Oppdag eig éin vedvarande `BackdropLayer` i `DiscoverStage`. Den dekoda
  biletekomposisjonen lever utanfor stega sin `SaveableStateProvider`.
  Første alfa-/geometriretting var utilstrekkeleg: ein ny biletekomposisjon
  fekk tomme første rammer også frå minnebufferen. Ny regresjonstest krev
  same biletnode gjennom fire faktiske stegbyte.
- Det siste Oppdag-opptaket viser to opningar og returar utan svarte
  mellomrammer med `blackdetect=d=0.01:pix_th=0.02`. Omtalen over det lyse
  helikopterbiletet er lesbar med den utvida lesekurva.
- Heim dempar kunsten ved blading. Lange kortnamn rullar roleg ved fokus;
  redusert rørsle held teksten i ro. Full tekst er bevart i semantikken.
- Smarthus forklarer mellomlagra verdiar ved sambandsbrot. Dashbord,
  opne panel og lagergrensa avviser kontrollsending utan samband. Legg til
  tilbyr forklaring og ny tilkopling; Sonos har veg til HA-oppsettet.
- Flytt i app-/snarvegmenyen startar eitt utkast: gjentekne piler flyttar
  same kort, OK lagrar og Tilbake avbryt. Uviste/nye favorittar blir bevarte
  ved lagring. Native prøve flytta Spole tre plassar og avbraut med korrekt
  rekkjefølgje og fokus tilbake, utan å starte appen.
- App- og radmenyar er kortare og har korrekt retur gjennom undersider.
  Breie applogoar bruker Fit med innvendig plass i den kompakte menyen.
- Romnamn står på ei line, Sonos har flate romrader, og valt rom har ei
  diskret markering uavhengig av fokus. Sonos-favorittane er framleis ved
  sida av romlista; alle vanlege kontrollar får plass på éin skjerm.
- Doble Sonos-namn er fjerna, kolliderande appnamn blir skilde, profilskjemaet
  har tittel, og nynorsk omgrep er samordna. Fast panelhjelp er redusert.
- HA sitt `area_registry/list.icon` følgjer det løyste rommet inn i Smarthus,
  Sonos og romveljaren ved Legg til. Kjøkkenikonet er observert på Shield i
  begge romlistene, også valt rom; Arkja, Bad og Gang Oppe har eigne ikon i
  romveljaren. Manglande/ukjende ikon har reservevising. Romikonet er skilt
  frå eininga sitt eige ikon, og følgjer eksisterande registermellomlager.

HA-feltet er kontrollert mot [Home Assistant sitt områderegister](https://github.com/home-assistant/core/blob/dev/homeassistant/helpers/area_registry.py).
Ingen ekstra ikonnettverk eller endring av eigaren sitt HA-oppsett.

## Kontrollar

`pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug assembleTv '-Pstova.screens=true'`

1 245 testar bestått, 0 feil og 0 hoppa over. `DesignRulesTest` og
`ThemePaletteTest` grøne. Lint fullført utan feil; 95 åtvaringar og eitt hint.
Større skrift, redusert rørsle, førstefokus i Sonos-favorittane, tastaturflytting,
avbrot/lagring og opne panel ved sambandsbrot er dekte av kontrollerte UI-testar.

## Native materiale

Materialet ligg i `.superpowers/ui-fixes-2026-11-05/`.
`accepted-evidence.json` listar kjeldefiler og APK-versjonen som vart brukt.
PNG-ar i 1280 × 720 er nedskalerte frå dei tilsvarande `-4k.png`-filene.

| Flyt | Materiale |
|---|---|
| Heil applogo og kort meny | `final-01-app-menu.png` |
| Smarthus og HA-romikon | `ha-icons-final-smart-home.png`, `ha-icons-03-selected-room.png` |
| HA-ikon i Legg til | `ha-icons-04-add-room.png` |
| Sonos, romikon og alle kontrollar | `ha-icons-final-sonos.png` |
| Fullskjerm og roleg volumpopup | `final-07-sonos-fullscreen.png`, `final-08-sonos-volume-popup.png` |
| Popup opning/lukking | `Smarthus-popup-final.mp4`, `final-popup-all-frames.jpg` |
| Oppdag med vedvarande kunst | `Oppdag-sidebyte-stable.mp4`, `stable-discover-frames.jpg` |
| Fleire flytt utan ny meny | `Flytt-fleire-plassar-latest.mp4`, `latest-move-three-places.png`, `latest-move-cancelled.png` |

`Oppdag-sidebyte-final.mp4` er eit mellomresultat som framleis hadde svarte
rammer. Det er bevart for samanlikning og er ikkje godkjend sluttdokumentasjon.
Dei siste ikonendringane endrar ikkje Oppdag-overgangen, flyttelogikken eller
popup-animasjonen. Dei ulike opptaka er knytte til rett APK i evidenslista.

Ingen flygande stripe eller heilt svart ramme observert i popup-opptaket.
Opptaka har variabel bilettakt og er ikkje ei generell måling av FPS.

## Avgrensingar og sluttilstand

Ingen kontrollkommandoar mot lyd, lys eller varme, ingen nye førespurnader,
kort eller kontoar. Flytting på den ekte profilen vart avbroten; lagring er
testa mot kontrollerte profilar. Sonos stod pausa på Kjøkken. Aktivt låtbyte,
fysisk MENY og TalkBack er ikkje prøvde i denne økta. HA-sambandsbrot er testa
med kontrollerte data; den ekte tenesta var tilkopla ved sluttkontrollen.
Shield står att på Sonos i same Øyvind-profil. Ingen push eller publisering.
