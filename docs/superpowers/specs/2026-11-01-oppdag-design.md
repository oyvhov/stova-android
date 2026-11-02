# Oppdag i Stova: Seerr gjennom Spole

2026-11-01. Eigaren: «Eg ønsker egentlig implementere oppdag/Seerr inni launcheren, men Stova sitt
designspråk. Lag den skikkelig bra.» Så «Berre start»: vala under er tekne av agenten og står her
som vala hans, ikkje som noko eigaren har godkjent punkt for punkt.

## Kva det er

Ei Oppdag-side i Stova der ein finn nye filmar og seriar (populært no, komande filmar, komande
seriar, søk), ser kva som alt ligg i biblioteket eller er bede om, og ber om det som manglar, med
same lune som resten av Stova: kunsten først, roleg fokus, ingen glas, lime berre for handling.

Spole har alt heile Seerr-delen: innlogginga (API-nøkkel eller personleg økt via Jellyfin),
Oppdag-navet, førespurnadsflyten med sesongar, kvalitet og mappe, status og varsel. Stova lagar
ikkje ein ny Seerr-klient. Spole opnar Oppdag for Stova gjennom launcher-provideren han alt har
(`content://app.reelstack.launcher`), og Stova teiknar.

## Kvifor slik

- Éin stad å logge inn (Spole), som for radene og profilane. Stova lagrar ingen Seerr-løyndom.
- Spole sine reglar gjeld: berre ein personleg Seerr-konto kan be om noko; API-nøkkel gir berre
  oversyn. Varsel og oppfølging av førespurnader held fram i Spole, som før.
- Modulært: utan Spole, utan Seerr i Spole, eller på ein barneprofil gjer Stova ingenting og spør
  ingen.

## Kvar Oppdag finst i Stova

1. **Rad på Heim** («Oppdag»): dei 12 første frå «Populært no» som vanlege mediekort, med ei
   «Oppdag»-flis til slutt som opnar sida. Rada kan flyttast, gøymast og formaterast som andre rader.
   OK på eit kort opnar tittelsida i Stova (ikkje Spole). Langt trykk gir radmenyen utan «Spel av».
2. **Oppdag-sida** (`Destination.DISCOVER`): sidehovud «Oppdag» med ein «Søk»-knapp, eit heroband
   for kortet med fokus (tittel, år, sjangrar, ★, status, samandrag) over ein bakgrunn frå tittelen,
   og radene «Populært no», «Dine førespurnader» (når det finst nokon), «Komande filmar», «Komande
   seriar».
3. **Tittelsida**: bakgrunn, plakat, tittel, fakta, status («I biblioteket», «Ventar på
   godkjenning», «På veg», «Delvis i biblioteket», «Sperra»), tagline og samandrag, handlingane
   «Be om» (film) eller «Vel sesongar» (serie), «Trekk tilbake» (eigen førespurnad som ikkje er
   ferdig) og «Opne i Spole» (alltid; Spole har trailer, rolleliste, personar og dei avanserte
   vala). Under: «Liknande» og «Tilrådd frå denne».
4. **Søk**: tekstfelt for fjernkontrollen, treff i eit rutenett med seks plakatar i breidda,
   «Hent fleire» til slutt.
5. **Appar-sida**: ei «Oppdag»-flis etter Spole-profilane, så sida er nåeleg når rada er gøymd.

Førespurnad: film gir ein kort stadfestingsboks («Send førespurnad»); serie gir sesongvalet med dei
sesongane som kan bli bede om, førehandsvalde (ikkje spesialar), og «Send førespurnad · N sesongar».
Kvalitet og mappe er Spole/Seerr sitt «vanlege val». Svaret kjem som ein kort tekst nedst
(«Sendt til Seerr», «Alt bede om eller i biblioteket», «Logg inn i Spole for å be om som deg sjølv»,
«Kontoen din kan ikkje be om dette», «Seerr svara ikkje»), og sida hentar status på nytt.

## Profilar, alder og tilgang

- Kva Spole-profil ein Stova-profil er: `Profile.spole` (lenkja vald i innstillingane), elles den
  første eigne Jellyfin/Emby-kontoen (`kind`, `userId`). Spole svarer berre for profilar han kjenner.
- Barneprofilar (`Profile.kid`) har ikkje Oppdag nokon stad, og Spole nektar barneprofilane sine.
- Tilgang (`seerr` i svaret frå Spole): `none` (ingen Seerr, eller barn), `browse` (API-nøkkel:
  sjå, men ikkje be om; Stova viser «Logg inn i Spole for å be om titlar»), `personal` (alt).
- Aldersgrensa til profilen gjeld ikkje i Oppdag i denne versjonen: Seerr gir ikkje aldersgrense på
  radene, og Oppdag er berre for vaksne profilar. Dette står som ei kjend avgrensing.

## Kontrakten Spole gir Stova

Alle stiane krev at kallaren er Stova (`launcherMayList`), og `kind`+`user` peikar på Spole-profilen.
Bilete er fulle TMDB-adresser (`https://image.tmdb.org/t/p/w500…` plakat, `w1280` bakgrunn), som
Stova lastar direkte.

| Kall | Svar |
|---|---|
| `discover?kind=&user=` | Rader `trending`, `upcoming_movies`, `upcoming_series` med kolonnane `row, type, tmdb, title, year, overview, poster, backdrop, status, community, genres`. Extras: `seerr` = `none`/`browse`/`personal`. |
| `discover/requests?kind=&user=[&refresh=1]` | `type, tmdb, title, poster, backdrop, stage, percent, seasons, request_id, is4k` for brukaren sine følgde førespurnader. `refresh=1` ber Spole hente status frå Seerr først. |
| `discover/title?kind=&user=&type=&tmdb=` | Éi rad: `type, tmdb, title, year, tagline, overview, poster, backdrop, runtime, genres, community, critic, status, can_request, request_id, request_status, trailer`. |
| `discover/seasons?…&type=tv&tmdb=` | `number, name, episodes, status, air_date`. |
| `discover/related?…&type=&tmdb=` | Rader `similar` og `recommended` med radkolonnane. |
| `discover/search?kind=&user=&q=&page=` | Rad `search` med radkolonnane. Extras: `page`, `has_more`. |
| `call("discover_request", {kind,user,type,tmdb,seasons})` | `result` = `ok`, `already`, `not_personal`, `forbidden`, `failed`, `unknown`. |
| `call("discover_withdraw", {kind,user,request_id})` | `result` = `ok`, `not_personal`, `failed`, `unknown`. |
| `spole://discover?kind=&user=&type=&tmdb=` | Opnar tittelen i Spole sitt Oppdag. |

`status` er Seerr sin mediestatus (1 ukjend, 2 ventar, 3 på veg, 4 delvis, 5 i biblioteket,
6 sperra). `stage` er Spole sitt steg (`requested`, `downloading`, `importing`, `available`,
`declined`, `failed`, `unknown`, `watching`). Kvart svar på ein `discover`-sti har `seerr`-extraen, ikkje
berre `discover`: Stova reknar eit svar som svart berre når han er der. Ein eldre Spole utan desse stiane
svarer med den tomme profilmarkøren utan `seerr`-extra; Stova les det som «ikkje svart» og viser ingenting.

## Ikkje med no

Personsider, sjangrar, trailer i Stova, varsel-bjølla, kvalitet/mappe-val, 4K-utgåve og
aldersgrense i Oppdag. Alt dette finst i Spole, og «Opne i Spole» er vegen dit.

## Mål for kvalitet

- Sida opnar med det Stova har frå før (snapshot i minnet), og hentar nytt stilt.
- Alt kan nåast med fjernkontrollen: ingen kort utan veg inn, Tilbake går eitt steg om gongen
  (tittel → side → Heim).
- Same kortstorleik, titlar under korta, hjørne og fokus som på Heim (`LocalAppearance`).
- Ingen fargelitteralar utanfor `core/theme`; status står i `onSurfaceVariant`, aldri i aksent.
- Testar først, i begge repoa; ekte skjermbilete frå emulatoren før sida blir kalla ferdig.
