# Stor rullande hero, klokkeval, Appar i Innstillingar og sesongpynt

Bestilling (2026-10-26): «Lag ei finare klokke. Flytt Apper ikon til under settings? Flytt gresskar
fro halloween over på venstre side av skjermen. Eg vil ha meir fin pynt på heile skjermen for jul og
Halloween. Heroen ruller ikkje lenger. Den viser alltid ei rad. Ingen av modusene gjer det eg vil.
Fin paralax. Feks lett fin snø på Jul.» Svar: Appar inn i Innstillingar; heroen stor, så rullar opp;
klokke: lag forslag. Seinare: «Media kortet og værkortet skal vere enda finare. Media kortet skal jo
ha høgtalerikon», «Sonos … TV litt trykt og feilplassert», «nokon settings hoppar mellom kvarandre».

- [x] 1. Appar inn i Innstillingar (e2a07ad). Ingen Appar-knapp i toppfeltet; menyen i Innstillingar har
  Appar ved Android, og Tilbake derifrå går til Innstillingar. «Opne Appar» på tom Heim er bevart.
- [x] 2. Sesongmerket står ved venstre kant i toppfeltet i glør-/snøfarge (TopBar i e2a07ad, merket a6fca57).
- [x] 3. «Rullande oversikt» opnar med stor hero og første rad nedst; Ned rullar heroen ut, og
  bakgrunnen glir saktare opp, veks litt og blir mørkare (4406da4). Null rørsle ved redusert rørsle.
- [x] 4. Klokkestil: Standard, Tynn med dato og Dato ved sida, med prøve i Toppfelt (af5e604).
- [x] 5. «Mykje» sesongpynt dekkjer heile Heim bak innhaldet: lett snø, glør og tre flaggermus (100049e).
- [x] Innstillingar hoppar ikkje mellom seksjonar lenger (fa55b25).
- [x] Mediekort med høgtalar, større tittel og runde knappar; vêrkort med himmelfarge og høg/låg (c62ca93).
- [x] Rullande visning: rader under nedste marg tonar ut i staden for å stikke fram (eigen commit).
- [ ] 6. Full kontroll ved c62ca93: 728/728, 0 skip, lint 0 feil (62 åtvaringar, 1 hint), bygg OK; Heim- og designprøver køyrde på nytt etter kantovergangen. Emulator prøvd med bilete. Shield ikkje rørt (brukaren: ikkje TV-en den neste timen frå ca. 18:45).

Ingen HA-styring, datatømming eller push.
