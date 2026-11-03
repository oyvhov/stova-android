# Sonos: roleg volum, fullskjerm og mikrorørsle — 3. november 2026

Plan: [`2026-11-03-sonos-fullskjerm.md`](../superpowers/plans/2026-11-03-sonos-fullskjerm.md).

## Designvurdering (skjermbilete frå Loftet)

Volumblokka var det tyngste elementet på sida: overskrifta «Volum · rom», ein grå boks med
«Volum» att og prosenten, og eit skarpt kvitt spor. Ho tok merksemda frå omslaget. Spor-knappane
var tekst («Førre spor», «Neste spor») som konkurrerte med kunsten. Ingen eigen visningsmodus for
berre musikken.

## Endringar

- `SonosVolumeRail`: høgtalarikon, tynn skinne med fyll og lite handtak (veks litt ved fokus),
  heile raden er fokus-/trykkflate. Ingen synleg tekst; namn, verdi og område til TalkBack.
- `SonosTransport`/`SonosIconButton`: runde ikonknappar for førre/spel-pause/neste, NN/EN-namn,
  mjuk skive bak spel/pause, trykk-dupp i lag-fasen, ikonet toner mellom spel og pause.
- Fullskjerm (`SonosImmersive`): kunst kant til kant med temafarga overgangar, tittel/artist,
  diskret rom og modell, ikonkontrollar og volum; utan fullskjermskunst står omslaget eller
  høgtalaren til høgre. Ingen romliste, bibliotek, gruppering eller val. Tilstanden er hoista i
  `SonosScreen`; inngang gir fokus til spel/pause (elles «Avslutt»), Tilbake/«Avslutt» gir fokus
  til fullskjermknappen, neste Tilbake følgjer vanleg navigasjon.
- Rørsle: side og fullskjerm kjem inn med kort fade/settle i lag-fasen; omslag og spor toner
  over; valt rom-plate toner i teiknefasen. Redusert rørsle: direkte byte. Ingen ny polling.
- TV-lyd: aldri kunst (heller ikkje utgåande overtoning) og ikkje lenger gammal framdrift.
- Modellnamnet står no i statuslinja i romkortet («Klar · One SL»), ikkje under høgtalaren ved volumet.

## Testar

Skrivne først (`SonosFullscreenTest`, feila ved kompilering): volum utan synleg tekst med
tilgjengeleg namn/verdi/område; 2 %-steg og éi sending; ingenting medan `busy` og fokus står;
avslegen for fråkopla rom; ingenting blir sendt etter rombyte; fullskjerm inn/ut/Tilbake/fokus
utan spelehandlingar; «Avslutt» og spor i fullskjerm; fokus på «Avslutt» utan støtte; TV-portvakt
bilete for bilete i begge visingar; ikonkontrollar med talte namn. TV-testen feila med portvakta
fjerna. I Robolectric går debouncen ut under sjølve tastetrykket, så rombyte-testen prøver at
ingenting blir sendt *etter* byttet og at nytt rom viser sin eigen verdi; i tillegg sender skinna
berre til rommet utkastet vart laga for. Eksisterande testar: veljarar endra frå tekst til
ikon-skildringar; kontraktane står.

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest '-Pstova.screens=true'
pwsh -NoProfile -File scripts/gradle.ps1 lintDebug assembleTv
```

**1156 testar, 0 feil, 0 hoppa over** (medrekna skjermtestar, DesignRules/ThemePalette); lint 0 feil;
`assembleTv` grønt. Ikkje installert av Claude.

## Skjermbilete (sett gjennom)

`docs/images/sonos-fullscreen.png`, `-noart`, `-tv`, `-themeonly`, `-christmas`, `-halloween`, `-large`,
og vanleg side `sonos-playing`, `-large`, `-christmas`, `-halloween`, `-grouped`, `-rooms-hidden`.
Retta etter første runde: TV-lyd viste framdrifta til førre låt; tema-only klemde spel/pause-skiva,
så omslaget/høgtalaren står no til høgre.

## Sluttgjennomgang og Loftet

Codex retta to konkrete funn etter Claude sin implementasjon (`a7ea298`): gjenoppretta fullskjerm
bad om fokus på kontrollar i den skjulte vanlege sida, og modusbyte lasta bakgrunnskunsten inn att
med eit kort tomrom. Begge regresjonstestane feila først. Sida ber no berre om fokus medan ho er
synleg, og `SonosContent` held den same bakgrunnskomposisjonen gjennom modusbytet.

Siste kontroll etter rettingane:

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests 'app.stova.sonos.*' --tests '*DesignRulesTest' --tests '*ThemePaletteTest' '-Pstova.screens=true' lintDebug assembleTv
```

**113 målretta testar, 0 feil, 0 hoppa over; lint 0 feil; TV-bygg grønt.** Heile pakken med 1156
testar ovanfor vart køyrd før dei to avgrensa rettingane; full pakke er ikkje køyrd på nytt.

Ny presisering frå eigaren: **berre Loftet, berre visuelt, ingen ekte lyd**. Siste APK installert
med `Install-Stova.ps1 -Serial 192.168.10.125:5555 -Tv` (`install -r`, kontoar/profilar bevarte).
Shield er ikkje endra i denne designrunden. Stova-emulatoren starta, men vart ikkje brukt til
vidare prøving etter presiseringa.

På Loftet, Øyvind/Halloween:

- Pausa Kjøkken-spor «Halloween Night in the Forest» vart brukt med eksisterande kunst. Framdrifta
  stod på 0:53/3:04 gjennom prøva. Ingen avspelings-, volum-, kjelde- eller gruppehandling vart utført.
- Tekstlaus volumstripe, vanleg visning og gøymd romliste passar skjermen utan rulling.
- D-pad inn i fullskjerm gir fokus til Spel; Høgre går til Neste og deretter volumstripa, utan
  aktivering. Tilbake returnerer fokus til fullskjermknappen; neste Tilbake går til Heim.
- Avslutt-ikonet og fråkopla Platespelar vart prøvde visuelt; valt rom vart sett attende til Kjøkken.
- Langt trykk på fullskjermknappen med `hold:1500`, fokusrespons og modusovergang vart filma.
  Første opptak viste den korte biletladinga; siste opptak viser kunst gjennom heile overgangen.
- Etter første installasjon mista HA kontakten etter ei kort stund. Inn i Android-innstillingane
  og Heim kopla opp att. Same kjende oppstartsproblem som før denne endringa; ikkje årsaksretta her.
  Siste installasjon fekk den same ut/inn-opninga. Krasjbuffer var tom, og Heim-tenesta var bound.

Ekte bilete/opptak i `.superpowers/sonos-immersive-2026-11-03/`:
`loftet-final-normal.png`, `loftet-final-fullscreen.png`, `loftet-final-back.png`,
`loftet-rooms-hidden.png`, `loftet-volume-focus.png`, `loftet-offline-fullscreen.png`,
`loftet-final-motion.mp4` og `loftet-final-transition-frames.png`.

## Avgrensingar

Ingen ekte avspeling, låtbyte eller volumjustering i denne runden. TV-kunstsperra og overtoning
ved låtbyte vart prøvde med kontrollerte data, utan å endre ei ekte lydkjelde. Stor skrift i
fullskjerm kan rulle. Skjermane for jul, redusert rørsle og tema utan kunst er lokale skjermtestar.
