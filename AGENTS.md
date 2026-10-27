# Stova: arbeidsrettleiing for AI-agentar

Stova er ein Android TV-launcher for NVIDIA Shield med Jellyfin/Emby-media, Spole som spelar,
profilar og eit Home Assistant-dashbord. Reglane her gjeld for alle agentar: Claude Code (via
`CLAUDE.md`), Codex og andre. Dokumentasjonen er på nynorsk. Svar brukaren på det språket
brukaren skriv på.

## Les først

1. `docs/ARCHITECTURE.md`: kvar du startar for kvar oppgåve, og korleis pakkane heng saman.
2. `CONTRIBUTING.md`: arbeidsflyt, bygg og testar.
3. `docs/EMULATOR.md`: emulator, demodata og skriptet som styrer TV-en.
4. `docs/superpowers/plans/README.md`: kva plan som gjeld no. Dei andre planane er historikk.
5. Den øvste oppføringa i `docs/HANDOFF.md`: kva førre agent gjorde sist. Resten av fila er ein
   logg over tidlegare økter og er inga bestilling.

Spesifikasjonen `docs/superpowers/specs/2026-10-24-stova-launcher-design.md` forklarer kvifor
Stova er som han er.

## Halde fram der førre agent slapp

1. `git status` og `git log --oneline -15`.
2. Gjer det brukaren ber om. Utan ny bestilling: den gjeldande planen i planoversikta. Opne
   avkryssingsboksar i eldre planar er inga bestilling.
3. Ligg det ucommitta endringar som høyrer til det steget, fullfør steget i staden for å starte
   på nytt. Endringar du ikkje forstår, tilhøyrer brukaren: ikkje slett dei, spør.
4. Køyr testane i oppgåva før du går vidare, sjølv om førre agent har kryssa av.

## Under arbeidet

- Test først: skriv ein test, sjå han feile, implementer, sjå han bestå, commit. Hak av
  `- [ ]` → `- [x]` etter kvart steg, og commit planfila saman med koden.
- Gradle: `pwsh -NoProfile -File scripts/gradle.ps1 <tasks>`. På Windows flyttar skriptet Gradle
  til ASCII-stiar om brukarmappa har bokstavar som «Ø»; elles køyrer det berre `./gradlew`. Ikkje
  start to bygg i same mappe samstundes.
- Design: ingen glas eller blur, ingen fargelitteralar utanfor `core/theme`, og lime berre for
  handling. Aksentfargar treng minst 7:1 kontrast mot alle bakgrunnar. `DesignRulesTest` og
  `ThemePaletteTest` må alltid vere grøne.
- UI-tekst på nynorsk i `res/values`, engelsk i `res/values-en`. Ingen hardkoda tekst.
- `*Screen` koplar data og livssyklus; `*Content` teiknar kontrollerte verdiar og kallar tilbake.
  Hald rullestilling og animasjonsverdiar i lag-/layoutfasen når dei endrar seg per ramme.
- Integrasjonar som Jellyfin, Emby og Home Assistant gjer ingenting når dei manglar eller er slått
  av. Spør aldri ei teneste som ikkje finst.
- Endrar du layout, sjå på ekte skjermbilete frå emulatoren, ikkje berre testane. Prøv lange trykk
  med `hold:` i `scripts/Send-TvRemote.ps1`, ikkje med `adb shell input keyevent --longpress`.
- Hald `docs/ARCHITECTURE.md` i takt når du flyttar filer eller legg til ein funksjon.

## Einingar og data

- Prøv på ein emulator (`docs/EMULATOR.md`). Installer med `scripts/Install-Stova.ps1`, som bruker
  `install -r`, så profilar, kontoar og innstillingar blir verande. Ikkje bruk `pm clear`,
  avinstallering eller demodata for å gjere ein test lettare på ein emulator med ekte data.
- Ein ekte TV, eller ein emulator med nokon sine ekte data: berre når eigaren ber om det der og då.
- Slå aldri av systemlauncheren (`pm disable-user`). Nokre einingar startar ikkje då.
- Ikkje bruk `am force-stop` på Stova på ein TV der Heim-knappen er på. Android slår då av
  tilgjengetenesta hans, og brukaren må slå ho på att.
- Token, passord og signeringsnøklar høyrer ikkje til i Git, loggar eller skjermbilete.
- Ikkje push og ikkje publiser noko (repo, release eller Pages) utan at eigaren ber om det.

## Ved overlevering eller pause

Før du stoppar (grense nådd, slutt på økta, eller blokkert): skriv 3–6 linjer øvst i
`docs/HANDOFF.md` med dato, kva som sist er fullført, kva som er påbyrja men ikkje ferdig, kva som
ikkje er verifisert, og eventuelle spørsmål til brukaren. Commit fila.

## Berre på maskina til eigaren

Dette gjeld når du arbeider for eigaren i `C:\Launcher`:

- Brukarmappa inneheld «Ø». Gradle-heimen ligg i `C:\JellyBin\.gradle-home`, som Spole deler.
  Hald worktrees og mellombelse filer under `C:\`.
- Emulator: berre `Stova_TV` på `emulator-5580`. **Aldri** installer Stova eller køyr testar på
  `emulator-5560`, `-5562` eller `-5564`. Dei tilhøyrer Spole og har ekte brukardata; skripta
  nektar desse.
- Shield: `adb connect <ip>:5555`, berre når eigaren ber om det. `scripts/Show-StovaScreen.ps1
  -Serial <ip>:5555` viser han i scrcpy, og `Send-TvRemote.ps1 -Serial <ip>:5555` styrer han.
- Spole-koden ligg i `C:\JellyBin\reelstack-android` og følgjer sine eigne `AGENTS.md` og
  `docs/AI_INSTRUCTIONS.md`.
- Har du superpowers-skills (`subagent-driven-development` eller `executing-plans`), bruk dei.
  Elles: gjer oppgåvene i rekkjefølgje sjølv. Ledger: `.superpowers/sdd/<plan-filnamn>/progress.md`
  (git-ignorert). Når ei oppgåve er ferdig og testane er grøne, legg til linja
  `Task N: complete (commits <base>..<head>, tests: <kommando> → <resultat>)`. Avvik frå planen blir
  skrivne som `Task N: Ruling: <kva> — <kvifor> — <kostnad om feil>`.

**Claude → Codex:** Claude les forbruket (`get_usage`) ved start av kvar oppgåve. Er 5-timarsvindauget
på ≥ 90 % (og det er meir enn 15 min til nullstilling), eller vekegrensa på ≥ 95 %, gjer Claude dette:
1. Skriv `docs/HANDOFF.md` og committar.
2. Startar Codex med
   `pwsh -NoProfile -File scripts/Start-CodexHandoff.ps1 -StopAfter "<nullstillingstid, lokal>"`.
3. Stoppar.

Codex stoppar etter `-StopAfter` og skriv `docs/HANDOFF.md`. Deretter held Claude fram derifrå.
Berre éin agent arbeider i mappa om gongen.
