# Testplan: Stova og Spole på TV-ane

2026-11-01. Eigaren bad om dette: «Du skal installere begge appene på Shield og teste alt», «Gjer full ytelsetest på begge appane etterpå. At alle animasjoner er fine og at alt er logisk i appen» og «Lag ein testplan på alt i appane».

Einingar:
- **Shield** (192.168.10.182:5555, Android 11, 4K).
- **Loftet** (Google TV Streamer, 192.168.10.125:5555, Android 14, PowerVR).

Byggja:
- **Stova:** TV-bygget, `app.stova.debug`, `Install-Stova.ps1 -Tv`.
- **Spole:** greina `launcher-rows`, release-signert. Versjonskoden er den same som beta29, så bygget er ikkje gitt ut.

Reglar under testen:
- Les fokus før kvar OK. Eit Smarthus-kort med lys eller ein knapp med «Logg ut» skal aldri få OK ved ein feil.
- Ekte lys: berre «Taklampe Stova» og «Vindu Stova», som eigaren har godkjent. Dei skal vere av når testen er ferdig.
- Ingen force-stop av Stova, ingen nye kontoar og ingen passord.
- Begge TV-ane skal stå slik dei stod før. Shield står på Stova-heimen. Loftet søv på Øyvind.

Status: ✅ fungerer, ⚠️ fungerer med merknad, ❌ feil (retta eller ikkje), – ikkje testa.

## A. Installering og oppstart

| ID | Kva | Forventa | Shield | Loftet |
|---|---|---|---|---|
| A1 | Installer Stova-TV og Spole (launcher-rows) | Begge installerte utan å slette data; Spole-innlogginga er der | – | – |
| A2 | Home-knappen etter ominstallering | Stova kjem fram; Heim-tenesta er bunden | – | – |
| A3 | Kaldstart av Stova (`am start -W`) | Under 1,5 s på Shield, under 2,5 s på Loftet | – | – |
| A4 | Kaldstart av Spole | Heim med rader utan raude feil; ingen krasj i logcat | – | – |

## B. Heim i Stova

| ID | Kva | Forventa | Shield | Loftet |
|---|---|---|---|---|
| B1 | Hero på Øyvind | Kunst, logo, fakta, ratings; byter tittel jamt | – | – |
| B2 | Hero på Eilev (Spole-profil utan konto) | Kunst frå Spole-radene, innanfor aldersgrensa (15) | – | – |
| B3 | Rader frå Spole for Eilev | «Hald fram», «Nyleg lagt til» med bilete | – | – |
| B4 | Fokus i ei rad | Berre kortet med fokus endrar seg; resten av rada er ikkje dempa | – | – |
| B5 | Opp/ned mellom rader | Fokus går til rett kort i neste rad, ingen hopp langt bort | – | – |
| B6 | Venstre/høgre i rada | Jamn rulling, fokus held seg synleg | – | – |
| B7 | Langt trykk på app-kort | Sidepanel kjem inn; Tilbake tonar det bort utan svart rand | – | – |
| B8 | Langt trykk på mediekort | Spel frå starten / Merk som sett / Fjern frå Hald fram (Spole gjer handlinga) | – | – |
| B9 | Toppfeltet | Brikker les rett; OK på profil opnar profilval | – | – |
| B10 | Sesongpynt (Halloween) | Pynt utan å dekkje tekst | – | – |
| B11 | Tilbake og Heim frå appar | Heim kjem att med fokus der det var | – | – |

## C. Appar-sida

| ID | Kva | Forventa | Shield | Loftet |
|---|---|---|---|---|
| C1 | Rekkjefølgje | Favorittar → Spole-profilar → Alle appar → Bibliotek → Scenar → Skjulte | – | – |
| C2 | Fire per rad | Fire banner per rad, jamne mellomrom | – | – |
| C3 | Langt trykk på app | Meny med flytt, skjul, ikon, favoritt; lukkar utan rand | – | – |
| C4 | Spole-profil-flis | Opnar Spole som den personen | – | – |

## D. Smarthus

| ID | Kva | Forventa | Shield | Loftet |
|---|---|---|---|---|
| D1 | Smarthus-sida opnar | Kort med rett tilstand innan 2 s | – | – |
| D2 | Lyspanel (langt trykk) | Ned frå lysstyrken når «Slå på», så «Farge og kvittone» | – | – |
| D3 | OK på lyskort (godkjende lys) | Lyset kjem på, også ei pære som ikkje svarar på «på» utan nivå | – | – |
| D4 | Mediepanel (Sonos) | Ned frå volumet når «Pause», så «Kjelde» | – | – |
| D5 | Legg til / fjern kort | Kort kjem og går; dashbordet elles urørt | – | – |
| D6 | Popupar lukkar | Ingen svart rand; berre toning | – | – |

## E. Innstillingar

| ID | Kva | Forventa | Shield | Loftet |
|---|---|---|---|---|
| E1 | Alle sider opnar | Heim og utsjånad, Smarthus og kontoar, Profilar, Appar, System | – | – |
| E2 | Kino-modus | Brytar, lysliste med nivå per lys, lagring | – | – |
| E3 | Profilar | Liste, Spole-profil-val, bilete frå Spole | – | – |
| E4 | Tilbake frå undersider | Fokus tilbake til raden som opna | – | – |

## F. Spole og samspelet

| ID | Kva | Forventa | Shield | Loftet |
|---|---|---|---|---|
| F1 | Profilbyte i Stova → Spole | Spole byter til same person | – | – |
| F2 | Opne tittel frå Stova | Detaljane i Spole; Tilbake til Stova | – | – |
| F3 | Spel ein film i Spole | Avspeling startar; framdrift kjem til Stova etter stopp | – | – |
| F4 | Kino-modus med ekte avspeling | «playing» dimmar, pause set tilbake (berre godkjende lys) | – | – |
| F5 | Spole Heim | Hero og rader som i Stova; «Sjå vidare» med rette miniatyrar | – | – |
| F6 | Spole navigasjon | Opp/ned/venstre/høgre logisk; ingen hopp langt bort i rada | – | – |
| F7 | Spole detaljside og tilbake | Fokus tilbake til kortet | – | – |

## G. Yting og animasjonar

Målt med `dumpsys gfxinfo` (framestat) under ei fast rørsle med D-pad, i tillegg til skjermopptak av opning og lukking av popupar.

| ID | Kva | Forventa | Shield | Loftet |
|---|---|---|---|---|
| G1 | Stova Heim: 20 × høgre, 6 × ned | Under 10 % trege bilete på Shield, under 25 % på Loftet | – | – |
| G2 | Stova Appar-sida: rull ned | Under 10 % på Shield | – | – |
| G3 | Stova Smarthus: rull | Under 10 % på Shield | – | – |
| G4 | Spole Heim: 20 × høgre, 6 × ned | Under 10 % på Shield | – | – |
| G5 | Popupar: opne/lukke (opptak) | Ingen hakk, ingen svart rand | – | – |
| G6 | Hero-byte (opptak) | Mjuk overtoning | – | – |

## Resultat

Fyllest ut under testen.
