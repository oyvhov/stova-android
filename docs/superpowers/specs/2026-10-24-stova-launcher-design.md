# Stova – Android TV-launcher for Shield · designspesifikasjon

*Arbeidsnamn: **Stova**. Pakkenamn `app.stova`. Namnet kan bytast før første commit (Plan 1, oppgåve 1).*
*Dato: 24. oktober 2026 · Eigar: oyvhov*

## 1. Mål

Ein rask, flat og gjennomført heimeskjerm for NVIDIA Shield (Android TV 11, API 30) som:

1. viser innhaldet frå Jellyfin og Emby med kunst (backdrop, clearlogo, plakat), score og framdrift,
2. opnar alt innhald i **Spole**, som er spelaren,
3. har profilar per person med eigne tenestekontoar, PIN og barnemodus,
4. har eit lite Home Assistant-dashbord i Tunet-stil, med eigne native kort,
5. erstattar Projectivy: apprad med favorittar og skjulte appar, bakgrunn frå media, Home-knappen
   går til Stova, og «Watch Next» frå andre appar.

Stova er ikkje ein mediespelar, ikkje ein Seerr-klient og ikkje eit fullt HA-panel. Spole og Tunet
eig desse rollene.

## 2. Designprinsipp (bindande)

Stova arvar Spole sitt visuelle språk. Spole er fasiten når denne spesifikasjonen ikkje seier noko.
Kjelder: `C:\JellyBin\reelstack-android\docs\DESIGN_SYSTEM.md`, `docs\LAYOUT.md`,
`app\src\main\java\app\reelstack\ui\theme\*`.

- **Ikkje glas.** Ingen `Modifier.blur`, ingen `RenderEffect`, ingen halvgjennomsiktige panel lagde
  over innhald. Det einaste gjennomsiktige laget er éin flat, mørk gradient-scrim over backdropen
  for lesbarheit.
- **Matte, mørke flater og **meir dempa fargar enn Spole** (brukarønske 25.10.2026).** Flatene er
  Spole sin FOREST. Alt oppå er dempa: `Ink #101211` (bakgrunn), `Surface #191C19`,
  `Raised #2E332E`, `Text #E9E9E2`, `Muted #9AA299`, `Primary` (dempa salvie-lime) `#BAC79C`,
  `PrimaryContainer #30372A`, `PrimarySoft #D2D9C2`, `Success #84BE9C`, `Caution #D8B97E`,
  `Warning #D48E8E`, `Outline #646E63`, `Divider #3D443C`. Avatarfargane er Spole sine åtte
  kulørar, desaturerte (sjå `AvatarColor` i `core/theme/Tokens.kt`). Tekst held minst 6:1 mot
  `Surface`.
- **Aksenten er berre handling.** Aksenten blir brukt på val, framdrift og direkte handlingar,
  aldri på tal, typemerke, kjeldenamn eller passiv status. Ingen skarpe eller metta fargar.
- **Hjørne:** fem steg, 4 / 8 / 12 / 16 / 24 dp. Ingen andre radiusar.
- **Typografi:** Spole sin `reelstackTypography()`-skala (sans-serif, 12 nivå), kopiert uendra.
- **Fokus:** 3 dp `onSurface`-strek som tonar inn på 110 ms og følgjer forma til kunsten.
  Fokus endrar aldri storleik eller layout (ingen skalering). Fokus er ikkje val.
- **Rørsle:** 180 ms standardovergang, FastOutSlowIn, ingen fjør eller overshoot. «Reduser
  rørsle» slår av krysstoning og rotasjon.
- **TV-margin:** 48 dp trygg kant (5 % av 1080p).
- **Tekst skal tole skriftstorleik 2,0.** Ingen kontroll har fast høgd som teksten må passe inni.
- **Ingen fargelitteralar utanfor `core/theme`.** Alle flater bruker tokena.
- **Tenestemerke** (Jellyfin, Emby) beheld originalfargane sine.

## 3. Ytelse (bindande)

«Raskt» betyr desse tala, målte på Shield med release-bygg:

| Måling | Krav | Korleis |
|---|---|---|
| Kaldstart til første ramme med bufra heim | ≤ 900 ms median av 10 | `adb shell am start -S -W` → `TotalTime` |
| Home-knapp til teikna heim (varm) | ≤ 300 ms | `am start -W` utan `-S` |
| Hakkande ramer ved D-pad-rulling i 30 s | ≤ 5 % | `dumpsys gfxinfo app.stova` → «Janky frames» |
| Minne (PSS) på heim | ≤ 250 MB | `dumpsys meminfo app.stova` |

Korleis vi held tala:

- Heim blir teikna frå eit lokalt øyeblikksbilete (JSON) før nettverket svarar. Nettverket
  oppdaterer i bakgrunnen.
- Bilete blir alltid bedne om i visningsstorleik (`maxWidth`/`maxHeight` og `tag` frå serveren).
- Backdrop følgjer spotlighten, ikkje fokus: eitt bilete kvart 12. s, ferdig dekoda før det tonar inn.
- Ingen blur, ingen skuggar på kort og ingen alfa-lag over rader.
- Release har R8 og Baseline Profile.
- HA-socketen og nettverksoppdateringane stoppar når Stova ikkje er synleg.
- Få avhengigheiter. Ingen DI-rammeverk, ingen Room og ingen DataStore. Lagring er JSON i
  `AtomicFile`.

## 4. Brukarar og profilar

- **Profil** = namn, avatarfarge, valfri PIN, barnemodus av/på, eigne Jellyfin-/Emby-kontoar,
  eigne appfavorittar og skjulte appar, eigne heimerader og eige HA-kortutval.
- **Hushaldsnivå** (felles for alle profilar): Home Assistant-tilkoplinga og innstillinga for
  Home-knappen.
- **Profilval** ved kaldstart når det finst meir enn éin profil. Innstilling: «Start i sist brukte
  profil». PIN-verna profilar krev PIN.
- **PIN:** 4–8 siffer, PBKDF2-HMAC-SHA256 med 100 000 rundar og 16 byte salt (som i Spole).
  Ventetid etter feil forsøk: 1–3: 0 s, 4: 5 s, 5: 15 s, 6: 30 s, 7: 60 s, 8: 120 s, 9 og fleire: 300 s.
  Teljaren blir nullstilt ved rett PIN.
- **Barnemodus:** ingen innstillingar utover «Byt profil», ingen HA-kort som styrer noko (berre
  visning) med mindre profilen eksplisitt har «Smarthus» på, ingen «Watch Next» frå andre appar,
  og berre appane den vaksne har kvitelista for profilen. Å forlate ein barneprofil krev PIN frå
  ein vaksen profil når ein vaksen profil har PIN.
- Den første profilen blir laga i oppsettet. Han kan ikkje vere barneprofil.

## 5. Skjermar og navigasjon

**Ingen sidemeny** (brukarønske 25.10.2026: «minimalistisk deilig launcher»). Heim er heile
skjermen. Den einaste krommen er ei lita topplinje øvst til høgre: HA-statusbrikker, klokke,
ikonknappar for **Smarthus** (når HA er sett opp), **Appar** og **Innstillingar**, og
profilavataren («Byt profil»). Ikona viser namnet sitt berre når dei har fokus. Ein kjem til
topplinja med opp-pila frå radene. Appar, Smarthus og Innstillingar er eigne fullskjermsider som
Tilbake lukkar. Tilbake på Heim gjer ingenting (launcheren lukkar seg ikkje).

### 5.1 Heim

```
┌────────────────────────────────────────────────────────────────┐
│  [Ny sesong]                   ☁ 4°  135 øre  21:14  ⌂  ▦  ⚙  ◉ │
│  [clearlogo / tittel]                                           │
│  2024 · 1 t 52 min · 12 · ★ 7,8 · 🍅 91 %                        │
│  Tre linjer omtale …                                            │
│                                                                 │
│  Favorittappar   [banner][banner][banner][banner]               │
│  Hald fram  Jellyfin   [16:9][16:9][16:9] ▬▬▬ framdrift          │
│  Hald fram  Emby       [16:9][16:9]                             │
│  Nyleg lagt til · Filmar  Jellyfin  [plakat][plakat]…           │
│  Nyleg lagt til · Seriar  Emby      [16:9]…                     │
│  Hald fram i andre appar (Watch Next) [16:9]…                   │
└────────────────────────────────────────────────────────────────┘
          ▲ heile bakgrunnen = backdrop for spotlight-tittelen + flat scrim
```

- **Hero = spotlight frå biblioteket** (endra 25.10.2026 etter ønske frå brukaren). Heroen
  følgjer ikkje fokus. Han roterer roleg gjennom vakre filmar og seriar frå heile biblioteket:
  tilfeldige titlar med backdrop (dei med clearlogo først), opptil 30, og eitt nytt utval kvar
  2. time. Seriar der sesong 2 eller seinare har hatt premiere dei siste 45 dagane kjem først,
  merka «Ny sesong». Aldri episodekunst, episodenummer eller tenestemerke i heroen.
  - Innhald: «Ny sesong»-merke, clearlogo (fallback: tittel i `displaySmall`), metadatalinje
    (år, og speletid berre for film), score og tre linjer omtale.
  - Kvar tittel står i 12 s. Neste bakgrunn blir lasta ferdig før han tonar inn over 900 ms,
    så det blir aldri eit svart blink. Ingen kontinuerleg rørsle (zoom), så Shield kan kvile.
  - Rotasjonen tikkar berre medan Heim er synleg, og held fram der han slapp.
    «Bakgrunnsrotasjon» av eller redusert rørsle stoppar han på første tittel.
- **Statuslinje** øvst til høgre: klokke og opptil tre HA-statusbrikker (kort merka «Vis på heim»).
- **Rader**, i denne standardrekkjefølgja, kvar for seg per konto og teneste (Jellyfin og Emby
  blir aldri blanda):
  1. Favorittappar
  2. Hald fram (resume + neste episode, slått saman innanfor same konto)
  3. Nyleg lagt til · Filmar
  4. Nyleg lagt til · Seriar
  5. Hald fram i andre appar (Watch Next)

  Profilen kan slå av og sortere rader i Innstillingar → Heim. Ei tom rad blir ikkje vist.
  Radoverskrifta er namnet på rada («Hald fram», «Nyleg lagt til · Filmar»), følgt av
  servernamnet i `Muted` (utan tenestelogo), slik at to kontoar kan skiljast.
- **Kunstformat:** filmar i «Nyleg lagt til» bruker plakat (2:3, 132 × 198 dp). Hald fram,
  episodar og seriar bruker brei thumbnail (16:9, 248 × 139,5 dp). Eit episodekort viser
  seriekunst (serie-thumb, elles seriebackdrop) og serienamnet, utan episodenummer. Apprada bruker banner
  (16:9, 248 × 139,5 dp). Ingen kunstige botnkantar på plakatar.
- **Framdrift:** 4 dp lime-strek i botnen av 16:9-kort for påbyrja titlar.
- **OK** på ein tittel opnar detaljsida i Spole. **Lang OK / Meny** opnar ein liten handlingsmeny:
  «Spel», «Detaljar i Spole».
- **Startfokus** (profilinnstilling): første medierad (standard) eller Favorittappar.

### 5.2 Smarthus

Smarthus v2 (25.10.2026) er ein naturleg del av Stova. Tunet er funksjonsreferansen;
korta brukar Stova sine matte `Surface`-flater, `MaterialTheme.shapes`, typografi og
`focusOutline` berre rundt kortflata. Ingen skalering, glas eller blur. Ikon står i ei
`Raised`-rute. Påslåtte lys har flat glød frå `HaTokens`: 8–22 % etter lysstyrken,
varm som standard og avmetta frå `hs_color`/`rgb_color` i fargemodus. Ikonruta er fylt
med glødfargen; avslåtte lys er nøytrale med dempa ikon. Verdiar, romnamn og passive
statusar har nøytrale tekstfargar.

- Overskrift med tilkoplingsstatus og roleg oppsummering av lys på, innetemperatur og
  straumpris frå korta på dashbordet. «Legg til kort» og «Rediger» ligg til høgre.
- Favorittar kjem først, deretter seksjonar per HA-rom. Kort utan rom ligg under
  «Andre einingar». Rekkefølgja er lagra per profil; flytting held seg innanfor seksjonen.
- Storleikar: **S (1 × 1), M (2 × 1), L (2 × 2)**. Tre kolonnar og 16 dp mellomrom.
  S er om lag 264 × 152 dp; M er to S i breidd med mellomrommet, og L to i begge retningar.
  Ei to-raders pakking let to S-kort stå ved sida av eit L-kort. Radhøgd blir målt frå
  tekstinnhaldet og kan vekse ved stor skrift; S/M har minst 152 dp, L minst 320 dp.
- Alle kort har ikon og namn øvst (`titleMedium`, `onSurface`), med hovudverdi og detalj
  nedst. Tilstandsbrikke kjem berre når ho tilfører informasjon; ingen duplisert På-brikke.
  Rom-undertekst blir berre vist i Favorittar. Fokusert kort blir bede heilt inn i synsfeltet,
  med plass til heile fokusringen. Hint er berre synleg med fokus i rutenettet ved vanleg
  skriftstorleik, i overskriftsfeltet slik at det korkje dekkjer eller tek høgd frå korta.
- `CardConfig` lagrar `size`, valfritt `name` og `icon`, `favorite` og `onHome`.
  Gamle JSON-kort med `wide=true` blir M når `size` manglar; eksplisitt `size` vinn.
  `wide` er berre eit kompatibilitetsfelt. Ny redigering brukar `size`.

| Korttype | Visning | OK | Venstre/høgre på sjølve kortet |
|---|---|---|---|
| Lys | Lysstyrke som hovudverdi når på, elles Av; Varm kvit (<3000 K), Nøytral (3000–4500 K), Kald kvit (>4500 K), eller fargeprikk | av/på | lysstyrke ±10 %, samla etter 250 ms |
| Brytar/vifte/input_boolean | På/av | av/på | vanleg fokusflytting |
| Klima | Noverande temperatur stort, mål med ±-hint ved fokus, modusbrikke med ikon | temperaturdialog | mål ±0,5 °C innan min/maks |
| Sensor | Verdi/eining og 24 t-sparkline | 24 t-graf når numerisk | vanleg fokusflytting |
| Binærsensor | Ord etter device class, t.d. Open/Lukka eller Rørsle/Ingen rørsle | lesing | vanleg fokusflytting |
| Vêr | No og fem dagar med ikon og høg/låg | lesing | vanleg fokusflytting |
| Straumpris | Timesnitt, nivåfarga søyler, noverande time markert, min/maks med klokkeslett | graf med I dag/I morgon og Timesnitt/15 minutt | vanleg fokusflytting |
| Mediaspelar | Kunst fyller venstresida av M/L, tittel/artist til høgre, framdrift, spel/pause/neste og volum | spel/pause | volum ±5 % |
| Scene/skript | Køyr | køyr med kvittering | vanleg fokusflytting |
| Kamera | Stillbilete kvart 10. s medan synleg | fullskjerm, kvart 2. s | vanleg fokusflytting |
| Gardin/port | Tilstand og prosent | opne/lukke/stopp-dialog | posisjon ±10 % når støtta |
| Lås | Låst/ulåst | stadfesting før endring | vanleg fokusflytting |

Meny eller lang OK opnar kortmenyen for vaksne, også utan redigeringsmodus: storleik,
flytt venstre/høgre/opp/ned, namn, ikon, Favorittar, Vis på Heim (maks tre), fjern.
Fokus kjem tilbake til kortet etter menyen. Opp/ned i klima-dialogen navigerer; dei endrar
ikkje temperaturen. Inne i medieknappane navigerer pilene mellom knappane.

Brytarar får optimistisk tilstand straks og eit ventemerke. Ved feil kjem den sist mottatte
HA-tilstanden tilbake med ei roleg melding. Doble kall til same ventande eining blir avviste.
Tenestekall er verna av `canControl`; barneprofilar utan styringsløyve kan framleis lese
sensorhistorikk og kamera, men kan ikkje endre einingar eller kortoppsettet.

Einingsveljaren ventar på CONNECTED, viser skjelett under lasting og lastar på nytt ved
ny tilkopling. Fråkopling, lastefeil og tomme søk er ulike tilstandar. Søk treff namn,
einings-ID og rom; flisane er sorterte på rom og namn og viser ikon, tilstand og «Lagt til».
Gruppebrikkene er Alle, Lys, Brytarar, Klima, Sensorar, Vêr, Media, Scener, Kamera,
Straumpris, Gardiner/port og Lås. «Vis alle» inkluderer skjulte, diagnostiske, konfigurasjons-
og deaktiverte registereiningar; deaktiverte einingar utan tilstand blir viste som utilgjengelege.

Straumgraf og minigraf brukar same dempa nivåfargar: `Success` under nedste tredel av
dagsspennet, `Caution` i midten og `Warning` øvst. Jamne prisar brukar `Caution`.
Noverande intervall får temaaksenten og etiketten «No …». Timesnitt er vekta etter
intervallengda. Normale dagar har 24 søyler; sommar-/vintertid held 23/25 timar og
begge 02-timane, med tidsakse 00/06/12/18/24 plassert etter lokal klokke i HA si tidssone.

Heim-brikkene brukar dei same ikona, overstyringane, temaformene, matte flatene og
lysgløden som korta, utan endringar i sjølve Heim-skjermen.

### 5.3 Appar

Alle installerte TV-appar (`LEANBACK_LAUNCHER`) og vanlege appar med `LAUNCHER`, sorterte
alfabetisk, med Favorittar øvst. Meny på ein app: «Legg til i favorittar / Fjern», «Flytt»
(venstre/høgre i favorittrada), «Skjul», «Appinfo». Skjulte appar ligg i ei eiga, samanfalda
gruppe nedst. Stova sjølv og den innebygde launcheren blir aldri viste.

### 5.4 Innstillingar

Grupper (som i Spole sine innstillingar):

- **Profil:** namn, avatar, PIN, barnemodus, startfokus, «Start i sist brukte profil».
- **Kontoar:** Jellyfin-/Emby-kontoar for profilen (legg til, logg ut).
- **Heim:** rader av/på og rekkjefølgje, bakgrunnsrotasjon av/på.
- **Smarthus:** HA-tilkopling (hushald), kortutval og rekkjefølgje for profilen.
- **Home-knappen:** status for tilgjengetenesta og adb-oppskrifta (sjå 9).
- **Utsjånad:** reduser rørsle, kraftig fokus (4 dp strek).
- **Om:** versjon og lisensar.

## 6. Media: Jellyfin og Emby

- **Pålogging per profil.** Serveradresse ved LAN-oppdaging (UDP 7359, «who is JellyfinServer?» /
  «who is EmbyServer?») eller manuell adresse. `GET System/Info/Public` avgjer om det er Jellyfin
  eller Emby. Jellyfin: Quick Connect (standard) eller brukarnamn/passord. Emby: brukarnamn/passord.
- **Same REST-klient for begge** (som i Spole). Endepunkt blir prøvde i rekkjefølgje, og klienten
  fell tilbake ved 404:
  - Hald fram: Jellyfin `UserItems/Resume?userId=` → `Users/{u}/Items/Resume`; Emby `Users/{u}/Items/Resume`
  - Neste episode: `Shows/NextUp?UserId={u}`
  - Nyleg lagt til: Jellyfin `Items/Latest?userId=` → `Users/{u}/Items/Latest`; Emby `Users/{u}/Items/Latest`
  - Stova hentar aldri enkelttitlar. Det gjer Spole når ein tittel blir opna.
- **Felt:** `Fields=Overview,ProductionYear,RunTimeTicks,OfficialRating,CommunityRating,CriticRating,ParentId,SeriesId`,
  `EnableUserData=true`, `EnableImageTypes=Primary,Backdrop,Logo,Thumb`, `ImageTypeLimit=1`, `Limit=20`.
- **Kunst** med tag og storleik: backdrop `Items/{id}/Images/Backdrop/0?maxWidth=1920&quality=80&tag=`,
  logo `Images/Logo?maxWidth=600&tag=`, plakat `Images/Primary?maxHeight=450&tag=`,
  thumb `Images/Thumb?maxWidth=640&tag=`. Episodar arvar backdrop og logo frå serien
  (`ParentBackdropItemId`/`ParentBackdropImageTags`, `ParentLogoItemId`/`ParentLogoImageTag`).
  Thumb til episodar: eiga `Primary` (stillbilete) før serien sin `Thumb`.
- **Score:** `CommunityRating` (0–10, vist med éin desimal og Spole sitt TMDB-merke) og
  `CriticRating` (0–100 %, vist med Spole sitt tomatmerke `ic_tomato`). Aldersgrense frå
  `OfficialRating`. Alt på éi linje, med same merke som Spole (`RatingMarks.kt`).
- **Autentisering:** Jellyfin bruker header `Authorization: MediaBrowser Client="Stova",
  Device="<modell>", DeviceId="<uuid>", Version="<versjon>", Token="<token>"`. Emby bruker same
  verdi i `X-Emby-Authorization`. Bilete går gjennom ein interceptor som legg til tokenet berre for
  verten til kontoen.
- **Hurtigbuffer:** siste vellukka rader per profil i `files/snapshots/{profileId}.json`. Ved
  oppstart blir øyeblikksbiletet vist straks, og nettverket oppdaterer. Feil på nettverket viser
  bufra rader med ein diskret «Fråkopla · Jellyfin» i radoverskrifta.
- **Bakgrunn frå media:** backdropen følgjer fokusert tittel (forseinking 250 ms, krysstoning
  300 ms). Når fokus ikkje er på ein tittel og «Bakgrunnsrotasjon» er på, roterer Stova gjennom
  backdropane i «Nyleg lagt til» kvart 20. sekund. Scrim: vertikal og horisontal flat gradient frå
  `Ink` (100 %) til `Ink` (0 %), dekkjer venstre 60 % og nedre 55 %.

## 7. Opning i Spole (kontrakt)

Stova opnar Spole med ei implisitt `VIEW`-intent til pakken `app.reelstack` (fallback
`app.reelstack.debug`):

```
spole://open?kind=jellyfin|emby&user=<userId>&item=<itemId>&action=details|play
```

- `item` og `user` må passe `[A-Za-z0-9_-]{1,128}`. Elles avviser Spole lenkja.
- Spole finn tilkoplinga der `kind` og `userId` passar. Om ho tilhøyrer ein annan Spole-profil,
  følgjer Spole sine eigne reglar for profilbyte (PIN og barnemodus). I barnemodus blir berre den
  aktive barnekontoen opna.
- Finst ingen slik tilkopling, opnar Spole heimesida med meldinga «Denne kontoen er ikkje logga inn
  i Spole.».
- `details` hentar tittelen frå serveren om han ikkje alt er lasta, og opnar detaljsida.
  `play` startar avspeling med resume-posisjon.
- Når Stova har opna Spole, er det Spole som styrer navigasjonen. Tilbake går til Spole si eiga
  heimeside, ikkje tilbake til Stova (avgjort av brukaren 25.10.2026). Home-knappen fører til Stova.
- Er Spole ikkje installert, viser Stova ein dialog: «Spole trengst for å spele av. Last ned frå
  github.com/oyvhov/spole-android/releases.»

Endringa i Spole er Plan 3 og blir gjord i Spole-repoet etter Spole sine eigne reglar.

## 8. Home Assistant

- **Tilkopling (hushald):** adresse (`http(s)://…:8123`) og pålogging med **OAuth i WebView** på TV-en:
  `client_id = https://oyvhov.github.io/stova/`, `redirect_uri = https://oyvhov.github.io/stova/auth-callback`
  (same opphav, så HA godtek det utan å hente sida). WebView fangar redirecten før lasting, hentar
  `code` og bytar han mot `access_token` + `refresh_token` på `POST /auth/token`. Fallback:
  «Lim inn langtidstoken». Tokena blir lagra krypterte.
- **Usikra HTTP:** Ei `http://`-adresse er berre lov når verten er ei privat adresse (10/8,
  172.16/12, 192.168/16, 127/8, `*.local`, `*.lan`, `*.home.arpa`). Andre `http://`-adresser blir
  avviste med «Bruk https for adresser utanfor heimenettet.».
- **WebSocket:** `ws(s)://…/api/websocket` → `auth_required` → `auth` → `auth_ok`. Deretter
  `supported_features` med `coalesce_messages: 1`, så `subscribe_entities` med
  `entity_ids` = alle entitetar i korta til den aktive profilen, med komprimerte endringar
  (`a`/`c`/`r`, nøklar `s`/`a`/`lc`/`lu`). Kvar kommando har aukande `id`. Tenestekall med
  `call_service`. Grafar med `history/history_during_period` (`minimal_response`, `no_attributes`).
- **Kamera:** `GET /api/camera_proxy/{entity_id}` med Bearer-token.
- **Livssyklus:** socketen koplar til i `onStart` og fråkoplar 30 s etter `onStop`. Ny tilkopling
  med backoff 1, 2, 4, 8, 16, 30 s (maks). Status blir vist i Smarthus-overskrifta.
- **Utgått access-token** blir fornya med `refresh_token` 60 s før utløp og ved `auth_invalid`.

Smarthus v2 utvidar protokollen utan å endre innlogginga:

- `get_states` gir katalogen; mislukka svar blir viste som lastefeil, ikkje ei tom liste.
  `get_config` gir heimen si tidssone. Tidspunkt med `+00:00` blir lesne med
  `OffsetDateTime`, også på Android 11.
- `config/area_registry/list`, `config/device_registry/list` og
  `config/entity_registry/list` gir rom og namn. Eininga sitt `area_id` går framfor
  apparatet sitt. `hidden_by`, `disabled_by` og `entity_category` styrer standardfilteret.
- Vêr blir henta med `weather.get_forecasts`, `type=daily` og `return_response=true`.
  Parsinga les `result.response[entity_id].forecast`; det gamle forecast-attributtet blir
  ikkje brukt. Vêr blir oppdatert kvart 15. minutt og ved ny tilkopling.
- Nordpool-liknande `raw_today`/`raw_tomorrow` og numeriske `today`/`tomorrow` blir viste
  som prisintervall. Berre prisdata blir klassifiserte som straumpris; eit vilkårleg
  `today`-attributt (t.d. sjøtemperatur) er ikkje nok.
- Tibber-prisar blir henta med lesehandlinga `tibber.get_prices`, utan target og med
  `return_response=true`. Heimar blir ikkje blanda. `start_time`/`price` blir omsette til
  intervall; øre-sensorar får same eining i grafen. Data blir delte i minnet i 15 minutt.
  Dagen og intervalla følgjer HA-tidssona, med støtte for 15 minutt og vintertid.
  Manglande morgondagsprisar får ei eiga melding.
- Sensorhistorikk blir lasta berre for synlege kort, ved ny tilkopling og kvart femte
  minutt. Kamera og alle periodiske kortjobbar stoppar når skjermen blir pausa.
- `HaSnapshot` viser siste verdiar og nødvendige romdata før nettverket svarar. Berre
  einingar frå den aktive profilen sine kort og ei kviteliste av visningsattributt blir
  lagra, maks éi skriving per fem sekund. Token og kamera-/kunst-URL-ar blir aldri lagra
  i øyeblikksbiletet. Historikk og tenesteresponsar blir fylte inn frå nettet.

## 9. Home-knappen og standard-launcher på Shield

- `MainActivity` har `MAIN` + `HOME` + `DEFAULT` (og `LEANBACK_LAUNCHER`, slik at Stova også ligg
  i applista).
- **Tilgjengeteneste** (`HomeButtonService`), same mønster som Glassio:
  1. **Hovudveg, tastefilter:** `FLAG_REQUEST_FILTER_KEY_EVENTS` er sett både i XML og i
     `onServiceConnected()`, fordi nokre TV-bygg ignorerer XML-flagget. `onKeyEvent` slepp alle
     andre tastar straks (`return false`). For `KEYCODE_HOME` når overstyringa er på, noterer
     tenesta tidspunktet ved første `ACTION_DOWN`, startar Stova ved `ACTION_UP` og returnerer
     `true` for både ned og opp. Då blinkar ikkje den innebygde launcheren.
  2. **Fallback, vindaugsendring:** Tenesta lyttar på `TYPE_WINDOW_STATE_CHANGED`, berre frå
     `com.google.android.tvlauncher` (Android TV / Shield) og
     `com.google.android.apps.tv.launcherx` (Google TV), filtrert i XML med
     `android:packageNames`. Ho startar Stova om det ikkje kom ein Home-tast dei siste 1500 ms.
     Dette fangar Home frå mobilfjernkontrollen.
  3. **Debounce:** Stova blir ikkje starta meir enn éin gong per 1000 ms.
  4. **Intent:** `ACTION_MAIN` + `CATEGORY_HOME` mot `MainActivity` med
     `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_REORDER_TO_FRONT | FLAG_ACTIVITY_SINGLE_TOP`.

  Tenesta les ikkje vindaugsinnhald (`canRetrieveWindowContent=false`). Ho lagrar eller loggar
  aldri tastar.
- **Kræsjvakt** (etter Marquise): Stova lagrar tidspunkt for ukontrollerte unntak. Ved tre kræsj
  på fem minutt blir Home-overstyringa slått av til brukaren slår henne på att. Innstillingar viser
  då «Home-knappen går til systemlauncheren fordi Stova kræsja. Slå på att?». Då kjem brukaren
  alltid tilbake til ein fungerande heimeskjerm.
- **Systemlauncheren blir aldri slått av.** Stova skal ikkje køyre eller tilrå
  `pm disable-user` på `com.google.android.tvlauncher`, fordi nokre einingar då ikkje startar.
- **adb-oppskrift** (vist i Innstillingar → Home-knappen):
  - `adb shell cmd package set-home-activity app.stova/.MainActivity` (valfritt, ikkje stadfesta
    på Shield 9.x)
  - Tilgjengetenesta blir berre slått på av brukaren i Android-innstillingane. Stova endrar ikkje
    dette valet ved oppstart, heller ikkje med `WRITE_SECURE_SETTINGS` (småfunn 25.10.2026).

## 10. Appar og Watch Next

- Applista kjem frå `PackageManager.queryIntentActivities` for `LEANBACK_LAUNCHER` og `LAUNCHER`,
  med `<queries>` i manifestet (ikkje `QUERY_ALL_PACKAGES`). Banner med `loadBanner`, elles ikon
  på `Raised`-flate med appnamn. Endringar blir fanga med `LauncherApps.Callback`.
- **Watch Next:** les `TvContract.WatchNextPrograms.CONTENT_URI` via `ContentResolver`. Krev
  runtime-løyvet `android.permission.READ_TV_LISTINGS` (farleg nivå). Då returnerer TvProvider rader
  der `package_name` er Stova eller `searchable=1`. Eigne `selection`-argument blir avviste, så
  spørjinga har ingen `selection`, og all filtrering skjer i koden. Stova spør om løyvet berre
  når brukaren vel «Gi tilgang» i Innstillingar → Heim, aldri automatisk ved oppstart. Eit
  innslag kan berre opne sin eigen app: intenten får `setPackage(packageName)` frå innslaget, og
  ein komponent i ein annan pakke blir avvist. Ved avslag eller `SecurityException` blir rada skjult, og Innstillingar
  viser «Ikkje tilgjengeleg på denne eininga». Innslag frå Spole (`app.reelstack*`) blir filtrerte
  bort fordi medieradene alt viser dei. Rada blir sortert på `LAST_ENGAGEMENT_TIME_UTC_MILLIS`
  (nyaste først), med maks 20 innslag. Opning bruker innslaget sin `INTENT_URI`
  (`Intent.parseUri(…, URI_INTENT_SCHEME)`).

## 11. Tryggleik og personvern

- Tokens (Jellyfin, Emby, HA) ligg krypterte med AES-256-GCM-nøkkel i Android Keystore. Dei blir
  aldri logga eller lagde i øyeblikksbilete.
- Ingen analyse, ingen sentral teneste. Nettverk går berre til serverane brukaren har lagt inn.
- `android:allowBackup="false"`.
- Djuplenkja til Spole inneheld aldri token eller serveradresse.

## 12. Arkitektur

Éin Gradle-modul `:app` (pluss `:baselineprofile` i Plan 5). Manuell avhengigheitsinjeksjon i
`AppContainer` (som Spole sin `container`). Pakkar etter ansvar:

```
app.stova
├─ core/theme      Tokens, StovaTheme, typografi (portert frå Spole)
├─ core/ui         FocusOutline, PinDialog, StovaButton, RemoteTextField, Clock, StovaIcons
├─ core/storage    JsonFileStore (AtomicFile + Mutex + StateFlow)
├─ core/security   SecretStore, KeystoreSecretStore, PinHasher, PinPolicy
├─ profiles        Profile, ProfileRepository, ProfileSession, profilval/-redigering
├─ apps            AppCatalog, AppArrangement, apprad og appside
├─ homebutton      HomeButtonService, StockLaunchers
├─ media           MediaAccount, MediaItem, klient, pålogging, rader, øyeblikksbilete, SpoleHandoff
├─ background      BackdropController, BackdropLayer
├─ watchnext       WatchNextReader
├─ ha              HaAuth, HaSocket, EntityStateDecoder, kort, Smarthus-side, heimestatus
├─ home            HomeScreen, HomeViewModel
├─ settings        Innstillingssider
└─ navigation      StovaApp (rot: innhald + bakgrunn), TopBar (minimal topplinje)
```

**Verktøy** (same versjonar som Spole, slik at Gradle-cachen blir delt): Gradle 9.4.1,
AGP 9.2.1 (innebygd Kotlin), Kotlin 2.3.10 (compose- og serialization-plugin), Compose BOM
2026.06.00, Material3 (Compose), core-ktx 1.17.0, activity-compose 1.13.0, lifecycle 2.10.0,
kotlinx-serialization-json 1.11.0, kotlinx-coroutines 1.10.2, OkHttp 4.12.0, Coil 3.4.0.
Test: JUnit 4.13.2, kotlinx-coroutines-test 1.10.2, Robolectric 4.14.1, MockWebServer 4.12.0,
Compose ui-test-junit4. compileSdk 36, targetSdk 36, **minSdk 30**, Java 17-mål.

**Språk:** `res/values` = nynorsk (standard for alle ikkje-engelske einingar, også bokmål),
`res/values-en` = engelsk. Lint `HardcodedText` er feil.

## 13. Testing og verifisering

- JVM-einingstestar for all logikk (parsing, endepunktval, samanslåing, PIN, dekoding av
  HA-meldingar, apprekkjefølgje, backdrop-timing med virtuell klokke).
- Robolectric for Compose-skjermar som treng vindauge (fokusrekkjefølgje, dialogar).
- `DesignRulesTest` skannar kjeldekoden: ingen `.blur(`, `RenderEffect` eller `Color(0x` utanfor
  `core/theme`.
- Instrumenterte testar på ein **eigen** TV-emulator (`Stova_TV`, API 30, android-tv). Stova blir
  aldri installert som launcher på Spole sine review-emulatorar.
- Manuell verifisering på Shield via `adb connect <shield-ip>:5555`, med sjekkliste per plan.

## 14. Utanfor omfang (v1)

Eigen spelar, Seerr, søk på tvers av tenester, oppdatering i appen, Plex, IPTV/kanalar,
inngangsbyte (HDMI-CEC), widgetar frå andre appar og import av Tunet-profilar.

## 15. Planar

| Plan | Innhald | Repo |
|---|---|---|
| 1 Fundament | Prosjekt, tema, HOME, topplinje, profilar og PIN, appar, Home-knappen | Stova |
| 2 Media | Kontoar, klient, rader, hero, score, bakgrunn, Spole-opning, Watch Next | Stova |
| 3 Spole-lenkje | `spole://open` i Spole | Spole |
| 4 Home Assistant | OAuth, WebSocket, kort, Smarthus, heimestatus | Stova |
| 5 Ytelse og release | Baseline Profile, måling på Shield, signering, oppsettdokument | Stova |

Rekkjefølgje: 1 → 2 → (3 parallelt med 2) → 4 → 5.

## 16. Inspirasjon og kjelder (research 24.10.2026)

| Prosjekt | Kva vi tek med | Kva vi ikkje tek med |
|---|---|---|
| [Glassio](https://github.com/mohammadmrh12/glassio) (Compose, GPL-3.0, 23.10.2026) | Home-tast via tastefilter i tilgjengeteneste, med vindaugsfallback, 1500 ms vindauge og 1000 ms debounce. Applista frå LEANBACK_LAUNCHER først og LAUNCHER etterpå, utan duplikat. Banner lasta ved XXHIGH-tettleik. | Glas/blur, AGSL og Nuvio/Trakt-katalogar |
| [GlassOS TV](https://github.com/AbhishekKaisar/glassos-tv) (Views, GPL-3.0, 22.10.2026) | Rekkjefølgje for oppsett av heim (systemprompt → innstillingar → `set-home-activity`). | Overlegg over systemheimen og Usage Access |
| [Projectivy](https://github.com/spocky/miproja1) 4.72 | Bakgrunn som følgjer fokus, Watch Next per app, oppstartsapp og tilgjengeteneste for overstyring. | Video-/GIF-bakgrunn, ikonpakkar og plugin-API |
| [Marquise](https://github.com/vitoramaral10/marquise) (20.10.2026) | Jellyfin-først med Resume og Next Up, Quick Connect og LAN-oppdaging. **Kræsjvakt** som gir systemheimen tilbake. | Lerret utan Gradle og self-ADB |
| [SygixOs](https://github.com/Sygix/SygixOs) (Compose, 20.09.2026) | Watch Next frå TvProvider med `READ_TV_LISTINGS` og utan duplikat. Token i Keystore. | Haze-blur og Shizuku |
| [Tigris](https://github.com/lesnolie/Tigris) v2.0.6 | «Desktop guard» via tilgjengeteneste. Åtvaring: aldri slå av systemlauncheren. | Generert glas |
| [wallpaparr](https://github.com/iManunator/wallpaparr) | Backdrop med tittel, score og sjåstatus, teikna native i Stova. | Docker-tenesta |
| [house-control-tv](https://github.com/0xProbabillity/house-control-tv), [Klangbild](https://github.com/hazymorning/Klangbild) | HA på TV, HTTP berre på LAN, «nå spelar»-kunst. | Telefon-companion (v1) |

Reddit-tråden (r/jellyfin `1xc8vrs`) kunne ikkje hentast: Reddit blokkerer automatiske
oppslag. Innhaldet er ikkje stadfesta.

Tekniske kjelder: [Jellyfin ItemsController](https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Api/Controllers/ItemsController.cs),
[ImageController](https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Api/Controllers/ImageController.cs),
[Emby REST](https://dev.emby.media/doc/restapi/Images.html),
[HA WebSocket API](https://contributors.home-assistant.io/core/internals/websocket-api/),
[HA auth API](https://contributors.home-assistant.io/core/internals/auth-api/),
[TvProvider android11](https://android.googlesource.com/platform/packages/providers/TvProvider/+/refs/heads/android11-release/src/com/android/providers/tv/TvProvider.java).
