# Smarthus-vising på Shield

2026-10-26 · konfigurering av installert Stova, utan kjeldekodeendring

Brukaren bad om ei fin Smarthus-vising og eit bilete. Oppsettet er lagra i den
aktive profilen på Shield gjennom dei ordinære kortmenyane.

Samla rutenett med Balansert tettleik og M-kort:

1. Ute: faktisk vêr og femdagarsvarsel, øvst til venstre.
2. Straumpris: faktisk Tibber-verdi og døgnkurve, øvst til høgre.
3. Stova: eksisterande lyskort, nedst til venstre.
4. Varmepumpa: eksisterande klimakort, nedst til høgre.
5. Sonos Lydplanke: eksisterande mediakort i neste rad.

Første skjerm er visuelt kontrollert: heile kort, synlege namn/verdiar,
vêrvarsel og graf, status Tilkopla og ingen opne menyar eller redigeringsrammer.
Sida vart lukka og opna igjen for å kontrollere det lagra oppsettet.

`verification.json` stadfestar at berre dashboardet i aktiv profil er endra.
Dei tre opphavlege kort-ID-ane og einingane er bevarte; to lesekort er lagde til.
Alle andre profilfelt og konto-/hushaldsdata er uendra. Ingen HA-tenestekall,
avspelingskommandoar eller endringar på Home Assistant-serveren er utførte.

Evidens ligg privat og git-ignorert i `.superpowers/smart-home-view/`:
`smarthus-shield.png` (3840×2160), før/etter-profilar, UI-uttrekk og
`verification.json`. Ingen ny bygg-/testkøyring var nødvendig for denne
konfigureringsoppgåva; installert app er uendra frå den verifiserte sliderutgåva.
