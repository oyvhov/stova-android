# Slik er Stova bygd

Stova har éin app-modul og éin modul for Baseline Profile. Funksjonane er delte i pakkar,
med manuell kopling i `AppContainer`. Ein ny funksjon treng ikkje eit nytt rammeverk.

## Finn rett stad å endre

| Oppgåve | Start her | Ansvar |
|---|---|---|
| Navigasjon og profilbyte | `navigation/StovaApp.kt` | Destinasjonar, tilbakeknapp og felles bakgrunn |
| Kopling til data på Heim | `home/HomeScreen.kt` | Samlar straumar, livssyklus og opning av Spole |
| Heim-layout og fokus | `home/HomeContent.kt` | Rulleflate, fokusretur, radflytting og menytilstand |
| Synlege rader | `home/HomeDeckGeometry.kt` | Fast radfelt nedst, målte teksthøgder og lik kunstskalering |
| Radene på Heim | `home/HomeRows.kt` | Media, appar og Watch Next med kontrollerte verdiar |
| Hero og parallax | `home/HeroPanel.kt`, `home/ParallaxHero.kt` | Innhald og lagrørsle i den rullande overskrifta |
| Smarthus på Heim | `home/HomeChips.kt` | Berre synlege brikketekstar; hendingar teiknar ikkje mediaradene på nytt |
| Smarthus-skjermen | `ha/ui/SmartHomeScreen.kt` | Livssyklus, datakopling og kontrollpanel |
| Smarthus-layout | `ha/ui/SmartHomeContent.kt` | Kontrollerte kort, fokus og grupper |
| Smarthus-geometri og val | `ha/cards/CardPacking.kt`, `ha/cards/CardFocusNavigation.kt`, `ha/ui/DashboardLayoutDialog.kt` | Same dynamiske pakking for teikning/piler, profillagra tettleik og gruppering |
| Legg til Smarthus-kort | `ha/ui/AddCardFlow.kt`, `ha/ui/RoomSetupContent.kt` | `AddCardDialog` deler romnavigasjonen med innhaldet: Tilbake går frå kortforslaga til det fokuserte rommet, så ut. Rom først, gjennomgått utval før lagring; full einingskatalog via «Finn éi eining» |
| Smarthus-kontrollpanel | `ha/cards/CardDialogs.kt`, `DeviceControls.kt`, `DeviceControlPages.kt`, `PanelSlider.kt` | Fast namn/rom og botn, rullbart innhald, korrekt hovudverdi og eigne modus-/kjeldesider med fokusretur |
| Innstillingar | `settings/SettingsScreen.kt`, `settings/SettingsLayout.kt`, `settings/SettingsNavigation.kt` | Datakopling, fokus mellom meny/innhald og fast overskrift; seksjonane med merke og ei forklarande line |
| Heim i lite format | `settings/HomeMiniature.kt`, `home/HomeContent.kt` (`LocalHomePreview`), `core/ui/FocusOutline.kt` (`LocalDrawnFocus`) | Den ekte Heim med utkastet, skalert med tettleiken, ved sida av tittelen på kvar Utsjånad-side; tek aldri fokus og teiknar startkortet som fokusert |
| Ikonprøve | `settings/AppIconPreview.kt` | Verkelege appar med utkastet til utsjånad under Ikon |
| Pip-verda (Heim for barneprofilen) | `kids/KidsHomeScreen.kt`, `kids/KidsHomeContent.kt`, `kids/KidsWorld.kt`, `kids/KidsSeason.kt`, `core/theme/KidsTokens.kt` | Ein barneprofil (`Profile.kid`) får sin eigen Heim: verda bak (himmel etter tida, åsar og pynt etter barnesesongen, partiklar berre medan nokon trykkjer) eller filmkunsten som hos dei vaksne (`PipPrefs.background`), store kort (breie miniatyrbilde i «Hald fram», plakatar elles: `kidsArt`), «Nytt!» på nye usette, og radnamnet i bobla til Pip. Foreldra stiller Pip på/av, opplesing, leggetid og bakgrunn under Profilar → barneprofilen (`PipPrefs`). Debug-bygg: `files/debug-kids-home`, `debug-kids-season`, `debug-kids-time` (sjå `docs/EMULATOR.md`) |
| Brua til Spole | `media/SpoleHandoff.kt` (`SpoleContract`, `SpoleApprover`), `media/ui/AddAccountFlow.kt`, `media/SpolePlayback.kt`, `MediaRepository.applyPlayback` | Logg inn éin gong: i Quick Connect-steget godkjenner Spole koden Stova viser (`app.reelstack.action.APPROVE_SIGN_IN`, starta for eit resultat), og Stova får sin eigen token frå tenaren. Framdrift: Spole sender `app.reelstack.action.PLAYBACK_PROGRESS` ved stopp; `SpolePlaybackReceiver` gir det til `applyPlayback`, som rettar radene i alle profilar med same brukar, held rettinga over oppfriskingar i to minutt og spør tenaren att etter fire sekund. Når eit kort med fokus skiftar plass i rada, følgjer rada det (`HomeRows.MediaRow`, `KidsCard`). Kontrakten står i `docs/superpowers/plans/2026-10-30-spole-bridge.md` og i `docs/LAUNCHER_LINK.md` i Spole |
| Profilbyte i begge appane | `profiles/ProfileSession.picks`, `media/SpoleProfiles.kt` (`spoleProfileFor`, `follow`, `select`), `profiles/Profile.spole` (`SpoleLink`), `settings/ProfileSettings.kt` (`SpoleProfileRow`) | Når nokon vel ein profil for hand i Stova, byter Spole til same person (`call("select_profile")` på profilleverandøren til Spole): Spole-profilen vald under Innstillingar → «Spole-profil», elles den med ein konto profilen har sjølv her. Lånte kontoar tel ikkje, eit barn går berre til ein barneprofil, og ein kaldstart er ikkje eit val. Spole går aldri ut av ein barneprofil med PIN på denne måten |
| Spole-rader for ein person | `media/SpoleRowSource.kt`, `media/SpoleProfiles.kt` (`rows`, `parseSpoleRows`, `spoleRowAccount`), `AppContainer.spoleRows` | Ein profil som følgjer ein Spole-person (Innstillingar → «Spole-profil») og ikkje har kontoar her, får radene til personen frå `content://<Spole-pakke>.launcher/rows`: «Hald fram», nye filmar og nye episodar, henta av Spole med si eiga innlogging. Radene har same nøklar som ein eigen konto ville gitt, med ein syntetisk konto (`spole-<kind>-<user>`, aldri lagra, ingen adresse) som opnar titlane i Spole. Spør på nytt ved retur til Heim (etter eitt minutt), etter ei handling og når Spole melder framdrift; aldri når profilen ikkje følgjer nokon |
| Handlingar på media-kort | `media/ui/ItemActionsMenu.kt` (`MediaItemAction`), `home/HomeScreen.kt` (`act`), `SpoleProfiles.itemAction`, `SpoleContract.uri(fromStart)` | Langt OK på ein film eller episode gir «Spel frå starten» (`spole://open?…&action=play&start=0`) og «Fjern frå Hald fram» for ein halvsett tittel, og «Merk som sett». Spole gjer handlinga med sin eigen konto (`call("item_action")`), og radene spør på nytt etterpå. Ikkje for barn |
| Spole-profilflis | `media/SpoleProfiles.kt`, `apps/FavoriteTile.kt` (`SpoleProfile`), `apps/Shortcut.kt` (`spole:<kind>:<user>`), `apps/ShortcutTile.kt`, `SpoleHandoff.openProfile` | Apps-sida viser «Spole-profilar» frå `content://<Spole-pakke>.launcher/profiles` (berre når Spole er installert og sida blir vist). Ei profilflis viser Spole-banneret med profilbiletet, kan leggjast i favorittane og opnar `spole://profile`, som byter til den profilen i Spole. Kontrakten står i `docs/LAUNCHER_LINK.md` i Spole |
| Barnebibliotek | `media/MediaAccountRepository.kt` (`shareLibraries`, `sharedLibraries`), `media/net/LibrariesClient.kt`, `media/net/JellyfinEmbyClient.kt` (`parentId`), `settings/ProfileSettings.kt` | Foreldra vel bibliotek frå kontoane på dei vaksne profilane (`MediaRepository.lendableLibraries`). Barneprofilen får éin lånt konto per vaksenkonto (`MediaAccount.sharedFrom`, `libraries`) med token frå kontoen han er lånt frå; kvar spørjing går med `ParentId` for eitt bibliotek, og radene frå fleire bibliotek blir fletta. Utan val: inga spørjing. Logg den vaksne ut, og lånet går med |
| Pip, roboten | `kids/PipMotion.kt`, `kids/PipScene.kt`, `kids/PipFigure.kt`, `kids/Pip.kt`, `kids/PipVoice.kt`, `tools/pip/make_pip.py` → `assets/pip/pip.glb` | `PipMotion` er kroppen: fysikk steg for steg (gange med vagging, hopp mellom rader, setje seg, antenne på fjør, auge som kjem først, små ting han gjer sjølv) og ber berre om bilete når noko skjer. `PipScene` teiknar han i ekte 3D med Filament (glTF med PBR-materiale, studiolys med refleksar, skugge og kantutjamning; ikkje bloom, som PowerVR-skjermkortet i Google TV Streamer teiknar kvitt og gjennomsiktig) i ein gjennomsiktig `TextureView`; utan OpenGL ES 3 (emulatoren sin programvare-GPU, einingstestar) teiknar `Pip.kt` han flatt. Modellen blir bygd av skriptet; kvileposisjonane i skriptet og i `PipScene` må endrast saman. `PipVoice` les opp namn med TextToSpeech |
| Sesongpynt | `home/SeasonalScene.kt`, `home/SeasonalTile.kt` | Max: to lag på éi klokke (bak og gjennomsiktig framfor korta), mange ulike delar per sesong (`seasonTouches`), og pynt på kvar appflis plassert etter appen sin nøkkel |
| Snarvegar i favorittrada | `apps/Shortcut.kt`, `apps/FavoriteTile.kt`, `apps/ShortcutTile.kt`, `apps/ShortcutMenu.kt`, `apps/ShortcutCatalog.kt`, `ha/HaShortcuts.kt` | Bibliotek og scene/skript som fliser mellom appane: eigne nøklar (`library:`, `ha:`) i `favorites`, namn og bilete i `AppPrefs.shortcuts`, så Heim teiknar dei utan nettverk. Bibliotek opnar Spole med `SpoleHandoff.openLibrary` (`spole://library`, elles Spole sin heim); scenar køyrer med `runHaShortcut`, som held tilkoplinga berre medan kallet går. Appar-sida hentar lista over bibliotek og scenar. Sceneflisa viser ikonet Home Assistant viser (`ha/HaIconFont.kt`): Material Design Icons 7.4.47 i `res/font/material_design_icons.ttf` (Apache 2.0, lisens i `assets/licenses`), og `assets/mdi/names.txt` blir berre lesen for namna som trengst |
| Nedtrekk frå toppfeltet | `core/ui/TopDropdown.kt`, `core/theme/Tokens.kt` (`StovaDropdown`) | Eitt kort for alt som fell ned frå toppfeltet: litt gjennomsiktig flate utan uskarpheit, under ankeret og innanfor TV-kanten, veks fram og lukkar seg på Tilbake |
| Brikkemeny | `ha/ui/HomeStatusChips.kt`, `ha/ui/ChipDropdown.kt` | OK på ei brikke for lys, mediaspelar, termostat eller brytar: lite kort under brikka med verdien stort og ei tynn stripe; Venstre/Høgre endrar lysstyrke, volum, måltemperatur eller viftefart (`PanelState`, same utsending som Smarthus), OK slår av/på eller pause/spel; Smarthus har eigen knapp i toppfeltet. Kort og eining blir lesne berre medan kortet er ope; andre brikker opnar Smarthus som før |
| Einingsmeny | `navigation/SystemQuickMenu.kt`, `system/SystemStatus.kt`, `homebutton/HomeButtonService.kt` | Langt OK/Meny på Innstillingar: ekte status lesen éin gong ved opning (nettverket berre som ikon: Wi-Fi med styrke, kabel eller overstreka), Bluetooth, omstart via straummenyen til Heim-knapp-tenesta og Alle Android-innstillingar |
| Heim-knappen | `homebutton/HomeActivities.kt`, `homebutton/HomeKeyLogic.kt`, `homebutton/HomeButtonService.kt` | Oppdaga HOME-aktivitetar, tastefilter og avgrensa vindaugsreserveveg |
| Utsjånad | `settings/AppearanceSettings.kt`, `settings/AppearanceMenu.kt`, `settings/AppearanceSettingsContent.kt` | Lagring, kategorioversikt og kontrollerte val kvar for seg |
| Skyveknappar og lagring | `core/ui/SettingsSliderRow.kt`, `profiles/AppearanceValues.kt`, `settings/AppearanceEdits.kt` | TV-/touch-justering, bakoverkompatible verdiar og delt editor per profil med samla lagring |
| Bakgrunnsutval | `profiles/BackgroundFilter.kt`, `media/BackgroundEligibility.kt`, `settings/BackgroundLibraryPicker.kt`, `settings/BackgroundAgeSetting.kt` | Konto-/bibliotekval, tolka aldersgrense, TV-dialogar og same sluttfilter på alle kunstvegar |
| Bakgrunn ved filterbyte | `background/ProfileBackdrop.kt` | Ny bilet-/overgangstilstand ved endra utval, slik at gamal kunst ikkje blir halden att |
| Fargar, mål og rørsle | `core/theme/` | Felles tokens; ingen fargelitteralar i skjermane |
| Skrift og tone frå kunsten | `core/theme/Typography.kt`, `core/theme/ArtTone.kt` | Outfit (OFL, `res/font/outfit.ttf`, lisens i `assets/licenses`) til titlar, klokke og store tal; brødtekst i systemskrift. Éin dempa farge frå bakgrunnsbiletet, same lysheit |
| Dialogar og fokus | `core/ui/` | Gjenbrukbare TV-komponentar, éin dialogovergang |
| Radoverskrifter og radval | `home/RowOptions.kt`, `home/RowHeadingEditor.kt`, `home/RowLabels.kt` | Same redigering frå langt OK og Innstillingar; lagra i profilens `RowStyle` |
| Media | `media/` | Kontoar, REST, parser, rader, diskbuffer og Spole-kontrakt |
| Home Assistant | `ha/HaSocket.kt`, `ha/HaRepository.kt` | Transport og profil-/dashboardtilstand |
| HA-buffer | `ha/HaReadCache.kt`, `ha/HaSnapshotWriter.kt` | Delte førespurnader og sjeldne diskskrivingar |
| Profilar og lagring | `profiles/`, `core/storage/`, `core/security/` | Profilreglar, atomisk JSON og krypterte løyndomar |

Alle kjeldestiane i tabellen er relative til `app/src/main/java/app/stova/`.

`ArtworkSize` har seks profillagra storleikar, med Kompakt (224 dp brei) og korttitlar på som
standard. Gamle SMALL/MEDIUM/LARGE bevarer dimensjonane. `ArtworkSizeLabels` gir dei same
namna i globale val og radval; overskriftsstorleik har ei eiga liste. `artworkDimensions` eig
formatproporsjonane, også i Appar-rutenettet. Watch Next avgrensar undertittelen til kunstbreidda.
`RowStyle.cardTitles` lèt ei rad ha eigne namn under korta (null følgjer profilen); `showsCardTitles`
blir lese av kort, Watch Next og radgeometrien. Appar-sida følgjer alltid profilvalet. Heim har ingen radteljar.
Appar-sida har fire banner per rad: `LocalArtworkScale` skalerer kortstorleiken frå Heim til breidda, medan ikon held storleiken sin. Rekkjefølgja er Favorittar, Spole-profilar, Alle appar (samla og alfabetisk), Bibliotek, Scenar og skript, Skjulte appar. Bibliotek og scenar kan vere lange lister, så dei står etter appane.
Favorittrada sluttar med «Alle appar». Appar finst også i menyen i Innstillingar (ved Android), og
`MainShell` sender Tilbake til sida som opna Appar. Toppfeltet har ingen Appar-knapp. Sesongmerket står ved venstre kant.
«Rullande oversikt» opnar med heroen over første rad (same geometri som éi rad). `HomeScrollShift`
gir bakgrunnen rulleframdrifta; berre `graphicsLayer`/teikning les ho. Innstillingsseksjonane lenkjer
ikkje til kvarandre; berre undersider (t.d. ein annan profil sin utsjånad) kan lenkje vidare.
Toppfeltet på Heim har berre Stova-innstillingar; Android-innstillingar ligg i menyen.
Sesongnedteljing gjeld berre jul, og av/på-valet blir respektert både i hero og toppfelt.

## Skil skjerm frå data

Ein `*Screen` koplar ein funksjon til repository, livssyklus og navigasjon. Ein `*Content`
teiknar verdiar og kallar tilbake ved handling. UI-testar bør normalt bruke `*Content` med
kontrollerte data. Integrasjonstestar blir brukte når sjølve koplinga eller livssyklusen er kontrakten.

Tilstand som må overleve ein tur til Spole, høyrer til i viewmodel/repository eller den felles
navigasjonen. Fokus og opne lokale menyar høyrer til skjermen. Bruk stabile konto-, profil-,
rad- og tittel-id-ar som nøklar.

## Gjer arbeid når det trengst

- Media blir vist frå snapshot før nettverket svarar. Vanleg retur til Heim bruker femminuttsbufferen;
  etter ei tittellenkje frå Heim til Spole blir framdrifta henta på nytt éin gong for den aktive profilen.
  Feila henting kan prøvast att ved neste retur etter 30 sekund, også utan bufra rader.
- Smalare spotlight-utval blir brukte om att i to timar. Nye kontoar og sesongbyte kan oppdatere tidlegare.
- Avgrensa bibliotekutval har nøkkel av kjelde, filter og kontoar, med to timars frist (fem minutt ved feil). `MediaSnapshot.backgroundPool` bevarer verifisert bibliotektilhøyrsle for bruk utan nett.
- Applista deler pakkeskanning og pakkelyttar. Ikonpakkar blir oppdaga når dei blir etterspurde.
  Breie pakkeikon bevarer sideforholdet. Projectivy blir installert separat; `ProjectivyIcons`
  legg til eit eige transparent Spole-merke i Stova sin katalog utan å endre tredjepartspakken.
- HA-socketen treng synlege Heim-brikker, ein open Smarthus-/legg-til-flyt eller synleg oppsett HA-innstillingsside. Innstillingane held transporten open for sann tilkoplingsstatus utan eigne einingsabonnement. Bakgrunn og tapt behov har ein kort fråkoplingsfrist.
- Heim abonnerer på høgst tre synlege HA-brikker; Smarthus abonnerer på korta i dashbordet.
- HA-registeret blir brukt om att i seks timar. Historikk og prognosar blir delte mellom kort og panel.
- HA-snapshot blir skrive høgst kvart minutt og forsøkt flusha ved stopp. Berre tidsstempelendringar gir inga skriving.
- Watch Next registrerer ingen innhaldslyttar utan løyve. Klokker stoppar når skjermen ikkje er aktiv.

Buffer er alltid avgrensa til rett profil, konto eller HA-tilkopling. Utlogging og ny tilkopling
må ugyldiggjere relevant tilstand. Ingen test skal ringje ekte tenester utan eit uttrykkeleg integrasjonssteg.

`MediaRowSource` gir `Profile.kid` til mediarepoet. Berre barneprofilar hentar `FAVORITES` og
`MOST_PLAYED`, med eigne feilgrenser og same profil-snapshot som kjerneradene. Radrekkjefølgja er
Hald fram, Favorittane dine, eventuell sesongrad, Det du ser mest, Nye filmar og Nye seriar;
apprada og profilen sine radval fungerer som før. Byte av barnemodus startar mediekjelda på nytt
og ugyldiggjer femminuttsbufferen. Vaksne skjuler også gamle barnerader frå snapshoten utan nye kall.

Favorittar bruker `Filters=IsFavorite`, sist sett og så nyast lagt til, høgst 20 filmar/seriar.
`PlaybackRanking` summerer `UserData.PlayCount` frå alle sidene med sette episodar per `SeriesId`,
og tel filmar kvar for seg. Berre dei 20 øvste titlane får metadata og kunst via eitt ID-oppslag;
seriekortet er serien sjølv. Teljingane kjem i små postar utan bilete, under tilleggsrada si
20-sekundsgrense. Jellyfin prøver moderne sti først og brukarstien ved 404; Emby bruker brukarstien.
`MediaItem.isNew` kjem frå `DateCreated` og uttrykkeleg `UserData.Played=false` for film/episode
dei siste 14 dagane, med innsetjbar `Clock` i parser og klient. Grupperte seriar har `false`:
svaret stadfestar ikkje opprettingsdatoen til dei usette episodane. Gamle snapshot får `false`.

Bakgrunn/hero har `AppearancePrefs.backgroundFilter`: alle bibliotek (`null`) og maks 15 år
som standard. Eit tomt biblioteksett betyr ingen titlar. Referansar inneheld konto-ID og
bibliotek-ID. Klienten spør `Users/{id}/Views` og bruker éin rekursiv `ParentId`-førespurnad
per valt bibliotek. `MediaItem.backgroundLibraryIds` kjem berre frå slike førespurnader;
`ParentId` på ein tittel blir ikkje tolka som toppbibliotek. Overlappande visingar slår saman
medlemskap før deduplisering og lagring. Gamle snapshot har ukjend tilhøyrsle og blir avviste
ved eit avgrensa utval til ny henting har stadfesta medlemskap.

`acceptsBackground` tolkar berre kjende heile aldersmerkingar og avviser ukjend alder når
ei grense er på. Serverens `MaxParentalRating` er ikkje år og blir ikkje brukt. Filteret gjeld
etter sesonginnsetjing, i søk, faste bilete og førehandsvising. Tomt resultat gir temaflata,
aldri eit vidare utval. Vanlege medierader er uendra. Kopiering av utsjånad bevarer mottakaren
sitt filter. `HomeViewModel` startar utvalet på nytt og tømmer gamal hero ved filterbyte.
Visingslaget kontrollerer gjeldande profil/filter på nytt før det viser ei eldre straumutgåve.
`ProfileBackdrop` erstattar både den dekoda biletbufferen og Crossfade-greina ved endra utval.

## Rørsle og parallax

Standardvisinga er éi heil rad i eit avklipt radfelt nedst. `HomeRows` og `HomeRowInset` er
profilval under Utsjånad → Heim og hero. `HomeDeckGeometry` måler overskrift/kjeldenamn/tekst
med den faktiske skrifta, vel lik plass per rad og bevarer sideforholdet på kunsten. To rader
tilpassar kunsten; ved stor skrift eller for trong plass blir éi heil rad vist. Heroen står
roleg over feltet. Opp/Ned når alle ikkje-tomme rader. `HomeViewModel` hugsar siste radnøkkel,
slik at like titlar i fleire rader får rett fokus ved retur. Byte av visingsmodus bevarer aktiv rad.

I det valfrie «Rullande oversikt»-moduset er heroen første element i same `LazyColumn` som radene. Første rad viser heroen; ved neste rad
rullar han ut og radene får heile høgda under topplinja. Tilbake til første rad viser heroen att.
Parallax les rullestillinga i `graphicsLayer`, utan ny komposisjon eller storleiksanimasjon per ramme.
Logoar som lastar eller feilar har tittel som reserve. Spotlight-byte endrar ikkje høgda på overskrifta.
Fokus tilbake til topplinja rullar også lista til toppen, så heroen kan hentast fram sjølv etter
oppstart direkte på ei medierad.

`StovaMotion` eig varigheit og easing. Bruk berre laganimasjonar for alfa, skala og forskyving.
Radflytting bruker `core/ui/SmoothScroll.kt`: 380 ms med mjuk start/stopp (sider 200 ms, små ting 150 ms), utan eit innleiande
hopp. Eitt rullejobb eig overgangen; nye piler kan snu eller velje neste logiske rad undervegs.
`SoftEntrance` tonar inn berre den nye innstillingssida, utan eit utgåande fokustre.
`LocalReduceMotion` gir null varigheit og slår av parallax. Dialogar går gjennom `StovaDialog`; alle langt-OK-menyar
(media, appar, rader, Smarthus-kort) går gjennom `StovaSheet`: eit panel frå høgre kant i full høgd med kunst, store
hovudhandlingar og rolege `SheetAction`-rader. Undersider (`SheetPages`) byter innhald i same panel; Tilbake går ei side
om gongen og fokuserer rada som opna sida. Begge deler same vindauge og same OK-vakt (`StovaOverlay`).
Appmenyen bruker kompakt kunst og «Opne» først; mediemenyen bruker framdrift for «Hald fram» og attståande minutt.
Smarthus har si eiga mørke flate med diskret vêrtoning, utan filmkunst. Fokuserte Heim-brikker viser einingsnamnet.
Dei store kontrollpanela bevarer namn og Lukk/Tilbake medan berre innhaldet rullar. Lysstyrke, måltemperatur og
volum får første fokus; modus, førehandsval, lyskvalitet og kjelder er eigne `SheetPages`.
Lys som er av har 0 % i begge kontrollstorleikane; ukjend lysstyrke på eit påslått lys blir ikkje dikta opp.
TV-kjelde i mediaspelaren viser volum og kjeldeval, utan transportknappar. Andre kjelder følgjer `supported_features`.
Klima skil romtemperatur frå måltemperatur; kjende førehandsval blir omsette, medan tenesta får originalnamna.
Vêrprognosen er ei horisontal rad som kan fokuserast og rullast med fjernkontrollen.
Ikonførehandsvisinga står i overskriftsfeltet ved vanleg tekststorleik. Mediekontoflyten erstattar kontooversikta
og Home Assistant-oppsettet medan ho er open. ADB-oppsettet for Heim-knappen ligg under «Avansert oppsett».
Felles fokus er svakt løft, 2,5 % forstørring på kunst og 1,5 % på kontrollar, og lysare flate.
Fokus teiknar ingen skugge eller glød; transparent kunst får inga ekstra flate.
Gamalt standardval `RING` blir lese som `LIFT`; ei synleg ramme krev valet `OUTLINE` eller
`RING_ZOOM`. Redusert rørsle bevarer flateforskjellen utan løft eller skalering. Menyar tvingar ikkje fram ring.
Tekstfelt bruker fylt flate og nøytral understrek ved fokus; dei lagar ikkje ei aksentramme rundt feltet.
Dialogvindauget godtek OK/Enter berre når trykket byrja med ein ny KeyDown i det vindauget.
Repetisjonar og slepp frå eit langt trykk på kortet bak blir etne opp, utan å sperre neste
vanlege trykk. Eigarskap til trykket blir rydda når vindauget mister fokus.

## Retningar med fjernkontroll

Heim sender Ned frå alle toppkontrollane til første ikkje-tomme rad. Opp/Ned følgjer
radrekkjefølgja, medan Opp frå første rad gjenopprettar siste toppkontroll.
Ei rad utanfor lazy-lista blir rulla inn før fokus blir bede om; eit nyare piltrykk avbryt
ventande flytting. Radflytting i redigeringsmodus har framleis eigarskap til pilene.

`SettingsLayout` eig innstikket frå sidekolonnen og viser berre den valde seksjonen i
fokustreet. Opp/Ned held seg i sitt område. Venstre får først flytte mellom kontrollar
eller i tekst; ved utgang frå innhaldet går fokus til den valde menyseksjonen.
Lenkjer parkerer fokus på den stabile målseksjonen før den gamle sida blir fjerna.
Innstillingar opnar `OVERVIEW`, med seks ikonlenkjer og to kolonnar (éi ved stor skrift).
`LocalSettingsPaneEntry` gjer same fokusparkering ved utskifting av profil- og HA-undersider.
Sidekolonnen bruker 34 dp minimumshøgd og veks/rullar med teksten; Android er siste mål.
Utsjånad har åtte små kategorisider; oversikta bruker to kolonnar, éi ved stor skrift.
`SettingsVisuals` bind kvart mål til same symbol i meny, kategori og `PageHeader`.
`SectionHeading` gir same tekstkant og teljarplass i Appar og Smarthus. Opne innstillingsrader
har små konkrete prøver av format, plassering, avstand og fokus. Berre Høgre/OK endrar eit val;
Venstre går tilbake til seksjonsmenyen. Kompakte verktøyknappar er 36 dp og veks med teksten.
Skyveknappar bruker OK for å gå inn/ut av pileredigering. Tilbake avsluttar justering før
kategorien; Opp/Ned avsluttar og flyttar fokus. Fleire piler før neste ramme blir akkumulerte.
Horisontale drag endrar verdi, vertikale sveip går til sida si rulling. `AppearanceValues`
les nye talfelt med null som arv frå eldre bool/preset. `BackdropScrim` deler dei interpolerte
kurvene mellom Heim og preview, med nøyaktige anker for gamle dempingsval.
`AppContainer.appearanceEdits` eig éin editor per profil. Utkastet oppdaterer preview straks;
ventande sliderskrivingar blir samla i 180 ms (knappar/innhaldsgrenser blir lagra straks), utan å kansellere pågåande atomisk skriving. Utgang
flushar siste verdi i app-scopet, rask ny opning ser same utkast, og nullstilling/kopi ventar
på eldre skrivingar. Lagra prosent på appkort påverkar berre flata, aldri alfa på ikon/tekst.
Tilbake går til kategorien ein kom frå. Innstillingar har fire seksjonar pluss Appar (`settingsOrder`): Heim og
utsjånad (med «Rader på Heim» som kategori), Smarthus og kontoar, Profilar, Appar og System (Heim-knappen, Om og
Android). Gamle seksjonsverdiar blir lesne via `SettingsSection.merged`. I `SettingsLayout` går Tilbake elles eitt steg:
ein seksjon opna frå ei lenkje går tilbake dit (via `LocalSettingsReturnedFrom`), innhald går til seksjonsmenyen, og berre menyen går ut av Innstillingar. Førehandsvisinga ligg etter vala og tek ikkje fast
plass frå kontrollane. Ikon-sida har i staden ei kompakt, ikkje-fokuserbar appprøve før vala,
med same `AppTile`, ikonpakke og bakgrunnsval som Heim. Langt-OK-panela samlar dei detaljerte radvala på sida «Tilpass rada».

Smarthus bruker `ha/cards/CardFocusNavigation.kt` for retningar mellom logiske kortrektangel
frå same pakking som visinga. Venstre/Høgre held seg på same kortband; Opp/Ned vel næraste
neste rad og tek omsyn til breie/høge kort. Fokus utanfor skjermen blir rulla inn før flytting.
Første kortband blir rulla inn saman med gruppeoverskrifta. Dersom eit høgt kort ikkje får
plass med overskrifta, har heile det fokuserte kortet prioritet. Små kort gir namnet eiga
tekstbreidd under ikon-/statusrada; breie kort kan ha namn ved ikonet.

`Profile.smartHome` har standardane `NONE`/`BALANCED`: eitt felt i lagra rekkjefølgje.
Valfri romgruppering viser favorittar først. Kompakt/Balansert/Romleg gir normalt 5/4/3
kolonnar; breidd og skrift avgjer det faktiske talet (2–6). S/M/L bevarer 1×1/2×1/2×2.
Pakkinga fyller ledige plassar i to-raders band; radhøgda tek omsyn til tekst. Store kort
har hovudinnhaldet sentrert i den ledige høgda. Menyen opnar på Storleik, og Tilbake
frå undermeny gir hovudmenyen. Flytting endrar lagra rekkjefølgje med Tidlegare/Seinare;
halde OK + Venstre/Høgre gjer det same. Fokus blir halde på kort-ID også når lagringa
og ompakkinga kjem etter at dialogen er lukka.

Heim-tenesta oppdagar installerte MAIN/HOME-aktivitetar, inkludert alias, og oppdaterer
filteret ved pakkeendringar. Berre sjølve HOME-aktiviteten utløyser retur, ikkje innstillingane
i den same pakken. Like hendingar frå same komponent/vindauge blir samla i eitt sekund;
ein annan launcher blir handsama straks. Eit Stova-vindauge eller nytt HOME DOWN nullstiller
samanslåinga. HOME UP startar Stova utan ei global tidssperre. Andre tilgjengetenester og
systemlauncheren blir bevarte. Debug-loggen inneheld berre HOME-/vindaugsavgjerder, aldri vindaugsinnhald.

## Verifisering

Testane speglar pakkane under `app/src/test/java/app/stova/`. Sjå [CONTRIBUTING.md](../CONTRIBUTING.md)
for kommandoar, [EMULATOR.md](EMULATOR.md) for emulator, demodata og `scripts/Send-TvRemote.ps1`,
og [AGENTS.md](../AGENTS.md) for arbeidsreglar. Demodata kjem frå `PerformanceSeedActivity` i
kjeldesettet `performance`, som berre debug og verktøyvariantane har. Planar og eldre verifikasjon ligg i
`docs/superpowers/` og `docs/verification/`; dei erstattar ikkje dei faktiske testresultata.
