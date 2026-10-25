# Cache og finpuss

Brukarbestilling 2026-10-25, berre `polish-cache` i denne worktreen. Tidlegare utsette
server-/Shield- og release-oppgåver er ikkje del av denne bestillinga.
Kvart punkt: raud test → implementasjon → grøn test → éin commit med planoppdatering.

- [x] 1. HA-cache i JsonFileStore under filesDir: aktivt kortutval, nødvendige attributt og
  tidsstempel, aldri token. Vis diskverdiane utan socket; oppdater straks i minne og maks
  éin gong per fem sekund på disk. Test fråkopla oppstart, profilbyte og samling av skrivingar.
  Verifisert: HaCacheTest 2/2, HaRepositoryTest 3/3, DesignRulesTest 3/3.
  Raud: manglande cachegrensesnitt, deretter regresjon for sletta kort. Grøn etter implementasjon.
- [x] 2. Rein URL-veljar: dei tre første titlane per medierad, unike URL-ar, maks tolv totalt.
  Coil-førehandslasting i 1920 × 1080 når radene er klare. Test grenser og duplikat.
  Raud: URL-veljaren mangla. Grøn: BackdropPreloadTest 4/4, BackdropControllerTest og DesignRulesTest.
- [ ] 3. Flat Ink → gjennomsiktig-gradient øvst i 30 % av BackdropLayer, bevar eksisterande
  scrim. Raud/grøn pikseltest og visuelt kontrollert skjermbilete med lys sol bak klokka.
- [ ] 4. Sentrer HA-status vertikalt mot Smarthus-overskrifta. Raud/grøn geometrisk UI-test
  og skjermbilete. Sluttkontroll: testDebugUnitTest lintDebug assembleDebug og separat
  AppScreensTest med '-Pstova.screens=true'. Oppdater HANDOFF; ingen push.
