# Sonos: større høgtalar i fullskjerm og modellbilete i romlista

Bestilling: større høgtalar i fullskjerm, romnamnet under høgtalaren og
høgtalarbilete i romlista på Sonos-sida.

- [x] 1. Oppdater UI-testane: modellbiletet skal visast også når HA-rommet har
  ikon; fullskjerm skal ha større høgtalar med namnet midtstilt under.
  Sjå dei nye kontraktane feile før koden blir endra.
  Alle tre nye/endra åtferdstestar feila på rett årsak før implementering.
- [x] 2. Bruk modellbileta i romlista og flytt fullskjermnamnet til høgtalaren.
  Bevar D-pad-fokus, volumdialog og namn når volumstyring manglar.
- [x] 3. Køyr Sonos-, design- og palettestar, lint og bygg. Sjå ekte
  emulatorbilete ved vanleg og dobbel skrift, og prøv D-pad/volumdialog.
- [x] 4. Installer på Shield og Loftet etter eigaren si uttrykkelege bestilling, kontroller begge
  visningane og oppdater arkitektur og overlevering. Ingen push.

Resultat: 150 grøne testar, lint utan feil, debug-/TV-bygg grøne. Visuell kontroll
på ein isolert `Stova_TV` på 5580 og begge TV-ar. Volumdialog opna/lukka på begge;
full D-pad-fokusretur prøvd på Loftet. Shield viste NRK ved siste kontroll og vart
ikkje styrt vidare. Sjå [verifisering](../../verification/2026-11-05-sonos-hogtalarbilete.md).
