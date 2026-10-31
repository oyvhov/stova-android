# Stova og Spole: logg inn éin gong, framdrift med ein gong og bibliotekfliser

2026-10-30, eigaren valde tre av forslaga til meir Spole i Stova: «2,3,4 .go».

2. **Logg inn éin gong.** Stova får sin eigen Jellyfin-token ved at Spole godkjenner Quick Connect-koden
   Stova viser. Ingen token går mellom appane, og du slepp å skrive koden på ei anna eining.
3. **«Hald fram» med ein gong.** Når du stoppar eller går ut av spelaren, fortel Spole Stova kvar du er.
   Stova rettar radene med ein gong og held rettinga til tenaren har fått det med seg.
4. **Bibliotekfliser rett inn.** Lenkja `spole://library` er alt i Spole (`b4775cf`, med i beta26).
   Det som står att, er å prøve henne frå ei bibliotekflis på ei ekte eining.

## Kontrakten mellom appane

Begge sider skriv namna under, så dei må endrast saman.

- Godkjenning: aktiviteten i Spole har handlinga `app.reelstack.action.APPROVE_SIGN_IN` og blir
  starta med `startActivityForResult`, slik at Spole kan sjå kva app som spør. Stova sender
  `app.reelstack.extra.SERVER_URL`, `app.reelstack.extra.SERVER_ID` (Jellyfin-`Id` frå
  `System/Info/Public`), `app.reelstack.extra.CODE` (Quick Connect-koden) og
  `app.reelstack.extra.FOR_CHILD`. `RESULT_OK` betyr godkjent, og Stova ventar då som før på
  `QuickConnect/Connect`. `RESULT_CANCELED` har `app.reelstack.extra.ERROR`: `not_signed_in`,
  `declined` eller `failed`.
- Framdrift: Spole sender ein eksplisitt kringkasting til `app.stova` og `app.stova.debug` med
  handlinga `app.reelstack.action.PLAYBACK_PROGRESS` og felta `kind` (`jellyfin`/`emby`), `user`,
  `item`, `position_ms`, `duration_ms` og `ended`. Meldinga er ikkje hemmeleg. Ei falsk melding kan
  berre flytte ein framdriftslinje lokalt til neste svar frå tenaren.

## Tryggleik

- Spole godkjenner berre for pakkane `app.stova` og `app.stova.debug`, viser namnet på appen og kontoen,
  og krev PIN-en til Spole når han er sett. Koden må vere 4–10 siffer (Jellyfin gir seks).
- Spole godkjenner berre på ein tenar der han sjølv er logga inn: same adresse eller same `Id`.
- Ein token blir aldri send mellom appane, og ingenting av dette blir logga.

## Steg

- [x] 1. Stova: `SpolePlayback` les meldinga, `MediaRepository.applyPlayback` rettar radene i alle
  profilar med same brukar (fjernar ferdigsette, flyttar resten fremst i «Hald fram»), held rettinga
  over oppfriskingar i to minutt og friskar opp frå tenaren etter nokre sekund. Mottakar i manifestet.
- [x] 2. Stova: «Godkjenn med Spole» i Quick Connect-steget når Spole kan godkjenne. Resultatet blir vist.
- [x] 3. Spole: `LauncherApproveActivity` (kontrakten over), med kontoval, PIN og feil på nynorsk.
- [x] 4. Spole: framdriftsmeldinga frå `JellyfinPlayerModel.report` ved stopp og når spelaren går i bakgrunnen.
- [x] 5. Bygg (Stova-TV-bygg, signert Spole-release lokalt), installer på Loftet med `install -r` og prøv
  alle tre punkta. Ikkje Shield. Framdrift er prøvd med ekte avspeling på emulator-5580, og 2026-10-31 på
  Loftet òg (Toy Story 5 i Pip-verda: stopp i Spole → fremst i «Hald fram» med ny framdrift).
- [x] 7. Funne på Loftet: Tilbake frå eit bibliotek Stova opna, gjekk til biblioteksida i Spole. No går siste
  Tilbake frå rota av biblioteket til Stova, som frå tittel-detaljar (`atLauncherLibraryRoot`).
- [x] 6. HANDOFF, ARCHITECTURE og Spole-dokumentasjonen.
