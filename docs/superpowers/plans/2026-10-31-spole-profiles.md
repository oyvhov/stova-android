# Spole-profilar: eigen profil for Eilev, og ei Spole-flis med profilbiletet

2026-10-31. Eigaren vil ha ei ekstra Spole-flis på framsida for ein annan profil, her Emby-kontoen til
Eilev, med profilbiletet til brukaren på flisa. Trykk på ho skal vise berre biblioteket til Eilev, i
vanlege Spole (ikkje barneutgåva), låst til nokon byter profil oppe i hjørnet i Spole. I dag er Eilev
berre Emby-kontoen i hovudprofilen til Spole, så han finst ikkje i hjørnet. Ordrett: «Vanlig Spole.
Go go go ikkje spør meir». Spole og Stova skal vevast saman på mange måtar.

## Kontrakten mellom appane

Begge sider skriv namna under, så dei må endrast saman.

- Lenkje: `spole://profile?kind=jellyfin|emby&user=<id>` opnar Spole som profilen som har den kontoen.
  Ein eigen profil (vaksen eller barn) vinn over hovudprofilen; finst kontoen berre i hovudprofilen,
  blir det hovudprofilen. Spole byter profil og går til Heim. Frå barnemodus byter ikkje ei lenkje
  profil (som før). Same ID-krav som `spole://open`.
- Profilliste: Spole har ein eksportert `ContentProvider` med autoriteten `<pakke>.launcher`
  (`app.reelstack.launcher`, `app.reelstack.debug.launcher`). `content://<autoritet>/profiles` gir éi
  rad per profil med ein innlogga Jellyfin- eller Emby-konto: `id`, `name`, `kind`
  (`jellyfin`/`emby`), `user`, `avatar` (adressa til brukarbiletet på tenaren, utan token) og `kid`
  (0/1). Hovudprofilen gir kontoen han vil opne med (Jellyfin før Emby). Berre `app.stova` og
  `app.stova.debug` får svar; andre får ingen rader. Ingen token, passord eller adresse til anna enn
  biletet går over.

## Spole

- Ein profil som ikkje er hovudprofilen, kan vere vaksen: same lagring som barneprofilar, med flagget
  `kid.<id>.adult`. Barnemodus er «den aktive profilen er ein barneprofil», ikkje «ikkje hovudprofilen».
- «Eigen profil for Eilev» under Tenestene dine → Profilar, for kvar Jellyfin-/Emby-konto i
  hovudprofilen som ikkje er eigaren av hovudprofilen og ikkje alt har ein profil. Kontoen blir kopiert
  inn i den nye profilen (ingen ny innlogging); hovudprofilen held fram med alt.
- Frå barnemodus til ein vaksen profil krev PIN-en, som til hovudprofilen. Frå ein vaksen profil til
  hovudprofilen krev ingenting (eigaren: «låst til han skiftar profil oppe i hjørnet»).

## Stova

- `FavoriteTile.SpoleProfile` med nøkkelen `spole:<kind>:<user>`; Apps-sida viser «Spole-profilar»
  frå profillista når Spole er installert, og flisa kan leggjast i favorittane som bibliotekflisene.
- Flisa viser Spole-banneret med profilbiletet i ein sirkel, og namnet. Trykk opnar `spole://profile`.
- Ingen spørjing til Spole når Spole ikkje er installert, og berre når Apps-sida blir vist.

## Steg

- [x] 1. Spole: vaksen profil i `ConnectionRepository` og `SessionScope`, PIN-regelen, test først.
- [x] 2. Spole: «Eigen profil for …» i Tenestene dine, med tekst på nynorsk, bokmål og engelsk.
- [x] 3. Spole: `spole://profile` (tolking, mål, ruting), test først.
- [x] 4. Spole: profillista for Stova (`LauncherProfilesProvider`), test først. `docs/LAUNCHER_LINK.md`.
- [x] 5. Stova: lesar for profillista, `FavoriteTile.SpoleProfile`, flis, Apps-sida, opning, test først.
- [x] 6. Bygg, installer på emulator-5580 og Loftet (`install -r`), prøv heile vegen. Ikkje Shield.
- [x] 7. HANDOFF, ARCHITECTURE og planoversikta i begge repo.

## Utfall

- Spole `e060eb8` og `471d7c1` på greina `launcher-profiles` (bygd på `launcher-bridge`, ikkje fletta, ikkje pusha).
- Funne på Loftet: lenkjefilteret i manifestet til Spole mangla verten `profile`, så lenkja vart opna som
  vanleg Spole (`OpenedWithoutLink`). Retta, med test for alle tre vertane.
- Funne undervegs: hovudprofilen kunne få namnet og biletet til Eilev (Emby-kontoen vart lagra sist).
  Ein konto med eigen profil namngir ikkje lenger hovudprofilen.
- Prøvd: emulator-5580 (Spole debug, Stova debug) og Loftet (Spole release `install -r`, Stova `-Tv`):
  «Spole-profilar» på Apps-sida med Spole-banneret og profilbiletet, og flisa → `spole://profile` →
  `LauncherOpenActivity` → Spole på Heim. Profillista gir ingen rader til `adb shell`.
- Ikkje prøvd på ei eining: «Eigen profil for Eilev». Spole på Loftet har ikkje Emby-kontoen til Eilev, og
  Shield er ikkje rørd. Flyten er dekt av einings- og UI-testar.
