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
| Legg til Smarthus-kort | `ha/ui/AddCardFlow.kt` | Einingskatalog, filtrering og fokus mellom søk, kategoriar og einingar |
| Innstillingar | `settings/SettingsScreen.kt`, `settings/SettingsLayout.kt`, `settings/SettingsNavigation.kt` | Datakopling, fokus mellom meny/innhald og fast overskrift |
| Utsjånad | `settings/AppearanceSettings.kt`, `settings/AppearanceMenu.kt`, `settings/AppearanceSettingsContent.kt` | Lagring, kategorioversikt og kontrollerte val kvar for seg |
| Fargar, mål og rørsle | `core/theme/` | Felles tokens; ingen fargelitteralar i skjermane |
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
- Applista deler pakkeskanning og pakkelyttar. Ikonpakkar blir oppdaga når dei blir etterspurde.
  Breie pakkeikon bevarer sideforholdet. Projectivy blir installert separat; `ProjectivyIcons`
  legg til eit eige transparent Spole-merke i Stova sin katalog utan å endre tredjepartspakken.
- HA-socketen treng synlege Heim-brikker eller ein open Smarthus-/legg-til-flyt. Bakgrunn og tapt behov har ein kort fråkoplingsfrist.
- Heim abonnerer på høgst tre synlege HA-brikker; Smarthus abonnerer på korta i dashbordet.
- HA-registeret blir brukt om att i seks timar. Historikk og prognosar blir delte mellom kort og panel.
- HA-snapshot blir skrive høgst kvart minutt og forsøkt flusha ved stopp. Berre tidsstempelendringar gir inga skriving.
- Watch Next registrerer ingen innhaldslyttar utan løyve. Klokker stoppar når skjermen ikkje er aktiv.

Buffer er alltid avgrensa til rett profil, konto eller HA-tilkopling. Utlogging og ny tilkopling
må ugyldiggjere relevant tilstand. Ingen test skal ringje ekte tenester utan eit uttrykkeleg integrasjonssteg.

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
Radflytting bruker `core/ui/SmoothScroll.kt`: 560 ms med mjuk start/stopp, utan eit innleiande
hopp. Eitt rullejobb eig overgangen; nye piler kan snu eller velje neste logiske rad undervegs.
`SoftEntrance` tonar inn berre den nye innstillingssida, utan eit utgåande fokustre.
`LocalReduceMotion` gir null varigheit og slår av parallax. Dialogar går gjennom `StovaDialog`.
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
Utsjånad har åtte små kategorisider; oversikta bruker to kolonnar, éi ved stor skrift.
`SettingsVisuals` bind kvart mål til same symbol i meny, kategori og `PageHeader`.
`SectionHeading` gir same tekstkant og teljarplass i Appar og Smarthus. Opne innstillingsrader
har små konkrete prøver av format, plassering, avstand og fokus. Berre Høgre/OK endrar eit val;
Venstre går tilbake til seksjonsmenyen. Kompakte verktøyknappar er 36 dp og veks med teksten.
Tilbake går til kategorien ein kom frå. Førehandsvisinga ligg etter vala og tek ikkje fast
plass frå kontrollane. Langt-OK-menyane samlar dei detaljerte radvala under «Tilpass rada».

Smarthus bruker `ha/cards/CardFocusNavigation.kt` for retningar mellom logiske kortrektangel
frå same pakking som visinga. Venstre/Høgre held seg på same kortband; Opp/Ned vel næraste
neste rad og tek omsyn til breie/høge kort. Fokus utanfor skjermen blir rulla inn før flytting.
Første kortband blir rulla inn saman med gruppeoverskrifta. Dersom eit høgt kort ikkje får
plass med overskrifta, har heile det fokuserte kortet prioritet. Små kort gir namnet eiga
tekstbreidd under ikon-/statusrada; breie kort kan ha namn ved ikonet.

## Verifisering

Testane speglar pakkane under `app/src/test/java/app/stova/`. Sjå [CONTRIBUTING.md](../CONTRIBUTING.md)
for kommandoar og [AGENTS.md](../AGENTS.md) for arbeidsreglar. Planar og eldre verifikasjon ligg i
`docs/superpowers/` og `docs/verification/`; dei erstattar ikkje dei faktiske testresultata.
