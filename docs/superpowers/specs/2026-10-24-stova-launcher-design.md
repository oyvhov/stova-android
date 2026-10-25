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
- **Tema (Tema v1):** Skog (standard), Midnatt, Kino, Plomme, Jul og Halloween er profilstyrte
  stemningar i `core/theme/VisualTheme`. Skog beheld Stova sine dempa verdiar ovanfor; dei andre
  bruker Spole sine background/surface/raised/muted/mutedHigh/outline/outlineHigh/divider-verdiar.
  `StovaTheme` set alle Material-flater og nøytralar, også for dialogar, Smarthus, toppfelt og scrim.
- **Ti aksentar:** Lime, Hav, Iris, Korall, Gull, Mint, Rose, Perle, Kristtorn og Gresskar. Dei åtte
  vanlege er Stova sine dempa avatarfargar; Kristtorn/Gresskar er Spole sine sesongaksentar. Alle
  held minst 7:1 mot bakgrunnen i alle seks tema. Tal, sjangrar og pyntmerke bruker nøytralar.
- **Sesong:** Ingen / **Etter kalenderen** / Jul / Halloween. Kalenderen er Halloween 20.10–1.11
  og jul 1.12–13.1. Han legg Halloween + Gresskar eller Jul + Kristtorn over den lagra stemninga,
  og overlaget blir aldri lagra. Eit anna profilval lagra under sesongen bevarer det underliggjande
  temaet. Datoen blir kontrollert kvart minutt medan appen er synleg, og straks ved retur.
- **Sesongpynt:** Av / Roleg / **Mykje**. Av held berre fargane. Roleg viser eit nøytralt gresskar
  eller snøfnugg ved klokka, ei helsing i tom spotlight og ei lågmeld nedteljing fram til
  Halloween/julaftan (høgst 31 dagar). Mykje legg til 12 glør eller 18 snøfnugg over høgre del av
  kunsten og ein Halloween-edderkopp etter 45 s utan brukaraktivitet. Fast frø, éin Canvas og
  høgst 20 oppdateringar/s; klokka blir berre lesen i teiknefasen. Alt som rører seg stoppar ved
  redusert rørsle, når Heim blir lukka, og når lifecycle går under STARTED. Pynt har ingen
  skjermlesartekst eller fokusmål. Ingen blur eller nye HA-filer.
- **Aksenten er berre handling.** Aksenten blir brukt på val, framdrift og direkte handlingar,
  aldri på tal, typemerke eller kjeldenamn. «Ny sesong» følgjer òg den valde aksenten (Utsjånad v2). Ingen skarpe eller metta fargar.
- **Hjørne (Tilpassing v3):** Skarp 4 / Mjuk 12 / Rund 20 / Ekstra rund 28 dp. Same radius
  går gjennom alle `MaterialTheme.shapes`, kunstkort, appflisar, dialogar, knappar og HA-kort.
  Gamle SHARP/ROUND blir 4/20 dp; gamle felt blir berre haldne for lesekompatibilitet.
- **Typografi:** Spole sin `reelstackTypography()`-skala (sans-serif, 12 nivå), kopiert uendra.
- **Fokus (25.10, eigaren sitt val):** Svakt løft er standard på både kort og menyval:
  2 dp opp, 2,5 % forstørring på kunst / 1,5 % på kontrollar og 10 % lysare kontrollflate.
  Ingen fokusskugge eller glød. Transparent kunst bevarer fargane og får inga ekstra flate.
  Gamalt standardval `RING` blir lese som `LIFT` utan omskriving av profilen. Forstørring,
  Lys opp, Ring og Ring + forstørring kan veljast uttrykkeleg. Ringen er 2/3/4 dp og følgjer
  kunstforma; menyar tvingar ikkje fram ring. Forstørring skjer berre i `graphicsLayer`.
  Lys opp dempar nabokorta 28 % i aktiv rad. Redusert rørsle bevarer lysare flate, utan
  løft eller skalering og utan å leggje til ring. Gamal `strongFocus` bevarer 4 dp som eige tilgjengeval.
  Tekstfelt har lysare fylt flate og nøytral understrek, også i søk og overskrifteditoren.
- **Rørsle (meny-/rørslebestilling 25.10):** `StovaMotion` eig små overgangar på 220 ms,
  inngangar på 340 ms, spotlight-utgang på 420 ms og roleg bakgrunn/spotlight-inngang på 900 ms.
  Fokus er 160/240 ms. Radflytting på Heim og Smarthus bruker 560 ms med mjuk start/stopp og
  kan avbrytast/snuast av nye piler. Ingen innleiande `scrollToItem` ved pilflytting; initial
  plassering og redusert rørsle kan framleis vere omgåande. Berre laganimasjonar for alfa, skala og forskyving; inga ny kontinuerleg rørsle,
  fjør eller storleiksanimasjon. «Reduser rørsle» gir null varigheit og slår av parallax og rotasjon.
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
- Backdrop følgjer spotlighten, ikkje fokus: eitt bilete kvart 8., 12. eller 20. s (12 som standard), ferdig dekoda før det tonar inn.
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
│                               ☁ 4°  135 øre  21:14  ⌂  ▦  ⚙  ◉ │
│  [liten clearlogo / tittel]                                     │
│  [Ny sesong] [★ 7,8] [2024] [12] [🍅 91 %]                       │
│  Film · Drama · Eventyr                                        │
│                                                                 │
│                                                                 │
│                                                                 │
│  Hald fram  Jellyfin                                            │
│  [16:9][16:9][16:9] ▬▬▬ framdrift                              │
│                                                  2 / 5 · Opp/ned│
└────────────────────────────────────────────────────────────────┘
          ▲ heile bakgrunnen = backdrop for spotlight-tittelen + flat scrim
```

- **Radvising (26.10.2026, eigaren sitt val):** Éi heil rad er synleg nedst som standard,
  med uendra bakgrunn og hero over. Opp/Ned byter mellom alle ikkje-tomme rader. Ingen neste
  rad stikk inn i kvile. Utsjånad → Heim og hero tilbyr Éi rad, To rader og Rullande oversikt,
  og plassering Nedst/Litt opp/Heva. Innstillingane er profillagra, òg eldre profilar får éi rad.
  To rader får lik høgd og proporsjonalt tilpassa kunst. Stor skrift (≥ 1,5) eller for trong plass
  får éi heil rad utan å skrive om valet. Header/kort/caption blir målte før kunstskalering.
  Startfokus er framleis eit eige val. Aktiv rad og kort blir bevarte ved modusbyte og retur,
  også når same film ligg i fleire rader. Tomme rader blir hoppa over i radfeltet.
- **Hero = spotlight frå biblioteket** (endra 25.10.2026 etter ønske frå brukaren). Heroen
  følgjer ikkje fokus. Han roterer roleg gjennom vakre filmar og seriar frå heile biblioteket:
  tilfeldige titlar med backdrop (dei med clearlogo først), opptil 30, og eitt nytt utval kvar
  2. time. Seriar der sesong 2 eller seinare har hatt premiere dei siste 45 dagane kjem først,
  merka «Ny sesong». Medan ein sesong gjeld, kjem sesongtitlar med backdrop først, deretter
  det vanlege utvalet; kvar tittel blir vist éin gong per konto. Aldri episodekunst, episodenummer
  eller tenestemerke i heroen.
  - Hero kan vere Av, Kompakt (standard) eller Stor. Av fjernar heroen heilt og stoppar
    spotlight-rotasjon og bakgrunnszoom. Radfeltet held fram nedst; i Rullande oversikt startar
    radene rett under toppfeltet. Spotlight-kjelda kan vere
    heile biblioteket, berre filmar, berre seriar eller favorittar (`UserData.IsFavorite`). Eigne
    bibliotekspørjingar gir eit nytt utval, med sesongprioritering og «Ny sesong» innanfor valet.
  - Innhald (Utsjånad v2): teksten står øvst til venstre rett under toppfeltet. Clearlogo er
    høgst 360 × 96 dp og får transparent kant trimma; utan logo blir tittelen vist i
    `headlineMedium`. «Ny sesong», stjernescore, år, aldersgrense og kritikarprosent står som
    rein tekst utan eigne bakgrunnar, kantar eller rundingar (brukarønske 25.10.2026).
    Numeriske aldersgrenser, også «NO-15», får lokaliserte einingar: «15 år» / «15 years».
    Andre sertifiseringar, som «TV-MA», held originalmerkinga. Kritikarprosenten har raud tomat
    ved minst 60 %, grøn Rotten-markering under 60 %; statusfargane følgjer ikkje profilaksenten.
    Ingen speletid eller omtale som standard.
    Sjangerlinja bruker `Muted`, med typen («Film»/«Serie») først og sjangrar etterpå.
    Berre community-score i 0–10 blir vist; til dømes 39,0 gir inga stjerne.
    Profilen kan velje «Alltid tittel» i staden for clearlogo. Tittelen er òg reserve medan logoen
    lastar og når lasting feilar, slik at heroen aldri mister namnet på filmen eller serien.
    Logoen får plassen som faktisk er att etter metadata, sjangrar og eventuell omtale. Ingen
    fast tekstreserve skal presse han til 24 dp når det finst meir plass. Heile logoen blir vist
    med bevart sideforhold. Herohøgda og radplasseringa blir ikkje endra av logoproporsjonane.
  - I **Rullande oversikt** er heroen ei rullande parallax-overskrift i same liste som radene.
    Når fokus går ned til neste rad, rullar heroen heilt ut av syne og mediaradene får heile høgda
    under topplinja. Opp til første rad viser heroen att. Teksten blir forskyvd 35 % av rullestillinga
    i `graphicsLayer` og tonar ut; redusert rørsle bruker vanleg rulling. Siste rad har nok botnrom
    til å nå toppen. Topplinja blir ståande. Heroen har fast **elementhøgd** for profil- og skriftvala,
    uavhengig av tittel, logo eller omtaletekst; han reserverer ikkje plass når han er rulla bort.
    Fokus til topplinja rullar også til toppen, slik at heroen alltid kan hentast fram med Opp.
    Kompakt og Stor reserverer høvesvis inntil 40 % og 52 % av området under toppfeltet. Ved
    vanleg skrift og avslått omtale blir høgda avgrensa slik at ei heil rad med vald kunststorleik
    og valfri tittel får plass. Påslått omtale reserverer minst 208 dp til heroen, slik at to
    linjer faktisk blir synlege også med tett metadata. Då bruker fallback-tittelen éi linje.
    Sjangerlinja er éi linje med ellipse ved behov. Logoen krympar vidare ved trong plass. Ved skriftstorleik ≥ 1,5 får heroen
    16 prosentpoeng meir høgd; omtalen blir utelaten som før for å prioritere logo og metadata.
  - Kvar tittel står i 12 s som standard (profilval: 8/12/20 s). Neste bakgrunn blir lasta ferdig før han tonar inn over 900 ms,
    så det blir aldri eit svart blink. Valfri roleg zoom 1,00→1,04 over tittelvisinga er Av
    som standard. Animasjonsverdien blir lesen berre i `graphicsLayer`; han stoppar straks ved
    redusert rørsle, under STARTED, eller når Heim blir teken ut av komposisjonen.
  - Rotasjonen tikkar berre medan Heim er synleg, og held fram der han slapp.
    «Bakgrunnsrotasjon» av eller redusert rørsle stoppar han på første tittel.
- **Bakgrunnskjelde:** Spotlight (standard), fast bibliotekstittel eller einsfarga tema. Ein enkel,
  søkbar veljar finn filmar/seriar med backdrop på tvers av profilen sine kontoar. ID og kontotilhøyrsle
  blir lagra i profilen, kunsten i snapshotet. Fråkopla server kan bruke snapshot; fjerna konto
  fjernar biletet. Fast bakgrunn følgjer ikkje spotlight-rotasjonen.
- **Statuslinje** øvst til høgre: klokke (24/12 t/skjult), Normal/Stor og valfri lokal dato;
  HA-brikker, sesongnedteljing, Appar, Smarthus og profilbilete kan visast/skjulast kvar for seg.
  Innstillingar er alltid tilgjengeleg. Smarthus krev framleis oppsett HA.
- **Rader**, i denne standardrekkjefølgja, kvar for seg per konto og teneste (Jellyfin og Emby
  blir aldri blanda):
  1. Favorittappar
  2. Hald fram (resume + neste episode, slått saman innanfor same konto)
  3. Nyleg lagt til · Filmar
  4. Nyleg lagt til · Seriar
  5. Hald fram i andre appar (Watch Next)

  I ein sesong kjem òg «Halloween» (gresskar) eller «Jul» (snøfnugg) per mediekonto. Halloween
  hentar `Tags=halloween` og `Genres=Horror|Skrekk` kvar for seg og slår saman med OR, utan duplikat.
  Jul hentar `Tags=christmas`. Begge bruker rekursive `Items` for Movie/Series, med same
  Jellyfin-/Emby-fallback som andre bibliotekspørjingar. Sesongrada blir lagra i snapshotet,
  merka fråkopla ved feil, og kan sorterast/skjulast som andre rader. Rader frå ein annan sesong
  blir ikkje viste. Det vanlege spotlight-utvalet ligg framleis i snapshotet; sesongprioriteringa
  blir avleidd frå den bufra sesongrada, også utan nett.

  Profilen kan slå av og sortere rader i Innstillingar → Heim. Meny eller lang OK på eit kort
  gir òg seksjonen «Rada»: Flytt rada, Skjul rada og Tilpass rada. Det siste valet opnar
  overskrift, kortformat, storleik og vising i ei eiga underflate; lukking gir fokus tilbake.
  Flyttemodus rammar inn heile rada; opp/ned flyttar live og lagrar `rowOrder`. OK/Tilbake
  avsluttar på det same kortet. Skjul lagrar `hiddenRows` og gir Angre i åtte sekund.
  Angre hugsar rada og kortet sjølv om fokus har flytta seg. Ei tom rad blir ikkje vist.
  `HomePrefs.rowFormats: Map<HomeRowKey, RowStyle>` lagrar valfritt format/storleik og radtittel
  (På som standard). Manglande overstyring følgjer dei globale vala og dagens standard per rad.
  Alle radoverskrifter kan få eigne namn per profil og stabil radnøkkel (høgst 80 teikn),
  frå same editor i langt OK-menyen og Innstillingar → Heim. Tomt felt eller Standardoverskrift
  tek tilbake det omsette standardnamnet. Overskriftstorleik Liten/Normal/Stor og vising av
  servernamn blir lagra i same `RowStyle`; namneendring påverkar ikkje format, storleik eller rekkjefølgje.
  Heroen har eigne visingsval for årstal, aldersgrense, vurderingar og Ny sesong, i tillegg til sjangrar og omtale.
  Heim-innstillingane lenkjer til utsjånad, og Kort og rader lenkjer tilbake til radredigeringa.
  Innstillingar → Heim viser dei same vala og ei formatprøve; flytteknappane kan styrast med D-pad.
  Radoverskrifta er namnet på rada («Hald fram», «Nyleg lagt til · Filmar»), følgt av
  servernamnet i `Muted` (utan tenestelogo), slik at to kontoar kan skiljast.
- **Kunstformat:** filmar i «Nyleg lagt til» bruker plakat (2:3, 132 × 198 dp). Hald fram,
  episodar og seriar bruker brei thumbnail (16:9, 248 × 139,5 dp). Eit episodekort viser
  seriekunst (serie-thumb, elles seriebackdrop) og serienamnet, utan episodenummer. Apprada bruker banner
  (16:9, 248 × 139,5 dp). Desse er middelsstorleikane; Liten og Stor skalerer alle kort
  proporsjonalt ut frå breidd 200 og 296 dp. Ikonstil gir kvadratiske appflisar med side lik
  bannerhøgda. Filmrada kan òg velje breie kort. Ingen kunstige botnkantar på plakatar.
  Titlar under kunst er av som standard, men tittelen er alltid tilgjengeleg for skjermlesar.
  Manglande kunst viser tittelen inne i plasshaldaren. Fokuseffekten følgjer berre kunsten.
  Radformat kan overstyrast til Plakat (2:3), Brei (16:9), Stor brei (1,2×), Liten brei (0,8×)
  eller Ikon (kvadrat). Storleik S/M/L gjeld oppå formatet. Vassrett avstand er 8/16/24 dp.
  Appkort kan visast utan bakgrunnsflate. Val av Projectivy Icon Pack byrjar transparent;
  brukaren kan slå bakgrunnen på att. Breie pakkelogoar bevarer originalt sideforhold.
  Spole-supplementet for Projectivy bruker det originale TV-merket og wordmark-stiane på
  transparent lerret. Det følgjer Stova, er søkbart som `stova_spole` og fungerer for release/debug.
- **Framdrift:** Tynn 2 dp / Tjukk 4 dp (standard) / Skjult i vald aksent for påbyrja titlar.
- **OK** på ein tittel opnar detaljsida i Spole. **Lang OK / Meny** opnar ein liten handlingsmeny:
  «Spel», «Detaljar i Spole».
- **Startfokus** (profilinnstilling): første medierad (standard) eller Favorittappar.

### 5.2 Smarthus

Smarthus v2 (25.10.2026) er ein naturleg del av Stova. Tunet er funksjonsreferansen;
korta brukar Stova sine matte `Surface`-flater, `MaterialTheme.shapes`, typografi og
den felles fokusmarkeringa på kortflata, utan fokusskugge eller glød. Ingen glas eller blur. Ikon står i ei
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
  med plass til heile fokusringen. Hint er synleg med fokus i rutenettet og ligg i den trygge botnmargen utan å
  dekkje eller ta høgd frå korta.
- `CardConfig` lagrar `size`, valfritt `name` og `icon`, `favorite` og `onHome`.
  Gamle JSON-kort med `wide=true` blir M når `size` manglar; eksplisitt `size` vinn.
  `wide` er berre eit kompatibilitetsfelt. Ny redigering brukar `size`.

#### Fjernkontroll og handlingar (bindande, 25.10.2026)

| Tast | Rolle | Gjeld |
|---|---|---|
| Piler | Berre navigasjon i rutenettet. Endrar aldri ei eining. | Alle kort |
| OK | Openberr, trygg og reversibel snøgghandling, sjå nedanfor. | Alle kort |
| Hald OK | Opnar kontrollpanelet («Fleire val»). | Alle kort |
| Meny (≡) | Kortval: storleik, flytt, namn, ikon, Favorittar, Vis på Heim, fjern. | Vaksne, òg i Rediger-modus |
| Spel/pause | Spel/pause på fokusert mediaspelar. | Media |
| Spol fram/tilbake | Neste/førre spor på fokusert mediaspelar. | Media |
| Tilbake | Lukkar panel, val eller Rediger-modus; elles Heim. | Alle |

| Korttype | OK | Kontrollpanel (hald OK) |
|---|---|---|
| Lys | Slå på/av. | Lysstyrke, Varm/Nøytral/Kald, fargeførehandsval når støtta, av/på. |
| Brytar, vifte, input_boolean | Slå på/av. | Av/på, viftefart og retning når støtta. |
| Scene, skript | Køyr; «Køyrt ✓» på kortet i 2 s etter vellukka kall. | Køyr og sist køyrt. |
| Mediaspelar | Spel/pause. | Førre, spel/pause, neste, volum og støtta kjelder. |
| Klima | Opne panelet. | Måltemperatur ±0,5 °C innan min/maks, støtta modus (Varme/Kjøling/Auto/Av m.fl.) og førehandsinnstillingar. |
| Gardin/port (cover) | Opne panelet. | Opne, Stopp, Lukk, posisjon når støtta. Garage/gate krev eiga stadfesting før handling, òg posisjon. |
| Lås | Opne panelet. | Lås med eitt trykk. Lås opp krev eige steg med «Ja, lås opp». |
| Kamera | Fullskjerm, oppdatert kvart 2. s. | Same. |
| Sensor, binærsensor | Opne panelet. | 24 t-graf der numerisk historikk finst, min/maks, status og sist endra. |
| Vêr | Opne panelet. | Timar og dagar. |
| Straumpris | Opne panelet. | Graf: i dag/i morgon, time/15 minutt. |
| Alarm | Berre lesing. | Ingen styring. |

Kontrollpanelet er eit roleg, matt ark med namn, rom og stor hovudverdi.
Opp/ned flyttar mellom kontrollrader; venstre/høgre justerer berre den fokuserte
glidebrytaren. OK aktiverer knappar. Prosentsteg aukar ved repetisjon frå 1 til 5
til 10; klima held 0,5 °C. Verdien blir vist optimistisk, og justeringskallet går
300 ms etter siste tastetrykk. Nye tastetrykk avbryt ikkje kall som alt er sende.
Ved feil kjem den sist mottekne HA-verdien tilbake med ei roleg melding.
Tilbake lukkar panelet og gjenopprettar fokus på same kort.

Snøgghandlingar har optimistisk tilstand og eit lite ventemerke. Doble kall til
same ventande eining blir avviste. Utilgjengelege kort er dempa og sender ingen
kall. Alarm kan aldri styrast. Ein barneprofil utan `kidSmartHomeControl` får
lesepanel både med OK og hald OK. Barn får ingen kortval eller Rediger-modus,
heller ikkje når dei har styringsløyve. Stadfestingar startar med fokus på Avbryt.

Hinten nedst følgjer det fokuserte kortet, til dømes «OK: slå av · Hald OK:
lysstyrke og farge · Meny: tilpass» og «OK: juster temperatur · Hald OK: fleire
val · Meny: tilpass». Han tonar inn ved fokusbyte og ut etter fire sekund.
Han ligg i den trygge botnmargen og tek ingen plass frå korta.

«3 lys på» i oppsummeringa er ein knapp som spør «Slå av alle 3 lys?» med
Ja/Avbryt, berre for dei unike påslåtte lyskorta på dashbordet. Andre verdiar er
informasjon. Rediger i overskrifta slår på Rediger-modus med flytteramme rundt
korta. OK opnar kortval; piler medan OK er halde flyttar same kort innanfor
seksjonen. Tilbake avsluttar. Meny opnar kortval direkte utan Rediger-modus.

Statusbrikkene på Heim opnar Smarthus med sitt eige kort fokusert og sender
aldri direkte handlingar. Vanleg retur til Smarthus hugsar sist fokuserte kort
per profil, òg når det ligg utanfor det første synsfeltet. Heim-skjermen,
heroen og mediekorta blir ikkje endra av denne interaksjonsmodellen.

Einingsveljaren ventar på CONNECTED, viser skjelett under lasting og lastar på nytt ved
ny tilkopling. Fråkopling, lastefeil og tomme søk er ulike tilstandar. Søk treff namn,
einings-ID og rom; flisane er sorterte på rom og namn og viser ikon, tilstand og «Lagt til».
Gruppebrikkene er Alle, Lys, Brytarar, Klima, Sensorar, Vêr, Media, Scener, Kamera,
Straumpris, Gardiner/port og Lås. «Vis alle» inkluderer skjulte, diagnostiske, konfigurasjons-
og deaktiverte registereiningar; deaktiverte einingar utan tilstand blir viste som utilgjengelege.
Pil Opp/Ned følgjer søk/Vis alle → kategoriar → einingsrutenett og tilbake. Ei horisontalt
rulla kategoriliste må aldri svare på Ned med sidelengs fokus. Rutenettet hoppar over
einingar som alt er lagde til; kategori og eining blir hugsa ved retur, og lazy-innhald
blir rulla inn før fokus. Ved lastefeil kan ein nå «Prøv igjen» frå kategoriane.

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
(venstre/høgre i favorittrada), «Skjul», «Appinfo» og «Endre ikon». Skjulte appar ligg i ei eiga, samanfalda
gruppe nedst. Stova sjølv og den innebygde launcheren blir aldri viste.

Lang OK skal opne appmenyen og halde han open når knappen blir sleppt. Repetisjonar frå
opningstrykket skal aldri velje første handling, endre favorittar eller lukke menyen.
Neste nye OK/Enter skal verke straks. Dette blir handtert i det felles dialogvindauget, på
både Heim og Appar, utan ei tidsbasert sperre.

**Ikonpakkar:** Heim og Appar bruker den profilen sin valde Candybar-/Blueprint-pakke, med
Arcticons som tilrådd linjepakke. Appfilter blir lese frå `res/xml/appfilter.xml`, elles
`assets/appfilter.xml`; ugyldige item-oppføringar blir hoppa over utan å miste resten. Dei fem
vanlege temahandlingane blir spurde med `queryIntentActivities` og står i manifestet sine
`queries`. Indeksen blir lagra på disk per pakkenamn, `versionCode` og oppdateringstid; nye,
oppdaterte og avinstallerte pakkar utløyser ny oppdaging. TV-ressursar blir dekoda på IO ved
XXHIGH-tettleik og haldne i ein avgrensa `AppArtCache`. Ingen ikonliste blir dekoda på førehand.

Banner-appstil viser pakkeikon sentrert på Raised-flata eller med «Fyll flata» (kvadratisk kunst
skalert og klipt til 16:9). Ikon-appstil viser same ikon på ei kvadratisk flate. Manglande treff,
manglande drawable eller ein utilgjengeleg pakke fell tilbake til eige banner/ikon. «Ingen» er
standard og bevarer appane si eiga kunst. Stova legg til eit Spole-merke for `app.reelstack` og
`app.reelstack.debug` når pakken ikkje har treff: kvit Arcticons-linjeteikning i monokrome pakkar,
flat Spole-lime på mørk flate i fargepakkar. Utan pakke blir Spole sitt eige banner brukt; det
flate merket er reserve dersom banneret manglar.

Lang OK eller Meny på ein app, både på Heim og i Appar, gir «Endre ikon» → «Frå ikonpakke».
Veljaren har pakkeval, søk og eit D-pad-styrt, late rutenett. Valet lagrar pakkenamn og
drawable-namn per app og profil, og gjeld på begge skjermane. «Tilbakestill» fjernar overstyringa
og bruker automatisk treff igjen. Aktivitetsskifte følgjer dei eksisterande appnøkkelreglane;
avinstallerte appar blir rydda bort. Barneprofilar får ingen appmeny. Ingen automatiske favorittar.


### 5.4 Innstillingar

Sidekolonnen er gruppert som **Heimeskjerm** (Heim, Utsjånad), **Tenester** (Kontoar,
Smarthus) og **Profil og system** (Profil, Home-knappen, Om, Android-innstillingar).
Han er 200 dp brei, med 44 dp minimumshøgd som kan vekse med skriftstorleiken.
Opp/Ned følgjer denne rekkjefølgja. OK/Høgre går inn; Venstre går tilbake frå innhaldet.
Lenkjer mellom sider sikrar fokus på målseksjonen før den gamle kontrollen blir fjerna.

- **Profil:** namn, avatar, PIN, barnemodus, startfokus, «Start i sist brukte profil».
- **Kontoar:** Jellyfin-/Emby-kontoar for profilen (legg til, logg ut).
- **Heim:** rader av/på og rekkjefølgje, bakgrunnsrotasjon av/på.
- **Smarthus:** HA-tilkopling (hushald), kortutval og rekkjefølgje for profilen.
- **Home-knappen:** status for tilgjengetenesta og adb-oppskrifta (sjå 9).
- **Utsjånad:** ei kompakt oversikt opnar åtte eigne sider: Tema, Heim og hero, Kort og rader,
  Bakgrunn, Fokus, Toppfelt, Ikon og Profil. Oversikta har to kolonnar, éi ved skriftstorleik ≥ 1,5.
  Berre den opne kategorien sine kontrollar er i fokustreet. Tilbake/«Alle utsjånadval» gir fokus
  tilbake til den valde kategorien. Ei førehandsvising etter vala viser bibliotekkunst når ho er
  bufra, valt hero/tittel, kortformat, hjørne, fokus og aksent, utan å ta fast plass frå kontrollane.
  Førehandsvisinga tek ikkje fokus. Alle verdiar kan endrast med høgre eller OK; venstre går til menyen;
  opp/ned flyttar mellom vala. Endringane blir viste straks og lagra per profil.
  Temagruppa bevarer fargeprøver og kalenderoverlegg; Ikon bevarer ikonpakke, Original/Aksent
  og Sentrert/Fyll flata. Eit direkte temaval slår av kalenderen. Utilgjengelege pakkar blir hugsa.
  Kvart mål har same streksymbol i sidekolonnen, kategorioversikta og detaljoverskrifta.
  Kategorisida viser éin tittel, opne rader og små konkrete prøver av det valde formatet,
  radvisinga, plasseringa, avstanden og fokuset. Fargane på kategorisymbola er dempa; lime er
  framleis for handlingar. Verktøyknappar er kompakte og veks med skriftstorleiken.
  Profil gir Tilbakestill utsjånad (stadfesting, Avbryt har første fokus) og Kopier frå profil.
  Begge omfattar utsjånad og heimerader; identitet, PIN, appar, kontoar og HA-oppsett blir bevarte.
  Kopiering omset radnøklar til mottakaren sine kontoar på same server. Fast bakgrunn blir vald
  på nytt i mottakaren sitt eige bibliotek. Barneprofilar ser framleis berre «Byt profil»;
  ein vaksen kan endre barnet sine globale utsjånadsval og rader via Profil → barnet → Utsjånad.
- **Om:** versjon og lisensar.

Utsjånadsval (standard i **feit**; alle nye JSON-felt har standardverdiar):

| Gruppe | Val | Verdiar og verknad |
|---|---|---|
| Tema | Tema | **Skog** / Midnatt / Kino / Plomme / Jul / Halloween; alle Material-roller og scrimen |
| Tema | Aksentfarge | **Lime** / Hav / Iris / Korall / Gull / Mint / Rose / Perle / Kristtorn / Gresskar; ≥ 7:1 mot alle temabakgrunnar |
| Tema | Sesong | Ingen / **Etter kalenderen** / Jul / Halloween; kalenderoverlegg blir ikkje lagra |
| Tema | Sesongpynt | Av / Roleg / **Mykje**; rørsle stoppar automatisk ved redusert rørsle og utanfor synleg Heim |
| Heim og hero | Synlege rader | **Éi rad** / To rader / Rullande oversikt; éi heil rad ved stor skrift eller trong plass |
| Heim og hero | Plassering av radene | **Nedst (32)** / Litt opp (48) / Heva (72 dp); gjeld radfeltet |
| Heim og hero | Herostorleik | Av / **Kompakt** / Stor; Av stoppar spotlight-rotasjonen; radfeltet held botnplasseringa |
| Heim og hero | Logo eller tittel | **Logo når han finst** / Alltid tittel |
| Heim og hero | Omtale / sjangrar | Omtale **Av**, sjangrar **På**; to omtale-linjer ved vanleg skrift |
| Heim og hero | Spotlight-tid | 8 / **12** / 20 s |
| Kort og rader | Kortstorleik | Liten (200) / **Middels (248)** / Stor (296 dp brei); proporsjonale plakatar/ikon |
| Kort og rader | Titlar under kort | **Av** / På; alltid skjermlesarnamn |
| Kort og rader | Appstil / filmrad | **Banner** / Ikon; filmrada **Plakat** / Brei |
| Kort og rader | Hjørne | Skarp 4 / Mjuk 12 / **Rund 20** / Ekstra rund 28 dp |
| Kort og rader | Kortavstand | Tett 8 / **Normal 16** / Luftig 24 dp vassrett |
| Kort og rader | Radavstand | Tett 12 / **Normal 22** / Luftig 34 dp loddrett; skjult når berre éi rad blir vist |
| Kort og rader | Framdriftsstrek | Tynn 2 / **Tjukk 4** dp / Skjult |
| Kvar rad | Format | Plakat / Brei / Stor brei / Liten brei / Ikon; **arv eksisterande format** |
| Kvar rad | Storleik / tittel | Liten / Middels / Stor (**arv global**); radtittel **På** / Av |
| Bakgrunn | Kjelde | **Spotlight** / Fast bilete / Einsfarga tema |
| Bakgrunn | Spotlight-kjelde | **Heile biblioteket** / Berre filmar / Berre seriar / Favorittar |
| Bakgrunn | Demping | Lys / **Middels** / Mørk |
| Bakgrunn | Roleg rørsle | **Av** / På; 1,00→1,04; alltid av ved redusert rørsle, hero Av og utanfor Heim |
| Fokus | Effekt | **Svakt løft** / Forstørring / Lys opp / Ramme / Ramme + forstørring |
| Fokus | Ringbreidd | Tynn 2 / **Normal 3** / Tjukk 4 dp |
| Fokus | Reduser rørsle | **Av** / På; ingen skalering, krysstoning eller kontinuerleg rørsle |
| Toppfelt | Klokke | **24 t** / 12 t / Skjult; **Normal** / Stor |
| Toppfelt | Dato | **Av** / På; lokal vekedag, dag og månad, òg med skjult klokke |
| Toppfelt | HA / nedteljing / Appar / Smarthus / profilbilete | Alle **På** / Av; Innstillingar kan alltid nåast |
| Ikon | Pakke / farge / fyll | **Ingen** eller installert pakke; **Original** / Aksent; **Sentrert** / Fyll flata |
| Profil | Tilbakestill / kopier | Stadfesta standard eller kopi frå annan profil, utan konto-/tryggleiksendringar |

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
- **Felt:** `Fields=Overview,ProductionYear,RunTimeTicks,OfficialRating,CommunityRating,CriticRating,Genres,ParentId,SeriesId`,
  `EnableUserData=true`, `EnableImageTypes=Primary,Backdrop,Logo,Thumb`, `ImageTypeLimit=1`, `Limit=20`.
- **Kunst** med tag og storleik: backdrop `Items/{id}/Images/Backdrop/0?maxWidth=1920&quality=80&tag=`,
  logo `Images/Logo?maxWidth=600&tag=`, plakat `Images/Primary?maxHeight=450&tag=`,
  thumb `Images/Thumb?maxWidth=640&tag=`. Episodar arvar backdrop og logo frå serien
  (`ParentBackdropItemId`/`ParentBackdropImageTags`, `ParentLogoItemId`/`ParentLogoImageTag`).
  Thumb til episodar: eiga `Primary` (stillbilete) før serien sin `Thumb`.
- **Score:** `CommunityRating` (berre 0–10, vist med éin desimal og stjerne) og
  `CriticRating` (0–100 %, vist med Spole sitt tomatmerke `ic_tomato`). Aldersgrense frå
  `OfficialRating`. Alt på éi linje, med same merke som Spole (`RatingMarks.kt`).
- **Autentisering:** Jellyfin bruker header `Authorization: MediaBrowser Client="Stova",
  Device="<modell>", DeviceId="<uuid>", Version="<versjon>", Token="<token>"`. Emby bruker same
  verdi i `X-Emby-Authorization`. Bilete går gjennom ein interceptor som legg til tokenet berre for
  verten til kontoen.
- **Hurtigbuffer:** siste vellukka rader per profil i `files/snapshots/{profileId}.json`. Ved
  oppstart blir øyeblikksbiletet vist straks, og nettverket oppdaterer. Feil på nettverket viser
  bufra rader med ein diskret «Fråkopla · Jellyfin» i radoverskrifta.
  Vellukka henting blir brukt om att i fem minutt ved retur til Heim. Dersom minst éin konto
  feilar, får tidsavbrot eller viser fråkopla rader, kan neste retur prøve att etter 30 sekund;
  dette gjeld også når kontoen enno ikkje har eit snapshot. Utgått innlogging krev ny innlogging.
- **Bakgrunn frå media:** backdropen følgjer spotlighten, aldri kortfokus. Neste bilete blir
  dekoda før 900 ms krysstoning. Éin flat Ink-scrim kombinerer horisontal fading bak den korte
  venstre tekstgruppa, vern øvst i 30 % for toppfeltet og fading nedst bak radene. Middels og
  Lys slepper fram mykje meir av høgre halvdel enn tidlegare; Mørk bevarer den førre scrimen.
  Pillane har matt Ink-bakgrunn. Lys kunst skal framleis gi minst 4,5:1 bak hero/logo/metadata
  og toppfelt; dette blir kontrollert i den native skjermbiletharnessen med alle tre dempenivåa.

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
- **Livssyklus:** socketen koplar berre til når appen er starta og det finst synlege Heim-brikker
  eller Smarthus/legg-til-flyten er open. Han fråkoplar 30 s etter `onStop` eller bortfall av behov.
  Heim abonnerer på høgst tre viste brikker; dashbordet på alle profilen sine kort. Ny tilkopling
  med backoff 1, 2, 4, 8, 16, 30, 60, 120 og 300 s (maks). Status blir vist i Smarthus-overskrifta.
- **Delte data:** HA-registeret er bufra i seks timar; historikk i fem minutt og prognosar i 15 minutt,
  delte mellom kort og kontrollpanel. Token blir haldne i minnet etter første dekryptering og
  ugyldiggjorde ved lagring/utlogging. Snapshot blir skrive høgst kvart minutt, utan skriving for
  reine tidsstempelendringar, og forsøkt flusha når Stova går i bakgrunnen.
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
