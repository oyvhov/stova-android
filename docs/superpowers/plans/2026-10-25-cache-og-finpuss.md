# Cache og finpuss

Brukarbestilling 2026-10-25, berre `polish-cache` i denne worktreen. Tidlegare utsette
server-/Shield- og release-oppgåver er ikkje del av denne bestillinga.
Kvart punkt: raud test → implementasjon → grøn test → éin commit med planoppdatering.

- [x] 1. HA-cache i JsonFileStore under filesDir: aktivt kortutval, nødvendige attributt og
  tidsstempel, aldri token. Vis diskverdiane utan socket; oppdater straks i minne og maks
  éin gong per fem sekund på disk. Test fråkopla oppstart, profilbyte og samling av skrivingar.
  Verifisert: HaCacheTest 2/2, HaRepositoryTest 3/3, DesignRulesTest 3/3.
  Raud: manglande cachegrensesnitt, deretter regresjon for sletta kort. Grøn etter implementasjon.
  Ekstra raud/grøn socketregresjon: eit tomt første svar og eit tomt svar etter ny tilkopling
  erstattar eldre verdiar. HaSocketTest 6/6; samla HA-/designkontroll 14/14.
- [x] 2. Rein URL-veljar: dei tre første titlane per medierad, unike URL-ar, maks tolv totalt.
  Coil-førehandslasting i 1920 × 1080 når radene er klare. Test grenser og duplikat.
  Raud: URL-veljaren mangla. Grøn: BackdropPreloadTest 4/4, BackdropControllerTest og DesignRulesTest.
- [x] 3. Flat Ink → gjennomsiktig-gradient øvst i 30 % av BackdropLayer, bevar eksisterande
  scrim. Raud/grøn pikseltest og visuelt kontrollert skjermbilete med lys sol bak klokka.
  Raud: kontrast 1,03:1. Grøn: krav minst 6:1 ved topplinja; lys testkunst stadfesta under
  gradienten. AppScreensTest.topBarIsReadableOverBrightSun + DesignRulesTest 4/4.
  Visuelt kontrollert: `docs/images/cache-home-bright-backdrop.png` (1920 × 1080).
- [x] 4. Sentrer HA-status vertikalt mot Smarthus-overskrifta. Raud/grøn geometrisk UI-test
  og skjermbilete. Sluttkontroll: testDebugUnitTest lintDebug assembleDebug og separat
  AppScreensTest med '-Pstova.screens=true'. Oppdater HANDOFF; ingen push.
  Raud: begge sentreringstestane feila med native tekstmåling. Grøn med CenterVertically:
  SmartHomeScreenTest 7/7, skjermbiletet og DesignRulesTest medrekna 11/11.
  Visuelt kontrollert: `docs/images/cache-smarthome-aligned.png` (1920 × 1080).
  Sluttkontroll: 264/264 testar, 0 hoppa over, DesignRulesTest 3/3, lint 0 feil (20 åtvaringar,
  1 hint), assembleDebug OK. Alle Gradle-kall via scripts/gradle.ps1.
  Separat AppScreensTest med '-Pstova.screens=true': 12/12 bestått, 0 hoppa over.
