# Sonos: roleg volum, fullskjerm og mikrorørsle

Bestilling 3. november 2026, eigaren (skjermbilete frå Loftet): volumlinja ser ikkje bra ut,
«Volum» står to gonger, «ingen tekst her». Vil ha eit eksplisitt fullskjermmodus med albumkunst og
enkle, vakre mediekontrollar, og meir mikrorørsle. Bindande: dempa knappar, ekte høgtalarfigurar,
tema/sesong/redusert rørsle, TV-lyd aldri som musikkunst, ingen blur/glas, fargar berre i `core/theme`.

Designvurdering: volumblokka (overskrift + boks med etikett + tal + kvitt spor) er det tyngste
elementet på sida og tek merksemd frå omslaget. Tekstknappane for spor konkurrerer med kunsten.

- [x] 1. Test først: volumet har ingen synleg tekst, men tilgjengeleg namn/verdi/rolle; Høgre/Venstre
  stegar 2 % med samla sending; ingenting blir sendt medan `busy` (fokus står); avslegen når rommet
  ikkje svarar; nytt rom tek ikkje med seg utkastet frå det førre.
- [x] 2. `SonosVolumeRail` (eigen fil): høgtalarikon og ei tynn skinne med fyll og lite handtak,
  heile raden er fokus-/trykkflate. Generisk `PanelSlider` er uendra.
- [x] 3. Test først: fullskjerm frå knapp i toppen; inngang gir fokus til spel/pause (elles
  «Avslutt»); Tilbake og «Avslutt» går til vanleg visning med fokus på fullskjermknappen, neste
  Tilbake følgjer vanleg navigasjon; ingen spelehandling ved inn/ut; TV-lyd/fråkopla/utan kunst
  er brukande; aldri gammal kunst for TV, heller ikkje i ei utgåande overtoning.
- [x] 4. Fullskjerm (`SonosImmersive`): kunst kant til kant (når bakgrunnskunst er tillaten),
  temafarga overgangar, tittel/artist, diskret rom, ikonknappar for spor og spel/pause, tynt volum.
  Utan kunst: omslag eller høgtalarfiguren. Tilstanden er hoista i `SonosScreen`.
- [x] 5. Ikonknappar for spor òg i vanleg visning (NN/EN-skildringar), trykk-/fokusrespons,
  spel/pause-ikon som tonar, omslag/spor som tonar over, rom som tonar val, modusovergang i
  lag-fasen. Redusert rørsle: direkte byte.
- [x] 6. Skjermtestar: vanleg/fullskjerm med kunst, utan kunst, TV, jul, Halloween, stor skrift.
- [x] 7. Målretta Sonos/HA-kort/DesignRules/ThemePalette med skjermtestar, `lintDebug`, `assembleTv`.
- [x] 8. Berre Loftet: visuell prøve av inn/ut, fjernkontrollfokus, fråkopla høgtalar og overgangar.
  Ny presisering frå eigaren: ingen ekte lyd, inga avspeling eller volumendring; Shield skal ikkje brukast.
  TV-kunstsperra er kontrollert i testane, utan å byte ekte lydkjelde.

Sluttgjennomgang: retta fokus ved gjenoppretta fullskjerm og held bakgrunnskunsten lasta gjennom
modusbyte. To regresjonar feila først; 113 målretta testar, lint og TV-bygg grøne etter rettingane.

Verifisering: [`../../verification/2026-11-03-sonos-fullskjerm.md`](../../verification/2026-11-03-sonos-fullskjerm.md).
