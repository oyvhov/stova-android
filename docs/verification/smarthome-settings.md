# Smarthus, innstillingsoversikt og Heim på Shield

2026-10-26

## Resultat

Innstillingar opnar ei oversikt med seks ikonlenkjer. Sidekolonnen er kompakt nok til
at Android er synleg ved normal skrift, og veks/rullar ved stor skrift. Profil- og
HA-undersider parkerer fokus før innhaldet blir bytt, så ein ikkje fell tilbake til
første menyval. Android-innstillingar heiter Android.

HA-innstillingane har eit samla tilkoplingsfelt, eige val for endring av tilkopling,
barnetilgang og utlogging. Synleg oppsett side held socketen open og viser faktisk
status utan å opprette eigne einingsabonnement. Ein kortvarig faktisk fråkopla
tilstand på Shield vart også observert; det vart ikkje dokumentert nokon separat
nettverksfeil eller gjort ei udokumentert transportretting. Etter ny prosess og
retur frå Projectivy viste både dashbord og innstillingar Tilkopla, også etter meir
enn 30 sekund på innstillingssida.

Smarthus har eitt samla rutenett som standard. Oppsett gir romgruppering og
Kompakt/Balansert/Romleg; normalt fem/fire/tre kolonnar. Balansert er standard.
Kort har visuelle S/M/L-val. Pakkinga fyller ledige plassar, deler geometri med
D-pad-navigering og tek omsyn til tekststorleik. Flytting redigerer lagra rekkjefølgje;
fokus blir halde på kortet også ved forseinka lagring og ompakking. Store kort har
hovudinnhaldet meir sentralt, slik at verdiane ikkje blir gøymde nedst i kortet.

Ikon-sida viser verkelege appar med same AppTile som Heim. Projectivy med/utan
kortbakgrunn kan sjåast direkte i førehandsvisinga; ugjennomsiktige delar av sjølve
ikonbiletet blir ikkje kunstig fjerna.

## Heim-feilen

Tenesta oppdagar alle installerte MAIN/HOME-aktivitetar, også Projectivy og alias.
Berre den faktiske HOME-klassen utløyser retur; innstillingar i same pakke får stå opne.
Pakkefilteret blir oppdatert ved installasjon/endring, og status tek omsyn til både
global tilgjenge og medlemskap i lista over aktive tenester.

Eigaren prøvde første versjon fysisk: første trykk var rett, neste feil. Sporing
viste Google-launcher og så Projectivy 334 ms seinare; den gamle globale tidssperra
avviste den siste. Nye regresjonar feila med den gamle logikken. No blir berre like
komponent-/vindaugshendingar samla i eitt sekund. Ein annan launcher får retur straks;
Stova-vindauge eller nytt HOME DOWN nullstiller samanslåinga. HOME UP har inga global sperre.

Endeleg APK bestod seks ADB HOME-trykk etter kvarandre på Shield med ulike mellomrom.
Android-innstillingar heldt seg opne, og HOME derifrå returnerte til Stova. Ny fysisk
prøve er etterspurd, men ikkje stadfesta ved denne overleveringa. Ingen andre
tilgjengetenester eller systemlauncherar er slått av eller endra.

## Automatisk kontroll

`pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'`

670/670 testar, 0 feil, 0 skip. Lint: 0 feil, 52 åtvaringar, 1 hint. Debug-bygg OK.
DesignRulesTest er grøn. Native skjermprøver er renderte og visuelt vurderte,
ikkje pikselbaserte baseline-samanlikningar.

Regresjonar dekker mellom anna standardoversikt, menygrenser og store skrifter,
HA-tilkopling utan einingsabonnement/tenestekall, samanhengande gruppering-/tettleiksval,
pakking utan overlapp i 2–6 kolonnar, forseinka storleikslagring, kortmeny/undermeny og
gjentekne Heim-trykk med konkurrerande launcherar. Kortmenytesten feila fyrst fordi
Lukk fekk startfokus; etter rettinga går første OK til storleiksvala.

Raude/grøne loggar og sluttlogg ligg i `.superpowers/smarthome-settings/`, mellom anna
`home-repeat-red.log`, `home-repeat-green.log`, `card-menu-focus-red.log`,
`card-menu-focus-green.log` og `final-check.log`.

## Einingar og bilete

Same APK er installert med `-r` og SHA-256-verifisert på `emulator-5580` og Shield:

`0b617fcdd937e561efa7f424b9181136b7fff5934406f8d2a8d818e915801ae4`

Oversikt, HA-status og Projectivy på/av er kontrollerte med ekte data på begge.
Emulatoren er prøvd med fire/fem kolonnar, S/M/L-meny, endring M → L → M og fokusretur.
Endeleg storleiksmenu opnar på Storleik, første OK på S, og store vêrtal/prognosar er synlege.
Begge einingar står på Heim. Ingen mediaavspeling eller HA-styring er utført.

Evidens i `.superpowers/smarthome-settings/`:

- `shield-settings-overview.png`, `shield-ha-after-hold.png` og `shield-home-lock.png`.
- `emulator-grid-five-columns.png`, `emulator-layout-final.png`,
  `emulator-card-menu-final.png`, `emulator-size-focus-final.png`, `emulator-card-large-final.png`.
- `shield-icons-solid.png`, `shield-icons-transparent.png`,
  `emulator-icons-preview-off-final.png`, `emulator-icons-preview-on-final.png`.
- `home-repeat-final.txt`, `shield-home-final.png` og `stova-smarthome-settings.apk`.

Native layoutbilete ligg i `app/build/screens/`, inkludert `ha-mixed-footprints.png`
og prøver med dobbel skrift. Bileta har synleg faktisk tekst.

## Databevaring

Konto- og hushaldsfiler er byteidentiske før/etter på begge einingar.
Emulatorprofilen har berre serialisert det nye standardfeltet smartHome=NONE/BALANCED;
alle eldre felt, kortstorleikar og rekkjefølgje er uendra etter at prøvevala vart sette tilbake.

På Shield kom det undervegs endringar i aksent/lagra sesongval, spotlight-utval,
kortbakgrunn og eit nytt Sonos-kort. Desse vart behandla som eigaren sine samtidige
endringar og bevarte. Kortbakgrunn var allereie På før vår Shield-prøve og vart sett
tilbake til På; emulatoren tilbake til Av. Nye standardfelt for Smarthus er serialiserte.
Shield-profilen er byteidentisk frå før siste installasjon/prøver til sluttkontrollen.
Radvise overstyringar er bevarte. Ingen datatømming, avinstallering eller push/publisering.
