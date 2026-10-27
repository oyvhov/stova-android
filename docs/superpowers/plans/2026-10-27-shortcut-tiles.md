# Snarvegar i favorittrada: bibliotek og Home Assistant

Bestilling (2026-10-27): «Hadde det vore ein ide og kunne legge inn bibliotek i Jellyfin som
"apper"? … Tenker på måter å integrere Spole og Home Assistant i Stova.» Etter samtalen:
- Trykk på ei biblioteksflis opnar Spole på biblioteket. Stova lagar ikkje si eiga biblioteksside.
- «Eg ønsker ikkje å "ødelegge" heroen»: flisene endrar ikkje heroen.
- «Det bør jo vere bildet som biblioteket har i Spole (Jellyfin) og lag klart for Home Assistant og.»
- «Go go go» på denne skildringa: biblioteksfliser og scene/skript frå Home Assistant som same slags
  flis som appane; flytt, fjern og same meny; lagt til frå Appar-sida i eigne grupper.

Mine val (kan endrast):
- Favorittane er framleis éi liste med nøklar. Snarvegar har eigne nøklar som aldri liknar
  app-nøklar (`library:<konto>:<bibliotek>`, `ha:<entitet>`), og namn og bilete blir lagra i
  `AppPrefs.shortcuts` når dei blir lagde til. Heim viser dei utan nettverk; Appar-sida
  oppdaterer namn og bilete når ho hentar biblioteka på nytt.
- Biblioteksflisa finst berre medan kontoen finst. Ei Home Assistant-flis finst berre når
  Home Assistant er sett opp. Barneprofilar får ingen snarvegar no.
- Trykk på ein scene: Stova held tilkoplinga til Home Assistant berre medan kallet går.
- Spole: ny lenkje `spole://library?kind=&user=&library=` (brief i
  `docs/briefs/2026-10-27-spole-bibliotekslenkje.md`). Til ho finst, opnar Spole startsida si.

Reglar som før: modulært, test først, `DesignRulesTest` grøn, nynorsk/engelsk tekst, Shield berre
når brukaren seier ja.

- [x] 1. Nøklar og lagring: `apps/Shortcut.kt` (nøklar, `AppPrefs.shortcuts`). Opprydding og
  omdøyping av appar rører ikkje snarvegar; å fjerne ein favoritt fjernar òg namn og bilete.
- [x] 2. Flisene i rada: `favoriteTiles(...)` set saman appar og snarvegar i favorittrekkjefølgja;
  `AppsRowSource` gøymer bibliotek utan konto og Home Assistant utan oppsett.
- [x] 3. Teikning og trykk: `ShortcutTile` med same mål, fokus, pynt og namn som `AppTile`;
  bibliotek opnar Spole (`SpoleHandoff.openLibrary`), scenar køyrer i Home Assistant.
- [x] 4. Hald inne OK: same panel som for appar med Opne, Fjern frå favorittar og Flytt.
- [x] 5. Appar-sida: gruppene «Bibliotek» (bilete frå Jellyfin/Emby) og «Scenar og skript», med
  Legg til i favorittar.
- [x] 6. Full kontroll, emulatorbilete, ARCHITECTURE og HANDOFF.
- [x] 7. Tillegg (brukaren: «Home assistant ikonet», «Pass på at ikke heile pakken blir lasta inn
  unødvendig»): sceneflisa viser ikonet Home Assistant viser, frå registeret, attributtet eller
  domenet. Berre fonten, CSS-en (til namnelista) og lisensen frå `@mdi/font` 7.4.47 er lasta ned;
  fonten blir lasta først når ei flis teiknar eit ikon, og namnelista blir lesen per namn.
