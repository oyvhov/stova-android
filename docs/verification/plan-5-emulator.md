# Plan 5: emulatorverifisering, 25. oktober 2026

Dette er funksjonskontroll på ein emulator, med fiktive data. Tala godkjenner ikkje ytelsesbudsjetta på Shield. Ingen produksjonsrelease er bygd, signert, tagga eller publisert.

## Miljø og avvik

- Worktree `C:/Launcher/.worktrees/plan-5`, grein `plan-5-perf`.
- AVD `Stova_TV34`, API 34, 1920 × 1080, 320 dpi, port 5582, `-no-snapshot-save`. Berre `emulator-5582` er brukt.
- SDK Manager avviste `system-images;android-34;android-tv;x86_64` med «Failed to find package». `--list` viste API 34-TV berre som `x86` og `arm64-v8a`; `x86` vart brukt. AVD-en ligg i det avtalte `ANDROID_AVD_HOME=C:/Launcher/.avd`.
- `adb root` svara «adbd cannot run as root in production builds». API 34 støttar [profilgenerering utan root](https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile).
- [Benchmark/Baseline Profile 1.4.1](https://developer.android.com/jetpack/androidx/releases/benchmark#1.4.1), [ProfileInstaller 1.4.1](https://developer.android.com/jetpack/androidx/releases/profileinstaller#1.4.1), compileSdk 36. Pluginen 1.4.1 kravde `android.newDsl=false` med AGP 9.2.1. Innebygd Kotlin er framleis på; den ekstra Kotlin-kjelda blir registrert gjennom variant-API-et.
- Profil- og benchmark-APK-ane er verktøyvariantane `nonMinifiedRelease` og `benchmarkRelease`, signerte med debug-sertifikat av Baseline Profile-pluginen. Dei er ikkje distribusjonsrelease og krev ingen Stova-signeringsnøkkel.
- Demodata ligg i kjeldesettet `performance`, som berre blir teke med i debug og dei to verktøyvariantane. Innsteget avviser fysisk maskinvare og personlege profilar. Éin profil «Demo», éin fiktiv Jellyfin-konto, tre bufra rader med 20 titlar kvar, lokal PNG-kunst og eit fråkopla HA-sensorkort. Ingen verkelege tenester eller løyndomar.

## Repeter køyringa

Installer biletet og lag AVD-en éin gong:

```powershell
& C:/Android/sdk/cmdline-tools/latest/bin/sdkmanager.bat 'system-images;android-34;android-tv;x86'
$env:ANDROID_AVD_HOME = 'C:/Launcher/.avd'
$env:ANDROID_SDK_ROOT = 'C:/Android/sdk'
'no' | & C:/Android/sdk/cmdline-tools/latest/bin/avdmanager.bat create avd -n Stova_TV34 -k 'system-images;android-34;android-tv;x86' -d tv_1080p
Start-Process C:/Android/sdk/emulator/emulator.exe -ArgumentList '-avd Stova_TV34 -port 5582 -no-snapshot-save -no-boot-anim' -WindowStyle Hidden
```

Vent på `sys.boot_completed=1`. `ANDROID_SERIAL` avgrensar alle tilkopla Gradle-testar til den avtalte emulatoren:

```powershell
$env:ANDROID_SERIAL = 'emulator-5582'
pwsh -NoProfile -File scripts/gradle.ps1 :app:generateReleaseBaselineProfile '-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR'
pwsh -NoProfile -File scripts/gradle.ps1 :baselineprofile:connectedBenchmarkReleaseAndroidTest '-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR'
```

Emulatoråtvaringa blir berre undertrykt for denne funksjonskontrollen. Ikkje bruk dette argumentet for Shield-målingar. Generatoren ventar på `home-ready`, rullar med D-pad og opnar Appar og Smarthus gjennom topplinja. Markøren er borte medan radkjeldene lastar og finst òg når ferdig lasta heim er tom. Navigasjonen bruker selector-basert aktivering i topplinja fordi mellomlagra Compose-tilgjengenoder kan bli ugyldige under fokus-animasjon.

Den genererte profilen ligg i `app/src/release/generated/baselineProfiles/`. Benchmarkrapporten ligg under `baselineprofile/build/outputs/connected_android_test_additional_output/benchmarkRelease/connected/` og HTML-rapporten under `baselineprofile/build/reports/androidTests/connected/benchmarkRelease/`.

Etter benchmarken, installer verktøy-APK-en att (Gradle ryddar installasjonen etter testar), og lag demodata før måling:

```powershell
& C:/Android/sdk/platform-tools/adb.exe -s emulator-5582 install -r app/build/outputs/apk/benchmarkRelease/app-benchmarkRelease.apk
& C:/Android/sdk/platform-tools/adb.exe -s emulator-5582 shell am start -W -n app.stova/.performance.PerformanceSeedActivity
pwsh -NoProfile -File scripts/Measure-Stova.ps1 -SelfTest
pwsh -NoProfile -File scripts/Measure-Stova.ps1 -Serial emulator-5582
```

Skriptet ventar på `home-ready` utanfor den målte `am start`-tida, tek ti kaldstartprøver, returnerer frå TV-innstillingane med prosessen varm, rullar i om lag 30 sekund med PowerShell si monotone `Stopwatch` og les PSS etter ti sekund ro. Klokka inkluderer adb-overhead og unngår heiltals-overflyt i 32-bits Android-shellar. Manglande måledata gir feil, og overskridne budsjett gir avslutningskode 1. PSS blir omrekna frå KiB til MiB (`/1024`). Varmmålinga er `am start -W` si `TotalTime`, ikkje eit sjølvstendig mål av fysisk Home-tast eller teikna ramme.

Slå emulatoren av etter bruk:

```powershell
& C:/Android/sdk/platform-tools/adb.exe -s emulator-5582 emu kill
```

## Resultat

Profilgenerering: **1/1 relevant test bestått**, 21 708 linjer i `baseline-prof.txt`, inkludert 334 reglar med `Lapp/stova/home/`. Ingen reglar for `app.stova.performance`. Startupbenchmark: **1/1 relevant test bestått**, ti iterasjonar med `StartupMode.COLD` og `CompilationMode.Partial(BaselineProfileMode.Require)`. Kvar køyring hoppar medvite over testen for den andre regeltypen.

| Startupbenchmark: `timeToInitialDisplayMs` | Millisekund |
|---|---:|
| Minimum | 206,374 |
| Median | 232,454 |
| Maksimum | 329,551 |

`Measure-Stova.ps1 -SelfTest`: **24/24 bestått**, avslutningskode 0. Parserane, medianen, ugyldige/manglande verdiar og grensene i D-pad-sekvensen er dekte. Raud fase gav «Not implemented» før parserane og før D-pad-funksjonen vart implementerte. Første integrasjonskøyring avdekte overflyt i shell-nanosekundaritmetikk og vart korrekt avvist med «Ingen teikna ramer»; dette er retta med den monotone vertsklokka.

Måleskriptet på `emulator-5582`, R8-minifisert og ikkje-debuggbar `benchmarkRelease`, **Android Debug-sertifikat**:

| Måling | Verdi | Budsjett for Shield | Skriptresultat |
|---|---:|---:|---|
| Kaldstart, median av ti `TotalTime` | 313,50 ms | ≤ 900 ms | OK |
| Varm Home, `TotalTime` | 98,00 ms | ≤ 300 ms | OK |
| Hakkande ramer | 3,66 % | ≤ 5 % | OK |
| PSS | 67,30 MiB | ≤ 250 MiB | OK |

Kaldstartprøvene var `263, 277, 274, 373, 384, 328, 369, 306, 321, 290` ms. Rulling: 147 tastetrykk over 30,14 sekund, med 150 ms pause mellom adb-kalla. Skriptet avslutta med kode **0**. Desse tala gjeld den lokale emulatoren med liten, syntetisk kunst og få installerte appar; dei godkjenner ikkje Shield-budsjetta eller langtidsbruk.

`docs/images/home.png`, `apps.png` og `smarthome.png` er tekne frå denne emulatoren med demodata og visuelt kontrollerte. Engelsk UI kjem frå emulatoren sitt engelske språkval. Ingen verkelege namn, tenesteadresser eller token er synlege. Smarthusbiletet viser eit utilgjengeleg demokort og innloggingsstatus. Emulatoren vart slått av med `adb -s emulator-5582 emu kill` etter opptaka.

Baseline-/benchmark-implementasjonen er committa i `8409d65`; måleskriptet og måleresultata i `7da6b7c`.

Sluttkontroll:

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'
```

**BUILD SUCCESSFUL**: 233/233 testar bestått, 0 feil, 0 hoppa over. DesignRulesTest 3/3. Lint: 0 feil, 22 åtvaringar og eitt hint. Debug-APK-en er bygd i `app/build/outputs/apk/debug/app-debug.apk`. Dei seks lokale dokumentasjonslenkjene er kontrollerte. Ingen kodeendringar vart nødvendige etter denne kontrollen.

## Attståande verifisering

Brukaren må opprette og sikre signeringsnøkkelen, fullføre signeringsoppsettet og verifisere installasjon/oppdatering med den same nøkkelen. Plan 5 oppgåve 4 (inkludert to timar langtidsmåling), faktisk Shield-oppsett etter oppskrifta og release-storleik/hash står att. Versjonen er framleis 0.1.0; ingen 1.0.0-tag er laga.
