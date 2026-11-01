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
| A1 | Installer Stova-TV og Spole (launcher-rows) | Begge installerte utan å slette data; Spole-innlogginga er der | ✅ | ✅ |
| A2 | Home-knappen etter ominstallering | Stova kjem fram; Heim-tenesta er bunden | ✅ | ❌→✅ |
| A3 | Kaldstart av Stova (`am start -W`) | Under 1,5 s på Shield, under 2,5 s på Loftet | ✅ 448–711 ms | ✅ 634–1140 ms |
| A4 | Kaldstart av Spole | Heim med rader utan raude feil; ingen krasj i logcat | ✅ 0,97 s | ✅ 0,84 s |

## B. Heim i Stova

| ID | Kva | Forventa | Shield | Loftet |
|---|---|---|---|---|
| B1 | Hero på Øyvind | Kunst, logo, fakta, ratings; byter tittel jamt | ✅ | ✅ |
| B2 | Hero på Eilev (Spole-profil utan konto) | Kunst frå Spole-radene, innanfor aldersgrensa (15) | ❌→✅ | ✅ |
| B3 | Rader frå Spole for Eilev | «Hald fram», «Nyleg lagt til» med bilete | ❌→✅ | – |
| B4 | Fokus i ei rad | Berre kortet med fokus endrar seg; resten av rada er ikkje dempa | ✅ | ✅ |
| B5 | Opp/ned mellom rader | Fokus går til rett kort i neste rad, ingen hopp langt bort | ⚠️ | – |
| B6 | Venstre/høgre i rada | Jamn rulling, fokus held seg synleg | ✅ | – |
| B7 | Langt trykk på app-kort | Sidepanel kjem inn; Tilbake tonar det bort utan svart rand | ❌→✅ | – |
| B8 | Langt trykk på mediekort | Spel frå starten / Merk som sett / Fjern frå Hald fram (Spole gjer handlinga) | ✅ | – |
| B9 | Toppfeltet | Brikker les rett; OK på profil opnar profilval | ✅ | ✅ |
| B10 | Sesongpynt (Halloween) | Pynt utan å dekkje tekst | ✅ | ✅ |
| B11 | Tilbake og Heim frå appar | Heim kjem att med fokus der det var | ✅ | – |

## C. Appar-sida

| ID | Kva | Forventa | Shield | Loftet |
|---|---|---|---|---|
| C1 | Rekkjefølgje | Favorittar → Spole-profilar → Alle appar → Bibliotek → Scenar → Skjulte | ✅ | ✅ |
| C2 | Fire per rad | Fire banner per rad, jamne mellomrom | ✅ | ✅ |
| C3 | Langt trykk på app | Meny med flytt, skjul, ikon, favoritt; lukkar utan rand | ✅ | – |
| C4 | Spole-profil-flis | Opnar Spole som den personen | – | – |

## D. Smarthus

| ID | Kva | Forventa | Shield | Loftet |
|---|---|---|---|---|
| D1 | Smarthus-sida opnar | Kort med rett tilstand innan 2 s | ✅ | – |
| D2 | Lyspanel (langt trykk) | Ned frå lysstyrken når «Slå på», så «Farge og kvittone» | ❌→✅ | – |
| D3 | OK på lyskort (godkjende lys) | Lyset kjem på, også ei pære som ikkje svarar på «på» utan nivå | ❌→⚠️ | – |
| D4 | Mediepanel (Sonos) | Ned frå volumet når «Pause», så «Kjelde» | ✅ | – |
| D5 | Legg til / fjern kort | Kort kjem og går; dashbordet elles urørt | ✅ | – |
| D6 | Popupar lukkar | Ingen svart rand; berre toning | ❌→✅ | – |

## E. Innstillingar

| ID | Kva | Forventa | Shield | Loftet |
|---|---|---|---|---|
| E1 | Alle sider opnar | Heim og utsjånad, Smarthus og kontoar, Profilar, Appar, System | ✅ | – |
| E2 | Kino-modus | Brytar, lysliste med nivå per lys, lagring | ✅ | – |
| E3 | Profilar | Liste, Spole-profil-val, bilete frå Spole | ✅ | – |
| E4 | Tilbake frå undersider | Fokus tilbake til raden som opna | – | – |

## F. Spole og samspelet

| ID | Kva | Forventa | Shield | Loftet |
|---|---|---|---|---|
| F1 | Profilbyte i Stova → Spole | Spole byter til same person | ✅ | – |
| F2 | Opne tittel frå Stova | Detaljane i Spole; Tilbake til Stova | ✅ | – |
| F3 | Spel ein film i Spole | Avspeling startar; framdrift kjem til Stova etter stopp | ✅ | – |
| F4 | Kino-modus med ekte avspeling | «playing» dimmar, pause set tilbake (berre godkjende lys) | ✅ | – |
| F5 | Spole Heim | Hero og rader som i Stova; «Sjå vidare» med rette miniatyrar | ✅ | ✅ |
| F6 | Spole navigasjon | Opp/ned/venstre/høgre logisk; ingen hopp langt bort i rada | ❌→✅ | – |
| F7 | Spole detaljside og tilbake | Fokus tilbake til kortet | ✅ | – |

## G. Yting og animasjonar

Målt med `dumpsys gfxinfo` (framestat) under ei fast rørsle med D-pad, i tillegg til skjermopptak av opning og lukking av popupar.

| ID | Kva | Forventa | Shield | Loftet |
|---|---|---|---|---|
| G1 | Stova Heim: 20 × høgre, 6 × ned | Under 10 % trege bilete på Shield, under 25 % på Loftet | ✅ 9–12 % | ⚠️ 19–26 % |
| G2 | Stova Appar-sida: rull ned | Under 10 % på Shield | ❌→✅ 4–11 % | ✅ 7 % |
| G3 | Stova Smarthus: rull | Under 10 % på Shield | ⚠️ 17–21 % | – |
| G4 | Spole Heim: 20 × høgre, 6 × ned | Under 10 % på Shield | ⚠️ 9–15 % | ⚠️ 10–30 % |
| G5 | Popupar: opne/lukke (opptak) | Ingen hakk, ingen svart rand | ✅ | – |
| G6 | Hero-byte (opptak) | Mjuk overtoning | – | – |

## Resultat

Testa 2026-11-01 frå ca. 15.20 til 17.15 på Shield og Loftet med byggja i dag. «❌→✅» tyder feil funnen, retta, installert og sett på eininga. «❌→⚠️» tyder retta og testa med einingstest, men ikkje sett på eininga.

### Funne og retta

- **B2 / B3 Eilev (Shield):** heroen hadde ikkje bilete. Stova kjende berre heroen frå sine eigne kontoar. No kjem heroen frå Spole-radene, innanfor aldersgrensa (Stova `SpoleRowSource.spotlight`). Emby sende ikkje aldersgrensa utan at Spole bad om ho, så alle titlane hadde ukjend alder (Spole `LIBRARY_RATING_FIELDS` + `official`). Seriar viste same serie fire gonger (fire episodar); no er det éitt kort per serie.
- **B4:** resten av rada vart dempa. Det er teke bort heilt, også for titlane.
- **B7 / D6:** popupar på sida gjekk ut som ei svart rand. No tonar dei bort der dei står. Opptak før og etter.
- **D2 / D4:** i lys- og mediepanelet hoppa fokus over «Slå på» / «Pause». No går opp og ned gjennom knappane.
- **D3:** OK på eit lyskort slo ikkje på «Taklampe Stova». No spør Stova éin gong til med nivået lyset hadde sist. Dette er ikkje prøvd på pæra.
- **G2 Appar-sida (Shield):** sida hakka med 27–39 % trege bilete fordi spindelvevet på kvar flis vart teikna på nytt i kvar ramme. Kvar flis har no sitt eige lag: 4–11 %. Loftet: 7 %.
- **F6 Spole:** kortet ytst til høgre vart kutta av skjermkanten, og høgre-pila på siste kortet hoppa ned i neste rad, mest under skjermen. No har rada marg på begge sider, og høgre-pila stoppar ved enden av rada.
- **Alder i tekst:** «NO-A» stod rått på heroen i Stova, og «NO-15» / «NO-A» i Spole. No står det «Alle» og «15 år» i begge.

- **A2 Loftet (Google TV):** etter ei oppdatering gjekk Home til Google TV til nokon opna Stova. Når oppdateringa drep Stova på skjermen, kjem Google TV-heimen fram medan Heim-tenesta startar på nytt, så tenesta går glipp av vindaugshendinga. Etterpå står Google TV-heimen allereie framme og gir ingen ny hending, og på Google TV går Home ikkje gjennom tastefilteret. No hugsar Stova om han var framme (`HomeFront`), og tenesta opnar Stova att når ho koplar seg til. Prøvd på Loftet: oppdatering med Stova framme gir Stova att etter under 3 s. Oppdatering med Spole framme lèt Spole stå, og Home gir Stova.

- **«Hald fram i andre appar» (Shield, Eilev):** same tittel stod tre gonger (ein annan app la inn «One Battle After Another» på nytt i staden for å oppdatere). No viser Stova kvar tittel éin gong per app, den nyaste.

### Merknader (ikkje retta)

- **B5:** éin gong kom fokus tilbake til første kortet i «Hald fram» etter tre rader ned og opp att. Det er ikkje gjenskapt (tre forsøk etterpå gav rett kort).
- **G1 Loftet:** Heim ligg på 19–26 % trege bilete (p50 27 ms). GPU-en brukar 12–14 ms, og like mykje går med til å sende teiknekommandoar. Ein buffra Halloween-scene endra ingenting. Truleg trengst ei lettare scene for svake TV-ar. Appar-sida på Loftet ligg på 7 %.
- **G3 / G4:** Smarthus og Spole Heim har om lag éi litt sein ramme per tastetrykk (p90 19–25 ms). Det ser ein knapt.
- **Spole-hero:** han byter tittel medan fokus står på «Spel av» (eit val frå beta11; han stoppar ved tastetrykk). OK spelar det som står på skjermen.
- **Innstillingar:** fokus på «Appar» viser framleis sida for «Profilar» til høgre, fordi «Appar» er ei lenkje til Appar-sida.
- **Fokuset utan demping:** Løft (standard) gjer kortet med fokus 6 % større utan ring. Utan dempinga er fokuset mindre tydeleg på stillbilete, men tydeleg når det rører seg. «Ring og zoom» under Fokus gir ein ring for den som vil ha det.

- **Watch Next på Eilev:** «Hald fram i andre appar» er felles for heile TV-en (Android TV), så Eilev ser til dømes Øyvind sin «One Battle After Another» (15 år). Det kan vere verdt eit val om å skjule rada på profilar som følgjer ein Spole-person.

### Side-effektar av testen

- «One Battle After Another» (Øyvind, Jellyfin) vart spela i om lag 15 + 7 sekund, så resumepunktet flytta seg tilsvarande.
- Kino-modus er slått på på Shield med «Taklampe Stova» = Av og «Vindu Stova» = 10 %. Begge lysa er av, som før testen.
