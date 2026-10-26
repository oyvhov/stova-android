# Appnamn per rad, Tilbake i Innstillingar og Smarthus-toppen

Bestilling: «Eg vil kunne ha av appnavn, samtidig som dei andre radane kan ha navn.»
«Tilbakeknappen hoppar langt tilbake i undermenyer i settings.» «1 lys på er feilplassert
under Smarthus. Det må vere sidestilt.» Seinare: «Ta vekk 1/6 opp ned ned til høgre.»

- [x] 1. Namn under korta per rad. `RowStyle.cardTitles` (null = globalt val) og eit val
  «Namn under korta» i «Tilpass rada» og Innstillingar → Heim → Rader. Kort, Watch Next og
  radgeometrien les rada sitt val; Appar-sida følgjer framleis det globale valet.
  Prøve først: app-rada utan namn medan ei medierad har titlar. Commit 1184f08.
- [x] 2. Tilbake eitt steg om gongen i Innstillingar. Ein seksjon opna frå Oversikt eller ei
  lenkje går tilbake dit, med fokus på kortet/lenkja ein kom frå. Tilbake frå innhaldet
  flyttar fokus til menyen; Tilbake frå menyen går ut. Undersider beheld sine eigne steg.
  To nye prøver var raude før rettinga. Commit 23e1913.
- [x] 3. Smarthus-samandraget på same linje som tittelen, før knappane, med felles midtlinje.
  Lang tekst får ellipse; lys-knappen beheld storleiken. Prøva var raud før. Commit 3708844.
- [x] Radteljaren «1 / 6 · Opp/ned» er fjerna; plassen under rada er bevart (same commit som 1).
- [ ] 4. Full kontroll: 718/718, 0 skip; lint 0 feil (62 åtvaringar, 1 hint); bygg OK.
  APK `5a4d4fa5…61c6` installert og hashverifisert på emulator-5580 med bilete av alle fire.
  Emulatorprofilen er byteidentisk etter prøvinga. Shield ventar på løyve frå brukaren.

Ingen HA-styring, datatømming eller push. Profildata elles uendra.
