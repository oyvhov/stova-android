# Home Assistant-merket på sceneflisene
2026-10-28, brukaren: «Skulle en hatt ein liten Ha logo badge på flisa? (Ekte nye Logoen)», «Det kan vere den gamle logoen. Det får du finne ut», «Eg vil ha det», «Lag det pittlitt større». MDI sitt `home-assistant`-ikon er den gamle logoen (pipe på taket); den nye frå 2023 er lasta ned som `HA-logomark-color.svg` (1,3 kB) frå OpenHomeFoundation/brand-assets og ligg som `res/drawable/ic_home_assistant.xml`, med merknad i `assets/licenses/home-assistant-logo-NOTICE.txt`. Etter merkereglane: uendra, utan ramme eller sirkel, luft rundt; 22 dp øvst til høgre på kvar sceneflis.
Kontroll: 814/814, lint 0 feil (63 åtvaringar), bygg OK. Emulator: `.superpowers/readme/b3_s.jpg`. Shield: ikkje installert. Ikkje pusha.

# Sceneflisene viser ikonet frå Home Assistant – ikkje på Shield
2026-10-28, brukaren: «Home assistant ikonet», «Last ned. Pass på at ikke heile pakken blir lasta inn unødvendig». Lasta ned berre `materialdesignicons-webfont.ttf` (1,3 MB), `materialdesignicons.css` (418 kB, berre til namnelista) og `LICENSE` frå `@mdi/font` 7.4.47 på jsDelivr; lisens Apache 2.0 for fontane.
`ha/HaIconFont.kt`: font i `res/font/material_design_icons.ttf`, sortert namneliste `assets/mdi/names.txt` (7448 ikon, 165 kB) som blir lesen per namn til første treff og hugsa; fonten blir lasta først når ei flis teiknar eit ikon. Ikonet kjem frå registeret (`icon`), attributtet `icon`, `original_icon` eller domenet (`mdi:palette`, `mdi:script-text`), blir lagra i `Shortcut.icon` og vist på flisa og i panelet. Ukjende ikon gir sola.
Kontroll: 814/814 (med skjermbilettestar), lint 0 feil (63 åtvaringar), bygg OK. Emulator-5580 med ekte Home Assistant: Appar-sida viser ikona (lampe, dør, TV, skriptark, hund), nokre få med sola fordi ikonet ikkje fanst i fonten (`.superpowers/readme/i1.png`). Ingen scene er køyrd. Shield har førre bygg (dbb08bf5); dette er ikkje installert der. Ikkje pusha.

# Bibliotek og scenar som fliser i favorittrada
2026-10-27 natt, brukaren: «legge inn bibliotek i Jellyfin som "apper" … integrere Spole og Home Assistant», «Eg ønsker ikkje å "ødelegge" heroen», «bildet som biblioteket har i Spole (Jellyfin) og lag klart for Home Assistant og», «Go go go», så «Kan du be Codex gjere spole delen?». Plan: `docs/superpowers/plans/2026-10-27-shortcut-tiles.md` (alle steg gjort).
Stova: favorittane kan innehalde `library:<konto>:<bibliotek>` og `ha:<entitet>`; namn og bilete ligg i `AppPrefs.shortcuts`, så Heim teiknar dei utan nettverk, og opprydding av appar rører dei ikkje. `ShortcutTile` deler ramme (`TileFrame`) med `AppTile`. Bibliotek opnar `spole://library?kind=&user=&library=` (`SpoleHandoff.openLibrary`); Spole utan lenkja opnar heimen sin med melding. Scenar/skript køyrer med `runHaShortcut`, som held tilkoplinga berre for kallet. Appar-sida har gruppene Bibliotek (bilete frå `ImageTags.Primary`) og Scenar og skript; hald OK for å leggje til. Barneprofilar får ingen snarvegar. Heroen er urørd.
Spole: Codex arbeider i `C:\JellyBin\spole-library-link` på greina `library-link` (logg `.superpowers/agents/spole-library-link-20261027-232736.log`, brief `docs/briefs/2026-10-27-spole-bibliotekslenkje.md`). Ikkje kontrollert eller fletta enno.
Kontroll: full køyring 808/808 (med skjermbilettestar), lint 0 feil (63 åtvaringar), bygg OK; etter lint-rettinga Heim/app-testane 155/155. Emulator-5580 med ekte data: Appar-sida viser biblioteka med bileta frå Jellyfin og scenane frå Home Assistant (`.superpowers/readme/t4.png`), «Barnefilmar» lagt til via hald OK og synleg på Heim (`t6.png`), trykk opna Spole sin heim med meldinga (Spole på emulatoren har ikkje lenkja), og favoritten er fjerna att. Scenar er ikkje trykte på ekte (dei styrer huset). `Send-TvRemote.ps1` tok første steg som `-Serial`; retta med `PositionalBinding = $false`. Shield: ikkje installert. Ikkje pusha.

# Lett for andre å setje opp emulator og AI
2026-10-27 kveld, brukaren: «Lag det lett for andre å sette opp simuleringer og AI» (repoet er no public).
Skripta verkar på alle maskiner med `pwsh` 7: `scripts/StovaSdk.ps1` finn SDK-en (ANDROID_HOME, ANDROID_SDK_ROOT eller standardstaden), `Start-StovaEmulator.ps1` installerer TV-biletet og lagar AVD-en ved første bruk (API 30, API 34 på ARM; `-Name`, `-Port`, `-Api`) og hoppar over start når emulatoren køyrer, `Show-StovaScreen.ps1` prøver på nytt utan lyd når maskina manglar lydeining, og `gradle.ps1` flyttar berre Gradle til ASCII-stiar på Windows med «Ø» i brukarmappa (eigaren held `C:\JellyBin\.gradle-home`). Nye: `Install-Stova.ps1` (byggjer ved behov, `install -r`, `-Demo` med `PerformanceSeedActivity`) og `Send-TvRemote.ps1` (`key:`, `hold:` med virtuell `hid`-fjernkontroll, `wait:`, `focus`, `shot:`). Alle nektar Spole-emulatorane.
Dokument: ny `docs/EMULATOR.md`; `AGENTS.md` delt i felles reglar og «Berre på maskina til eigaren»; `docs/superpowers/plans/README.md` seier at berre `2026-10-26-outstanding-launcher.md` gjeld og resten er historikk; CONTRIBUTING, README og ARCHITECTURE viser dit.
Kontroll: ny AVD `Stova_Demo` (5584) laga og starta med skriptet, `Install-Stova.ps1 -Demo` gav Demo-profilen og demoradene, `Send-TvRemote.ps1` med `hold:1500` på Innstillingar opna snøggmenyen; AVD-en er sletta etterpå. På `emulator-5580` nekta `-Demo` fordi det finst eigne profilar, og andre start sa «køyrer alt». Alle skript blir parsa, `Measure-Stova.ps1 -SelfTest` 27/27, `gradle.ps1 --version` OK, lenkjene i dokumenta finst. Ikkje prøvd på macOS eller Linux, og ikkje ARM-biletet.

# README med ekte bilete frå Shield
2026-10-27 kveld, brukaren: «laste opp litt skikkelig bilder til GitHub av menyer og skjerm osv. Oppdater readme», så «heller ta fra Shield … Iallefall frå Framsida med ekte apper» og «Ikkje ta med navnet mitt på det siste bildet».
Sju bilete frå Shield (4K-skjermbilete, skalerte til 1920 × 1080 JPEG) i `docs/images/shield-*.jpg`: Framsida med Max-Halloween og favorittappar, mediarader, snøggmenyen, appmenyen, Smarthus, Innstillingar og Tema med den vesle Heim-sida. Profilveljaren med namnet er teken bort. README beskriv no funksjonane og seier at bileta er ekte; dei gamle demobileta ligg att fordi eldre dokument viser til dei.
Shield vart berre styrt med tastar og ein virtuell `hid`-fjernkontroll for halding; ingen val er endra, og TV-en står på Framsida att. Snøggmenyen er no sett på TV-en: haldet opnar han med fokus på Nettverk utan å velje raden.

# Einingsmenyen lukka seg på slippet av det lange trykket – retta, på Shield
2026-10-27 kveld, brukaren (Shield): «Snøggmenyen lukker seg før eg får trykt på noko og trykker på første valg». Årsak: menyen er ein eigen `Popup` utan OK-vakta som `StovaDialog`/`StovaSheetWindow` har; gjentakingane og slippet frå haldet som opna han kom til første rad («Nettverk»), som vart vald. Vakta er flytta til `core/ui/FreshPresses.kt` (`Modifier.freshPressesOnly()`) og brukt av både dialogane og menyen: berre eit nytt trykk inne i vindauget eig slippet. Test: `SystemQuickMenuTest.theHoldThatOpensTheMenuNeitherChoosesNorClosesIt` (rå gjentaks- og slipphendingar), feila før rettinga.
Kontroll: 789/789 (58 skjermbilettestar hoppa over utan `-Pstova.screens`), lint 0 feil (63 åtvaringar), APK `ffc56b0a…`. Emulator-5580: `input keyevent --longpress DPAD_CENTER` på Innstillingar → menyen står open med fokus på Nettverk (`quick-menu-hold.png`); nytt OK på Bluetooth opna TV-innstillingane, Tilbake gav Stova. Shield (brukaren: «No» på «no eller vent?»): APK `ffc56b0a…` installert og hashverifisert; Heim-knapp-tenesta er framleis på. Ikkje sett på TV-en.

# Juleraudt, Max-pynt, større hero og einingsmeny på Innstillingar – på Shield
2026-10-27 kveld, brukaren: «Juletema må jo ha ein meir rød jule accent», «sesongpynten skal vere mykje mykje meir … kalle den Max», «Eg ser ikkje forskjell på Herostorleik kompakt og Stor», «long press på Settingsikon … dropdown meny med dei viktigaste Android Tv funksjoner … Og ekte status». Undervegs: «ikkje berre meir snøfnugg … mykje forskjellig», «Noko på appikona? Eller boksen til den?», «spindelvev … forskjellig på kvar app».
(1) `Accent.HOLLY` er ekte raud `FF7575` (7,1:1 mot mørkaste bakgrunn, regelen på 7:1 held) og eigen djup bærraud container `6E1219`, så valde rader og brikker er raude, ikkje brune.
(2) `SeasonalDecor.FULL` heiter no «Max» (enumen er uendra for lagra profilar). Jul: julelys med glaskuler øvst (over toppfeltet, 3–16 dp), snø i tre djupner med krystallar, blinkande stjerner, stjerneskot, slede med reinsdyr, snøfonn, grantre i hjørna, snø og kristtorn på appflisa. Halloween: glør, sju flaggermus (kropp, øyre, takka venger), spindelvev i hjørna, edderkopp, tåke, gresskarlykter, spøkjelse, heks, vev på appflisa. Scena er to lag på éi `SeasonalClock` i `HomeContent`: bak innhaldet (himmel, fjern snø, dei fleste glørne) og gjennomsiktig framfor (kantar, nær snø, besøkande). Pynten på appflisene (`seasonalTile(app.key.hashCode())`) er plassert etter appen: hjørne, storleik og edderkopp for vev; istappar og kristtorn for snø. Kvile etter 120 s og redusert rørsle gjeld som før.
(3) Herostorleik: før endra han berre ein usynleg boks. No har Stor logo opptil 156 × 540 dp, tittel i displaySmall, tekstkolonne 560 dp og tre liner omtale; i dekket tek Stor all plass over radene.
(4) Langt OK eller Meny på Innstillingar-ikonet (berre vaksne): eit lite kort under ikonet med eining, Android-versjon og oppetid, og radene Nettverk (Wi-Fi/kabel, signal, band, IP), Bluetooth (på/av, para einingar på Android 11), Bilete og lyd (oppløysing, Hz), Lagring (ledig av total), Start på nytt (straummenyen via `HomeButtonService`, eller beskjed om å slå på Heim-knappen), Alle Android-innstillingar. Status blir lesen éin gong ved opning, ikkje polla. Nye install-løyve: ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, BLUETOOTH (maxSdk 30); Wi-Fi og Bluetooth er valfri maskinvare.
Kontroll: 788/788, 0 skip, lint 0 feil (63 åtvaringar), bygg OK, APK `740a5ee9…`, commit a4318fe. Emulator-5580 (`.superpowers/design-review/`): `max-halloween-hero.png`, `max-halloween-apps.png` (ulik vev per app), `max-christmas.png`, `max-christmas-settings.png`, `hero-large.png`, `quick-menu.png`. Vala på emulatoren er sette tilbake (Halloween, Kompakt). Omstart er ikkje prøvd: Heim-knapp-tenesta er av på emulatoren. Pusha til origin/main (33a2f6c). Shield (brukaren: «Installer»): APK `740a5ee9…` installert med `install -r` og hashverifisert; dei tre nye løyva er gitt. Brukaren har slått på `HomeButtonService` att (står i `enabled_accessibility_services`), så omstart via menyen skal verke der; ikkje prøvd, og ikkje sett på TV-en.
Ope: når appflisa ikkje har bakgrunn (standard), heng snø og vev på den usynlege boksen rundt ikonet.

# Ekte førehandsvising av Heim og større hovudkategoriar i Innstillingar – på Shield
2026-10-27, brukaren: «førhandsvisinga i Settings må bli mykje meir ekte» og «burde dei vore større? Altså hovedkategorienane til venster?» → «Ja og ja».
(1) `HomeMiniature`: den ekte `HomeContent` med bakgrunn, toppfelt (HA-brikker berre når HA er sett opp), hero og radene frå `HomeViewModel`, teikna med utkastet (`StovaTheme(prefs)`). Skalert med `LocalDensity` (density × breidd/960 dp), så det er same oppsett som på TV-en. Står ved tittelen på kvar Utsjånad-side unntatt Profil, ligg fast medan kontrollane rullar; stor tekst (≥ 1,5) legg han øvst i rullinga. Inert: `focusProperties { onEnter = cancelFocusChange() }` på gruppa stoppar både piltastar og `requestFocus`; `clearAndSetSemantics` gir éi skildring («Førehandsvising av Heim»). Fordi kort i førehandsvisinga er nærare enn brødsmula, har brødsmula `down = controls` og første kontroll `up = breadcrumb`. `LocalHomePreview` (Heim plasserer ikkje fokus og teiknar startrada som aktiv) og `LocalDrawnFocus` (startkortet teikna fokusert, så fokuseffekten synest). Dei grå skissene (`SettingsOptionPreview`) og skissa nedst (`ThemePreview`) er fjerna; tema- og aksentprøvene er att i `ThemeSwatches.kt`.
(2) Seksjonslista: 240 dp brei, rader på minst 60 dp med merke (`CategoryIcon`, same farge som sida) og ei line under namnet (`settings_rail_*`). «Smarthus og kontoar» står på éi line på nynorsk; den engelske brytast i to.
Kontroll: 775/775, 0 skip, lint 0 feil (63 åtvaringar), bygg OK, APK `db1570d8…`. Emulator-5580: `real-settings-rail.png`, `real-preview-cards.png`, `real-preview-cards-xl.png` (kortstorleik endra → førehandsvisinga følgjer), `real-preview-up.png` (Opp → brødsmula), `real-preview-focus-ring-zoom.png` (ring rundt startkortet) i `.superpowers/design-review/`. Vala på emulatoren er sette tilbake (COMPACT, LIFT). Shield (brukaren: «Installer på Shield»): APK `db1570d8…` installert med `install -r` og hashverifisert; ikkje sett på TV-en. Stova si `HomeButtonService` er framleis ikkje i `enabled_accessibility_services`.
Steg 4 i planen har att 4 (profilval) og 5 (appfliser).

# Rullande Heim som standard og roleg Smarthus – ikkje på Shield enno
2026-10-27, brukaren: «kor er parallaxen på Hero?» → «Den skal vere rullande som default». Parallaksen var aldri fjerna; han finst berre i «Rullande oversikt», og standarden var «Éi rad» sidan ec0d1cb. 968c586: `AppearancePrefs.homeRows` er no SCROLLING for nye og nullstilte profilar. Lagra profilar har verdien skriven (`encodeDefaults = true`), så Shield-profilen står framleis på «Éi rad» (Innstillingar → Heim og utsjånad → Heim og hero → Synlege rader). Emulator-5580: profilen sett til SCROLLING via `profiles.json` for kontroll; parallaksen verkar (`parallax-*.png`).
Ope: i rullande vising står start-rada øvst. Startar ein på første medierad (standard) med appraden over, rullar heroen bort straks (`start-sheet.png`). Shield startar på Favorittappar (øvst), så der står heroen.
Brukaren, same økt: «korta for høge … heile skjermen bevegar seg», «popup … utvider seg og hopper», «ta vekk Kjøling, avfukting, vifte og heat_8_15». 3965b8f: (1) TV-ens pivot (`LocalBringIntoViewSpec` med FEATURE_LEANBACK) drog kvart fokusert kort ein tredjedel ned og skuva rutenettet sjølv når kortet synte; Smarthus-rutenettet brukar no `ScrollOnlyWhenHidden` (delt med panelet). Test med `setSystemFeature(FEATURE_LEANBACK)` i `MixedDashboardLayoutTest`. (2) Kontrollpanela fekk ei line «Ventar …»/feil berre medan ho synte; no fast statuslinje og panelet krympar aldri medan det er ope. (3) `CardConfig.hiddenModes`; langt OK på eit klimakort → «Modusar» der ein vel bort modusar/førehandsmodusar; panelet filtrerer dei.
Kontroll: 769/769, 0 skip, lint 0 feil (63 åtvaringar), bygg OK, APK `bfccf8ec…`. Emulator: `modes-page.png`. Shield (brukaren: «Ja»): APK `bfccf8ec…` installert og hashverifisert. Profilen endra via run-as `profiles.json` (kopi i scratchpad `shield-backup/`): homeRows SCROLLING, `climate.varmepumpe` hiddenModes cool/dry/fan_only/heat_8_15. FEIL: `am force-stop` før endringa fjerna `HomeButtonService` frå `enabled_accessibility_services` (Android gjer det ved force-stop); brukaren må slå tenesta på att i Android-innstillingar → Tilgjengelegheit. Aldri force-stop Stova på Shield.

# Skrift (Outfit) ferdig – steg C i plan `2026-10-26-outstanding-launcher.md` ferdig; på Shield
2026-10-27 morgon, brukaren: «Ja! Do it» (last ned Outfit). Lasta ned `Outfit[wght].ttf` (110 884 byte, sha256 `fc728727…`) og `OFL.txt` (4 389 byte) frå raw.githubusercontent.com/google/fonts/main/ofl/outfit/. 1243cd8: `res/font/outfit.ttf`, lisensen i `assets/licenses/outfit-OFL.txt`, `StovaFonts.Display` med éi oppføring per vekt (100–700 på wght-aksen) på displaySmall, headline* og titleLarge; resten i systemskrift. Dermed får klokka, side- og radtitlar og dei store tala på Smarthus-korta Outfit.
Funne: Outfit-linjer er om lag 2,5 dp høgare enn Roboto (høgare ascent/descent); `LineHeightStyle` (Trim/Mode.Fixed) endrar ikkje einlinjehøgda. Små Smarthus-kort fekk difor 3 dp mellomrom i staden for 4 og held 152 dp (`MixedDashboardLayoutTest`).
Kontroll: 762/762, 0 skip, lint 0 feil (63 åtvaringar), bygg OK. Emulator-5580: `font-home.png`, `font-smarthome.png`, `font-settings.png` i `.superpowers/design-review/`. Shield: brukaren sa «Installer på Shield og vis meg». APK `dbdff9d0…` installert og hashverifisert; Heim, Smarthus og Innstillingar sett på TV-en (`shield-font-*.png`), Shield står på Heim. scrcpy opna med `--no-audio`: PC-en hadde ingen standard lydeining, så `Show-StovaScreen.ps1` (AAC) døydde med «Could not open audio device».
Neste: plan steg 2 (harde klipp nedst – synleg nedst på Smarthus), 3 (småfunn), 4, 5, 6, 7, 8, 9.

# Fokus, rørsle og tone frå kunsten – på Shield; skrifta ventar på ja til nedlasting
2026-10-26 kveld, brukaren: «Ja go» (neste i planen `2026-10-26-outstanding-launcher.md`: steg 1, B, C).
eb4706e fokus: kunst 6 % større på fjør utan sprett, resten av den aktive rada 70 %, titlar ved sidan 55 %, kontrollar 16 % lys, inga glød (alle effektar unntatt Omriss dimmar naboane). 9697bb9 rørsle: radrulling på fjør som ber farten vidare ved raske trykk, rader kjem forskuvd på første Heim per profil, sider frå toppfeltet veks ut av ikonet og krympar tilbake, bakgrunnen løyser seg opp i neste bilete i staden for å gå via blekk. 29a281f tone: `ArtTone` tek éin dempa farge frå bakgrunnsbiletet (ingen for grå bilete) og lèt scrim, fokuslys og appflater lene seg mot han med same lysheit; berre Heim og Smarthus.
Kontroll: 759/759, 0 skip, lint 0 feil (63 åtvaringar), bygg OK. Emulator: fokus og tone sett på bilete (`focus-home.png`, `tone-home-1/2.png`). Shield: `ac0fb235…` installert og hashverifisert, skjermsparar på, ikkje sett på TV-en.
Står att i C: skrift. Framlegg til brukaren: Outfit (OFL) `Outfit[wght].ttf`, 110 884 byte, og `OFL.txt` (4 389 byte) frå github.com/google/fonts `ofl/outfit/`, til titlar og klokke. Ikkje lasta ned utan ja.

# Plan `2026-10-26-fast-settings-sheet.md` ferdig: fem seksjonar og langt-OK-panel frå høgre – på Shield
2026-10-26 kveld: 0855d92 slår Innstillingar saman til Heim og utsjånad (Rader på Heim er ein kategori der) · Smarthus og kontoar · Profilar · Appar · System (Heim-knappen, Om, Android som rad). Oversikt og gruppeoverskrifter er borte; Innstillingar opnar på første seksjon. Gamle seksjonsverdiar går via `SettingsSection.merged`. f24f1e7: alle langt-OK-menyar (media, appar, rader/Watch Next, Smarthus-kort) er `StovaSheet`, eit panel frå høgre i full høgd: kunst, store hovudhandlingar, rolege rader, undersider i same panel (Tilpass rada, Endre ikon, kortstorleik/flytt/namn/ikon). Tilbake går ei side om gongen og fokuserer rada som opna sida; OK-vakta er delt med `StovaDialog`.
Kontroll: 746/746, 0 skip, lint 0 feil (63 åtvaringar), bygg OK. Emulator-5580: alle fire panela og Innstillingar sett på bilete (`.superpowers/design-review/sheet-*.png`, `settings-five.png`); ingen val trykte, profilen uendra. Shield: APK `6197504f…` installert og hashverifisert; Shield stod utan fokusvindauge (skjermsparar), så panelet er ikkje sett på TV-en.
Funne undervegs: `clip` med skeive hjørne (rette til høgre) stengde for trykk i Robolectric, så panelet teiknar forma med `background(shape)`. TV-pivoten (`LocalBringIntoViewSpec`) drog fokuserte rader opp ein tredjedel og kutta kunsten; panelet bruker difor plattformregelen «rull berre når det trengst». Slide-ut treng layout, så testen med stoppa klokke ventar på idle.
Neste: plan `2026-10-26-outstanding-launcher.md` steg 1 (synleg fokus), B (rørsle), C (skrift og farge frå kunsten).

# Plan `2026-10-26-fast-settings-sheet.md`: steg 1 (fart) ferdig; neste er fem seksjonar og sidepanel
93c7dec: alle overgangar kortare (sider 200 ms, små 150, rader 380, fokus 120/180); lys scrim-knekk tilbake til .46 (skjermkontrasttest). 738/738, lint 0 feil. APK `74bad614…` installert og hashverifisert på Shield. Steg 2 (fem seksjonar) og 3 (panel frå høgre) står att.
2026-10-26 kveld: Brukaren bad om raskare menyar, færre hovudkategoriar i Innstillingar og nytt langt-OK-panel. Valde fem seksjonar (Heim og utsjånad · Smarthus og kontoar · Profilar · Appar · System) og panel frå høgre. Plan-fila har detaljane. Julefilter for heroen: brukaren sa «Nei» no.

# Mjuk hero-gradering ferdig; julefilmer utanfor jul er neste
2026-10-26: 9b3ed61 mjukar `BackdropScrim` (monoton kubisk kurve gjennom stoppa, 48 gradientstopp, mjuke topp/botn; midtstopp ved .52 fjerna, testa anker held). Oppfyllinga (c4508b8) er installert på Shield (`eb0c6804…`) og stadfesta på emulatoren: Favorittar + maks 12 rullar no (E.T., Edward Saksehånd, The Nightmare Before Christmas …). Emulatoren er sett tilbake til LIBRARY/15. Gradering installert på Shield (`908b70d6…`, hash stemmer); Shield stod i skjermsparar, så graderinga er ikkje sett på TV-en. Brukaren valde «Nei» til julefilter no.
Brukaren: «Eg fekk mange julefilmer utan å ha valt jul.» Shield 22:08: LIBRARY, fire bibliotek, maks 12, Barnetid på med barnebibliotek 51b5…/1c5a…; kl. 22 er utvalet berre 3c80…/f658… (70 titlar, ingen jul). Julefilmane kom truleg 20:50–22:08 då barnebiblioteka var med. Bibliotek blir respekterte; det manglar ein regel. Neste: utanfor jul-sesongen skal heroen halde ute titlar med Jellyfin-taggen `christmas` (same som `Endpoint.CHRISTMAS_TAG`): hent ID-settet éin gong (buffer 2 t, berre når heroen er på), filtrer i `MediaRepository.spotlight`. Test først. Same for Halloween-taggen utanfor Halloween kan vurderast.

# Hero fylt opp frå biblioteket – ikkje prøvd på eining; neste er fokus, rørsle, skrift
2026-10-26: Brukaren svara «Ja ja»: Barnetid-builden (`3660a944…`) er installert og hashverifisert på Shield (Projectivy kom fram etter installasjon; Stova ikkje henta fram att). Deretter: ny commit fyller heroen frå biblioteket når færre enn tre titlar passar, og held tillatne titlar medan nytt utval lastar (`fillSpotlight`, `HomeViewModel.lastPool`). Media/home/background/design-testar grøne. IKKJE prøvd på eining: emulatornavigasjonen bomma og trykte i Play Store-innstillingar («Auto-update apps at any time») – sjekk/rett den innstillinga på emulator-5580. Stova-profilen på emulatoren er uendra (LIBRARY, maks 15). Ikkje installert på Shield.

# Barnetid ferdig; neste er fokus (steg 1), rørsle (B), skrift og farge (C)
2026-10-26: 663de0e: Barnetid under Utsjånad → Bakgrunn, bibliotekbasert etter brukaren («eg vil ikkje sjå barnefilmer og serier på kvelden»): på dagtid (06–20, justerbart) viser heroen berre barnebiblioteka; resten av døgnet blir dei trekte frå dei valde bakgrunnsbiblioteka. Av som standard. Testar grøne (profiles/home/settings/navigation/design). Installert på emulator-5580 og sett i Bakgrunn; ikkje på Shield. Brukaren må slå på Barnetid og velje barnebibliotek sjølv.
Opne spørsmål: skal heroen fyllast opp frå biblioteket når færre enn tre titlar passar (brukaren har ikkje svart)? Brukaren bad om high-end-punkta 1 (rørsle), 2 (fokus) og 4 (skrift/farge frå kunsten): plan steg 1, B og C.

# «Hero skiftar ikkje bilete» – årsak funnen, ingen kodeendring
2026-10-26: Shield hadde Bakgrunnstitlar = Favorittar og maks 12 år (brukaren senka frå 15 mellom 17:07 og 18:35). Same Jellyfin-brukar på emulatoren: Favorittar utan grense rullar mellom The Housemaid (R), Affeksjonsverdi, One Battle After Another (15) og Roofman; med maks 12 slepp berre Affeksjonsverdi gjennom, så heroen står. Koden roterer rett. Kl. 20:50 bytte brukaren til Heile biblioteket med fire valde bibliotek, maks 12; då må kvart bibliotek hentast før heroen kjem att (tom hero nokre sekund). Forslag til brukaren: vis tal på titlar som passar i Bakgrunn, fyll opp frå biblioteket når færre enn tre passar, og behald førre hero medan nytt utval lastar dersom han òg passar det nye filteret. Emulatoren er sett tilbake til Heile biblioteket/maks 15.

# Neste: plan `2026-10-26-outstanding-launcher.md` (ikkje påbyrja)
2026-10-26: Logo (7fa1114): hus med glødande skjerm + geometrisk «stova»-ordmerke; TV-banner og ikon, generator i `scripts/logo/`. Sett i Android TV-applista på emulatoren; installert på begge (APK `2f33e08c…`), Shield stod i skjermsparar.
HA: appnamn lagt til i begge Android TV-integrasjonane (Android TV Remote og Android Debug Bridge) via Chrome etter brukaren sitt ønske; stadfesta `app_name: Stova` på `media_player.shield`. Eksisterande namn er urørde.
Brukaren bad om punkt 2, 3, 4, 6 med vala i planfila. Start med steg 1 (synleg fokus).

# Smarthus med stemning og levande kort – installert på begge
2026-10-26: Brukaren bad «Lag smarthus fin» og valde «Stemning bak korta» og «Korta med meir liv». Commits e9d7ce4 (dempa Heim-bilete og himmel frå vêrkortet bak korta; ingen nye førespurnader) og 5466db6 (kvitlys i eigen kelvin, liten boge på varmepumpa, billegaste time på straumprisen). 732/732, 0 skip, lint 0 feil (62 åtvaringar, 1 hint), bygg OK. APK `73e7543d48735d1b288d43ae162fd18269f188f356087c6685a8346983e31cbc` er installert og hashverifisert på emulator-5580 og Shield; Shield-filene er byteidentiske før/etter, og Shield står på Heim.
Den førre builden (hero/klokke/pynt) er dermed òg på Shield. Brukaren spurde korleis appnamn kan setjast i HA sin Android TV-integrasjon: forklart (Konfigurer → Configure applications), med pakkeliste frå Shield; ingenting er endra i HA. Opne spørsmål til brukaren: kva skal til for ein eineståande launcher, og korleis integrere meir (svar i chat).

# Stor rullande hero, klokkestilar, Appar i Innstillingar, sesongpynt, finare HA-kort – emulator, ikkje Shield
2026-10-26: Plan `2026-10-26-hero-clock-season.md` steg 1–5 og tillegg er ferdige (e2a07ad, a6fca57, 4406da4, af5e604, fa55b25, 100049e, c62ca93 og kantovergangen etter). Full kontroll ved c62ca93: 728/728, 0 skip, lint 0 feil (62 åtvaringar, 1 hint), bygg OK; Heim- og designprøver køyrde på nytt etter kantovergangen.
Emulator-5580: alt prøvd med bilete (toppfelt utan Appar, graskar til venstre, Appar i Innstillingar → Tilbake, stor rullande hero med parallax, tre klokkestilar, pynt over heile skjermen). Emulatorprofilen er tilbakestilt; einaste skilnad er nytt `clockStyle`-felt og fjerna `showAppsButton`. Mediekortet er ikkje sett på eining (emulatoren har ikkje Sonos).
Ikkje gjort: Shield. Brukaren bad kl. ca. 18:45 om at TV-en ikkje skal brukast den neste timen; spør før installasjon. «Rullande oversikt» må veljast i Utsjånad → Heim og hero for å få den nye heroen; Shield står på «Éi rad».

# Appnamn per rad, Tilbake i Innstillingar, Smarthus-toppen – emulator, ikkje Shield
2026-10-26: Plan `2026-10-26-row-names-back-header.md` steg 1–3 er ferdige (commits 1184f08, 23e1913, 3708844). Radteljaren er fjerna. 718/718, 0 skip, lint 0 feil (62 åtvaringar, 1 hint), bygg OK. APK `5a4d4fa58754971f9657d6c424e752a9e71a444c3e9016a6abda31c6b88161c6` er installert og hashverifisert berre på emulator-5580; profilen der er byteidentisk etter prøvinga.
Ikkje gjort: Shield (ventar på løyve). Ny bestilling frå brukaren står i kø: finare klokke, flytte Appar-ikonet (uklart kvar, spør), graskaret til venstre side, meir sesongpynt på heile skjermen (t.d. lett snø til jul), og ein hero som rullar ut med fin parallax («ingen av modusane gjer det eg vil»).

# Favorittar med fjernkontroll – stadfesta på Shield
2026-10-26: Brukaren sa «Gjer det». Før installasjonen hadde Shield sin `profiles.json` (skriven 15:50, då brukaren prøvde) NRK TV som favoritt, medan Stova berre viste Spole og Emby. Det stadfestar at skrivinga nådde fila, men ikkje minnet. APK `cbccb731…f571` er installert med -r og hashverifisert. Etter omstart viser Heim Spole, Emby og NRK TV.
Shield: ein virtuell Bluetooth-fjernkontroll (`hid`, OK halde 1 s) opna menyen. Eit kort OK la AetherSX2 til, på disk og på skjermen, og «Fjern frå favorittar» tok han bort att. `profiles.json`, `household.json` og `media-accounts.json` er byteidentiske med det dei var før prøva (berre NRK TV-favoritten frå brukaren sjølv). Shield står på Heim.
Merknad: adb `--longpress` og adb `KEYCODE_MENU` opnar ikkje menyen påliteleg på Shield (fire tastefiltrerande tenester: Projectivy, QuickBars, Button Mapper og Stova). Eitt adb-langtrykk starta AetherSX2, som vart lukka med Tilbake. Bruk `hid` til fjernkontrollprøver der.

# Favorittar med fjernkontroll – retta, prøvd på emulator, ikkje på Shield
2026-10-26: Brukaren melde at «Legg til i favorittar» under Appar ikkje verka med fjernkontrollen på Shield (menyen kom, OK gjorde ingenting). Årsak: AppMenuHost starta endringa i scopet til menyen og lukka menyen i same klikk; neste ramme avbraut skrivinga. Med `fsync` på Shield vart endringa borte, eller ho nådde fila utan å nå minnet. Kodecommit 7c0ecbb: favoritt/flytt/skjul går i app-scopet (`AppRepository.launchChange`), og `JsonFileStore.update` fullfører ei påbyrja skriving i minnet sjølv om kallaren blir avbroten.
711/711 native/unit-prøver, 0 skip (2 nye: raude før rettinga), lint 0 feil (62 åtvaringar, 1 hint), bygg OK. APK `cbccb7317e92f0e6285f1c23e28a6423e10a16cf07d479e6960fc8a3f214f571` er installert med -r og hashverifisert berre på emulator-5580.
Emulator: ein virtuell Bluetooth-fjernkontroll (`hid`, DPAD_CENTER via consumer-sida) heldt OK i 0,9–1 s og trykte så OK. YouTube vart lagt til, og fem favorittar vart fjerna att med same flyt. Emulatorprofilen har att berre «Android TV Remote Service» som favoritt. Tilgjengetenesta var mellombels på for ei prøve og er slått av att.
Ikkje stadfesta: Shield (brukaren: «Nokon ser på», ikkje rørt). Installasjon på Shield ventar på løyve frå brukaren. Same mønster (`rememberCoroutineScope` + lagring + lukking i same trykk) kan finnast i andre menyar og er ikkje gjennomgått.

# Designgjennomgang – berre gjennomgang, ingen kodeendring
2026-10-26: Brukaren bad om ein heil designgjennomgang («visuelt slåande og vakkert», ikkje knappestorleiksreglar). 45 ekte skjermbilete frå Shield og emulator-5580 ligg i `.superpowers/design-review/` (annoterte i `web/`); rapport: https://claude.ai/artifact/8J7YwXCGrQgZWr8nYR1HCC.
Viktigast: (1) fokus på kunst er berre 2,5 % skala og nesten usynleg, (2) heroen viser spotlight, ikkje fokuskortet (spesifikasjonen §6, brukaren må avgjere), (3) Innstillingar/Smarthus klipper innhald hardt 48 dp over botnen (`SettingsLayout.kt:56`, `SmartHomeContent.kt:183`), (4) profilval/Appar/Innstillingar manglar kunsten frå Heim, (8) førehandsvisingane i Tema/Kort viser plasshaldarar.
Ingen kode er endra og ingen plan er påbyrja. Shield vart vekt frå skjermsparar; eitt feiltrykk opna Emby utan avspeling. «Slå av alle 3 lys?» på emulatoren vart lukka med Tilbake, ingen HA-styring. Begge står på Heim. Ventar på brukaren: kva funn som skal rettast først, og om heroen skal følgje fokus.

# Diskret tilkoplingsstatus – ferdig på Shield og emulator
2026-10-26: Plan `2026-10-26-quiet-connection-status.md` er fullført. Felles HA-status viser tilkopla som ein nøytral 6 dp prikk og tilkopling som ein 8 dp ring, med semantisk omtale. Feil/oppsett får labelSmall; status står inline ved tittelen i HA-innstillingane.
709/709 native/unit-prøver, 0 skip; lint 0 feil (62 åtvaringar, 1 hint), bygg OK. APK `08c252f1fc8471b706d5b8036ad2e3c7116772370a8c139e0b3fffdd38794ff9` er installert med -r og hashverifisert på begge. Rapport: `docs/verification/quiet-connection-status.md`.
Smarthus og HA-innstillingar er visuelt kontrollerte på begge, kortveljaren på Shield. Fem ferske UI-uttrekk stadfestar små markørar utan synleg Tilkopla/Connected-tekst. Faktisk reint Shield-bilete: `.superpowers/quiet-connection-status/smarthus-shield.png`.
Profil-, konto- og hushaldsfiler på begge er byteidentiske før/etter. Ingen HA-styring, avspeling, datatømming, transport-/launcher-/tilgjengeendringar eller push; brukarvedlegg er urørde. Begge står på Smarthus med tidlegare kortoppsett bevart.
Ingen påbyrja kodearbeid er uferdig. Eldre fysisk Heim-knapp-oppfølging er framleis ikkje stadfesta; denne oppgåva endrar ikkje Heim-tenesta.

# Blanda Smarthus-kort – ferdig på Shield og emulator
2026-10-26: Plan `2026-10-26-mixed-smarthome-cards.md` er fullført etter «Ikkje berre breidde kort?». S har tettare luft/ikonflate utan mindre tekst eller tap av detaljar; L-vêr har større temperatur, vêrikon og fullbrei prognose under. M og dynamisk vekst ved stor skrift er bevarte.
709/709 native/unit-prøver, 0 skip; lint 0 feil (62 åtvaringar, 1 hint), bygg OK. APK `9bd09278c1abbb8b3f29cee4701760493696cb2e5bacd2f294531508cad66e4b` er installert med -r og hashverifisert på begge. Rapport: `docs/verification/mixed-smarthome-cards.md`.
Shield har kompakt samla rutenett: Ute L, Straumpris M, Stova S, Sonos M, Varmepumpa S. Alle fem er synlege ved opning. Pilnavigasjon til Sonos/klima og ny opning er prøvde. Reint faktisk bilete: `.superpowers/smart-home-view/smarthus-mixed-shield.png`.
Berre Shield-kortstorleik/rekkjefølgje og tettleik er endra; andre profilverdiar er like. Emulatorprofil og konto-/hushaldsfiler på begge er byteidentiske. Ingen HA-styring, avspeling, sletting, launcher-/tilgjengeendringar eller push; brukarvedlegg er urørde.
Ingen påbyrja kodearbeid er uferdig. Begge står på Smarthus. Eldre fysisk Heim-knapp-oppfølging er framleis ikkje stadfesta; vanleg TV-fokus kan rulle bandet litt når fokus går ned, medan fokusert kort er heilt synleg.

# Smarthus-vising og skjermbilete – ferdig på Shield
2026-10-26: Brukaren bad om ei fin Smarthus-vising og eit bilete. Sett opp faktisk Shield-profil gjennom Stova-menyane: samla/Balansert rutenett, M-kort for Ute, Straumpris, Stova og Varmepumpa på første skjerm; Sonos i neste rad.
Vêr og Tibber-straumpris er lagde til som lesekort. Dei tre opphavlege kort-ID-ane/einingane er bevarte, med breiare storleik; korte lokale namn på vêr/pris/varme. Ingen HA-tenester eller avspeling er utløyste.
Ny opning av sida er kontrollert. Reint, faktisk 3840×2160-skjermbilete med alle tekstar og heile første rutenettet: `.superpowers/smart-home-view/smarthus-shield.png`. Rapport: `docs/verification/smart-home-view.md`.
Før/etter-samanlikning består: berre aktivt dashboard er endra, alle andre profilfelt er like og konto-/hushaldsfiler er byteidentiske. Ingen kjeldekodeendring, bygg eller ny installasjon; Shield står på den ferdige Smarthus-sida.
Ingen påbyrja kodearbeid er uferdig. Eldre fysisk Heim-knapp-oppfølging er framleis ikkje stadfesta. Emulator, brukarvedlegg og andre launcher-/tilgjengeval er urørde; ingen push.

# Skyveknappar for utsjånad – fullført på begge einingar
2026-10-26: Plan `2026-10-26-appearance-sliders.md` er fullført, kodecommit 41557f4. Fem skyveknappar: appkortbakgrunn 0–100 %, demping 0–100 %, hjørne 0–32, kortavstand 0–40 og spotlight 5–60 s. OK/piler/Back, direkte mus/berøring og straks-preview; eldre bool/preset bevarer utsjånaden.
705/705 native/unit-prøver, 0 skip; lint 0 feil (62 åtvaringar, 1 hint), bygg OK. APK 141aa05fe8993a4708d0b6237593e99d6c5553a5d4599584e22ab77ffc4e32b3 er installert med -r og hashverifisert på emulator-5580 og Shield. Rapport: `docs/verification/appearance-sliders.md`.
Prosessdelt editor per profil samlar ventande sliderskrivingar, bevarer siste verdi ved rask retur og ventar før kopi/nullstilling. Regresjonar for ny opning under skriving, fleire piler før ramme, vertikal sveip og Android-retur er grøne. Native 0/50/100 %-pikselprøve og dobbel skrift er visuelt kontrollerte.
Alle fem sliderar er prøvde på emulatoren; Shield vart vekt og prøvd med 100→97 %, Back og Venstre-retur til Utsjånad. Prøvevala er sette tilbake (emulator 0/50/20/16/12, Shield 100 og eldre preset). Eldre profilfelt og konto-/hushaldsdata er bevarte; berre nye talfelt og Shield sin standard bakgrunnsfilter er serialiserte.
Begge står på Heim. Ingen avspeling, HA-styring, datatømming, launcher-/tilgjengeendringar eller push. Evidens/APK ligg i `.superpowers/appearance-sliders/`. Ingen kodearbeid er uferdig; fysisk Heim-knapp-oppfølging frå eldre oppgåve er framleis ikkje stadfesta. Brukarvedlegg er urørde.

# Bibliotek og aldersgrense for bakgrunn – fullført på begge einingar
2026-10-26: Plan `2026-10-26-background-library-age.md` er fullført, kodecommit 7c52582. Utsjånad → Bakgrunn har faktiske Jellyfin/Emby-bibliotek og direkte aldersval; standard maks 15 år, ukjend alder skjult. Same filter på hero, sesong, faste bilete, søk, buffer og førehandsvising.
689/689 native/unit-prøver, 0 skip; lint 0 feil (53 åtvaringar, 1 hint), bygg OK. APK 1214aaba87321574466b8aff1bb09916aafc2b55b6387cd4acba36eefea38b8a er installert med -r og hashverifisert på emulator-5580 og Shield. Rapport: `docs/verification/background-library-age.md`.
Faktiske biblioteklister og aldersdialog er prøvde på begge. Emulator: berre Filmar/maks 6 gav rett bakgrunn og sju verifiserte titlar i buffer; ingen valde bibliotek gav temabakgrunn. Shield: D-pad og fokusretur prøvd. Begge står no på Heim med alle bibliotek/maks 15.
Alle eldre profilfelt er bevarte. Emulatoren har berre serialisert det nye filterfeltet; Shield-profilen og konto-/hushaldsfiler på begge er byteidentiske. Ingen avspeling, HA-styring, serverregel-/launcher-endring, datatømming eller push. Evidens/APK ligg i `.superpowers/background-filter/`.
Ingen kodearbeid er påbyrja og uferdig. Fysisk Heim-knapp-oppfølging frå førre oppgåve er framleis ikkje stadfesta; denne endringa rører ikkje Heim-tenesta. Brukarvedlegg er urørde.

# Smarthus, innstillingar og gjentekne Heim-trykk – installert på begge einingar
2026-10-26: Plan `2026-10-26-smarthome-settings.md` er gjennomført, kodecommit 8dc7fa5. Ny innstillingsoversikt/fokus, faktisk HA-status, ekte appikonprøve, samla dynamisk Smarthus-rutenett med tettleik og S/M/L; Android har kort namn.
670/670 native/unit-prøver, 0 skip; lint 0 feil (52 åtvaringar, 1 hint), bygg OK. APK 0b617fcdd937e561efa7f424b9181136b7fff5934406f8d2a8d818e915801ae4 er installert med -r og hashverifisert på emulator-5580 og Shield. Rapport: `docs/verification/smarthome-settings.md`.
Heim-låsen oppdagar faktiske HOME-aktivitetar. Eigaren si melding «først rett, så feil neste trykk» vart spora til global sperre som avviste Projectivy etter stock-launcher; retta med samanslåing per vindauge. Endeleg APK bestod 6/6 ADB HOME-trykk og Android-retur. Ny fysisk fjernkontrollprøve er etterspurd, enno ikkje stadfesta; dette er den einaste opne oppfølginga, ingen kodearbeid er påbyrja og uferdig.
Projectivy med/utan kortbakgrunn, HA-status, oversikt og faktisk kortmeny/fokus er prøvde. Emulatoren har alle gamle profilfelt uendra; berre nye smartHome-standardar er serialiserte. Shield sine samtidige profilval og nye Sonos-kort er bevarte. Konto-/hushaldsfiler er byteidentiske før/etter på begge.
Begge står på Heim. Ingen HA-styring, mediaavspeling, datatømming, andre launcher-/tilgjengeendringar eller push. Bilete, APK, før/etter-data og loggar ligg i `.superpowers/smarthome-settings/`; brukarvedlegg er urørde.

# Kompakte kort og synlege titlar – fullført på emulator og Shield
2026-10-26: Plan `2026-10-26-card-sizes-and-captions.md` er fullført, kodecommit 33e4410. Seks storleikar med Kompakt (224) og titlar på som standard; same mål i Appar. Undertittelbreidd og småkorttekst er retta. Android-ikonet på Heim og Halloween-nedteljing er fjerna.
652/652 native/unit-prøver, 0 skip; lint 0 feil (48 åtvaringar, 1 hint), bygg OK. APK c9888417c17b29b2911607a0d166af8bbb09deac49421f4d60b9ff9fec49ece3 er installert med -r og hashverifisert på emulator-5580 og Shield. Rapport: `docs/verification/card-sizes-and-captions.md`.
Faktiske kortnamn, seks menyval, app-rutenett, piler, meny-/radretur er prøvde. Begge aktive profilar har no COMPACT/showTitles=true. Shield sine radvise overstyringar er bevarte; nye homeRows=ONE/homeRowInset=LOW er serialiserte. Alle andre profilfelt og konto-/hushaldsfiler er uendra.
Begge står på Heim. Shield vart vekt frå dvale for prøvinga. Ingen avspeling, HA-styring, datatømming, systemlauncher-endring eller push. Bilete/APK og kontrollskript ligg i `.superpowers/card-sizes/`.
Ingen påbyrja steg eller spørsmål står att. Brukarvedlegg er urørde. Eldre utsette release-/signeringsoppgåver er framleis ikkje ny bestilling.

# Éi rad og visuelt system – fullført på emulator og Shield
2026-10-26: Plan `2026-10-25-cinematic-visual-system.md` er fullført. Éi heil rad nedst er standard; éi/to/rullande og høgdeplassering er profillagra. Innstillingar har felles ikon og konkrete prøver; Smarthus/Appar/profil-/menyflater deler formspråk. Kode sist eb5d1e3.
645/645 testar med native skjermprøver, 0 skip; lint 0 feil (46 åtvaringar, 1 hint). APK aa9acb9140387cf4baabd4cfa3533ee82e35d4bc6dfc9784d41f739b4aab2627 er installert med -r og hashverifisert på emulator-5580 og Shield. Rapport: `docs/verification/cinematic-visual-system.md`.
Faktiske radval/plasseringar, piler, meny-/kategoriretur og Heim → Utsjånad er prøvde; radretur og gruppeoverskrift er retta med raude/grøne prøver. Opptak frå begge einingar ligg i `.superpowers/cinematic/`, om lag 12 s kvar. Begge står på Heim.
Shield sine profil-/konto-/hushaldsfiler er byteidentiske. Emulatoren har berre serialisert nye standardfelt homeRows=ONE/homeRowInset=LOW etter prøvinga; alle eldre profilfelt og konto-/hushaldsfiler er uendra. Ingen HA-styring, mediaavspeling, datatømming eller push.
Ingen påbyrja steg står att. Brukarvedlegg er urørde. Ingen spørsmål til brukaren; eldre utsette release-/signeringsoppgåver er framleis ikkje ny bestilling.

# Menysystem og mjuke overgangar – fullført på emulator
2026-10-25: Plan `2026-10-25-menu-motion.md` er fullført. Innstillingar er grupperte, Utsjånad har åtte små sider, og langt-OK-menyane samlar radval under «Tilpass rada». Lenke-/profilfokus er retta. Kodecommit bd6bc49.
626/626 testar med skjermprøver, 0 skip; lint 0 feil (46 åtvaringar, 1 hint), debug-bygg OK. APK 99aca2d90fc95d8931a38234c465247e1e1f49832470f1d16c3407bec78650b0 er installert/verifisert med -r på emulator-5580. Rapport: `docs/verification/menu-motion.md`.
Heim/Smarthus bruker avbrytbar 560 ms rulling; side-/dialog-/fokusovergangar er mjukare, med null varigheit ved redusert rørsle. Faktisk Heim → Utsjånad, kategori/retur, lenkja til radredigering og appmeny/undermeny er prøvde med ekte data. Klipp: `.superpowers/menu-motion/row-motion.mp4` (12,04 s).
Profil-, konto- og hushaldsfiler er byteidentiske før/etter. Ingen tenestestyring, datatømming, push eller Shield-installasjon. Shield har framleis Smarthus-bygget frå førre oppføring; fysisk fjernkontroll og Shield-rørsle er ikkje prøvde i denne bestillinga.
Ingen påbyrja steg står att. Dei eksisterande Smarthus-endringane vart bevarte og ligg no i d25dc22 frå den førre oppgåva; brukarvedlegg er urørde. Emulatoren står på Heim. Ingen spørsmål til brukaren.

# Smarthus-einingsveljaren er retta og oppdatert på Shield
2026-10-25: Plan `2026-10-25-entity-picker-focus.md` er fullført. Feilen var i «Legg til kort», ikkje dashbordet: Ned frå Kamera gjekk sidelengs til Straumpris. Søk/kategoriar/einingar har no eksplisitte vertikale vegar og stabil retur.
616/616 testar inkludert skjermprøver, 0 skip; lint 0 feil (39 åtvaringar, 1 hint), debug-bygg OK. Endeleg testa APK a25e7e61171514de7113fb544bf58f8312b0fb48c9379b14b34b8a84af38d327 er installert med -r på emulator-5580 og Shield; einingshash-ar er like. APK-kopi ligg i `.superpowers/entity-picker-focus/`.
Faktisk veljar med ekte data er prøvd på begge einingar: rulla Kamera/Cameras → Ned til eining → Opp til same kategori og fleire Ned gjennom rader utanfor første synsfelt. Allereie tillagde einingar, tom/fråkopla liste og tilgjengeleg Prøv igjen er dekte av tastetestane. Rapport: `docs/verification/entity-picker-focus.md`.
Profil- og kontofiler er byteidentiske før/etter. Ingen einingar lagde til eller styrte. Shield står i veljaren og er frigjeven til eigaren. Ingen påbyrja steg i denne planen står att; ingen push.
Det kom samstundes andre endringar i tema/utsjånad/innstillingar og tilhøyrande testar i det delte arbeidstreet etter sluttbygget. Dei er urørde og er ikkje del av Smarthus-commiten eller den testa APK-en. Brukarvedlegg er òg urørde.

# Ferdig: fokus/navigering og Spole-tittellenkje på Shield
2026-10-25: Stova-plan `2026-10-25-directional-focus.md` og Shield-installasjonsplanen er fullførte, kodecommit 9784990. Glød er fjerna; faste fokusvegar i Heim, Innstillingar og Smarthus er prøvde med tastar og ekte data.
609/609 Stova-testar med skjermprøver, 0 skip; lint 0 feil (39 åtvaringar, 1 hint). Stova a05d0c47931a9f57a8e1e25f1a5fae134f6bd09c89ba09bf1483e47478e75297 er installert/verifisert på emulator-5580 og Shield. Rapport: `docs/verification/directional-focus.md`.
Spole beta22/159 frå kjeldecommit 830cd4d i `C:/JellyBin/spole-launcher-link` er installert med same eigarnøkkel; einingshash f309682bc23f04535877fe0a54624950037d234893f1b77b86957788447351e8. 1293 testar bestått, 1 LAN-skip, lint utan feil. Faktisk Stova-kort → rett detaljside er verifisert varm/kald etter eigaren si innlogging, utan avspeling.
Kontoar/favorittar er bevarte. Eigaren endra app-radformat under prøvinga; sluttkontrollen viser berre det feltet endra. Smarthus har ekte kort på emulator, tomt dashbord på Shield. Touch-hald og D-pad i appmeny er prøvde; fysisk halde-OK er ikkje generert av ADB-prøva.
Ingen kjende påbyrja steg står att, ingen push/publisering, HA-styring eller datatømming. Shield er sett tilbake til Stova og frigjeven til eigaren. Tre underagentar deltok; alle bygg/einingsprøver er ferdige.

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
