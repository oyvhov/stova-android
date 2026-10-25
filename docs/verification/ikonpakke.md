# Ikonpakkar – verifisering 25.10.2026

Bestillinga `docs/briefs/2026-10-25-ikonpakke.md` er fullført i
`C:/Launcher/.worktrees/appearance`, grein `appearance-v2`.

## Gjennomført

Installert Candybar-/Blueprint-kunst blir funnen via dei fem temahandlingane med eksplisitt
Android 11-pakkesynlegheit. Ressurslesaren prøver kompilert `res/xml/appfilter.xml`, deretter
`assets/appfilter.xml`. Indeksen toler skadde item-linjer, normaliserer korte aktivitetsnamn og
supplerer appfilter-treff med den frie ikonlista i `drawable.xml`. Diskindeksen er nøkkla på
pakkenamn, versionCode og oppdateringstid. Pakkeendringar gir ny oppdaging.

Utsjånad har profilstyrt Ikonpakke, Original/Aksent og Sentrert/Fyll flata. Heim og Appar bruker
same kunstbuffer og same meny. «Endre ikon» → «Frå ikonpakke» opnar eit søkbart, D-pad-styrt
rutenett. Manuelle ikonval gjeld berre den aktuelle profilen, overlever omstart og kan
nullstillast. Appnøklar følgjer aktivitetsskifte og blir rydda når appar blir avinstallerte.
Barneprofilar får ingen meny. Ingen automatiske favorittar er innførte.

Spole har ei linjeutgåve i Arcticons-stil og ei flat fargeutgåve med original Spole-lime. Dei blir
brukte når pakken manglar Spole, for både `app.reelstack` og `app.reelstack.debug`. Eige Spole-banner
blir brukt utan pakke, med det flate merket som reserve. SVG og Android-vektor har identisk
bane; 48 × 48 px-lerret, 1 px kvit strek, avrunda endar/hjørne, ingen fyll. Heile streken passar
innanfor 31 × 39 px-malen. Sjå [ikonfilene](../icons/README.md).

Arcticons er den tilrådde pakken. Kjelda er lesen frå [Arcticons-Team/Arcticons](https://github.com/Arcticons-Team/Arcticons),
commit `2ff828c82e65f9792c750996bceac5e96a697eee`, med bidragsrettleiing frå
[Creating icons](https://docs.arcticons.com/contribute/creating-icons). To uendra appfilter-linjer
ligg som testfikstur; ingen Arcticons-ikon eller APK er pakka inn i produksjonsappen.
Spole sitt `spole_launcher_foreground.xml`, `tv_banner.xml` og `SpoleIcons.kt` er lesekjelder.
Ingen referansekjelder er endra, og ikonforslaget er ikkje sendt inn.

## Raud/grøn

- Parser/indeks: først manglande API, så feila manifest-synlegheit; deretter 6/6.
  Dekker ekte Arcticons-utdrag, korte aktivitetar, skadde linjer/kommentarar, alle fem handlingane,
  duplikat, versjonsbyte, omstart med diskindeks, korrupt diskfil og frie ikon frå drawable-lista.
- Kunst/persistens: først manglande API, deretter pakkeprioritet, TV-tettleik, bufferidentitet,
  Spole for release/debug, manglande treff/drawable/pakke, manuelle overstyringar og profilisolasjon.
- UI: først manglande innstillingar/veljar. D-pad-testen avdekte at pakkeval bytte fokus til Farge;
  stabile ressursoppslag bevarer no fokus gjennom høgre/venstre/OK. Meny-tasten på Heim,
  lang OK-flyten i Appar, søk, val, profilpersistens, tilbakestilling og tom pakkeliste er prøvde.
- Sluttreview: ny raud/grøn-test for monokrome pakkar med andre pakkenamn. Klassifisering skjer
  ved IO-dekoding, ein gong per bufra ikon. Spole-stilen bruker høgst tre prøveikon frå ukjende
  pakkar; eit fargeikon gir flat reserve. Arcticons blir gjenkjend direkte.
- SVG: native banetest feila først på 39,25 px med strek. Endeleg merke er 26,824 × 38 px,
  altså 27,824 × 39 px med streken. Testen kontrollerer òg identiske SVG-/VectorDrawable-banar.

## Sluttkontroll

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'
```

**365/365 bestått**, 0 feil, 0 hoppa over. Av dette AppScreensTest **31/31**,
DesignRulesTest **4/4**. Lint: **0 feil, 23 åtvaringar og 1 hint**, same tal som før bestillinga.
`assembleDebug` OK. Siste samla køyring: `BUILD SUCCESSFUL in 1m 8s`.
Logg lokalt: `.superpowers/ikonpakke-final.log`. Debug-APK:
`app/build/outputs/apk/debug/app-debug.apk`.

## Skjermbilete

Alle sju PNG-filene er opna og visuelt kontrollerte etter siste samla køyring. Kunst, fokus,
metadata og felt er lesbare; sentrering, klipping og temaaksent svarar til vala.
Native Robolectric, fast kalenderdato 25.10.2026 og lokale geometriske mediefiksturar.
Den falske linjeikonpakken bruker det same innskotne kjeldegrensesnittet som produksjonspakkar;
ingen APK, nettverkskunst eller installasjon er nødvendig. Pikselkontroll inne i Spole-kortet
stadfestar at ikonet faktisk er lasta og farga med effektiv temaaksent.

- [Heim, vanleg Skog + Aksent](../images/ikonpakke-vanleg.png).
- [Heim, Halloween + Aksent](../images/ikonpakke-halloween.png).
- [Heim, kvadratisk Ikon-appstil](../images/ikonpakke-ikonstil.png).
- [Heim, Original + Fyll flata](../images/ikonpakke-fyll.png).
- [Spole-banner utan ikonpakke](../images/ikonpakke-spole-banner.png).
- [Ikonpakkeinnstillingar](../images/ikonpakke-innstillingar.png).
- [Søkbar ikonveljar](../images/ikonpakke-veljar.png).

Dei seks første er 1920 × 1080 px. Veljaren er 1920 × 972 px og er teken frå den verkelege
Compose-dialogvindaugsrota; eit opptak av berre aktivitetsvindauget ville ha vist ei tom flate.
Kjeldefilene ligg med same namn under `app/build/screens/`.

## Avgrensingar

Ingen APK-ar er lasta ned. Ingen emulator, Shield, installasjon, push eller publisering er brukt.
Testane bruker ei innskoten pakkekjelde; ekte installerte Candybar-/Blueprint-pakkar, fysisk
fjernkontroll og Shield-ytelse står att til brukaren si installasjonsøkt. Disk-/bitmaparbeidet og
monokromklassifiseringa køyrer på IO, medan søk køyrer på Default; dette er ikkje ei målt
Shield-garanti. Bitmapbufferen har høgst 64 oppføringar, og ikonrutenettet lastar berre synlege
celler. Spesifikasjonen §5.3 og §5.4 og overleveringa er oppdaterte.
