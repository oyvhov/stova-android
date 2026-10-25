# Bestilling: Tilpassing v3 – «alt mogleg», og direkte på Heim

Worktree: `C:\Launcher\.worktrees\appearance`, greina `appearance-v2`, no snøggspola til
integrasjonsgreina `plan-2-media` (b04f9a6). Ho har utsjånad v2, tema, ikonpakkar og Smarthus.
Les og følg `AGENTS.md`. Spesifikasjonen er `docs/superpowers/specs/2026-10-24-stova-launcher-design.md`.

## Brukaren sine ord (25.10.2026)

> «Har du tenkt på alt av customization? Flytte rader, storleikar, kortbue? +++»
> «Dette skal bli den beste launcheren som finst. Sjukt moderne og vakker. Hugs å lage rom til artworken.»

## Dette finst allereie (ikkje lag på nytt)

- **`AppearancePrefs`:**
  - kort: `cardSize` (global S/M/L), `showTitles`, `movieStyle`;
  - appar og ikon: `appStyle`, `iconPack`, `iconColor`, `iconFill`;
  - hero og bakgrunn: `heroSize` (Kompakt/Stor), `showOverview`, `showGenres`, `spotlightTime`,
    `backdropDimming`;
  - form og fokus: `corners` (berre ROUND/SHARP), `focusEffect` (Ring/Zoom), `strongFocus`;
  - farge og tema: `accent`, `visualTheme`, `seasonChoice`, `seasonalDecor`;
  - anna: `clock`, `rowSpacing`, `reduceMotion`.
- **`HomePrefs`:** `rowOrder` og `hiddenRows` (berre via Innstillingar → Heim), `startFocus`,
  `backdropRotation`.

## Dette skal til

### 1. Flytt og forme rader direkte på Heim

- **Radval frå kortmenyen:** Meny eller hald OK på eit kort i ei rad gir kortmenyen. Han har ein
  seksjon «Rada» med:
  - **Flytt rada:** rada får ei tydeleg ramme. Opp og ned flyttar heile rada live, OK eller Tilbake
    avsluttar, og ho blir lagra i `rowOrder`.
  - **Skjul rada:** blir lagra i `hiddenRows`, med kort angreknapp i ei roleg melding.
  - **Kortformat for rada:** Plakat (2:3), Brei (16:9), Stor brei, Liten brei eller Ikon.
  - **Storleik for rada:** Liten, Middels eller Stor, som overstyrer den globale.
  - **Vis radtittel:** På eller Av.
- **Lagring per rad:** `rowFormats: Map<HomeRowKey, RowStyle>` i `HomePrefs`, som Spole sitt
  `homeRowFormats`. `RowStyle` har format, storleik og tittel, med standardverdiar.
  - Gjeldande standard per rad er den same som i dag: «Nyleg lagt til · Filmar» som plakat, resten
    breie.
- **Innstillingar → Heim** viser same val per rad. Lista over rader kan flyttast med fjernkontrollen,
  med førehandsvising av formatet.

### 2. Kortbue og form

- **`corners` erstatta av `cornerRadius`:** Skarp (4 dp), Mjuk (12 dp), Rund (20 dp), Ekstra rund
  (28 dp). Gamle verdiar blir migrerte: SHARP → Skarp, ROUND → Rund.
- **Same bue overalt:** kort, appflisar, HA-kort (via `MaterialTheme.shapes`), dialogar og knappar,
  så formspråket heng saman.
- **Kortavstand** (vassrett): Tett, Normal eller Luftig.

### 3. Fokus

- **`focusEffect`:** Ring, Forstørring, Ring + forstørring, eller «Lys opp» (dei andre korta i rada
  blir lett dempa, det fokuserte står fram). Alle respekterer redusert rørsle.
- **Ringbreidd:** Tynn, Normal eller Tjukk. Erstattar `strongFocus` og blir migrert.
- **Framdriftsstrek:** Tynn, Tjukk eller Skjult.

### 4. Hero og bakgrunn

- **`heroSize`:** Av (ingen hero; radene startar høgt og bakgrunnen er roleg), Kompakt eller Stor.
- **Bakgrunnskjelde:**
  - Spotlight (standard);
  - Fast bilete (vel ein tittel frå biblioteket i ein enkel veljar);
  - Einsfarga tema (berre temaflata, ingen bilete).
- **Spotlight-kjelde:** Heile biblioteket, Berre filmar, Berre seriar eller Favorittar. «Ny sesong»
  og sesongtitlar følgjer framleis med.
- **Logo eller tittel** i heroen: Logo når han finst (standard), eller alltid tittel.
- **Valfri rørsle i bakgrunnen:** ein svært langsam, roleg zoom (Ken Burns, 1,00→1,04 over
  tittelvisinga). Standard **Av**; han er alltid av ved redusert rørsle og når Heim ikkje er synleg.
  Teikn han med `graphicsLayer`, ikkje rekomposisjon.

### 5. Toppfeltet

- **Kva som skal visast:** klokke (24 t, 12 t, skjult), dato («laur. 25. okt.»), HA-brikker,
  sesongnedteljing, Appar-knapp, Smarthus-knapp og profilbilete.
- **Klokkestorleik:** Normal eller Stor.

### 6. Profil og tryggleik

- **Tilbakestill utsjånad** til standard (med stadfesting).
- **Kopier utsjånad frå ein annan profil.**
- **Barneprofilar:** vaksne kan endre utsjånaden deira, som i dag.

### 7. Innstillingar → Utsjånad

- **Grupper:** Tema, Heim og hero, Kort og rader, Bakgrunn, Fokus, Toppfelt, Ikon, Profil.
  Ei kort forklaring per gruppe.
- **Førehandsvising** øvst, oppdatert live: eit lite utsnitt av Heim med valt hero, eitt kort, bue,
  fokus og aksent.
- **Navigasjon:** heile sida skal vere rask og logisk med D-pad (venstre/høgre byter verdi).

## Rammer

- **TDD med raud/grøn for kvar del:**
  - migrering av gamle profilar (SHARP/ROUND, `strongFocus`);
  - radformat per rad;
  - flytting av rad via fjernkontroll;
  - skjul og angre;
  - fokuseffektar;
  - hero av;
  - bakgrunnskjelde;
  - toppfeltval;
  - tilbakestill og kopier.
- **Skjermbilete** med `-Pstova.screens=true`: minst åtte kombinasjonar, mellom anna:
  - hero av;
  - plakatrad stor;
  - Ekstra rund + Lys opp;
  - toppfelt med dato;
  - flyttemodus for rad;
  - Innstillingar.
  - Lagre dei i `docs/images/tilpassing-*.png` og vurder dei kritisk mot «beste launcheren som
    finst» og «rom til artworken».
- **Forbodne filer:** ikkje rør `app/src/main/java/app/stova/ha/**`. Ein annan agent gjer
  Smarthus-handlingar på greina `ha-v2`.
- **Emulator:** ikkje installer på nokon emulator; Claude gjer det.
- **Strengar:** i blokka `<!-- Tilpassing v3 -->`.
- **Design:** ingen glas, ingen fargelitteral utanfor `core/theme`.
- **Sluttkontroll:** `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug`
  skal vere grøn.
- **Commits:** små, med `Co-Authored-By`-linja frå AGENTS.md. Ingen push.
- **Til slutt:**
  - oppdater spesifikasjonen §5.1, §5.4 og tabellen over utsjånadsval;
  - skriv ein kort status i `docs/HANDOFF.md`;
  - svar med commit-liste, testtal og bilete.
