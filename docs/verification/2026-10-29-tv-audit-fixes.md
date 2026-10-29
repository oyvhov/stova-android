# Rett TV-funn og smarthuspopup-ar

29.10.2026. Bestillinga omfattar M2, M1, A1, A2, S1, S2, K1, K2, K3, U1, P1 og Y2 frå [gjennomgangen](2026-10-29-tv-audit.md). Eigaren godkjende testing og installering på Shield og Loftet. Ingen publisering.

## Endringar og kontroll

| Funn | Ferdig åtferd | Kontroll |
|---|---|---|
| M2 | Delvis sett film viser «Hald fram · 143 min att»; ferdig sett viser «Spel». | Einingstest og ekte Loftet. |
| M1 | Tilbake frå filmdetaljar opna av Stova går rett til same kort i Stova. | 73 målretta Spole-testar; ekte Shield og Loftet. |
| A1 | Alle appar ligg sist i favorittrada. | Fokus/callback-test og ekte Loftet. |
| A2 | Lite ikon og namn, Opne først; fjerning lenger nede. | Ferskt OK etter langt opningstrykk testa; ekte Loftet. |
| S1 | Først rom, så gjennomgang av føreslåtte kort før lagring; tekniske sensorar blir ikkje automatisk foreslått. Tomt dashbord fokuserer Legg til kort. | Rom-, lagrings- og tomtilstandstestar; emulator. Ekte Loftet henta rom og viste forslag for Arkja og Bad. Etter siste retting: eitt Tilbake frå Bad → romlista med Bad fokusert; eitt til → uendra tomt dashbord. |
| S2 | Eiga roleg smarthusflate og namn ved fokus på heimbrikker. | Ekte Shield og emulator; ingen filmbilete blir lasta for denne bakgrunnen. |
| K1 | Avslått lys viser Av og 0 %, ikkje ein oppdikta 100 %-verdi. | Manglande brightness testa; emulator Av → 1 % med Høgre; ekte Shield på 80 %. |
| K2 | Romtemperatur og Mål er skilde; Drift og Førehandsval opnar eigne lister. Kjende presetnamn er omsette. | Emulator og ekte Shield; rå tenesteverdiar bevarte i test. |
| K3 | Fast namn/rom og Lukk; volum først ved TV-kjelde; eiga kjeldeliste; vêr på tvers og rullbar straumoversikt. | Fem panel på Shield, D-pad-retur til Kjelde; emulator og UI-testar, også skriftstorleik 2,0. |
| U1 | Heile ikonførehandsvisinga er synleg ved inngang. | Fokus-/grensetest og ekte Loftet. |
| P1 | Legg til mediekonto har eiga overskrift og viser berre innlogginga. | Fokus-/innhaldstest og ekte Loftet. |
| Y2 | Teknisk adb-oppsett ligg under Avansert oppsett; status for Heim-knappen kjem først. | UI-test og ekte Loftet. |

## Bygg og testar

Full Stova-runde fann 859 testar: 800 bestod, 58 skjermtestar var hoppa over, og éin gammal appmenytest venta favorittendringa som første handling. Testen vart oppdatert til den bestilte Opne-handlinga, med same vern mot at det lange opningstrykket aktiverer handlinga. Alle ti AppsScreen-testane bestod ved ny køyring. Samla 801 beståtte og 58 hoppa over; ikkje éin samanhengande grøn fullkøyring.

Etter siste popupjustering og diagnostikk: 36 testar grøne (`TvControlPanelTest`, `StovaDialogTest`, `DesignRulesTest`, `ThemePaletteTest`, `HaSocketTest`). Siste romretting: 43 testar grøne (`RoomSetupTest`, `AddCardV2Test`, `SmartHomeScreenTest`, `StovaDialogTest`, `DesignRulesTest`, `ThemePaletteTest`). Den nye testen av Tilbake i heile romdialogen feila først, og består med delt navigasjon og fokusretur. Siste `lintDebug`: null feil, 69 åtvaringar og eitt hint. `assembleDebug` og `assembleTv` bestod. Ingen påstand om nye målte rammetidsgevinstar.

Endeleg Stova TV-APK SHA-256: `fa867dced50151eab843fa3eaa6ada76a5b0a79c9e58c2e98b7fbed63a1716a6`. Same hash lesen frå den installerte APK-en på begge TV-ar.

Spole: commit `720979c`, 73 målretta testar, signert release og lintRelease grønt. APK SHA-256 `b2b29163648f82ca57cd59e35308faee98f1e4bec9e4bbff68851e9129cef39b`, beta26/163. Same hash verifisert på begge TV-ar. Sjå `C:\JellyBin\reelstack-android\docs\verification\2026-10-29-launcher-return.md`.

## Einingar og avvik under testen

Stova er installert med `Install-Stova.ps1 -Tv` / `install -r` på Shield (Android 11) og Loftet (Android 14). Heim-knapp-tenesta var aktiv etter oppdateringa på begge. Ingen sletting av appdata, avinstallering, tvangsstopp eller deaktivering av systemlauncheren.

Ein feil i den manuelle D-pad-testen aktiverte Logg ut av Midttunet på Loftet. Kontoen vart straks gjenoppretta som Øyvind gjennom Quick Connect i den eksisterande Jellyfin-nettlesarøkta. Dette var ein testfeil, ikkje ein installasjonsfeil. Ingen passord vart endra. Kontolista, profilbiletet og Hald fram-rada med same filmar og framdrift er visuelt stadfesta etter gjenopprettinga. Lokal konto-ID er ny; eventuelle gamle radval eller biblioteksnarvegar knytte til den gamle ID-en er ikkje samanlikna. Spole-returen vart også prøvd på nytt etter innlogginga, med Not Without Hope → eitt Tilbake → same Stova-kort.

Home Assistant viste Fråkopla på begge etter første installering. HTTP til den konfigurerte tenaren svara 200 frå begge TV-ar, og WebSocket-handtrykket frå PC-en svara auth_required. Etter siste installering og ny inngang i Stova var begge tilkopla utan endring av HA-konto eller adresse. Shield henta ferske prognosar/prisar og oppdaterte romtemperaturen frå 25 til 24 °C; tilkoplinga heldt seg også etter meir enn 30 sekund i Android-innstillingane og retur. Loftet henta det verkelege romregisteret og kortforslaga. Årsaka til den tidlege fråkoplinga er ikkje fastsett; ingen funksjonell transportretting blir hevda. Avgrensa transportdiagnostikk er no også med i TV-bygget: berre feilkategori og HTTP-status, aldri token, adresse eller responsinnhald.

Ingen Stova-krasj funne i krasjbufferen ved sluttkontroll. Heim-knapp-tenesta var framleis aktiv på begge. Begge vart sette tilbake til Heim og deretter i dvale; Asleep og skjerm OFF vart kontrollerte.

## Bevis og avgrensing

Originalbilete, byggloggar og testoversikt ligg lokalt og git-ignorert i `C:\Launcher\.superpowers\audit-fixes-2026-10-29`. Galleriet brukar berre visuelt kontrollerte bilete; skjermspararar, tidlege blanke rammer og misnamngitte mellomsteg blir utelatne.

Kontrollkommandoar vart prøvde mot ein isolert emulator-fixture med minnedata. Ingen ekte lys-, varme-, volum-, kjelde-, lås- eller portkommando vart sendt. Ingen filmavspeling. Kamera, fysisk fjernkontroll, TalkBack, barneprofil og full avspeling er ikkje fysisk prøvde. Stor skrift er prøvd på emulatoren. Spole-returen er fysisk prøvd med den vaksne Jellyfin-profilen; Emby og barnevern er dekte av målretta testar.
