# Stova Plan 5 – Ytelse og release · implementasjonsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stova held ytelsesbudsjettet i spesifikasjonen §3 på Shield. Releasen er eit signert APK med Baseline Profile, med eit oppsettdokument for Shield.

**Architecture:** Ein `:baselineprofile`-modul genererer profilen frå ein typisk brukarflyt (kaldstart → heim → rulling). `profileinstaller` i appen installerer han. Eit PowerShell-måleskript køyrer dei fire målingane med adb og feilar når eit budsjett er brote. Ytelsesfeil blir funne med Perfetto og retta i den aktuelle koden.

**Tech Stack:** Som Plan 1–4, pluss `androidx.baselineprofile` (Gradle-plugin), `androidx.benchmark:benchmark-macro-junit4` og `androidx.profileinstaller:profileinstaller`. Bruk nyaste stabile 1.4.x av benchmark/baselineprofile og 1.4.x av profileinstaller som ikkje krev compileSdk 37. Sjekk <https://developer.android.com/jetpack/androidx/releases/benchmark>, og skriv versjonane i commit-meldinga.

**Spec:** `docs/superpowers/specs/2026-10-24-stova-launcher-design.md` §3, §9, §13

## Avgrensing for økta 2026-10-25

Brukaren har bestilt oppgåve 2 og 3 og dokumentasjonsdelen av oppgåve 5 (steg 1). Oppgåve 1 og 4, produksjonsrelease, versjonsauke, oppdateringstest, Shield-verifisering og tag er utsette. Berre `emulator-5582` / `Stova_TV34` er tillaten i denne økta. SDK-registeret manglar API 34-TV `x86_64`; `x86` er brukt. Sjå `docs/HANDOFF.md` under Plan 5 og `docs/verification/plan-5-emulator.md` for køyrde kommandoar og resultat.
## Global Constraints

- Alle Global Constraints frå Plan 1–4 gjeld.
- Budsjett (release-bygg på Shield, alle må haldast):
  - kaldstart median av 10 ≤ 900 ms
  - varm Home ≤ 300 ms
  - hakkande ramer ≤ 5 % over 30 s D-pad-rulling
  - PSS ≤ 250 MB på heim
- Release-nøkkelen ligg utanfor git i `C:\Launcher\.signing\stova-release.jks`, med `signing.properties` i rota (git-ignorert). Ein release-build utan nøkkel skal feile med meldinga «Release krev signing.properties og Stova-nøkkelen. Ikkje lag ein ny nøkkel.» (etter førstegongsoppsettet i oppgåve 1).
- Systemlauncheren blir aldri slått av, verken i skript eller dokumentasjon.

## Review Focus

1. **Shield med 100+ installerte appar og store bannerar.** Forventa: Appar-sida rullar utan hakking, og banner-cachen held seg innanfor 64 innslag. Måling i oppgåve 4.
2. **Kaldstart utan nett (Wi-Fi ikkje oppe enno etter boot).** Forventa: heim blir teikna frå øyeblikksbiletet innan budsjettet, og nettverket blir ikkje venta på. Måling i oppgåve 4.
3. **Langvarig bruk (Stova open i dagevis).** Forventa: PSS veks ikkje over tid. Ingen lekkasje frå WebSocket, kameraoppdatering eller backdrop-rotasjon. Måling i oppgåve 4 (før og etter 2 timar).
4. **Oppdatering over ein eldre installasjon.** Forventa: `install -r` bevarer profilar, kontoar og tokens. Test i oppgåve 5.
5. **Brukar følgjer oppsettdokumentet på ein Shield som aldri har sett Stova.** Forventa: alle steg verkar i rekkjefølgje, og ingen steg krev å slå av systemlauncheren. Sjekk i oppgåve 5.

---

### Task 1: Release-signering

**Files:**
- Modify: `app/build.gradle.kts` (signeringsoppsett og sperre som i Spole sitt `app/build.gradle.kts`), `.gitignore` (`.signing/`)
- Create: `signing.properties` (lokalt, ikkje i git)

**Interfaces:**
- Produces: `assembleRelease` signert med `stableRelease`-konfigurasjonen, og ein `doFirst`-sperre på `validateSigningRelease`/`packageRelease`.

- [ ] **Step 1: Lag nøkkelen (éin gong, brukaren skriv passordet sjølv)**

```powershell
New-Item -ItemType Directory -Force C:\Launcher\.signing | Out-Null
keytool -genkeypair -v -keystore C:\Launcher\.signing\stova-release.jks -alias stova -keyalg RSA -keysize 4096 -validity 10000
```
`signing.properties`: `storeFile=.signing/stova-release.jks`, `storePassword=…`, `keyAlias=stova`, `keyPassword=…`. Brukaren tek reservekopi av nøkkelen. Ein tapt nøkkel tyder at oppdateringar ikkje kan installerast over eksisterande Stova.

- [ ] **Step 2: Legg til signering og sperre i `app/build.gradle.kts`**

- [ ] **Step 3: Verifiser sperra og signeringa**

Run: flytt `signing.properties` mellombels → `pwsh -NoProfile -File scripts/gradle.ps1 assembleRelease`
Expected: FAIL med meldinga i Global Constraints. Flytt fila tilbake → same kommando → BUILD SUCCESSFUL. `& "C:\Android\sdk\build-tools\<nyaste>\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk` viser `stova`-sertifikatet.

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts .gitignore && git commit -m "build: release signing with refusal when key is missing"
```

---

### Task 2: Baseline Profile

**Files:**
- Create: `baselineprofile/build.gradle.kts`, `baselineprofile/src/main/java/app/stova/baselineprofile/BaselineProfileGenerator.kt`, `StartupBenchmark.kt`
- Modify: `settings.gradle.kts` (`include(":baselineprofile")`), rot-`build.gradle.kts` (plugin `androidx.baselineprofile` og `com.android.test`), `app/build.gradle.kts` (plugin `androidx.baselineprofile`, `implementation(profileinstaller)`, `baselineProfile(project(":baselineprofile"))`)
- Create (generert): `app/src/release/generated/baselineProfiles/baseline-prof.txt`

**Interfaces:**
- Produces:
  - `class BaselineProfileGenerator { @get:Rule val rule = BaselineProfileRule(); @Test fun generate() }`. Reise:
    1. `pressHome()`, `startActivityAndWait()`.
    2. Vent på `By.desc("home-ready")` (5 s).
    3. 10 × `pressDPadRight`, 3 × `pressDPadDown`, 10 × `pressDPadRight`.
    4. Opne Appar via topplinja, deretter Smarthus om det finst.
  - `class StartupBenchmark { @Test fun coldStartup() }` med `StartupMode.COLD`, `CompilationMode.Partial(BaselineProfileMode.Require)` og 10 iterasjonar.
  - `HomeScreen` set `Modifier.semantics { contentDescription = "home-ready" }` på rotboksen når første rader er teikna (og berre då). Dette blir brukt av generatoren og måleskriptet.
  - Generatoren treng `adb root` før API 33. API 34 støttar generering utan root. Stova_TV (API 30) og Stova_TV34 (API 34) er produksjonsbilete utan root. For denne økta er Stova_TV34 laga med `system-images;android-34;android-tv;x86` på port 5582; `x86_64` finst ikkje i SDK-registeret.

- [x] **Step 1: Skriv generatoren og benchmarken, og legg til `home-ready`-markøren**

- [x] **Step 2: Generer profilen på Stova_TV34 med seed-data (éin profil, éin Jellyfin-konto)**

Run: `$env:ANDROID_SERIAL='emulator-5582'; pwsh -NoProfile -File scripts/gradle.ps1 :app:generateReleaseBaselineProfile`
Expected: BUILD SUCCESSFUL. `baseline-prof.txt` finst og inneheld linjer med `Lapp/stova/home/`.

- [x] **Step 3: Køyr benchmarken**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 :baselineprofile:connectedBenchmarkReleaseAndroidTest`
Expected: PASS. Rapporten viser `timeToInitialDisplayMs`. Noter medianen i commit-meldinga.

- [x] **Step 4: Commit**

```bash
git add -A && git commit -m "perf: baseline profile generator, startup benchmark and profile installer"
```

---

### Task 3: Måleskript for Shield

**Files:**
- Create: `scripts/Measure-Stova.ps1`

**Interfaces:**
- Produces: `pwsh -NoProfile -File scripts/Measure-Stova.ps1 -Serial <serial> [-Package app.stova] [-SelfTest]`. Skriptet skriv ein tabell (måling, verdi, budsjett, OK/FEIL) og avsluttar med kode 1 når eit budsjett er brote. Funksjonar i skriptet:
  - `Get-StartTotalTime([string]$amOutput): int`: les `TotalTime: <n>`.
  - `Get-JankPercent([string]$gfxinfo): double`: les `Janky frames: <n> (<p>%)`.
  - `Get-TotalPssKb([string]$meminfo): int`: les `TOTAL PSS: <n>`, eller første tal etter `TOTAL` i eldre format.
  - Målingar:
    1. **Kaldstart:** 10 × `am force-stop`, deretter `am start -S -W -n $Package/.MainActivity`. Median av `TotalTime`.
    2. **Varm Home:** `input keyevent KEYCODE_HOME` mot ein annan app i forgrunnen (`am start -n com.android.tv.settings/.MainSettings`), og `am start -W` utan `-S`.
    3. **Hakking:** `dumpsys gfxinfo $Package reset`. Deretter 30 s med `input keyevent` som vekslar 20 × DPAD_RIGHT og 20 × DPAD_LEFT med 150 ms pause, og 3 × DOWN/UP. Til slutt `dumpsys gfxinfo $Package`.
    4. **PSS:** `dumpsys meminfo $Package` på heim etter 10 s ro.
  - `-SelfTest` køyrer dei tre parse-funksjonane mot innebygde eksempeltekstar og skriv `SELFTEST OK`, eller avsluttar med kode 1.

- [x] **Step 1: Skriv skriptet med `-SelfTest` først**

Eksempeltekstar i skriptet:
- `"Status: ok`nLaunchState: COLD`nTotalTime: 812`nWaitTime: 830"` → 812
- `"Total frames rendered: 1200`nJanky frames: 36 (3.00%)"` → 3.0
- `"           TOTAL PSS:   187654            TOTAL RSS:   250000"` → 187654

- [x] **Step 2: Køyr sjølvtesten**

Run: `pwsh -NoProfile -File scripts/Measure-Stova.ps1 -SelfTest`
Expected: `SELFTEST OK`, kode 0.

- [x] **Step 3: Køyr mot Stova_TV34 med debug-signert benchmarkRelease installert**

Run: `adb -s emulator-5582 install -r app/build/outputs/apk/benchmarkRelease/app-benchmarkRelease.apk; pwsh -NoProfile -File scripts/Measure-Stova.ps1 -Serial emulator-5582`
Expected: tabellen blir skriven (emulatortal er berre for å teste skriptet, ikkje for budsjettet).

- [x] **Step 4: Commit**

```bash
git add scripts/Measure-Stova.ps1 && git commit -m "perf: Shield measurement script for startup, jank and memory budgets"
```

---

### Task 4: Ytelsespass på Shield

**Files:**
- Modify: `StovaApplication.kt` (StrictMode i debug: `detectDiskReads`, `detectDiskWrites`, `detectNetwork`, `penaltyLog`), og dei filene målingane peikar på
- Create: `docs/verification/plan-5-ytelse.md`

**Interfaces:**
- Consumes: `Measure-Stova.ps1`, Perfetto (`adb shell perfetto` eller Android Studio Profiler).

- [ ] **Step 1: Mål release-bygget på Shield**

Run: `adb connect <shield-ip>:5555; adb -s <shield-ip>:5555 install -r app/build/outputs/apk/release/app-release.apk; pwsh -NoProfile -File scripts/Measure-Stova.ps1 -Serial <shield-ip>:5555`
Expected: tabell. Noter tala i `docs/verification/plan-5-ytelse.md`.

- [ ] **Step 2: For kvar måling over budsjett: spor, finn årsaka, fiks, mål på nytt**

Kjende kandidatar å sjekke i denne rekkjefølgja:
1. Disk-I/O på hovudtråden ved oppstart (StrictMode-logg).
2. `JsonFileStore`-lesing før første ramme (skal vere `profiles.json` + éitt øyeblikksbilete).
3. Bilete dekoda større enn visningsstorleiken (Coil `size`).
4. Ustabile Compose-parameterar i `MediaCard`/`HaCard` (sjå `composeCompiler`-rapport som i Spole sitt `app/build.gradle.kts`, `compose_stability.conf` for `MediaItem`, `HaEntity`, `JsonObject`).
5. `BackdropLayer` som rekomponerer heile heimen ved kvar krysstoning.

Kvar fiks blir eigen commit med før/etter-tal i meldinga.

- [ ] **Step 3: Langtidsmåling**

Run: `Measure-Stova.ps1` → la Stova stå 2 timar med HA tilkopla, kamerakort synleg og rotasjon på → `Measure-Stova.ps1` igjen
Expected: PSS-auke < 20 MB. Skriv tala i dokumentet.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "perf: meet Shield budgets (see docs/verification/plan-5-ytelse.md)"
```

---

### Task 5: Oppsettdokument og første release

**Files:**
- Create: `docs/SHIELD_SETUP.md` (nynorsk), `README.md`
- Modify: `app/build.gradle.kts` (`versionName = "1.0.0"`, `versionCode = 1`)

**Interfaces:**
- Produces:
  - `SHIELD_SETUP.md` i denne rekkjefølgja:
    1. Slå på utviklaralternativ og nettverksfeilsøking på Shield.
    2. `adb connect`, deretter `adb install` av APK-en.
    3. Opne Stova frå applista, og lag profil.
    4. Slå på «Stova Home-knapp» i Innstillingar → Tilgjenge.
    5. Valfritt: `cmd package set-home-activity` og `pm grant … WRITE_SECURE_SETTINGS`.
    6. Legg til Jellyfin-/Emby-konto og HA.
    7. Installer Spole.
    8. Feilsøking: kræsjvakta, og korleis du slår av Home-overstyringa.

    Med tydeleg åtvaring: «Ikkje slå av systemlauncheren (`pm disable-user`). Nokre einingar startar ikkje då.»
  - `README.md`: kort produktomtale, skjermbilete (`docs/images/`), krav og lenkje til oppsettet.

- [x] **Step 1: Skriv dokumenta og ta skjermbilete på Stova_TV med demodata**

Run: `adb -s emulator-5582 exec-out screencap -p > docs/images/home.png` (og `smarthome.png`, `apps.png`). Skjermbileta skal ikkje innehalde namn, adresser eller tokens.

- [ ] **Step 2: Oppdateringstest over eldre installasjon**

Run: installer førre release-APK på Stova_TV, lag profil og konto, `assembleRelease` på nytt med høgare `versionCode`, og `adb -s emulator-5582 install -r app-release.apk`
Expected: `Success`. Profil, konto og HA-tilkopling er der etter oppdatering.

- [ ] **Step 3: Følg `SHIELD_SETUP.md` på Shield frå steg 1 (etter `pm clear app.stova`)**

Expected: alle steg verkar. Avvik blir retta i dokumentet.

- [ ] **Step 4: Bygg release, og noter storleik og SHA-256**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleRelease; Get-FileHash app\build\outputs\apk\release\app-release.apk -Algorithm SHA256`
Expected: alle testar bestått, 0 lint-feil. Hash og storleik blir skrivne i `docs/verification/plan-5-ytelse.md`.

- [ ] **Step 5: Commit og tag**

```bash
git add -A && git commit -m "release: Stova 1.0.0 with Shield setup guide"
git tag v1.0.0
```
Publisering til GitHub (repo, release eller GitHub Pages) blir gjort berre når brukaren ber om det.
