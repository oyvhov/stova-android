# Spole-rader for personen og handlingar på korta

Bestilling frå eigaren 1. november 2026: «Ta 1 og 2 først. Du kan teste på Loftet.»

1. Ein Stova-profil som følgjer ein Spole-person, og som ikkje har eigne kontoar, får radene til personen
   frå Spole: «Hald fram» (halvsett og neste episode), nye filmar og nye episodar. Eilev får då radene sine
   utan Emby-passord i Stova.
2. Langt trykk på eit media-kort gir «Spel frå starten», «Merk som sett» og «Fjern frå Hald fram». Spole
   gjer handlinga med sin eigen innlogging, så begge appane er samde etterpå.

Ingen innlogging kryssar mellom appane: Spole hentar og handlar, Stova får metadata og bildeadresser.

## Spole (`C:\JellyBin\reelstack-android`)

- [x] `MediaSyncRepository.launcherRows(connection)`: éin konto sin `feed`, med same reserveadresse som
      Heim, til «Hald fram» (`combinedWatching(resume, nextUp)`), nye filmar og nye episodar. Rører ikkje
      Spole sitt eige lagra sett med rader (det finst berre eitt, for den aktive profilen).
- [x] `LauncherProfilesProvider.query(/rows?kind=&user=)`: berre for Stova; personen blir funnen som i
      `select_profile`, og kvar Jellyfin/Emby-konto i profilen gir rader. Kolonnar utan token.
- [x] `call("item_action", kind, user, item, action)`: `watched` og `remove_resume` med kontoen til
      personen; fast svarord (`ok`/`failed`/`unknown`).
- [x] `spole://open?…&action=play&start=0`: spel frå starten utan spørsmål om å halde fram.
- [x] Testar, `docs/LAUNCHER_LINK.md`.

## Stova (`C:\Launcher`)

- [x] `SpoleProfiles.rows` og `SpoleProfiles.itemAction`; `SpoleContract.uri(..., fromStart)`.
- [x] `SpoleRowSource`: rader berre for ein profil med Spole-person og utan kontoar; spør aldri når Spole
      manglar. Nøklar `spole_continue:`, `spole_latest_movies:`, `spole_latest_series:`. Oppdatering ved
      retur til Heim, etter ei handling og når Spole melder framdrift.
- [x] `ItemActionsMenu`: dei tre handlingane; rada blir oppdatert etterpå.
- [x] Testar, `docs/ARCHITECTURE.md`.

## Prøv

- [x] Loftet: Spole og Stova (TV-bygg). Ein profil utan kontoar som følgjer Spole-personen får rader;
      handlingane på eit kort eigaren vel. Radene og opninga i Spole er prøvde; menyen viser handlingane.
- [x] Ei handling utført på ein ekte tittel eigaren vel: «Merk som sett» på Emil i Lønneberget S1 E1 (4 min att) på Øyvind-profilen på Loftet. «Merkt som sett», og Emil forsvann frå «Hald fram» med ein gong. «Fjern frå Hald fram» og «Spel frå starten» er ikkje prøvde på eining.
