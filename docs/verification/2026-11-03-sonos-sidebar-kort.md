# Sonos: romliste som sidemeny og Sonos-kort — 3. november 2026

Plan: [`2026-11-03-sonos-sidebar-kort.md`](../superpowers/plans/2026-11-03-sonos-sidebar-kort.md).

## Automatisk kontroll

Testane vart skrivne først og feila ved kompilering (manglande `ShortcutKey.Sonos`,
`FavoriteTile.Sonos`, `sonosHomeWanted`, `sonosCard`, `LocalSonosCard`, `roomsOpen`).

- `SonosCardTest`: favorittportvakt (vaksen, modul på + HA, lagra snarveg; aldri barn),
  Heim-økta går for kortet med widget/Heim-kunst av (aldri barn/utan HA/modul av), status og kunst
  (spelar/pause/lastar/fråkopla/TV/klar/ingen; TV og fråkopla utan gammal kunst), romval og native
  namn under MA, heile `ShortcutTile` opnar Sonos og langt trykk opnar menyen utan spelehandling,
  feila bilete gir reserveflate, TV-kort ber aldri om bilete, `rememberShortcutOpener` sender
  Sonos-favoritten til sida, og snarvegspanelet tilbyr «Legg til i favorittar».
- `SonosContentTest`: «Gøym rom» gøymer lista og gir spel/pause fokus; Venstre deretter treffer
  ingen fjerna komponent; «Rom · Stova» opnar att med fokus på rommet; romval-callback virkar;
  med lista gøymd står volum, Fleire val og Music Assistant innanfor skjermen; tom/fråkopla side
  har ingen knapp.

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'
```

**1122 testar, 0 feil, 0 hoppa over**; lint 0 feil; debug-APK bygd (ikkje installert, ikkje assembleTv).
`DesignRulesTest` og `ThemePaletteTest` grøne.

## Skjermkontroll

`docs/images/sonos-rooms-hidden.png`: lista gøymd, større omslag, alle daglege kontrollar på éin
skjerm. `docs/images/sonos-card.png`: kortet med musikk, på pause med feila bilete (høgtalar), TV-lyd
(høgtalar, «Stova · TV-lyd») og utan økt (roleg merke). Etter første bilete vart hjørnemerket fjerna
på kortet utan rom, sidan merket elles stod tre gonger. `sonos-playing/grouped/large.png` oppdaterte
med den nye knappen.

## Grenser

- Kortet fylgjer at favoritten er lagra, ikkje om favorittrada er gøymd i Heim-oppsettet; då går
  økta medan Heim er framme utan at kortet blir sett.
- På Appar-sida har kortet inga live-økt og viser ei roleg Sonos-flate.
- Kortet startar Heim-økta på nytt når favoritten blir lagt til; det er same eine abonnement.
- Ikkje prøvd på eining: fokus/rørsle på TV, ekte omslag, TV-lyd og fråkopla rom på anlegget.

## Tilbake i biblioteket (funn frå Loftet)

Funn: rot → Favorites → Tracks, og rot → Favorites → Playlists → spel, så Tilbake: sida hoppa til
Sonos-rota i staden for Favorites (`.superpowers/sonos-sidebar-card-2026-11-03/favorites-back-step.png`).

Årsak, frå primærkjelda `homeassistant/components/sonos/media_browser.py` (HA core, `dev`):
`favorites_payload` og `favorites_folder_payload` returnerer begge `media_content_id=""` og
`media_content_type="favorites"`. `SonosLibrary` bytte ut toppen av stabelen når svaret hadde
same id/type som toppen, så «Tracks»/«Playlists» erstatta «Favorites».

Retting: å opne ei mappe legg alltid eitt nivå på stabelen; berre ei omlasting av noden som alt
ligg øvst (same objekt) erstattar han. Ingen ID-heuristikk.

Testar først (`SonosLibraryNavigationTest`, HA-forma svar): Tilbake-tasten går Tracks → Favorites →
rot → ut; topp-knappen etter spel frå Playlists går til Favorites; ei mislukka mappe som blir prøvd
att, kjem øvst utan å erstatte foreldra. Alle tre feila før rettinga (Favorites var borte etter
Tilbake) og er grøne etter. Målretta `app.stova.sonos.*`, `app.stova.ha.cards.*`, `DesignRulesTest`,
`ThemePaletteTest` med skjermtestar: **187 testar, 0 feil**. `lintDebug` 0 feil, `assembleTv` grønt
(`app/build/outputs/apk/tv/app-tv.apk`). Ikkje installert av Claude.
