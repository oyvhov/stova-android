# Bestilling: Smarthus v2 – finpuss til «beste launcheren som finst»

Worktree: `C:\Launcher\.worktrees\ha-v2`, greina `ha-v2`. Byggjer på Smarthus v2
(`docs/briefs/2026-10-25-smarthus-v2.md`), som er ferdig. Les og følg `AGENTS.md`.

## Brukaren sine ord

> «Dette skal bli den beste launcheren som finst. Sjukt moderne og vakker.» Smarthus skal vere i
> stil med Stova («denne appen vi lagar»).

## Kritikk etter gjennomgang av dei ekte skjermbileta

Bileta står i `docs/images/ha-v2-*.png`. Funksjonen er god, men det visuelle må opp eit nivå.

1. **Hierarkiet på korta er snudd.**
   - I dag står «On» stort, medan lampenamnet står lite nedst, og «On» står både som brikke og
     som hovudverdi.
   - Ny rekkjefølgje på alle korttypar: ikon og **namn** øvst (`titleMedium`, `onSurface`).
     Hovudverdien kjem stort under: lysstyrke «87 %» når lyset er på, «Av» når det er av;
     temperatur for klima og sensor; pris for straum.
   - Statusbrikka kjem berre der ho tilfører noko, til dømes «Varmar», «Spelar» eller «Open».
   - Rom-undertekst trengst berre i Favorittar, ikkje under ein romseksjon.
2. **Lys skal gløde.**
   - Når lyset er på, blir kortflata tona varmt i høve til lysstyrken: om lag 8 % ved låg og 22 %
     ved full styrke, i ein dempa varm token i `core/theme`.
   - Har lyset farge (`hs_color`/`rgb_color`), brukar tonen ein avmetta variant av den fargen.
   - Ikonruta er fylt med glødfargen. Når lyset er av, er flata nøytral og ikonet dempa.
   - Ingen gradient-glas: dette er flat toning.
3. **Ord i staden for tekniske tal.**
   - Fargetemperatur: «Varm kvit» under 3000 K, «Nøytral» frå 3000 til 4500 K, «Kald kvit» over
     4500 K.
   - Fargelys viser ein liten fargeprikk.
4. **Kompakte storleikar utan tom flate.**
   - S er om lag 264 × 152 dp, M er 2 × S i breidd, og L er 2 × 2.
   - Innhaldet fyller kortet: topp (ikon og namn) og botn (verdi og linje). Ingen stor tom botn.
   - Rutenettet har 16 dp mellomrom.
   - Fokusert kort skal alltid vere heilt synleg når ein blar; ikkje kutta i botnen.
5. **Straumpris skal vere vakker og lesbar.**
   - Vis timespris som 24 søyler (snitt av 15-minuttsprisar). Eit val i dialogen viser 15 minutt.
   - Søylene blir farga etter nivå mot dagen sitt spenn: billig `Success`, middels `Caution` og
     dyr `Warning`, dempa som i Stova.
   - Noverande time er markert med aksenten og ein etikett «No 74 øre».
   - Tidsakse med 00, 06, 12, 18 og 24, og min og maks med klokkeslett («Billigast 03–04»).
   - Minigrafen i kortet brukar same farging.
6. **Klima:** noverande temperatur stort, mål med tydeleg ±-hint når kortet er fokusert, og
   modusbrikke med ikon (Varme, Kjøling, Auto, Av).
7. **Media:** kunsten fyller venstre del av kortet (M/L). Tittel og artist står til høgre, med
   framdrift når HA gir han.
8. **Hintlinja nedst** («OK: styr · ← →: juster · Meny: tilpass») skal vise seg roleg berre dei
   første gongene, eller berre når fokus er i rutenettet, og ikkje ta plass frå korta.
9. **Heim-brikkene i toppfeltet** skal ha same ikonbruk og glød som korta (lys på = glødfarge).

## Rammer

- **TDD med raud/grøn for kvar del:** fargetemperaturord, glødstyrke etter lysstyrke,
  prisnivåfarging, timesnitt, korthøgder og hierarki (namn før verdi).
- **Ekte test:** test på `emulator-5580` mot brukaren sitt Home Assistant, som før.
  - Installer berre `app.stova.debug` med `install -r`, aldri `uninstall` eller `clear`.
  - Set lys og temperatur tilbake og stadfest det.
  - Aldri alarm, lås eller garasjeport. Aldri emulator-5560/-5562/-5564.
- **Skjermbilete:** ta nye ekte bilete og vurder dei kritisk: «er dette det vakraste
  smarthus-panelet på ein TV?». Fiks til svaret er ja. Lagre dei som `docs/images/ha-v2-*.png`.
- **Forbodne filer:** ikkje rør heim, hero, mediekort, utsjånads- eller temafiler. Ein annan
  agent arbeider der. Bruk `MaterialTheme`-fargar og -former, og legg nye tokens i `core/theme`
  i eigen fil, `HaTokens.kt`, for å unngå flettekonfliktar.
- **Strengar:** i blokka `<!-- Smarthus v2 -->`.
- **Sluttkontroll:** `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug`
  skal vere grøn.
- **Commits:** små. Ingen push.
- **Til slutt:** oppdater spesifikasjonen §5.2 og `docs/HANDOFF.md`, og svar med commit-liste,
  testtal og bilete.
