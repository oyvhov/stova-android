# Loftet: favorittappar og eige smarthusoppsett

2026-10-29. Konfigurasjon på eigaren sin Google TV Streamer, med Shield som lesereferanse. Ingen appkode eller APK for Stova er endra.

## Avklart omfang

Eigaren presiserte at berre appane som var **favorittar på Shield**, skulle vere installerte på Loftet; alle andre appar skulle fjernast. Smarthusdelen skulle ha same design, men berre einingar i Home Assistant-rommet **Loftet**.

Den første tolkinga om å kopiere alle Shield-appane var for vid. Etter presiseringa vart 25 andre brukarappar avinstallerte, også dei tre tidlegare installerte Disney+, Netflix og Prime Video. Play Filmer, Play Spill og Galleri vart fjerna for Android-brukar 0. Systemlauncheren, Play Butikk og Android-innstillingane er bevarte.

## Endelege appar og favorittrekkjefølgje

1. Spole — `app.reelstack`
2. NRK TV — `no.nrk.tv`
3. YouTube — `com.google.android.youtube.tv`
4. Spotify — `com.spotify.tv.android`
5. Wholphin — `com.github.damontecres.wholphin`
6. YouTube Kids — `com.google.android.youtube.tvkids`
7. TV 2 Play — `no.tv2.sumo`

`pm list packages -3` er samanlikna med denne tillatlista pluss `app.stova.debug`: nøyaktig åtte pakkar, ingen ekstra. Alle sju appane har ein ekte TV-startaktivitet. NRK TV og TV 2 Play viste seg å vere arkiverte Android-plasshaldarar; begge er gjenoppretta gjennom Play Butikk og har fått sine eigentlege aktivitetar att. Favorittrekkjefølgja er kontrollert i Stova etter gjenopprettinga.

## Utsjånad og smarthus

Heim startar på favorittappane. Halloween/Max, gresskaraksent, tynn ring med forstørring, breie favorittkort i liten storleik, skjult radtittel og kortnamn, 36 prosent appkortbakgrunn og 31 prosent bakgrunnsdemping følgjer Shield-oppsettet. Ikonpakken vart fjerna saman med dei andre appane etter presiseringa; appane nyttar eigne bilete. Mediekontoar og bibliotekval er bevarte.

Samla, kompakt rutenett med desse sju korta, alle frå rommet Loftet:

| Rekkja | Kort | Storleik | På Heim |
| --- | --- | --- | --- |
| 1 | Loftet (lysgruppe) | M | Ikon og verdi |
| 2 | Loftet Golv | M | Ikon og verdi |
| 3 | Golvlampe Loftet | S | Nei |
| 4 | Loftet Nest | M | Berre ikon |
| 5 | Loftet Taklampe | S | Nei |
| 6 | Lyskastar Loftet | S | Nei |
| 7 | Lysstripe Kafê Eilev | S | Nei |

Romforslaget hadde to Nest-oppføringar med same visingsnamn og tilstand; den andre vart fjerna frå dashbordet. Ingen HA-eining vart sletta. Nest sender tittelen «Not Provided», så heimbrikka bruker berre ikon. Golvvarme fekk M-storleik for å unngå at desimaltalet bryt over to linjer.

## Kontroll

- Endeleg pakkeliste og TV-aktivitet for kvar favoritt er kontrollerte med ADB.
- Favorittrekkjefølgje, heimlayout, romkort og golvvarmepanel er kontrollerte med ekte skjermbilete frå Loftet.
- Fjernkontrollnavigasjon og Tilbake frå golvvarmepanelet er prøvde utan å endre temperatur, lys eller avspeling. Termostatmålet var framleis 16,5 grader, Nest-volumet 20 prosent og lysa av ved kontrollen.
- Langt OK med `Send-TvRemote.ps1 hold:1500` opna menyen for YouTube Kids, og Tilbake gav same favorittkort. Heim vart returnert til Spole etter kontrollen.
- Stova brukar framleis TV-bygget frå førre økt; tilgjengetenesta for Heim-knappen er aktiv. Ingen `pm clear`, avinstallering av Stova, `am force-stop` eller deaktivering av systemlauncheren.
- Innlogging og avspeling i dei nyinstallerte tredjepartsappane er ikkje prøvde. Ingen nye Gradle-testar: dette er berre einingskonfigurasjon og dokumentasjon.
- Begge TV-einingane er tilbake i dvale (`Asleep`), og Heim-tenesta er aktiv på begge. Ingen installering står i kø. Shield-oppsettet er uendra.

Lokale kontrollbilete og pakkelister ligg i `.superpowers/loft-sync-2026-10-29/`. Desse er git-ignorerte. Ingen push eller publisering.
