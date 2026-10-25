# Stova utan glød og med faste fokusvegar – installert; Spole beta22 under bygg
2026-10-25: Plan `2026-10-25-directional-focus.md` er fullført. Heim, Innstillingar og Smarthus har eksplisitte fokusvegar; fokusskuggen er heilt fjerna. Tre underagentar deltok; hovudagenten eigde samla QA.
609/609 testar inkludert skjermprøver, 0 hoppa over; lint 0 feil (39 åtvaringar, 1 hint), debug-bygg OK. Endeleg APK a05d0c47931a9f57a8e1e25f1a5fae134f6bd09c89ba09bf1483e47478e75297 er installert med -r på emulator-5580 og Shield; installerte hash-ar er like.
D-pad toppikon/rader, lange innstillingslister og appmeny etter touch-hald er prøvde med ekte data. Smarthus-kort er prøvde på emulator; Shield har tomt dashbord. Profil- og kontofiler er byteidentiske før/etter navigasjonsrunden. Sjå `docs/verification/directional-focus.md`.
Spole beta21/158 er signert/installert og opnar direkte varm/kald lenkje etter eigaren si innlogging. Faktisk Stova-korttrykk avdekte ein ekstra Android-task-feil; Spole-agenten byggjer beta22/159 med raw-URI-mottak og separat receiver-affinity i `C:/JellyBin/spole-launcher-link`. Ekte korttrykk/retur må prøvast før Spole blir meldt ferdig.
Ingen push. Kontoar, favorittar, eigaren sine nye radformat og standardlauncher er bevarte. Ingen HA-styring eller datatømming.

# Shield installert; glød, fokusvegar og Spole-oppdatering under arbeid
2026-10-25: Etter uttrykkeleg bestilling er Stova `0ed3e34` og Projectivy 1.1.9 installerte med -r på Shield. Installerte hash-ar er verifiserte; Projectivy og transparente appkort er valde. Kontoar og andre profilval/favorittar er bevarte. Sjå `docs/verification/shield-projectivy-install.md`.
Eigaren melder kraftig glød og D-pad som hoppar feil i toppfelt, Smarthus og menyar. Ny plan: `2026-10-25-directional-focus.md`. Ingen slik kode er endra enno; fjern shadowElevation/ambientShadowColor/spotShadowColor og legg eksplisitte fokusvegar med tastetestar.
Eigaren ber òg om Spole som opnar rett tittel. Shield har produksjon `app.reelstack` beta20 / 157 utan tittellenkje. Implementasjonen er på rein eksisterande grein `feat/spole-launcher-link` i `C:/JellyBin/spole-launcher-link` (b850ea0, base beta18); origin/main er no beta20 (1a8003a). Primærmappa reelstack-android er rein, men beta13 og skal ikkje brukast som grunnlag for ny oppdatering.
Gjeldande Spole release-reglar er lesne. Same eigarnøkkel finst; signing.properties finst i primærmappa, men ikkje i feature-worktree. Neste: flett beta20 inn i featuregreina, bygg/verifiser høgare lokal signert utgåve og installer på Shield. Ingen publisering er bestilt.
Éin agent arbeider. Stova-testgrunnlag er 588/588 grøne før nye fokusrettingar. Ingen nye testar for desse rettingane eller ny Spole-APK er køyrde/bygde enno. Ingen push.

# Projectivy og originalt transparent Spole-merke – fullført
2026-10-25: Plan `2026-10-25-projectivy-spole.md` er fullført. Offisiell Projectivy Icon Pack 1.1.9 er installert separat og vald på emulator-5580; appkortbakgrunn er Av. Pakka sine 953 mappingar / 899 ikon blir brukte, pluss Stova sitt eige Spole-supplement i veljaren.
Eigaren bad om originalen: endeleg Spole SVG/PNG/Android-vektor bevarer dei seks originale stiane og transformasjonane frå Spole sitt TV-banner; berre bakgrunnen er fjerna. Sjå `docs/icons/PROJECTIVY.md`. Breie logoar bevarer sideforholdet.
588/588 testar inkludert native skjermprøver, 0 hoppa over; lint 0 feil (38 åtvaringar, 1 hint), debug-bygg OK. Endeleg APK er installert med -r: SHA-256 `1569035b96db6e5d7d4d8840233d874515418759c93c4c01013fde52205cf361`.
Rapport: `docs/verification/projectivy-spole.md`. Spole/YouTube utan kortbakgrunn og søkbart `stova_spole` er prøvde med ekte appar. Favorittar/overstyringar er uendra; berre bestilt ikonpakke/bakgrunn er endra i profilen, kontofila er byteidentisk. Ingen HA-styring.
Tekstfelt har no fylt flate/nøytral understrek, også i ikonveljaren; eiga raud/grøn pikselprøve dekkjer at aksentramma er borte. Ingen påbyrja steg står att, ingen push eller Shield-installasjon. Fysisk fjernkontroll er ikkje prøvd i denne runden.

# Roleg fokus og redigerbar framside – fullført; Projectivy er neste
2026-10-25: Plan `2026-10-25-soft-focus-home-editing.md` er fullført. Svakt løft er standard i kort og menyar; gamle RING-profilar får same vising. Radnamn/storleik/kjeldevising kan endrast frå Heim og Innstillingar; hero-fakta er valfrie.
583/583 testar inkludert skjermprøver, 0 hoppa over; lint 0 feil (38 åtvaringar, 1 hint), debug-bygg OK. Rapport: `docs/verification/soft-focus-home-editing.md`. Oppdatert på emulator-5580 med ekte data, ikkje Shield; ingen push.
Mine appar vart prøvd som radnamn, overlevde ny installasjon/kald start og er sett tilbake til standard. Tidlegare profilfelt er uendra, kontofila er byteidentisk. Redusert rørsle er kontrollert i native skjermprøver; fysisk fjernkontroll står att.
Ny bestilling under arbeidet: ta inn `SicMundus86/ProjectivyIconPack` og lag eit Spole-ikon i same stil. Offisiell APK 1.1.9 er lasta til `.superpowers/Projectivy_Icon_Pack_1.1.9.apk`; pakke `dev.projectivy.blueprint.app`. Installasjon på emulator er påbyrja; ingen Stova-kode for denne pakken er endra enno.
Berre éin agent i mappa. Originalpakka skal haldast separat; eige Spole-supplement kan følgje Stova. Ingen spørsmål til brukaren.

# Kortare retry etter fråkopla medierader – fullført
2026-10-25: Ny bestilling om å halde fram etter Claude; planen `2026-10-25-offline-retry.md` er fullført. Feila henting kan prøvast att etter 30 sekund, også utan snapshot; vellukka henting held femminuttsbufferen og Spole-invalideringa.
Tre regresjonar vart køyrde raude før retting; fire nye testar er grøne. Full sluttkontroll inkludert skjermtestar: 575/575, 0 hoppa over, lint 0 feil (38 åtvaringar, 1 hint), debug-bygg OK.
Emulator-5580 er oppdatert med `install -r` og ekte data. Kald start og retur til Heim gir levande rader; snapshot kan vise «Offline» under første henting. Profil- og kontofiler er byteidentiske før/etter.
Rapport: `docs/verification/offline-retry.md`; tre stillbilete i `docs/images/`. Overgangsklipp: `.superpowers/offline-retry-transitions.mp4`, 30,21 s, 1280 × 720, git-ignorert. Langt OK opnar vêrpanel, Back lukkar det; ingen styringshandlingar.
Ingen påbyrja steg står att. Ingen push eller installasjon på Shield i denne bestillinga. Eldre utsette Shield-/signerings-/release-punkt er uendra; ingen spørsmål til brukaren.

# Stoppvilkår: Plan 1 er allereie avkryssa
2026-10-25 kl. 18:29 lokal tid: Lese AGENTS.md, arkitektur, arbeidsflyt, spesifikasjon og siste overlevering. Plan 1 har 65/65 avkryssa steg, sist oppgåve 13 steg 4; stoppar etter brukaren sitt uttrykkelege Plan 1-vilkår.
Ingen nye kodesteg er påbyrja eller fullførte i denne økta. Plan `2026-10-25-offline-retry.md` frå førre overlevering står urørt frå første steg.
Ingen testar, emulator- eller Shield-prøver er køyrde på nytt; eldre utsette verifiseringspunkt er uendra. Ingen ny fullført oppgåve å føre i ledgeren.
Arbeidstreet var reint ved start. Berre denne statusen er endra; ingen push eller publisering. Ingen spørsmål til brukaren.

# Fråkopla medierader og overgangsklipp – overlevert til Codex
2026-10-25: Claude si vekegrense er på 96 %, så denne bestillinga er overlevert etter AGENTS.md. Aktuell plan: `docs/superpowers/plans/2026-10-25-offline-retry.md`; ingen steg er påbyrja.
Claude såg «Fråkopla» på medieradene på emulator-5580 etter ein ny installasjon (HA var levande). Mistanke: `refresh` merkjer ei feila henting som fersk i fem minutt. Planen har raud test, retting, emulatorprøve og eit klipp av overgangane.
Ikkje installer på Shield og ikkje push i denne bestillinga. Claude held fram når vekegrensa er nullstilt (26.10 kl. 01:00).

# Appmeny etter langt OK – retta
2026-10-25: Kode, emulatorprøver og testplan `2026-10-25-app-longpress.md` er fullførte. Repetert OK frå opningstrykket kunne velje første dialoghandling og lukke menyen; `StovaDialog` krev no ei eiga ny KeyDown og ryddar trykket ved fokustap, utan tidsbasert sperre.
Sluttkontroll: 571/571 inkludert skjermtestane, 0 hoppa over; lint 0 feil (38 åtvaringar, 1 hint), debug-bygg OK. Native regresjonar dekkjer OK/Enter/numpad, rest-slepp og første nye trykk; appmeny/favoritt og resten av dialogane er med i fulltesten.
Heim/Appar, langt OK, touch-hald, neste menyval, Meny, Back og kort appstart er prøvde på emulator-5580 med ekte data. Profil- og kontofiler er byteidentiske før/etter på begge einingane. Rapport: `docs/verification/app-longpress.md`.
APK er installert med `-r` på emulator og Shield: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `8cf162faac67fba4397d1277345e48024b8af077ff852c1caaf43d15f60c4d7a`. Shield viste YouTube, så fysisk menyprøve står att. Valfritt spørsmål om brukaren sin inndatatype er sendt; inga avklaring var naudsynt for den påviste nøkkelfeilen.
Hovudgrein/repo er `main` i private `oyvhov/stova-android`. Tre testfiksturar er stabiliserte/oppdaterte etter fulltesten; ingen Watch Next-produksjonskode er endra. Eldre restpunkt om produksjonsrelease er uendra.

# Større medielogo i heroen – fullført
2026-10-25: Bestillinga i `2026-10-25-hero-logo.md` er fullført. Fast tekstreserve pressa logoen til 24/26 dp; faktisk metadata-/omtalehøgd bestemmer no logoplass, innanfor maks 360 × 96 dp. Transparent trimming og heile sideforholdet er bevarte.
Herohøgda, første rad og parallax er bevarte. Tre nye layoutregresjonar dekkjer kompakt hero, omtale og stor skrift. Sluttkontroll: 93/93 relevante testar, 0 hoppa over, lint 0 feil (38 åtvaringar, 1 hint), debug-bygg OK.
Endeleg APK er installert med `-r` på emulator-5580 og Shield med ekte data. Shield-logoramma gjekk frå 24 til 70,5 dp i det målte tilfellet; D-pad ned rullar framleis heroen bort. Profil- og kontofiler er byteidentiske før/etter.
APK: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `beff6d758088ca51253ddb30afaa47fadfe5eb0d12561baf2993075ccddfddf9`. Rapport og ekte skjermbilete: `docs/verification/hero-logo.md`.
Ingen påbyrja steg eller spørsmål står att i denne bestillinga. `main` i det private `oyvhov/stova-android`; eldre restpunkt om produksjonsrelease er uendra.

# Rein hero-metadata – fullført
2026-10-25: Bestillinga i `2026-10-25-hero-metadata.md` er fullført. Rating, år, alder og «Ny sesong» har ingen eigne bakgrunnar, kantar eller rundingar. Numerisk alder (også NO-15) viser lokaliserte år; andre sertifiseringar er bevarte.
Raud tomat ved minst 60 %, grøn Rotten-markering under 60 %. Statusfargar er samla i `core/theme/RatingColors`, uavhengige av profilaksent; alder brukar språkressursar med eintal/fleirtal.
Sluttkontroll: 87/87 Heim-, media-UI-, design- og relevante skjermtestar, ingen hoppa over; lint 0 feil (38 åtvaringar, 1 hint), debug-bygg OK. Stor skrift og lys/medium/mørk bakgrunn er visuelt kontrollerte; sjå `docs/verification/hero-metadata.md`.
Endeleg APK er installert på emulator-5580 og Shield med ekte data. Profil- og kontofiler er byteidentiske med før denne bestillinga. APK SHA-256: `e41c73cc28e5be98758f976e09a3e7e4d9183f39e5dcfac3d2616f3d4df8e4b4`; sti `app/build/outputs/apk/debug/app-debug.apk`.
Ingen påbyrja steg eller spørsmål står att i denne bestillinga. Hovudgrein/repo: `main` i det private `oyvhov/stova-android`. Eldre restpunkt om produksjonsrelease er uendra.

# Brukstest og releaseklargjering – fullført
2026-10-25: Plan `2026-10-25-brukstest-og-releaseklargjering.md` er fullført: ekte ikonpakke, spelarretur, profilkontroll, Shield og signeringssperre. Rapport: `docs/verification/brukstest-og-release.md`.
Retta Android/ICU-problemet som gjorde store ikonpakkar svært trege. Retur frå ei tittellenkje til Spole oppdaterer no framdrift éin gong for rett profil; vanleg femminuttsbuffer og spotlight-buffer er bevarte.
Sluttkontroll: 558/558 JVM-/skjermtestar utførte og beståtte over to køyringar, Android-regresjon 1/1, måleskript 27/27; lint 0 feil (38 åtvaringar, 1 hint), debug-bygg OK. Signeringssperra avslår release utan eigaren sitt oppsett som venta.
Endeleg APK er installert med `-r` på emulator-5580 og fysisk Shield; ekte kontoar er bevarte. APK: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `1f3f7815146bd2217eb58e4b3019d8691e00e9bbb9509425fd57fd87364a52e2`. Arcticons ligg på emulatoren; mellombelse ikonval er sette tilbake.
Shield: parallax, lange rader, Back/Home og ny framdrift frå biblioteket er prøvde. Debug-måling: kaldstartmedian 2138,5 ms, varm Home 348 ms, hakkande ramer 4,15 %, PSS 182,98 MiB. Fartskrava er ikkje release-godkjende.
Restpunkt: Spole beta20 på Shield manglar tittellenkjer. Eigaren sin produksjonsnøkkel/oppsett manglar; ingen ny nøkkel er laga. Signert release, ytelsesmåling og sertifikatfast oppdatering må gjerast seinare, sjå `docs/RELEASE_SIGNING.md`. Hovudgrein/repo: `main` i `oyvhov/stova-android` (privat).

# Design, parallax og privat GitHub – fullført
2026-10-25: Claude sine uferdige kjerne-, HA- og rørsleendringar er fullførte; ny design-/vedlikehaldsplan er ferdig. Kodecommit `2c4bb32`; ingen påbyrja steg står att i denne bestillinga.
Heim har rullande parallax, frigjord plass nedover, retur via første rad/topplinja og tittelreserve ved logofeil. Skjermar er delte i datakopling, vising og mindre komponentar; sjå `docs/ARCHITECTURE.md` og `CONTRIBUTING.md`.
Full kontroll: 556/556, ingen hoppa over, lint 0 feil, debug-bygg OK. Etter siste fokusretting: Heim/navigasjon 81/81, lint 0 feil og bygg OK. Rapport og fire ekte bilete: `docs/verification/design-og-vedlikehald.md`.
Endeleg debug-APK er installert med `-r` på emulator-5580 med ekte Jellyfin/HA; profil- og kontofiler har identiske hashar. Ingen styringshandlingar på HA eller installasjon på fysisk Shield. APK ligg i `.worktrees/plan-2/app/build/outputs/apk/debug/app-debug.apk`.
Nytt privat repo `https://github.com/oyvhov/stova-android`, standardgrein `main`; push og samsvarande fjern-commit er stadfesta. `C:/Launcher` er no hovudutsjekk på `main`. Gamle signerings-/Shield-/release-steg er framleis uverifiserte og ikkje del av denne bestillinga.

# Smarthus – fjernkontrollhandlingar fullført
2026-10-25: Heile bestillinga `2026-10-25-smarthus-handlingar.md` er fullført i `ha-v2`; ingen påbyrja steg står att. §5.2 og eigen plan er oppdaterte.
Sluttkontroll via `scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug -Pstova.screens=true`: 381/381, ingen hoppa over; HA 165, DesignRulesTest 4/4; lint 0 feil (32 åtvaringar, 1 hint), APK OK.
Ekte HA på emulator-5580: PH av/på og 222 → 224 → 222/255; mål 23,5 → 24,0 → 23,5 °C. Ferske HA-svar stadfestar alle fem relevante einingar tilbake; ti kort og tre Heim-brikker er identiske med før prøva.
Ti visuelt kontrollerte ekte bilete: `docs/images/ha-v2-handling-*.png`. Rapport: `docs/verification/smarthus-handlingar.md`; TDD-ledger og tilbakeføringskvitteringar ligg lokalt i `.superpowers/`.
Eit anna bygg avløyste APK-en under prøva; vårt bygg vart installert på nytt og kontrollert med SHA-256. Ingen spørsmål eller push. Alarm, opplåsing og garasje er berre testa med falske einingar; fysisk Shield er ikkje verifisert. Forbodne filer er urørte.
# Tilpassing v3 ferdig
2026-10-25: Heile bestillinga i `docs/briefs/2026-10-25-tilpassing-v3.md` er fullført på `appearance-v2` i appearance-worktreet; alle sju steg i planen er avkryssa. Ingen påbyrja steg står att.
Direkte radredigering på Heim, radformat/storleik/tittel, bue/fokus/framdrift, hero/bakgrunn/spotlight, toppfelt, reset/kopi, barneprofilredigering og åtte grupper med fast live førehandsvising er implementerte. Siste kodecommit: `a60fded`.
Sluttkontroll via `scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug -Pstova.screens=true`: 487/487, 0 hoppa over; AppScreensTest 40/40, DesignRulesTest 4/4; lint 0 feil (35 åtvaringar, 1 hint), debug-APK OK.
Ni visuelt kontrollerte 1920 × 1080-bilete: `docs/images/tilpassing-*.png`. Rapport med stiar, TDD-evidens og vurdering: `docs/verification/tilpassing-v3.md`; spesifikasjonen §2, §5.1 og §5.4 er oppdatert.
Ingen spørsmål, emulatorinstallasjon eller push; `app/src/main/java/app/stova/ha/**` er urørt. Fysisk fjernkontroll, ekte mediebibliotek og Shield-ytelse er ikkje verifiserte her.

# Smarthus v2 – finpuss fullført
2026-10-25: Heile finpussbestillinga er fullført i `ha-v2`; ingen påbyrja steg står att. Spesifikasjonen §5.2 og finpussplanen er oppdaterte.
Sluttkontroll via `scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug -Pstova.screens=true`: 348/348, 0 hoppa over; HA 132, DesignRulesTest 4/4; lint 0 feil (25 åtvaringar, 1 hint), APK OK.
Ekte HA på emulator-5580: to lys av/på og stadfesta tilbake på 222/255 og 84/255; mål 23,5 → 24,0 → 23,5 °C. Ti kort og tre Heim-brikker bevarte; tryggleiksutstyr urørt.
Ti visuelt kontrollerte bilete: `docs/images/ha-v2-polish-*.png` (ni ekte, eitt Robolectric/font 2). Rapport: `docs/verification/smarthus-v2-finpuss.md`; lokal TDD-ledger og tilbakeføringskvitteringar i `.superpowers/`.
Ingen spørsmål eller push. Smarthus står ope i vanleg MainActivity. Fysisk Shield og eldre release-steg er framleis ikkje verifiserte; dei forbodne filene er urørte.

# Smarthus v2 – fullført
2026-10-25: Bestillinga og alle seks oppgåvene i `2026-10-25-smarthus-v2.md` er fullførte i worktreet `ha-v2`; ingen påbyrja steg står att.
Sluttkontroll via `scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug -Pstova.screens=true`: 329/329, 0 hoppa over; DesignRulesTest 4/4, lint 0 feil (25 åtvaringar, 1 hint), APK OK.
Ekte HA på emulator-5580: ti varige kort, registerrom, femdagarsvêr, Tibber-graf og Heim-brikker; to lys er tilbake på med original lysstyrke, måltemperatur er stadfesta 23,5 → 24,0 → 23,5 °C. Tryggleiksutstyr er ikkje endra.
Rapport: `docs/verification/smarthus-v2.md`; åtte visuelt kontrollerte bilete i `docs/images/ha-v2-*.png`, originalar og kvitteringar i worktreet si `.superpowers/`.
Ingen spørsmål eller push. Fysisk Shield, verkeleg Nordpool og tryggleiksstyring er ikkje prøvde; dei gamle release-/Shield-stega står framleis som før.
# Ikonpakkar ferdige
2026-10-25: Heile ikonpakkebestillinga er fullført på `appearance-v2` i appearance-worktreet; plan `2026-10-25-ikonpakke.md` er ferdig. Ingen påbyrja steg står att.
Pakkefinning, tolerant versjonsbufra appfilter, profilval, Heim/Appar-kunst, delt appmeny og søkbar ikonveljar med profilvise overstyringar/tilbakestilling er implementerte. Ingen automatiske favorittar.
Spole har SVG/VectorDrawable i Arcticons-stil og flat fargeutgåve; malen inkluderer strekbreidda. Arcticons og Spole er berre lesne; ingenting er sendt inn.
Sluttkontroll via `scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'`: 365/365, 0 hoppa over, AppScreensTest 31/31, DesignRulesTest 4/4; lint 0 feil (23 åtvaringar, 1 hint), debug-APK OK. Sju kontrollerte bilete: `docs/images/ikonpakke-*.png`; evidens: `docs/verification/ikonpakke.md`.
Ingen APK-nedlasting, emulatorinstallasjon eller push. Ekte installerte pakkar, fysisk fjernkontroll og Shield-ytelse er ikkje verifiserte. Brukarens to ucommitta bestillingsfiler er urørte. Ingen spørsmål.

# Tema v1 ferdig
2026-10-25: Heile `docs/briefs/2026-10-25-tema-v1.md` er fullført på `appearance-v2` i appearance-worktreet; plan `2026-10-25-tema-v1.md` er ferdig. Ingen påbyrja steg står att.
Seks tema, ti aksentar, kalenderoverlegg utan persistens, pyntnivå, lifecycle-/rørslesperra Canvas og 45 s edderkopp, sesongrad i snapshot, sesongprioritert spotlight og temagruppe med fargeprøver/førehandsvising er implementerte.
Sluttkontroll via `scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'`: 340/340, 0 hoppa over, AppScreensTest 25/25, DesignRulesTest 4/4; lint 0 feil (23 åtvaringar, 1 hint), debug-APK OK.
Seks visuelt kontrollerte bilete ligg i `docs/images/tema-{halloween,jul,midnatt-hav,kino-perle,skog,innstillingar}.png`; evidens og kostnadsoverslag i `docs/verification/tema-v1.md`. Spesifikasjonen §2, §5.1 og §5.4 er oppdatert.
Ingen emulatorinstallasjon, Shield-test, push, Spole-endring eller endring av HA-filer. Ekte serverar/fjernkontroll/Shield er ikkje verifiserte; brukarens to ucommitta bestillingsfiler er urørte. Ingen spørsmål.

# Utsjånad v2 ferdig
2026-10-25: Heile `docs/briefs/2026-10-25-utsjanad-v2.md` er fullført i appearance-worktreet, greina `appearance-v2`; plan `2026-10-25-appearance-v2.md` er ferdig. Ingen påbyrja steg står att.
Hero v2, kort utan titlar som standard, 14 profilval med D-pad, profilpersistens, klokke, aksent/fokus og lettare scrim er implementerte. Ekstra tett hero med omtale har to faktisk synlege linjer; radene flyttar seg ikkje ved spotlight-byte.
Sluttkontroll via `scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'`: 318/318, 0 hoppa over, AppScreensTest 19/19, DesignRulesTest 4/4, lint 0 feil (23 åtvaringar, 1 hint), debug-APK OK.
Fem visuelt kontrollerte 1920 × 1080-bilete: `docs/images/appearance-{default,large-titles,small-icons,light,ocean}.png`; full evidens i `docs/verification/appearance-v2.md`. Spesifikasjonen §2, §5.1, §5.4 og §6 er oppdatert.
Ingen emulatorinstallasjon, Shield-test, push eller endring av HA-/SmartHome-filer. Ingen spørsmål. Brukaren si nye, ucommitta `docs/briefs/2026-10-25-tema-v1.md` er urørt.

# Spotlight og innlogging på emulatoren
2026-10-25: Spotlight og innlogging er gjort i `polish` på `polish-cache` og snøggspola inn i `plan-2-media`. Siste commit er `cc217b8`.
- **Brukarønske:** ingen episodekunst eller episodenummer, ingen tenestelogo, og ein vakker hero som roterer frå biblioteket.
- **Hero:**
  - Heroen er ein spotlight (spec §5.1, endra): filmar og seriar med backdrop, dei med logo først, maks 30, nytt utval kvar 2. time.
  - Seriar der sesong 2 eller seinare kom dei siste 45 dagane, står først med «Ny sesong». Serieomtale, score og logo blir henta med éin `Ids=`-førespurnad.
  - Kvar tittel står i 12 s. Rotasjonen tikkar berre når Heim er synleg (`WhileSubscribed`).
  - Bakgrunnen tonar inn først når biletet er dekoda (900 ms). Logoar får transparent kant klipt bort.
  - Heroen har fast høgd, så radene flyttar seg ikkje.
- **Kort og rader:**
  - Episodekort bruker seriekunst. Fokusringen rammar berre kunstverket.
  - Fokusert rad smett til toppen, så forrige rad aldri stikk fram.
  - Startfokus følgjer medierader som kjem seint, til sjåaren har trykt ein tast.
- **Verifisering:**
  - `testDebugUnitTest lintDebug assembleDebug` → 291/291 bestått, 16 hoppa over (berre skjermbilettestar), lint 0 feil.
  - På emulatoren (`Stova_TV`, `emulator-5580`): Jellyfin «Midttunet» er innlogga via Quick Connect, og spotlight og rader er kontrollerte med skjermbilete i `.superpowers/spot-0*.png`.
- **Spole:**
  - `feat/spole-launcher-link` i `C:/JellyBin/spole-launcher-link` er fletta med Spole beta18: `d24a8dc`, pluss `b850ea0` for CHANGELOG-rekkjefølgja.
  - Ein subagent porterte lenkja til `DetailsCoordinator`/`KidsLibraryCoordinator`. Resultat: 1 281 testar, 1 280 bestått, 1 hoppa over (nettverkstest), lint 0 feil.
  - Debug-APK-en (beta18) er installert på emulatoren og ventar på Quick Connect-godkjenning.
- **Står att:**
  - Brukaren godkjenner Spole-koden.
  - Overleveringa Stova → Spole skal testast på emulatoren.
  - Emby og HA krev at brukaren skriv inn eigne opplysningar.
  - Ingen push.

# Cache og finpuss
2026-10-25: Hero-lesbarheit er fullført med raud/grøn TDD: heile tekstkolonna er maks 560 dp ved 960 dp skjerm; flat Ink-scrim har 100 % ved venstre kant, 85 % ved 35 %, 80 % ved 60 %, 70 % ved 62,5 % og 0 % ved 65 %. Ekstra fargestopp held slutten av omtalen lesbar; tre stopp åleine gav for låg kontrast.
Verifisering: `testDebugUnitTest --tests app.stova.AppScreensTest '-Pstova.screens=true'` → 12/12; deretter `testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'` → 265/265, ingen hoppa over, DesignRulesTest 3/3, lint 0 feil (20 åtvaringar, 1 hint), debug-APK OK. Alle kall via `pwsh -NoProfile -File scripts/gradle.ps1`.
Oppdaterte/lagra og visuelt kontrollerte bilete: `docs/images/cache-home-bright-backdrop.png` og `docs/images/cache-home-dark-backdrop.png` (1920 × 1080); kjelder `app/build/screens/13-home-bright-backdrop.png` og `09-home-media-hero.png`. Ingen påbyrja steg står att; ekte Shield er ikkje verifisert. Berre `polish`/`polish-cache` er endra; ingen push.

2026-10-25: Alle fire bestilte punkt er fullførte med raud/grøn TDD i `polish` på `polish-cache`; ingen påbyrja steg står att.
HA-cache: `a6856d9` (berre aktivt kortutval og nødvendige attributt/tidsstempel, aldri token, maks éi skriving per 5 s, tomme ferske svar ryddar cachen). Førehandslasting: `cd53232` (3 første titlar per rad, unike URL-ar, maks 12, Coil 1920 × 1080).
Toppscrim: `1928c0c` (flat Ink-gradient øvst i 30 %). Smarthus-status er sentrert i denne committen, verifisert ved skriftstorleik 1 og 2.
Sluttkontroll: `testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'` → 264/264 bestått, 0 hoppa over, DesignRulesTest 3/3; lint 0 feil (20 åtvaringar, 1 hint), debug-APK OK. Separat `testDebugUnitTest --tests app.stova.AppScreensTest '-Pstova.screens=true'` → 12/12. Alle Gradle-kall via `pwsh -NoProfile -File scripts/gradle.ps1`.
Nye skjermbilete: `docs/images/cache-home-bright-backdrop.png` og `docs/images/cache-smarthome-aligned.png`, begge 1920 × 1080 og visuelt kontrollerte; kjelder i `app/build/screens/13-home-bright-backdrop.png` og `14-smarthome-heading-aligned.png`. Ekte HA/Shield er ikkje prøvd; ingen spørsmål og ingen push.

# Status
2026-10-25: Alle 17 utsette Plan 1-småfunn er behandla i `C:/Launcher/.worktrees/minors`, greina `review-minors`.
15 funn er retta med raud/grøn TDD og éin commit per funn; 2 er hoppa over med grunn under «Småfunn».
Sluttkontroll: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'` → 252/252 bestått, ingen hoppa over; DesignRulesTest 3/3; lint 0 feil (20 åtvaringar, 1 hint); debug-APK OK.
Ingen påbyrja steg står att. Ekte fjernkontroll/Shield er ikkje verifisert; ingen spørsmål, ingen push og ingen arbeid i andre worktrear.
Skjermbiletet `app/build/screens/minor-pin-font-2.png` er generert og visuelt kontrollert; «Slett» held éi linje.
2026-10-25: Plan 5 oppgåve 2 og 3, og dokumentasjonsdelen av oppgåve 5 steg 1, er fullførte i `C:/Launcher/.worktrees/plan-5` på `plan-5-perf`.
Baseline Profile/benchmark: `8409d65`; måleskript: `7da6b7c`; oppsettdokument, README, tre emulatorbilete og målerapport er med i denne dokumentasjonscommitten.
Sluttkontroll `testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'`: 233/233 bestått, 0 hoppa over, DesignRulesTest 3/3, lint 0 feil (22 åtvaringar, 1 hint), debug-APK OK. Sjølvtest 24/24; profil og benchmark kvar 1/1 relevant test bestått.
Emulator (debug-signert benchmarkRelease): kald 313,50 ms, varm 98 ms, hakking 3,66 %, PSS 67,30 MiB; startupbenchmark TTID-median 232,454 ms. `emulator-5582` er slått av med `emu kill`.
Ingen påbyrja arbeid står att innanfor bestillinga. Brukaren må gjere signering/passord, Shield-målingar og oppsettkontroll, oppdateringstest og produksjonsrelease/hash; oppgåve 1 og 4 og resten av 5 er urørte. Versjonen er framleis 0.1.0.
Avvik står under Plan 5 nedanfor og i `docs/verification/plan-5-emulator.md`. Ingen spørsmål, ingen tag, ingen push eller publisering.

## Før Plan 5

2026-10-25: Samanslåingsoppgåve 1–3 er fullførte på `plan-2-media` i `C:/Launcher/.worktrees/plan-2`; Plan 2 og Plan 4 er bevarte.
Merge: `a568e8b`. Aldersgrense med raud/grøn TDD: `3865293`; HeroPanelTest er òg oppdatert til éi alderspille.
Sluttkontroll: `testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'` → 232/232 bestått, ingen hoppa over, DesignRulesTest 3/3, lint 0 feil (22 åtvaringar, 1 hint), debug-APK OK.
Eigen AppScreensTest-køyring: 10/10 bestått; nytt bilete med hero, media og HA-status ligg i `docs/images/home-media-ha.png` og er visuelt kontrollert.
Ingen påbyrja steg står att. Ekte medieserverar, Home Assistant, Shield og emulator er ikkje verifiserte; oppgåve 10 i Plan 2 og 4 er framleis urørt. Ingen spørsmål og ingen push.

## Samanslåing

2026-10-25: HA-greina `8216d9b` er fletta inn i `a568e8b`; etterfølgjande fletting av Plan 1 gav «Already up to date». Dei krevjande konfliktane i `AppContainer`, `HomeScreen` og `StovaApp` er løyste med både hero/backdrop/mediarader og HA-tilkopling/statusbrikker/Smarthus-navigasjon bevarte. Hushaldsmodellen har både `deviceId` og `haBaseUrl`, innstillingane har Kontoar og Smarthus, og avhengigheiter og INTERNET-løyve er utan duplikat. `3865293` fjernar dobbel aldersgrense med raud/grøn TDD; UI-testen krev òg éi alderspille. [Heim med media og HA-status](images/home-media-ha.png) bruker falske data og lokal kunst og er visuelt kontrollert. Sluttkontroll: 232/232 testar, AppScreensTest 10/10, DesignRulesTest 3/3, lint 0 feil og debug-APK OK. Ingen push.

## Før samanslåinga (Plan 2)
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
## Plan 4

- 2026-10-25: Fletting av `plan-1-fundament` (`72624eb`) inn i `plan-4-home-assistant` er fullført i merge-commit `bdff9f2`; begge foreldra er bevarte.
- Verifisering: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug` → grønt (164 bestått, 10 valfrie skjermbilettestar hoppa over); same kommando med `'-Pstova.screens=true'` → 174/174 bestått, 0 hoppa over, inkludert 3/3 `DesignRulesTest`. Lint: 0 feil, 21 åtvaringar og 1 hint; debug-APK bygd.
- Ingen påbyrja steg står att. Plan 4 oppgåve 10 er framleis urørt; ekte HA, emulator og Shield er ikkje verifiserte etter flettinga. Ingen spørsmål til brukaren.
- Dei nygenererte `app/build/screens/ha-dashboard.png`, `ha-double-font-header.png` og `ha-double-font-sensor.png` er visuelt kontrollerte. Ingen push eller arbeid i andre worktrear.

## Fletting

Konfliktane låg i `AppContainer`, `MainActivity`, `HomeScreen` og `StovaApp`. `AppContainer` tek no både den tidleg installerte kræsjvakta og HA sitt `SecretStore`; kallestadene bruker namngjevne argument der desse blir sende inn. `MainActivity` beheld HA-start/stopp saman med `onNewIntent`, `homeRequests` og `LaunchGuard`. `HomeScreen` beheld statusbrikkene medan null-rader viser lasting utan tomtilstand. `StovaApp` kombinerer Smarthus-knappen og sida med Home-navigasjon og valfri vaksen-PIN ved profilbyte. Dei andre Plan 1-rettingane vart automatisk fletta inn. Ein ny integrasjonstest stadfestar at Home lukkar Smarthus, som framleis kan opnast att.

## Før fletting

- 2026-10-25: Oppgåve 1–9, alle steg til og med commit, er fullførte på `plan-4-home-assistant` i `C:/Launcher/.worktrees/plan-4`. Ingen påbyrja steg står att.
- Oppgåve 10 er medvite urørt etter brukarønske. Ekte Home Assistant, emulator og Shield er ikkje verifiserte; ingen spørsmål til brukaren.
- Sluttkontroll: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'` → 159/159 bestått, 0 hoppa over, inkludert alle 3 `DesignRulesTest`-testane; lint 0 feil (21 åtvaringar, 1 hint).
- Debug-APK er bygd i `app/build/outputs/apk/debug/app-debug.apk`. Smarthus er ein vilkårsstyrt ikonknapp i topplinja og ei fullskjermside som Tilbake lukkar. Ingen push eller endringar i andre worktrear.
- Visuelt kontrollert: `app/build/screens/ha-dashboard.png`, `ha-double-font-sensor.png` og `ha-double-font-header.png`. Topplinja held navigasjonen synleg ved skriftstorleik 2. Ledger med TDD/commit-evidens: `.superpowers/sdd/2026-10-24-plan-4-home-assistant.md/progress.md` (ignorert).

## Avvik

- Oppgåve 2 og 7: Plan 2 er ikkje implementert på denne greina. OkHttp/MockWebServer 4.12.0 og Coil 3.4.0 er difor lagde til her. Samordne desse avhengigheitene ved seinare fletting.
- Oppgåve 2: Tilbakekalling bruker `POST /auth/token` med `action=revoke`, ikkje `/auth/revoke`, etter den offisielle dokumentasjonen: https://contributors.home-assistant.io/core/internals/auth-api/.
- Oppgåve 3: `kidSmartHomeControl` er lagt til før oppgåve 6 fordi innstillingsbrytaren treng feltet. INTERNET og cleartext-støtte er òg lagde til her; LAN-regelen blir handheva før HA-adressa blir brukt. OAuth-utveksling kan ta ei validert adresse før hushaldsinnstillinga blir lagra.
- Oppgåve 5: `HaConnection` er eit lite transportgrensesnitt som `HaSocket` implementerer. Kort/UI og falske tilkoplingar bruker same kontrakt. All skriving til socket-tilstandane går gjennom éin kanal på IO-scope.
- Oppgåve 7: `StovaIcons.kt` fanst og er utvida. Korta bruker minstehøgd 160 dp slik at større skrift ikkje blir klipt. `Graph(today)` skil dagens straumpriskurve frå 24 t-sensorhistorikk. Kamerabilete bruker same-origin Bearer-header og ingen disk-/minnebuffer; oppdateringar stoppar med livssyklus/synlegheit.
- Oppgåve 8: UI-testane bruker `SmartHomeContent` og ei `HaConnection`-basert legg-til-flyt. Ein eigen integrasjonstest dekkjer ekte AppContainer, vilkåret for ikonknappen og Tilbake. Rutenettet er avgrensa til 752 dp for tre kort på 240 dp med 16 dp mellomrom (breie kort 496 dp).
- Oppgåve 9: `ChipContent` har eit valfritt strengressursfelt slik at På/Av kjem frå omsette Android-ressursar. HomeScreen sender brikkene til topplinjesloten. Brikkene har avgrensa breidd og ellipse for å halde klokke og ikon synlege; ein test med native font-rendering og skriftstorleik 2 fangar denne regresjonen.

## Småfunn

- OK mistar fokus etter PIN-forsøk: hoppa over som alt handtert av noverande Compose-versjon. `rejectedPinReturnsFocusToDigits` viser at D-pad-fokus går til «1» når OK blir deaktivert, både medan eit ekte suspendert svar ventar og etter avvist PIN. Ingen produksjonsendring.
- Langt OK-trykk kan velje første appmenyval: hoppa over etter brukarønske; krev ekte fjernkontroll/Shield.

Alle nummer nedanfor følgjer rekkjefølgja til dei 17 opphavlege `Final: minor (deferred)`-linjene. Kvar retting har ein test som feila før rettinga og bestod etter; testnamna står i `docs/verification/plan-1-ledger.md`.

| Funn | Commit | Retting |
|---|---|---|
| 1 | `264a6f3` | Vaksen-PIN medan profilfila lastar |
| 2 | `f511d9c` | Avgrensa PIN-ventetid ved klokkehopp bakover |
| 5 | `9ab6687` | Fast layout for fokusetikettar i topplinja |
| 6 | `11f5c46` | Klokka blir oppdatert ved resume |
| 7 | `82b57c0` | Profilendringar utan ny pakkeskanning |
| 8 | `7940989` | Fokus følgjer flytta heimerad |
| 9 | `9a67bb3` | Nye forsøk på manglande appkunst og oppfrisking ved pakkeoppdatering |
| 10 | `ad3ab5b` | Respekter avslått tilgjengeteneste |
| 11 | `f7a9c60` | JSON-lesing toler I/O-feil, òg ved korrupt kopi |
| 12 | `12f0f7d` | Same vaksen slepp dobbel PIN ved eitt profilbyte |
| 13 | `f520fe4` | PIN-tastatur med «Slett» på éi linje ved skriftstorleik 2,0 |
| 14 | `371417d` | Tydeleg feil ved tilgang til launcher-container i feil prosess |
| 15 | `593810f` | Home-overstyring av ved uleseleg kræsjvaktfil |
| 16 | `9a7fef9` | Varm JSON-lesar når Home-tenesta blir kopla til |
| 17 | `a438d7c` | «siffer skrivne» i fleirtal, korrekt eintal og engelsk |

Funn 3 er dokumentert med regresjonstest i `c0e3d9f`; testen var grøn før produksjonsendringar, også medan PIN-svaret var suspendert. Funn 4 er medvite urørt og krev fysisk fjernkontroll.

Funn 10: Android fortel ikkje om ei avslått teneste vart slått av av systemet eller av brukaren. Automatisk aktivering ved oppstart er difor fjerna, saman med den unyttige adb-grant-oppskrifta i UI-et; brukaren slår tenesta på i Android-innstillingane. Spesifikasjonen er oppdatert med denne avgjerda.

Sluttverifikasjon: 252/252 bestått (inkludert skjermtestane), DesignRulesTest 3/3, lint 0 feil / 20 åtvaringar / 1 hint, assembleDebug OK. Ingen emulator eller Shield er brukt. Arbeidstreet er reint etter overleveringscommit.
## Plan 5

- 2026-10-25: Arbeidet er avgrensa til `C:/Launcher/.worktrees/plan-5`, grein `plan-5-perf`. Brukarønsket går framfor standardregelen om port 5580: berre `emulator-5582` er brukt. Oppgåve 1 og 4 blir hoppa over; ingen produksjonsrelease, signeringsnøkkel eller tag blir laga.
- Oppgåve 2: Det bestilte API 34-TV-biletet `x86_64` finst ikkje i SDK-registeret (`sdkmanager` feila, og `--list` stadfesta det). AVD `Stova_TV34` er derfor laga med `system-images;android-34;android-tv;x86`, i `C:/Launcher/.avd`, på port 5582 med `-no-snapshot-save`. Også dette er eit produksjonsbilete utan root; API 34 kan generere profilen utan root.
- Oppgåve 2: Benchmark/Baseline Profile-plugin 1.4.1 og ProfileInstaller 1.4.1 er brukte med compileSdk 36. Pluginen krev AGP-kompatibilitetsflagget `android.newDsl=false`; innebygd Kotlin er bevart. Ein eventuell AGP 10-oppgradering må oppgradere profilpluginen og fjerne flagget.
- Oppgåve 2: Eit emulatoravgrensa demoinnsteg ligg berre i debug, nonMinifiedRelease og benchmarkRelease. Produksjonsrelease får det ikkje. Éin fiktiv profil og Jellyfin-konto, lokale bilete og bufra rader gjer køyringa repeterbar utan verkelege tenester. Generatoren rullar med D-pad og opnar topplinjehandlingane med ein selector som blir slått opp ved aktivering; UiObject2 gav stale-node-feil under fokus-animasjon.
- Oppgåve 2: `HomeReadyTest` gjekk frå AssertionError utan markør til 1/1 bestått. Profilgenerering og startupbenchmark er begge grøne (éin relevant test per køyring; den andre regelen blir medvite hoppa over). Profilen har 21 708 linjer, 334 med `Lapp/stova/home/`, og ingen reglar for demoinnsteget. Startupbenchmark: 10 iterasjonar, median `timeToInitialDisplayMs` 232,454 ms (min 206,374; maks 329,551). Verktøyvariantane er debug-signerte av profilpluginen; `assembleRelease` er ikkje køyrd.
- Oppgåve 3: `Measure-Stova.ps1 -SelfTest` er utvikla raud/grøn og bestod 24/24 kontrollar. Skriptet krev eksplisitt serie, avviser alle andre emulatorportar, ventar på `home-ready`, gir feil ved manglande data og kode 1 ved brote budsjett. Første integrasjonskøyring avdekte 32-bits overflyt i Android-shellen sin nanosekundaritmetikk; PowerShell si monotone `Stopwatch` styrer no dei 30 sekunda. D-pad-sekvensen har eigne grensetestar.
- Oppgåve 3: Målt på R8-minifisert, ikkje-debuggbar `benchmarkRelease` med Android Debug-sertifikat (ingen produksjonsrelease eller Stova-nøkkel): kaldstartmedian 313,50 ms (10 prøver), varm Home 98 ms, hakking 3,66 %, PSS 67,30 MiB. 147 tastetrykk over 30,14 s. Alle fire radene OK, avslutningskode 0. Dette er funksjonskontroll på emulator; Shield-budsjetta er ikkje godkjende. PSS bruker KiB/1024. Varmmålet er `am start -W` si TotalTime.

- Oppgåve 5 steg 1: `docs/SHIELD_SETUP.md` og `README.md` er skrivne på nynorsk. Tre bilete frå emulator-5582 med demodata ligg i `docs/images/home.png`, `apps.png` og `smarthome.png` og er visuelt kontrollerte; ingen personlege namn, adresser eller token er synlege. Målerapport og repeterbare kommandoar ligg i `docs/verification/plan-5-emulator.md`. Dei seks lokale dokumentasjonslenkjene verkar.
- Oppgåve 5: Berre dokumentasjon og bilete er fullførte etter bestillinga. Versjonsauke, signering, oppdatering over eldre signert installasjon, fysisk Shield-oppsett, produksjonsrelease/storleik/hash og tag er ikkje gjorde. Ingen `assembleRelease`, push eller publisering. Emulatoren vart slått av med `adb -s emulator-5582 emu kill` (OK).
- Sluttkontroll: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'` → BUILD SUCCESSFUL, 233/233 bestått, 0 hoppa over, DesignRulesTest 3/3; lint 0 feil, 22 åtvaringar og 1 hint. Debug-APK bygd. Ingen nye kodeendringar etter denne kontrollen.
