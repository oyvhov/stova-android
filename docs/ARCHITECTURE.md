# Slik er Stova bygd

Stova har éin app-modul og éin modul for Baseline Profile. Funksjonane er delte i pakkar,
med manuell kopling i `AppContainer`. Ein ny funksjon treng ikkje eit nytt rammeverk.

## Finn rett stad å endre

| Oppgåve | Start her | Ansvar |
|---|---|---|
| Navigasjon og profilbyte | `navigation/StovaApp.kt` | Destinasjonar, tilbakeknapp og felles bakgrunn |
| Kopling til data på Heim | `home/HomeScreen.kt` | Samlar straumar, livssyklus og opning av Spole |
| Heim-layout og fokus | `home/HomeContent.kt` | Rulleflate, fokusretur, radflytting og menytilstand |
| Radene på Heim | `home/HomeRows.kt` | Media, appar og Watch Next med kontrollerte verdiar |
| Hero og parallax | `home/HeroPanel.kt`, `home/ParallaxHero.kt` | Innhald og lagrørsle i den rullande overskrifta |
| Smarthus på Heim | `home/HomeChips.kt` | Berre synlege brikketekstar; hendingar teiknar ikkje mediaradene på nytt |
| Smarthus-skjermen | `ha/ui/SmartHomeScreen.kt` | Livssyklus, datakopling og kontrollpanel |
| Smarthus-layout | `ha/ui/SmartHomeContent.kt` | Kontrollerte kort, fokus og grupper |
| Innstillingar | `settings/SettingsScreen.kt`, `settings/SettingsNavigation.kt` | Val av seksjon og fast overskrift |
| Utsjånad | `settings/AppearanceSettings.kt`, `settings/AppearanceSettingsContent.kt` | Lagring/førehandsvising og rein vising kvar for seg |
| Fargar, mål og rørsle | `core/theme/` | Felles tokens; ingen fargelitteralar i skjermane |
| Dialogar og fokus | `core/ui/` | Gjenbrukbare TV-komponentar, éin dialogovergang |
| Media | `media/` | Kontoar, REST, parser, rader, diskbuffer og Spole-kontrakt |
| Home Assistant | `ha/HaSocket.kt`, `ha/HaRepository.kt` | Transport og profil-/dashboardtilstand |
| HA-buffer | `ha/HaReadCache.kt`, `ha/HaSnapshotWriter.kt` | Delte førespurnader og sjeldne diskskrivingar |
| Profilar og lagring | `profiles/`, `core/storage/`, `core/security/` | Profilreglar, atomisk JSON og krypterte løyndomar |

Alle kjeldestiane i tabellen er relative til `app/src/main/java/app/stova/`.

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
- Smalare spotlight-utval blir brukte om att i to timar. Nye kontoar og sesongbyte kan oppdatere tidlegare.
- Applista deler pakkeskanning og pakkelyttar. Ikonpakkar blir oppdaga når dei blir etterspurde.
- HA-socketen treng synlege Heim-brikker eller ein open Smarthus-/legg-til-flyt. Bakgrunn og tapt behov har ein kort fråkoplingsfrist.
- Heim abonnerer på høgst tre synlege HA-brikker; Smarthus abonnerer på korta i dashbordet.
- HA-registeret blir brukt om att i seks timar. Historikk og prognosar blir delte mellom kort og panel.
- HA-snapshot blir skrive høgst kvart minutt og forsøkt flusha ved stopp. Berre tidsstempelendringar gir inga skriving.
- Watch Next registrerer ingen innhaldslyttar utan løyve. Klokker stoppar når skjermen ikkje er aktiv.

Buffer er alltid avgrensa til rett profil, konto eller HA-tilkopling. Utlogging og ny tilkopling
må ugyldiggjere relevant tilstand. Ingen test skal ringje ekte tenester utan eit uttrykkeleg integrasjonssteg.

## Rørsle og parallax

Heroen er første element i same `LazyColumn` som radene. Første rad viser heroen; ved neste rad
rullar han ut og radene får heile høgda under topplinja. Tilbake til første rad viser heroen att.
Parallax les rullestillinga i `graphicsLayer`, utan ny komposisjon eller storleiksanimasjon per ramme.
Logoar som lastar eller feilar har tittel som reserve. Spotlight-byte endrar ikkje høgda på overskrifta.
Fokus tilbake til topplinja rullar også lista til toppen, så heroen kan hentast fram sjølv etter
oppstart direkte på ei medierad.

`StovaMotion` eig varigheit og easing. Bruk berre laganimasjonar for alfa, skala og forskyving.
`LocalReduceMotion` gir null varigheit og slår av parallax. Dialogar går gjennom `StovaDialog`.

## Verifisering

Testane speglar pakkane under `app/src/test/java/app/stova/`. Sjå [CONTRIBUTING.md](../CONTRIBUTING.md)
for kommandoar og [AGENTS.md](../AGENTS.md) for arbeidsreglar. Planar og eldre verifikasjon ligg i
`docs/superpowers/` og `docs/verification/`; dei erstattar ikkje dei faktiske testresultata.
