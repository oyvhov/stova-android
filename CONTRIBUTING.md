# Arbeide med Stova

Les [arkitekturoversikta](docs/ARCHITECTURE.md), [designspesifikasjonen](docs/superpowers/specs/2026-10-24-stova-launcher-design.md)
og [AGENTS.md](AGENTS.md). Dei same kontraktane gjeld for menneske, Claude og Codex.

## Før du endrar noko

1. Sjekk `git status` og `docs/HANDOFF.md`. Ta vare på arbeid som alt ligg der.
2. Finn pakken som eig åtferda i arkitekturoversikta. Hald endringa lokal.
3. Ved ein feil eller ny kontrakt: legg til ein målretta test, sjå han feile, og rett årsaka.
4. Bruk kontrollerte data for UI-testar og MockWebServer for transport. Ekte data og demodata høyrer til emulatorverifiseringa.

## Bygg og testar

JDK 17 eller nyare, Android SDK 36. På Windows må wrapper-skriptet brukast, sidan det set
ASCII-stiar for Gradle og mellombelse filer:

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests 'app.stova.home.*'
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'
```

Den siste kommandoen køyrer også skjermtestane. Resultat ligg i `app/build/test-results/testDebugUnitTest/`,
lint i `app/build/reports/lint-results-debug.html`, og debug-APK i `app/build/outputs/apk/debug/`.
Kontroller Gradle-resultatet og XML-resultata. Ikkje start to bygg i same worktree samstundes.

## Emulator

[Køyr Stova på ein emulator](docs/EMULATOR.md) forklarer oppsett, demodata og skriptet som styrer
TV-en. Kort sagt:

```powershell
pwsh -NoProfile -File scripts/Start-StovaEmulator.ps1
pwsh -NoProfile -File scripts/Install-Stova.ps1 -Demo
```

Har emulatoren ekte profilar og kontoar, installer utan `-Demo`. Skriptet bruker `install -r`, så
profilar, kontoar og innstillingar blir verande. Ikkje bruk `pm clear` eller avinstallering for å
gjere ein test lettare. Ikkje rør andre emulatorar eller ein ekte TV utan at eigaren ber om det.

Prøv D-pad ned og opp, retur frå ei anna side, profilbyte, store skrifter og redusert rørsle.
Smarthusverifisering av layout krev ingen endring av lys, låsar eller andre verkelege einingar.

## Ferdig endring

Køyr relevante testar og sluttkontrollen. Sjå på skjermbileta når layouten er endra. Oppdater
spesifikasjonen når åtferda endrar seg, og hald arkitekturoversikta i takt med filflytting.
Skriv konkrete commit-meldingar. Push og publisering krev bestilling frå eigaren.

Token, signeringsnøklar, lokale innstillingar, Gradle-cache og emulatorfiler høyrer ikkje til i Git.
