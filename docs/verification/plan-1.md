# Verifisering · Plan 1 Fundament

*Dato: 25.10.2026 · Grein: `plan-1-fundament` · Bygg: debug (`app.stova.debug`)*

## Automatiske testar

| Pakke | Resultat |
|---|---|
| JVM- og Robolectric-testar (`testDebugUnitTest`) | 94 / 94 bestått |
| Lint (`lintDebug`) | 0 feil |
| Instrumentert på Stova_TV (`connectedDebugAndroidTest`) | 1 / 1 bestått (`KeystoreSecretStoreTest`, AES-GCM i ekte Android Keystore) |

## Stova_TV (emulator, Android TV 11 / API 30, x86, WHPX, port 5580)

Emulatorbiletet er eit produksjonsbygg utan root. Språket kunne ikkje setjast til norsk frå adb, så
skjermbileta under er engelske. Nynorsken er verifisert i Robolectric-testane (`nn`-qualifier).

- [x] Første oppsett med fjernkontrollen: tastaturet opnar seg først på OK, og Lagre blir aktiv når
  namnet er skrive. Lagre gir Heim med klokke og topplinje.
- [x] Appar: ekte bannerar (YouTube, Google Play). Langt OK-trykk opnar menyen, og «Legg til i
  favorittar» gir favorittrada på Heim med fokusramme.
- [x] Home-knappen: med tenesta på gir Home frå Innstillingar Stova.
  - `adb shell input keyevent HOME` går forbi inputfilteret til Android, så det er
    vindaugsfallbacken som tek over. Logcat viser at `tvlauncher` blir starta først og Stova rett
    etter, så eit kort blink er mogleg på denne vegen.
  - Tastefilter-vegen (ingen blink) kan berre testast med ein ekte fjernkontroll. `adb emu event`
    når ikkje inputsystemet på dette biletet.
- [x] Kræsjvakt: tre kræsj (`am crash`) gir `tripped: true` i `crash_guard.json`. Home går då til
  systemlauncheren.
- [x] **Funn og retting:** etter eit kræsj merkar Android tenesta som «Crashed services» og bind
  henne ikkje att. Tenesta køyrer no i eigen prosess (`:homebutton`) og les innstillinga frå disk.
  Verifisert: etter `am crash <UI-pid>` er tenesta framleis bunden, og Home gir Stova.
- [x] **Funn og retting:** tom Heim hadde ikkje noko fokus, så første D-pad-trykk forsvann.
  «Opne Appar» får no fokus (`emptyHomeFocusesOpenApps`).
- [x] Skriftstorleik 2,0: Heim og innstillingane er lesbare. Lange appnamn blir korta med «…»,
  ingenting er klipt.
- [x] Barneprofil og PIN: dekte av `ProfileGateTest`, `SettingsScreenTest` og
  `ProfileRepositoryTest`. Ikkje køyrt manuelt på emulatoren.

## Shield

- [ ] **Ikkje køyrt.** Ingen Shield var tilkopla adb. Før Stova blir brukt som launcher:
  `adb connect <shield-ip>:5555`, installer, slå på «Stova Home-knapp» i Tilgjenge, og sjekk at Home
  på den fysiske fjernkontrollen gir Stova utan at systemlauncheren blinkar.
