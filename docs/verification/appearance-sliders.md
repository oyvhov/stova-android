# Skyveknappar for utsjånad

2026-10-26 · plan `2026-10-26-appearance-sliders.md`

## Resultat

Fem utsjånadsval har no synleg linje, handtak og presis verdi:

| Side | Val | Område |
|---|---|---|
| Ikon | Bakgrunn på appkort | 0–100 % |
| Bakgrunn | Bakgrunnsdemping | 0–100 % |
| Kort og rader | Hjørne | 0–32 |
| Kort og rader | Kortavstand | 0–40 |
| Heim og hero | Spotlight-tid | 5–60 s |

OK startar justering, Venstre/Høgre endrar eitt steg og halde pil aukar til fem.
Endepunkt stoppar utan wrapping. OK/Tilbake avsluttar på same rad; Opp/Ned flyttar
vidare. Venstre utanfor justering går til sidekolonnen. Berøring/mus kan velje ein
plass på linja eller dra vassrett; loddrette sveip blir sende til rulleflata.
Fleire tastetrykk før neste ramme blir akkumulerte.

0 % appkortbakgrunn er transparent, 100 % er full kortflate. Berre flata får alfa;
ikon og tekst bevarer full styrke. Transparente nabokort får heller ikkje ei mørk
plate ved redusert rørsle/Lys opp. Val av Projectivy set prosenten til 0, også etter
eit tidlegare eksplisitt prosentval. Den eksisterande prøva viser verkelege appikon.

Gamle av/på-val og preset blir lesne uendra når nye talfelt manglar. Standardane er
framleis 100 % appkortbakgrunn, 50 % demping, hjørne 20, avstand 16 og 12 s.
Gamle Lys/Middels/Mørk-kurver er ankera 25/50/75 i den delte, interpolerte scrimen.
Heim og preview bruker same teikning; toppfeltet bevarer lesestøtta si.

Preview får utkastet straks. Ventande sliderskrivingar blir samla i 180 ms; knappar
og innhaldsgrenser blir lagra straks. Pågåande atomisk skriving blir ikkje kansellert.
Éin editor per profil lever i AppContainer, slik at rask lukking/ny opning bevarer
ventande verdiar. Utgang flushar i app-scopet. Kopi/nullstilling ventar på eldre
skrivingar, og fast-bilete-lagring bruker siste utkast etter suspensjonen.

## Kontroll

`pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'`

705/705 testar, 0 feil, 0 skip. DesignRulesTest er grøn. Lint: 0 feil, 62 åtvaringar,
1 hint. Debug-bygg OK. `controls-red.log` viser at det gamle av/på-valet mangla
prosentkontrakten. `reopen-red.log` viser faktisk tap av eit ventande prosentval
før editoren vart delt over skjermretur. Endeleg logg: `final-check.log`.

Regresjonar dekker eldre JSON/ugyldige grenser, preset-kurver, samanhengande piltastar,
redigeringsmodus/Back og endepunkt, mus/berøring, loddrett rulling frå linja,
samla skriving, forseinka skriving med rask ny opning, Android-retur til Heim,
aktiv profil/barneprofil og egendefinert spotlight-tid utan ny mediehenting.
Android-returprøva bruker den faktiske back-dispatcheren; rå Compose-tastar når
berre kategorien i denne testverten.

Native bilete er renderte og visuelt vurderte, ikkje ein ny pikselbaseline:

- `app/build/screens/appearance-slider-icons-1.0.png` og `appearance-slider-icons-2.0.png`.
- `spole-projectivy-transparent.png`, `spole-projectivy-half-background.png`,
  `spole-projectivy-solid.png` og `spole-projectivy-transparent-reduced-motion.png`.

Appkortprøva kontrollerer faktisk bakgrunnspiksel ved 0/50/100 %, uendra geometri
og eit ugjennomsiktig punkt i logoen. Ved 50 % er toleransen éin byte per kanal
fordi Skia rundar premultipliserte fargar medan heiltalsreferansen trunkerer.

## Faktiske einingar

Same APK er installert med `-r` og SHA-256-verifisert på emulator-5580 og Shield:

`141aa05fe8993a4708d0b6237593e99d6c5553a5d4599584e22ab77ffc4e32b3`

Emulatoren vart prøvd med direkte 50 % appkortbakgrunn og ny opning som framleis
viste 50 %. Med fjernkontrolltastar vart hjørne 20→22, avstand 16→18, demping
50→52 % og spotlight 12→13 s prøvde. Opp/Ned avslutta justering og flytta fokus.
Shield vart vekt frå kvileskjermen; 100→97 % og Back til same slider vart prøvd.
Venstre deretter returnerte til Utsjånad i sidekolonnen, og heldt rett side open.

Alle prøveval er sette tilbake: emulator 0 %/50 %/20/16/12 s, Shield 100 % med dei
gamle andre presetverdiane. Alle eldre profilfelt er uendra. Nye talfelt er berre
serialiserte der dei vart prøvde; Shield har òg serialisert førre oppgåve sin
standard for bakgrunnsfilter (maks 15 år). Konto-/hushaldsfiler er byteidentiske.
Begge står på Heim. Ingen avspeling, HA-styring, datatømming, launcher-/tilgjenge-
endring eller push. Fysisk Heim-knapp-oppfølging frå eldre oppgåve er framleis open.

Private bevis i `.superpowers/appearance-sliders/`: før/etter-data, APK,
`verification-summary.txt`, `emulator-reopened-50.png`, `emulator-corners-22.png`,
`emulator-spacing-18.png`, `emulator-dimming-52.png`, `emulator-spotlight-13.png`,
`shield-opacity-97.png`, `shield-slider-sidebar-return.png` og Heim-bilete frå begge.
