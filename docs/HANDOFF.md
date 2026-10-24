# Status
2026-10-25: Plan 2, oppgåve 1–9 er ferdige og committa etter raud/grøn TDD; alle stega er haka av.
Ingen påbyrja oppgåver står att. Oppgåve 10 er medvite ikkje køyrd etter brukarønske.
Sluttkontroll: 147/147 køyrde einingstestar (10 valfrie skjermtestar hoppa over), DesignRulesTest 3/3, lint 0 feil, assembleDebug OK.
Eigen AppScreensTest-køyring: 9/9 bestått, ti PNG-filer i app/build/screens; dei tre nye mediebileta er visuelt kontrollerte.
Ekte Jellyfin-/Emby-serverar, Shield og emulator er ikkje verifiserte. Ingen spørsmål; ingen push. Arbeidet er gjort i plan-2-worktreen.

## Plan 2
- Berre denne worktreen er lesen og endra, slik brukaren bad om. Spole-kjelder utanfor mappa er derfor ikkje kopierte; tenestelogoane er lokale, enkle vektorvariantar med originalfargane. Eksakt samsvar med Spole er ikkje verifisert.
- Tomatmerket og TMDB-merket er lokale, dempa variantar; Spole sine originalfiler er ikkje lesne utanfor worktreen.
- Heroen har 45 % som minimum og kan vekse ved stor skrift for å unngå klipping; omtalen fell bort ved skriftstorleik ≥ 1,5.
- Watch Next avviser òg intent-selectorar og fjernar eksterne flagg før opning i eigarpakken.
- Ekstra regresjonar etter oppgåve 9: tokenbyte i biletlaster, Emby-header ved vidarekopling, korrekt tenestetype ved LAN-svar utan produktnamn, avvist løyve, blokkert TV-provider og D-pad/OK/Meny på mediekort.
- UI-testane bruker den smale AddAccountContent-grensa med ekte REST mot MockWebServer og eit lokalt kontolager; produksjonsflyten brukar same kode.
- Quick Connect-nettverkstesten bruker reell coroutine-klokke: virtuell timeout kan gå ut før MockWebServer-tråden får svart.


### Commit-oversikt

| Oppgåve | Commit |
|---|---|
| 1 Modell og parser | `f829d7d` |
| 2 REST og fallback | `487b0ca` |
| 3 Kontoar og pålogging | `39b6ffa` |
| 4 Konto-UI | `71d947a` |
| 5 Rader og øyeblikksbilete | `c1c31f1` |
| 6 Kunst, kort og score | `26aa6ea` |
| 7 Hero og bakgrunn | `4a5627a` |
| 8 Spole-opning | `2ac689e` |
| Brukarønskte skjermbilete | `acc5ae6` |
| 9 Watch Next | `de4eb16` |
| Ekstra regresjonar og rettingar | `6d4e8dc` |

### Verifikasjon

- `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug`: PASS. 157 registrerte testar, 147 bestått, 10 valfrie skjermtestar hoppa over; ingen feil. Lint: 0 feil, 22 åtvaringar, 1 hint.
- `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests app.stova.AppScreensTest '-Pstova.screens=true'`: 9/9 PASS. PowerShell treng hermeteikn rundt eigenskapen med punktum.
- Nye bilete: `app/build/screens/09-home-media-hero.png`, `10-home-media-posters.png`, `11-home-media-font-2.png`, alle 1920 × 1080. Falsk profil «Demo», falsk konto på `demo.invalid`, lokalt teikna kunst; ingen ekte tenestekontoar eller namn.
- APK: `app/build/outputs/apk/debug/app-debug.apk`. Ingen installasjon eller manuell sjekk mot ekte serverar (oppgåve 10).
- Raud fase for kvar oppgåve var manglande produksjonsgrensesnitt i dei nye testane. Quick Connect-testen avdekte òg virtuell timeout mot ekte MockWebServer-tråd; redirect-regresjonen feila med uønskt Emby-header før rettinga.
