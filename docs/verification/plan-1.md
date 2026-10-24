# Verifisering · Plan 1 Fundament

*Dato: 25.10.2026 · Grein: `plan-1-fundament` · Bygg: debug (`app.stova.debug`)*

## Automatiske testar

| Pakke | Resultat |
|---|---|
| JVM- og Robolectric-testar (`testDebugUnitTest`) | _fylt inn under_ |
| Lint (`lintDebug`) | _fylt inn under_ |
| Instrumentert på Stova_TV (`connectedDebugAndroidTest`) | _fylt inn under_ |

## Manuell sjekkliste

### Stova_TV (emulator, API 30 Android TV, port 5580)

- [ ] Første oppsett → profil → Heim med klokke og topplinje
- [ ] Barneprofil og PIN på vaksenprofilen; byte frå barneprofilen krev PIN
- [ ] Appar: favoritt, flytt, skjul, appinfo; appane startar
- [ ] Tilgjengetenesta på → Home på fjernkontrollen gir Stova
- [ ] Tre tvinga kræsj → Home går til systemlauncheren; «Slå på att» verkar
- [ ] Skriftstorleik 2,0: profilval, topplinje og innstillingar er lesbare

### Shield

- [ ] Ikkje køyrt: ingen Shield var tilkopla adb (`adb devices` var tom). Må gjerast med
  `adb connect <shield-ip>:5555` før Stova blir brukt som launcher.
