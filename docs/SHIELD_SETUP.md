# Set opp Stova på NVIDIA Shield

Stova er ein Android TV-launcher. Spole spelar media; Jellyfin/Emby og Home Assistant er tenester du koplar til sjølv. Denne oppskrifta er skriven for Shield med Android TV 11 (API 30) eller nyare. Ho er enno ikkje prøvd på ein fysisk Shield.

**Ikkje slå av systemlauncheren (`pm disable-user`). Nokre einingar startar ikkje då.**

## 1. Slå på nettverksfeilsøking

Opne Shield-innstillingane → Einingsinnstillingar → Om. Trykk sju gonger på byggnummeret for å aktivere utviklaralternativ. Gå tilbake til Utviklaralternativ og slå på nettverksfeilsøking. Menynamna kan variere med Shield-versjonen. Noter IP-adressa som Shield viser. Datamaskina og Shield må vere på same nettverk.

Installer Android SDK Platform-Tools på datamaskina, slik at `adb` er tilgjengeleg. Godta feilsøkingsførespurnaden på TV-en når du koplar til.

## 2. Kople til og installer APK-en

Erstatt dømeadressa med adressa til din Shield og APK-stien med APK-en du har fått. Det er ikkje publisert ein signert Stova-release frå denne arbeidsøkta.

```powershell
adb connect 192.168.1.50:5555
adb -s 192.168.1.50:5555 install -r .\stova-release.apk
```

Bruk APK-ar med same signeringsnøkkel ved oppdatering. `install -r` bevarer appdata. Ikkje avinstaller appen eller bruk `pm clear` for ei vanleg oppdatering: då mistar du profilar, kontoar og lokale innstillingar.

Debug-bygg har pakkenamn `app.stova.debug` og ligg ved sida av produksjonspakken `app.stova`. Kommandoane nedanfor gjeld produksjonspakken.

## 3. Opne Stova og lag profil

Opne Stova frå applista på Shield. Lag den første profilen med namn og avatar. Den første profilen er ein vaksenprofil. Fleire profilar, PIN og barnemodus blir sette opp under Innstillingar → Profil.

## 4. Slå på Home-knappen

Opne Shield sine Innstillingar → Tilgjenge og slå på «Stova Home-knapp». Kontroller òg Home-knappen under innstillingane i Stova. Trykk Home på fjernkontrollen og kontroller at Stova kjem fram.

Tenesta handsamar Home-tasten og systemlauncheren sine vindaugshendingar. Ho les ikkje skjerminnhald og lagrar ikkje tastetrykk.

## 5. Valfrie adb-løyve

Du kan prøve å setje Stova som standard heim:

```powershell
adb -s 192.168.1.50:5555 shell cmd package set-home-activity app.stova/.MainActivity
```

Denne kommandoen er ikkje stadfesta på Shield 9.x. Tilgjengetenesta er den primære løysinga dersom systemet ikkje godtek valet.

Du kan òg gi Stova løyve til å slå tilgjengetenesta på att dersom systemet har slått henne av:

```powershell
adb -s 192.168.1.50:5555 shell pm grant app.stova android.permission.WRITE_SECURE_SETTINGS
```

## 6. Legg til mediekonto og Home Assistant

Opne Innstillingar → Kontoar → Legg til konto. Vel ein oppdaga Jellyfin-/Emby-server eller skriv adressa sjølv. Jellyfin tilbyr Quick Connect eller brukarnamn/passord; Emby bruker brukarnamn/passord. Kontoar og heimerader høyrer til den aktive profilen.

Under Innstillingar → Smarthus legg du inn Home Assistant-adressa og loggar inn, eller bruker eit langtidstoken. HTTP er berre tillate til private adresser på heimenettet; bruk HTTPS utanfor dette. Vel kort for profilen og eventuelt «Vis på heim» for opptil tre statusbrikker. Home Assistant-tilkoplinga er felles for hushaldet. Barneprofilar har styring låst som standard.

## 7. Installer Spole

Hent Spole frå [Spole-utgjevingane](https://github.com/oyvhov/spole-android/releases) og installer APK-en på Shield. Logg inn med den same Jellyfin-/Emby-brukaren i Spole som i Stova. Spole-versjonen må støtte `spole://open`.

OK på ein medietittel opnar detaljane i Spole. Hald OK inne for handlingane Spel og Detaljar i Spole. Spole handhevar sine eigne profil-, PIN- og barnemodusreglar.

## 8. Feilsøking og retur til systemheimen

Ved tre kræsj på fem minutt slår kræsjvakta av Home-overstyringa. Opne Stova frå applista, rett årsaka og slå overstyringa på att under Innstillingar → Home-knappen.

For å stoppe overstyringa slår du henne av i Stova og slår av «Stova Home-knapp» i Shield sine tilgjengeinnstillingar. Dersom Stova var sett som standard heim, vel systemlauncheren att i systeminnstillingane. Du kan òg setje den installerte systemlauncheren med adb. Finn først den faktiske aktiviteten:

```powershell
adb -s 192.168.1.50:5555 shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME -p com.google.android.tvlauncher
```

Bruk komponentnamnet frå svaret som argument til `cmd package set-home-activity`. Dersom systemet ikkje returnerer ein komponent, bruk systeminnstillingane; ikkje gjett eit aktivitetsnamn.

Ved tomme mediarader kontrollerer du aktiv profil, serveradresse og innlogging. «Fråkopla» tyder at Stova viser førre lagra rader. Dersom Spole ikkje finn kontoen, logg inn med rett brukar også i Spole.

Slå av nettverksfeilsøking når du er ferdig med oppsettet dersom du ikkje treng vidare adb-tilgang.
