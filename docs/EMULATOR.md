# Køyr Stova på ein emulator

Med desse stega får du Stova opp på ein Android TV-emulator, med demodata eller med eigne tenester.
Skripta verkar på Windows, macOS og Linux med PowerShell 7 (`pwsh`). Dei same stega fungerer for
menneske og AI-agentar.

## Dette treng du

- JDK 17 eller nyare.
- Android SDK med Platform-Tools, Emulator og Command-line Tools. Android Studio installerer alt
  tre. Skripta finn SDK-en via `ANDROID_HOME` eller `ANDROID_SDK_ROOT`, elles på standardstaden.
- PowerShell 7: `winget install Microsoft.PowerShell` eller `brew install powershell`.
- [scrcpy](https://github.com/Genymobile/scrcpy) om du vil sjå og styre TV-en i eit vindauge
  (valfritt).

## Kom i gang

```bash
pwsh -NoProfile -File scripts/Start-StovaEmulator.ps1
```

Første gongen lastar skriptet ned Android TV-biletet (om lag 1 GB) og lagar emulatoren `Stova_TV`
i mappa `.avd/`, som ikkje er med i Git. Emulatoren har API 30 (Android 11, som Shield), 1080p,
og blir `emulator-5580`. På maskiner med ARM, som Mac med Apple-brikke, blir han API 34, sidan
API 30 ikkje finst som TV-bilete for ARM. Når emulatoren har starta, opnar scrcpy han om det er
installert.

```bash
pwsh -NoProfile -File scripts/Install-Stova.ps1 -Demo
```

Dette byggjer debug-APK-en om han manglar, installerer han og fyller Stova med demodata:
- ein profil «Demo» og fem favorittappar
- ein fiktiv Jellyfin-konto med tre bufra rader på 20 titlar, med lokal kunst
- eit Home Assistant-kort som står fråkopla

Ingenting går på nettet. Demodata blir berre lagt inn på ein emulator og på ei fersk installering.
Skriptet nektar om det alt finst eigne profilar.

Utan `-Demo` installerer skriptet berre APK-en og opnar Stova. Profilar, kontoar og innstillingar
blir verande. Med `-Build` byggjer det APK-en på nytt først.

## Ekte tenester

Hopp over `-Demo` og legg til kontoane i Stova: Innstillingar → Smarthus og kontoar. Jellyfin og
Emby blir funne på lokalnettet, eller du skriv inn adressa. For Home Assistant skriv du inn
adressa og loggar inn, eller du bruker ein langtidstoken frå profilen din i Home Assistant. Ein
emulator når datamaskina på `10.0.2.2`.

## Styr TV-en

I scrcpy er piltastane D-pad, Enter er OK, høgreklikk er Tilbake, midtklikk er Heim og Alt+M er
Meny. Agentar og skript bruker `Send-TvRemote.ps1`, som tek stega i rekkjefølgje:

```bash
pwsh -NoProfile -File scripts/Send-TvRemote.ps1 key:DPAD_UP focus key:DPAD_RIGHT hold:1500 shot:meny.png
```

| Steg | Gjer |
|---|---|
| `key:NAMN` | Trykkjer ein tast: `DPAD_UP`, `DPAD_DOWN`, `DPAD_LEFT`, `DPAD_RIGHT`, `DPAD_CENTER` (OK), `BACK`, `HOME`, `MENU` |
| `hold:MS` | Held OK inne i MS millisekund, slik ein ekte fjernkontroll gjer |
| `wait:S` | Ventar S sekund |
| `focus` | Skriv ut plassering og tekst for det som har fokus |
| `shot:FIL` | Lagrar eit skjermbilete som PNG |

`-Serial` vel eininga; standard er `emulator-5580`. Lange trykk er det som er vanskelegast å
prøve: `adb shell input keyevent --longpress` slepp tasten akkurat når tida for langt trykk går
ut, og då kan Stova sjå det som eit vanleg trykk. `hold:` registrerer i staden ein virtuell
Bluetooth-fjernkontroll med Android sitt `hid`-verktøy og held OK-tasten hans inne. Då gjentek
Android tasten slik han gjer med ein ekte fjernkontroll.

Bruk `focus` før du går vidare. Kva som får fokus i toppfeltet, avheng av kva som er slått på
for profilen. Til dømes kan ei statusbrikke stå før Smarthus og Innstillingar.

## Testar utan emulator

Einings- og UI-testane køyrer i Robolectric og treng ingen emulator. Sjå [CONTRIBUTING.md](../CONTRIBUTING.md).
Med `-Pstova.screens=true` teiknar skjermtestane også bilete til `app/build/screens`.

## Fleire emulatorar og feil

- `-Name` og `-Port` lagar og startar ein emulator til, til dømes
  `Start-StovaEmulator.ps1 -Name Stova_Demo -Port 5584`. `-Api` vel ein annan Android-versjon.
- `-Headless` startar utan vindauge, til automatisering. `-EmulatorWindow` viser emulatoren sitt
  eige vindauge i staden for scrcpy.
- Loggane ligg i `.avd/logs/`.
- På Windows kan ikkje Gradle bu i ei brukarmappe med bokstavar som «Ø». Bruk difor
  `scripts/gradle.ps1`, som flyttar Gradle og mellombelse filer til ASCII-stiar.
- Ein ekte TV: sjå [Set opp Stova på NVIDIA Shield](SHIELD_SETUP.md). Skripta tek
  `-Serial <ip>:5555` for ein TV på nettverket.
