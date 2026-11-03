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

## Ikkje verifisert

Rørsle i fart, fokusring/trykk-dupp med ekte fjernkontroll, og korleis ekte omslag ser ut under
overgangane er ikkje sett på TV. Stor skrift i fullskjerm rullar.
