# Bestilling: Ikonpakkar som i Projectivy, og eit Spole-ikon

Worktree: `C:\Launcher\.worktrees\appearance`, greina `appearance-v2`. Denne bestillinga byggjer
vidare på utsjånad v2 og tema v1 (`docs/briefs/2026-10-25-tema-v1.md`), som er ferdige før denne
startar. Les og følg `AGENTS.md`. Spesifikasjonen er
`docs/superpowers/specs/2026-10-24-stova-launcher-design.md`.

## Brukaren sine ord (25.10.2026)

> «Eg vil du skal finne ein god ikonpakke, til dømes den ein kan leggje til i Projectivy, og lage eit
> ikon til Spole-appen òg. Han er ikkje med der.»

Brukaren vil ikkje ha automatiske favorittar; ikkje lag det.

## Fakta

- Projectivy (4.60+) støttar ikonpakkar bygde på Candybar og Blueprint. Det er standard 1:1-pakkar
  med `appfilter.xml`. Ekte 16:9-bannerpakkar finst ikkje; Projectivy har «Crop to fill» for å fylle
  bannerflata med eit 1:1-ikon.
- **Tilrådd pakke:** Arcticons.
  - Linjeikon (kvite strekar), open kjeldekode (kode GPL-3.0, ikon CC BY-SA 4.0), Candybar-basert,
    over 10 000 ikon, gratis på Play og F-Droid (`com.donnnno.arcticons`, «Material You»-varianten
    `com.donnnno.arcticons.you`).
  - Han passar Stova sitt flate, dempa uttrykk og kan fargast med aksentfargen.
- **Referanse, berre til lesing:** `gh repo clone Arcticons-Team/Arcticons C:\CodexCaches\arcticons-ref`.
  Les bidragsrettleiinga (SVG-format, lerret, strekbreidd, avrunda endar, ingen fyll) og `appfilter.xml`.

## Kva Stova skal få

### 1. Støtte for ikonpakkar

- **Finn installerte pakkar** med `queryIntentActivities` på dei vanlege handlingane:
  `org.adw.launcher.THEMES`, `com.novalauncher.THEME`, `org.adw.ActivityStarter.THEMES`,
  `com.gau.go.launcherex.theme` og `org.adw.launcher.icons.ACTION_PICK_ICON`.
  - Legg til `<queries>` i manifestet, sidan Android 11 krev det.
- **Les `appfilter.xml`:** først `res/xml/appfilter.xml` via `getResourcesForApplication`, elles
  `assets/appfilter.xml`.
  - Bruk `<item component="ComponentInfo{pkg/aktivitet}" drawable="namn"/>`.
  - Tål feilformaterte linjer: hopp over dei og legg pakken til grunn for resten.
- **Hurtigbuffer:** lagre koplinga per pakke på disk, nøkla på `versionCode`. Last ikonet i
  TV-tettleik og cache det i `AppArtCache`. Heim skal ikkje hakke.

### 2. Bruk på Heim og i Appar

- **Innstillingar → Utsjånad → Ikonpakke:** Ingen (appanes eigne banner, standard), eller ein av
  dei installerte pakkane.
  - **Farge:** «Original» eller «Aksent». Aksent fargar monokrome pakkar som Arcticons i
    temaaksenten, slik at sesongtemaa òg slår gjennom.
  - **Fyll:** «Sentrert» (ikon på `Raised`-flate) eller «Fyll flata» (som «Crop to fill»).
- **Appstilane frå utsjånad v2:** Banner viser pakkeikonet sentrert eller fylt på bannerflata,
  og Ikon viser pakkeikonet. Apper utan treff i pakken brukar eige banner eller ikon som før.
- **Ikon per app:** appmenyen (lang OK eller Meny) får «Endre ikon» → «Frå ikonpakke».
  - Det opnar eit søkbart rutenett med pakkeikon. Valet blir lagra per profil, og «Tilbakestill»
    finst.

### 3. Eit Spole-ikon

- **Arcticons-versjon:** Spole (`app.reelstack`, `app.reelstack.debug`) finst ikkje i pakkane.
  Teikn eit Spole-ikon i Arcticons-stil, basert på Spole sitt merke.
  - Sjå Spole sitt launcher-ikon og `SpoleIcons` i `C:\JellyBin\spole-launcher-link`, som er berre
    til lesing.
  - Følg rettleiinga deira nøyaktig (lerret, strek, endar).
- **Filer:** lagre SVG-en i `docs/icons/spole-arcticons.svg`, klar til å sendast inn til
  Arcticons-prosjektet. Ikkje send inn; Claude spør brukaren først. Legg òg ei VectorDrawable-utgåve
  i appen.
- **Stova sine eigne tillegg:** når den valde pakken manglar Spole, brukar Stova dette ikonet (med
  same farge- og fyllval).
  - Lag òg ei flat fargeutgåve (Spole sitt lime-merke på mørk flate) for fargerike pakkar.
- **Bannerkunst:** sjå over at «Ingen pakke» gir Spole eit fint banner òg.

## Rammer

- **TDD med raud/grøn:**
  - parsing av `appfilter.xml` med ekte utdrag frå Arcticons;
  - val av ikon (pakke, Stova-tillegg, eige banner);
  - innstillingane;
  - «Endre ikon» per app.
  - Bruk eit innskote grensesnitt for pakkekjelda, så testane ikkje treng ein installert pakke.
- **Skjermbilete** med `-Pstova.screens=true`: Heim med ein falsk Arcticons-liknande pakke i Aksent
  (Halloween-tema og vanleg), og Ikon-appstil. Lagre dei i `docs/images/ikonpakke-*.png` og sjå på
  dei sjølv.
- **Ingen nedlasting** av APK-ar og ingen installasjon på nokon emulator; Claude gjer det. Aldri bruk
  emulator-5560/-5562/-5564.
- **Strengar:** legg dei til slutt i blokka `<!-- Ikonpakke -->` i `values/strings.xml` (nynorsk) og
  `values-en/strings.xml`.
- **Design:** ingen glas eller blur, ingen fargelitteral utanfor `core/theme` (`DesignRulesTest`).
- **Sluttkontroll:** `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug`
  skal vere grøn.
- **Commits:** små, med `Co-Authored-By`-linja frå AGENTS.md. Ingen push.
- **Til slutt:**
  - oppdater spesifikasjonen (§5.3 Appar og §5.4);
  - skriv ein kort status øvst i `docs/HANDOFF.md`;
  - svar med commit-liste, testtal og bilete.
