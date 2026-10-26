# Bibliotek og aldersgrense for bakgrunn

2026-10-26 · kodecommit `7c52582` · plan `2026-10-26-background-library-age.md`

## Resultat

Utsjånad → Bakgrunn har to nye val: Bibliotek til bakgrunnen og Høgaste aldersgrense.
Bibliotekdialogen hentar dei verkelege videobiblioteka frå Jellyfin/Emby, grupperte
etter konto. Fleire bibliotek kan veljast. Alle bibliotek og ingen valde er ulike
tilstandar; eit tomt utval gir temabakgrunn. Avbryt endrar ingenting, Bruk utvalet
lagrar samla, og fokus går tilbake til kontrollen som opna dialogen.

Standard er maks 15 år, også for eksisterande profilar utan det nye feltet. Direkte
aldersval er 0, 6, 9, 12, 15, 16, 18 eller Utan grense. Ein treng ikkje gå gjennom
eit vidare utval for å velje lågare alder. Ukjend, manglande eller tvitydig
aldersmerking blir skjult når ei grense er aktiv. R, NC-17 og TV-MA blir konservativt
rekna som 18; PG som 10. Dette er Stova sine visingsgrenser, ikkje tenaren sine
interne foreldrekontrollpoeng.

Filteret er lagra per profil og gjeld bakgrunn og hero, inkludert sesonginnslag,
favorittar, faste bilete, tittelsøk, førehandsvising og fråkopla buffer. Bibliotek-
medlemskap kjem frå rekursive ParentId-førespurnader; eit ukjent eller utilgjengeleg
bibliotek opnar aldri for eit vidare utval. Overlappande bibliotek bevarer alle
verifiserte medlemskap. Kopiering av utsjånad bevarer mottakaren sitt filter.

Endra profil/filter fjernar gamal hero og dekoda bakgrunn straks, også under ei
pågåande kryssblanding eller om neste bilete ikkje kan lastast. Vanlege medierader
er uendra. Ingen bibliotektilgang eller aldersreglar på medietenaren er endra.

## Automatisk kontroll

`pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'`

689/689 testar, 0 feil, 0 skip. DesignRulesTest er grøn. Lint: 0 feil, 53 åtvaringar,
1 hint. Bygg OK. `controls-red.log` dokumenterer manglande kontrollar før
implementasjonen; `age-picker-red.log` viser at den gamle sykliske kontrollen ikkje
kunne velje lågare alder direkte. Endeleg fullkøyring er i `final-check.log`.

Regresjonar dekker ratingtolking, gamle profilfiler, konto-/profilgrenser,
ParentId/Recursive og servervariantar, sesong/favorittar, faste bilete/søk, overlappande
bibliotek, kald fråkopla buffer, feil/tomt utval, profilkopi, forseinka ny spørring,
fjerning av gamle dekoda bilete, lagring/avbryt og D-pad-fokusretur. Den dekoda
biletprøva sender snapshot-varsling eksplisitt før virtuelle rammer; ho ventar ikkje
på at ei eksisterande utblanding skal bli ferdig.

Native skjermprøver er renderte og visuelt vurderte, ikkje pikselbaserte
baseline-samanlikningar. `app/build/screens/` inneheld `background-filter-controls.png`,
`background-libraries-1.0.png`, `background-libraries-2.0.png` og
`background-age-selection.png`. Dialogen får faktisk dobbel skrift i 2.0-prøva;
innhaldet rullar medan avbryt/lagre blir verande tilgjengelege.

## Einingar og databevaring

Same APK er installert med `-r` og SHA-256-verifisert på emulator-5580 og Shield:

`1214aaba87321574466b8aff1bb09916aafc2b55b6387cd4acba36eefea38b8a`

Begge viser dei verkelege biblioteka Jul, Filmar, Seriar og Barnefilmar under
rett konto. På emulatoren vart berre Filmar valt og aldersgrensa sett direkte til
6 år. Heim viste The End of the Storm med 6-årsmerking. Den lagra avgrensa poolen
hadde sju titlar, alle med same verifiserte bibliotek-ID: tre med 6, to med G,
éin med A og éin med NO-A. Ingen valde bibliotek gav rein temabakgrunn og sesonghelsing.

På Shield vart aldersdialogen opna med D-pad; 15 år fekk fokus, og Tilbake gav
fokus til Høgaste aldersgrense. Sluttbiletet viste Affeksjonsverdi, merkt 9 år.
Emulatoren er sett tilbake til alle bibliotek og maks 15 år. Shield nyttar same
standard utan å endre den eksisterande profilfila. Begge står på Heim.

Alle eldre profilfelt er identiske før/etter på begge einingar. Emulatoren har berre
serialisert det nye filterfeltet med maks 15; Shield-profilen er byteidentisk.
Konto- og hushaldsfiler er byteidentiske på begge. Ingen mediaavspeling, HA-styring,
datatømming, launcher-/tilgjengeendring eller push er utført.

Evidens i `.superpowers/background-filter/`: `verification-summary.txt`, før/etter-
data, `emulator-selected-library.png`, `emulator-filtered-home.png`,
`emulator-empty-library-home.png`, `emulator-final-controls.png`, bibliotekdialogar
frå begge einingar, `shield-age-dpad.png`, `shield-age-dpad-return.png`,
sluttbilete frå Heim og `stova-background-filter.apk`. Private data og bilete er
git-ignorerte. Fysisk Heim-knapp-oppfølging frå førre oppgåve er framleis ikkje
stadfesta; denne endringa rører ikkje Heim-tenesta.

## Kjeldegrunnlag for API og ratingtolking

- Jellyfin sine [bibliotekvisingar](https://raw.githubusercontent.com/jellyfin/jellyfin/master/Jellyfin.Api/Controllers/UserViewsController.cs)
  og [Items-førespurnader](https://raw.githubusercontent.com/jellyfin/jellyfin/master/Jellyfin.Api/Controllers/ItemsController.cs)
  gir Views og rekursive ParentId-utval.
- [Emby si bibliotekdokumentasjon](https://dev.emby.media/doc/restapi/Browsing-the-Library.html)
  dokumenterer tilsvarande biblioteknavigering.
- Jellyfin sine [norske ratingar](https://raw.githubusercontent.com/jellyfin/jellyfin/master/Emby.Server.Implementations/Localization/Ratings/no.json)
  og [amerikanske ratingar](https://raw.githubusercontent.com/jellyfin/jellyfin/master/Emby.Server.Implementations/Localization/Ratings/us.json)
  vart kontrollerte mot dei faktiske ratingane i lokal buffer.
- [MPA si NC-17-forklaring](https://www.filmratings.com/rating/nc-17/) støttar
  konservativ 18-årstolking. Tenaren sitt MaxParentalRating er med vilje ikkje brukt
  som eit årstal.
