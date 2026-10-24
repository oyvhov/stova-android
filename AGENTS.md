# Stova: arbeidsrettleiing for agentar (Claude og Codex)

Stova er ein Android TV-launcher for NVIDIA Shield med Jellyfin/Emby-media, Spole som spelar,
profilar og eit Home Assistant-dashbord. Same reglar gjeld for Claude Code og Codex, og begge
kan ta over frå kvarandre midt i ein plan.

## Les først

1. Spesifikasjon: `docs/superpowers/specs/2026-10-24-stova-launcher-design.md`
2. Planane, i denne rekkjefølgja:
   1. `docs/superpowers/plans/2026-10-24-plan-1-fundament.md`
   2. `docs/superpowers/plans/2026-10-24-plan-2-media.md`
   3. `docs/superpowers/plans/2026-10-24-plan-3-spole-lenkje.md` (blir gjord i `C:\JellyBin\reelstack-android`, etter Spole sine eigne `AGENTS.md` og `docs/AI_INSTRUCTIONS.md`. Kan gjerast parallelt med Plan 2.)
   4. `docs/superpowers/plans/2026-10-24-plan-4-home-assistant.md`
   5. `docs/superpowers/plans/2026-10-24-plan-5-ytelse-og-release.md`
3. `docs/HANDOFF.md`, dersom fila finst: siste status frå førre agent.

## Halde fram der førre agent slapp

1. `git -C C:\Launcher status` og `git -C C:\Launcher log --oneline -15`.
2. Finn første plan med `- [ ]`, og første steg som ikkje er avkryssa.
3. Ligg det ucommitta endringar som høyrer til det steget, fullfør steget i staden for å starte på nytt. Endringar du ikkje forstår, tilhøyrer brukaren: ikkje slett dei, spør.
4. Køyr testane i oppgåva før du går vidare, sjølv om førre agent har kryssa av.

## Under arbeidet

- Følg stega i planen nøyaktig: test først, sjå han feile, implementer, sjå han bestå, commit.
- Hak av `- [ ]` → `- [x]` etter kvart steg som er gjort, og commit planfila saman med koden.
- Har du superpowers-skills (`subagent-driven-development` eller `executing-plans`), bruk dei.
  Elles: gjer oppgåvene i rekkjefølgje sjølv.
- Alle Gradle-kall: `pwsh -NoProfile -File scripts/gradle.ps1 <tasks>`. Han set ASCII-sti for Gradle-heim og temp, sidan brukarmappa inneheld «Ø».
- Emulator: berre `Stova_TV` på port 5580 (`scripts/Start-StovaEmulator.ps1`) og Shield via `adb connect <ip>:5555`. **Aldri** installer Stova eller køyr testar på emulator-5560, -5562 eller -5564. Dei tilhøyrer Spole og har ekte brukardata.
- Design: ingen glas eller blur, ingen fargelitteralar utanfor `core/theme`, og lime berre for handling. `DesignRulesTest` må alltid vere grøn.
- UI-tekst på nynorsk i `res/values`, engelsk i `res/values-en`. Ingen hardkoda tekst.
- Ikkje publiser noko (GitHub-repo, release eller Pages) og ikkje push utan at brukaren ber om det.
- Systemlauncheren på Shield skal aldri slåast av (`pm disable-user`).

## Ved overlevering eller pause

Før du stoppar (grense nådd, slutt på økta, eller blokkert): skriv 3–6 linjer øvst i
`docs/HANDOFF.md` med dato, kva plan/oppgåve/steg som er sist fullført, kva som er påbyrja men
ikkje ferdig, kva som ikkje er verifisert, og eventuelle spørsmål til brukaren. Commit fila.
