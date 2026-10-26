# Kortstorleikar, synlege titlar og rydda Heim

2026-10-26 · kodecommit `33e4410`

## Resultat

Standardkort er Kompakt (224 mot tidlegare 248 dp brei), med titlar under kunsten.
Seks storleikar finst både globalt og per rad: Ekstra liten (152), Liten (200), Kompakt
(224), Middels (248), Stor (296), Ekstra stor (344). Tidlegare lagra storleikar bevarer
mål og namn. Innstillingane for overskrift har framleis sine eigne tre storleikar.

Appar-rutenettet bruker faktisk vald kunstbreidd og jamne mellomrom. Watch Next sin
undertittel held seg innanfor kunstbreidda. Reservevisinga ved manglande kunst tilpassar
ikon og innvendig luft, slik at tekst får plass også med dobbel skrift og liten kunst.
Android-innstillingar ligg berre inne i Innstillingar. Halloween har inga nedteljing;
julenedteljinga følgjer av/på-valet både i toppfeltet og i tom hero.

## Automatisk kontroll

`pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'`

652/652 testar, 0 feil og 0 skip. Lint: 0 feil, 48 åtvaringar, 1 hint. Dei to ekstra
åtvaringane gjeld no ubrukte Halloween-nedteljingsressursar. Debug-bygg OK.

Nye kontraktar vart først prøvde raudt: standardmål/titlar, seks val, undertittelbreidd,
ekstra systemikon, Halloween-nedteljing, app-rutenett og tekst ved 0,6 kunstskalering.
Native prøver dekker lange namn, fontskala 2, ekstra små kort, skalering 1/0,7/0,6,
titlar på/av, global/radvis storleik og JSON-kompatibilitet. Eksisterande fokus-,
dialog-, profil-, éi/to-rad- og designregelprøver er grøne. Skjermprøvene er renderte
og visuelt vurderte; dei er ikkje pikselbaserte baseline-samanlikningar.

## Emulator og Shield

Same APK er installert med `-r` og SHA-256-verifisert på `emulator-5580` og
`192.168.10.182:5555`:

`c9888417c17b29b2911607a0d166af8bbb09deac49421f4d60b9ff9fec49ece3`

- Emulator: alle seks storleiksvala i den faktiske menyen; titlar på; Kompakt lagra.
  Synlege media-/apptitlar, rett breidd og mellomrom i Appar, Ned/Høgre, meny og
  Tilbake, radflytting/retur på Heim. Fokus held kortet og teksten innanfor skjermen.
- Shield: han hadde gått i dvale og vart vekt for prøvinga. Utsjånad → Kort og rader
  opnar rett side. Kompakt og titlar på er lagra. Spole/Emby har synlege namn under
  logoane; Ned frå fokusert Spole viser Hald fram med alle korttitlar, Opp returnerer
  til same apprad. Ingen ekstra Android-knapp eller Halloween-nedteljing på Heim.
- Begge står på Heim. Prøvene brukte ADB-tastar og trykk; ingen fysisk fjernkontroll
  vart brukt. Ingen avspeling eller styring av HA-einingar.

Evidens og kopi av APK ligg i `.superpowers/card-sizes/` (git-ignorert), mellom anna
`emulator-home-captions.png`, `emulator-apps-focus.png`, `shield-home-captions.png`,
`shield-continue-captions.png` og `shield-final-home.png`, med tilhøyrande UI-tre.
Native småkortbilete ligg i `app/build/screens/tiny-cards-double-text-*.png`.

## Databevaring

Begge profilane har berre dei bestilte endringane `cardSize: MEDIUM → COMPACT` og
`showTitles: false → true`. Shield har i tillegg serialisert dei allereie gjeldande
standardverdiane `homeRows: ONE` og `homeRowInset: LOW`, som mangla i den eldre fila.
Alle andre profilfelt, inkludert Shield si mindre apprad utan radoverskrift, er
uendra. Konto- og hushaldsfiler er byteidentiske før/etter på begge einingar.
Ingen tømming, avinstallering, endring av systemlauncher eller push/publisering.
