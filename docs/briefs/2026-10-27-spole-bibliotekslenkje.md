# Brief til Spole: opne eit bibliotek frå Stova

Lim inn i ei ny økt i `C:\JellyBin\reelstack-android` (Spole-repoet).

Legg til ei launcher-lenkje i Spole som opnar eitt mediebibliotek, ved sida av `spole://open`.

Lenkja Stova sender:
`spole://library?kind=<jellyfin|emby>&user=<userId>&library=<libraryId>`

- `kind`, `user` og `library` er påkravde. Id-ane følgjer `[A-Za-z0-9_-]{1,128}`, med same strenge
  avkoding og reglar for doble parametrar som `LauncherLinks.parse` i
  `app/src/main/java/app/reelstack/launcher/LauncherLink.kt`. Alt anna blir avvist som ei dårleg
  `open`-lenkje.
- `library` er id-en til ei biblioteksvising (CollectionFolder frå `/Users/{user}/Views`).
- Spole opnar skjermen han alt bruker til å bla i det biblioteket, for tilkoplinga som passar
  `kind` + `user`. Finst ingen slik, eller brukaren ikkje er logga inn: gjer som ei `open`-lenkje
  utan treff i dag (`PendingLauncherOpen`).
- Nytt intent-filter for vert `library` i `app/src/main/AndroidManifest.xml`, ved sida av
  `spole`/`open`. `spole://open` skal vere som før.
- Kvifor ny vert: ein eldre Spole har ikkje filteret, så Stova får `ActivityNotFoundException` og
  opnar startsida til Spole i staden. `open` skal ikkje ta imot bibliotek-id-ar.

Følg `AGENTS.md` og `docs/AI_INSTRUCTIONS.md` i Spole. Test først: parsertestar for gyldige og
ugyldige `library`-lenkjer og ein test for at lenkja går til biblioteksskjermen. Ikkje rør
`C:\Launcher`. Ikkje push, publiser eller installer på Shield utan å spørje.

Ferdig: lenkja blir tolka, opnar rett bibliotek på ein Spole-emulator (skjermbilete), dei gamle
`open`-testane står, og arbeidet er committa på ei eiga grein.
