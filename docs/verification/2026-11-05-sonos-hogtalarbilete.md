# Sonos: høgtalarbilete, fullskjerm og TV-prøve

2026-11-05. Bestillinga er utført: større høgtalar i fullskjerm med romnamnet
midtstilt under, og høgtalarmodellar i Sonos-romlista sjølv om HA-romma har ikon.

## Kode og automatiske testar

- Fullskjermbiletet er 112 dp breitt mot 44 dp før. Høgda følgjer modellen,
  slik at låge lydplankar får mindre tomrom før namnet. Namnet står på ei
  ugjennomsiktig temaflate for å tole lyse omslag.
- Volumpopupen er forankra under både høgtalaren og namnet. Ukjent volum
  deaktiverer handlinga og bevarer modell og namn. Ukjend modell får den
  eksisterande nøytrale høgtalarforma.
- Tre nye/endra åtferdstestar feila før implementering. Siste køyring:

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests 'app.stova.sonos.*' --tests 'app.stova.DesignRulesTest' --tests 'app.stova.core.theme.ThemePaletteTest' '-Pstova.screens=true' lintDebug assembleDebug assembleTv
```

150 testar, 0 feil, 0 hoppa over. Lint: 0 feil, 95 åtvaringar og 1 hint.
Debug- og TV-bygg grøne. Skjermtestane lagar bilete; dei er ikkje automatiske
baseline-samanlikningar. `app/build/screens/sonos-fullscreen.png`,
`sonos-fullscreen-era.png`, `sonos-fullscreen-long-room.png` og
`sonos-playing.png` finst og vart visuelt kontrollerte. Langt namn og dobbel
skrift er prøvde, i tillegg til liggjande og ståande høgtalarar.

## Emulator

Den vanlege AVD-en har tidlegare hatt ekte oppsett. Prøva brukte difor ein
ny, isolert `Stova_TV` på **emulator-5580**, med AVD-heim
`.superpowers/sonos-speaker-emulator/avd/`. Den vanlege `.avd/` vart bevart,
og ingen Spole-emulator vart brukt. Debug-APK installert utan demo eller
hovudappstart; berre `SonosPreviewActivity` med kontrollerte data vart brukt.
Vanleg/dobbel skrift, romlista, fullskjerm, Opp til volum, OK, Tilbake og
fokusretur vart kontrollerte. Prøveemulatoren er stoppa.

## Shield og Loftet

Eigaren bad uttrykkeleg om test på begge. TV-APK installert med
`Install-Stova.ps1 -Tv` / `install -r`, med profilar og kontoar bevarte.
Lokal APK og installert `base.apk` på begge har SHA-256
`32e48e9121feb45367d8f7e1c55c3372867d996f2511a97b9e18c47cf6a4bc1b`.

- Begge: Play:3, Playbar, Era 100 og Play:1 som modellbilete i romlista;
  større Play:3 i fullskjerm med «Kjøkken» under. Paradise / Coldplay,
  pausa ved 0:00 / 4:38. Volumdialog opna og lukka ved 0 %.
- Loftet: D-pad til høgtalaren, OK opnar volum, Tilbake gir fokus tilbake,
  Ned til spel, Opp til høgtalaren og Tilbake til fullskjermknappen på
  vanleg Sonos-side. Opna fullskjerm att ved avslutning.
- Shield: layout, popup og D-pad-fokus på høgtalaren kontrollerte.
  Ved siste OK/Tilbake-prøve viste Shield NRK. Ingen fleire tastetrykk vart
  sende dit. Den siste fullstendige fokusreturprøva er difor berre frå Loftet.

Bilete ligg i `output/screenshots/Sonos-speaker-2026-11-05/`, mellom anna
`Shield-Sonos.png`, `Shield-fullskjerm.png`, `Shield-hogtalarfokus.png`,
`Loftet-Sonos.png`, `Loftet-fullskjerm.png`, `Loftet-volum.png`,
`Loftet-fokusretur.png` og `Loftet-fullskjerm-retur.png`. Dei endelege
TV-prøvene brukte skjermbilete mellom tastetrykka: UIAutomator-fokusdump
gav tilstand som ikkje samsvara med den påfølgjande skjermfangsten.
Ingen Sonos-avspeling, volum-, kjelde- eller gruppeendringar i prøva.
