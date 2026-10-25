# Medieradene skal ikkje bli ståande «Fråkopla»

Bestilling (brukaren, 25.10): «Jobb vidare». Claude såg «Offline» (Fråkopla) på alle medieradene på
emulator-5580 rett etter ein ny installasjon, medan HA-brikkene viste levande data og Jellyfin var nåbar.

Truleg årsak: `MediaRepository.refresh` set `slot.refreshedAt=now` *før* hentinga
(`MediaRepository.kt:157-158`). Ei henting som feilar (tidsavbrot på 10 s, eller nettet er ikkje klart
ved kald start) tel difor som fersk, og ein retur til Heim spør ikkje på nytt før om fem minutt.
Radene står «Fråkopla» heile tida. Regelen frå brukaren gjeld: «ikkje spør utan at det trengst», men
heller ikkje i ei tett løkkje, og ei feila henting skal prøvast att snart.

- [x] Raud test i `MediaRepositoryTest`: første `refresh()` feilar for kontoen (radene blir `offline`).
      `refresh()` på nytt etter 31 s med same klokke, når klienten svarar, hentar igjen og fjernar
      `offline`. `refresh()` på nytt etter berre 5 s gjer ingen ny førespurnad.
- [x] Rett: ei henting der minst éin konto fekk `offline`-rader eller kasta (ikkje `Unauthorized`)
      kan prøvast att etter `FAILED_REFRESH_RETRY_MILLIS = 30_000L` i staden for fem minutt.
      Vellukka hentingar held femminuttsregelen og `playbackRevision`-regelen uendra.
- [x] Prøv på emulator-5580 med ekte data: kald start (`am force-stop` og start) og retur til Heim.
      Ingen «Fråkopla» når Jellyfin er nåbar. Profil- og kontofiler skal vere byteidentiske før og etter.
- [x] Ta opp eit klipp (`adb -s emulator-5580 shell screenrecord`, ca. 20–30 s) som viser overgangane:
      Heim → ned i radene (parallax) → opp → Innstillingar (fleire seksjonar) → tilbake → Smarthus →
      opne eit kortpanel (langt OK) og lukk med Back (ingen styringshandlingar) → Heim.
      Legg klippet i `.superpowers/` (git-ignorert) og to–tre stillbilete i `docs/images/`.
- [x] Køyr full suite, lint og bygg (`scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug`);
      dokumenter i `docs/verification/offline-retry.md` og commit.

Avgrensing:
- Ikkje installer på Shield i denne bestillinga; brukaren seier i frå sjølv.
- Ikkje push før brukaren ber om det.
- Berre emulator-5580; aldri -5560, -5562 eller -5564.
