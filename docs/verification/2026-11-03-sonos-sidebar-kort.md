# Sonos: romliste som sidemeny og Sonos-kort — 3. november 2026

Plan: [`2026-11-03-sonos-sidebar-kort.md`](../superpowers/plans/2026-11-03-sonos-sidebar-kort.md).

## Automatisk kontroll

Testane vart skrivne først og feila ved kompilering (manglande `ShortcutKey.Sonos`,
`FavoriteTile.Sonos`, `sonosHomeWanted`, `sonosCard`, `LocalSonosCard`, `roomsOpen`).

- `SonosCardTest`: favorittportvakt (vaksen, modul på + HA, lagra snarveg; aldri barn),
  Heim-økta går for kortet med widget/Heim-kunst av (aldri barn/utan HA/modul av), status og kunst
  (spelar/pause/lastar/fråkopla/TV/klar/ingen; TV og fråkopla utan gammal kunst), romval og native
  namn under MA, heile `ShortcutTile` opnar Sonos og langt trykk opnar menyen utan spelehandling,
  feila bilete gir reserveflate, TV-kort ber aldri om bilete, `rememberShortcutOpener` sender
  Sonos-favoritten til sida, og snarvegspanelet tilbyr «Legg til i favorittar».
- `SonosContentTest`: «Gøym rom» gøymer lista og gir spel/pause fokus; Venstre deretter treffer
  ingen fjerna komponent; «Rom · Stova» opnar att med fokus på rommet; romval-callback virkar;
  med lista gøymd står volum, Fleire val og Music Assistant innanfor skjermen; tom/fråkopla side
  har ingen knapp.

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'
```

**1122 testar, 0 feil, 0 hoppa over**; lint 0 feil; debug-APK bygd (ikkje installert, ikkje assembleTv).
`DesignRulesTest` og `ThemePaletteTest` grøne.

## Skjermkontroll

`docs/images/sonos-rooms-hidden.png`: lista gøymd, større omslag, alle daglege kontrollar på éin
skjerm. `docs/images/sonos-card.png`: kortet med musikk, på pause med feila bilete (høgtalar), TV-lyd
(høgtalar, «Stova · TV-lyd») og utan økt (roleg merke). Etter første bilete vart hjørnemerket fjerna
på kortet utan rom, sidan merket elles stod tre gonger. `sonos-playing/grouped/large.png` oppdaterte
med den nye knappen.

## Grenser

- Kortet fylgjer at favoritten er lagra, ikkje om favorittrada er gøymd i Heim-oppsettet; då går
  økta medan Heim er framme utan at kortet blir sett.
- På Appar-sida har kortet inga live-økt og viser ei roleg Sonos-flate.
- Kortet startar Heim-økta på nytt når favoritten blir lagt til; det er same eine abonnement.
- Einingstestane under er gjorde med eigaren sitt løyve. Barneprofil/stor skrift og feilvariantar er dekte av kontrollerte testar, ikkje alle på begge TV-ane.

## Tilbake i biblioteket (funn frå Loftet)

Funn: rot → Favorites → Tracks, og rot → Favorites → Playlists → spel, så Tilbake: sida hoppa til
Sonos-rota i staden for Favorites (`.superpowers/sonos-sidebar-card-2026-11-03/favorites-back-step.png`).

Årsak, frå primærkjelda `homeassistant/components/sonos/media_browser.py` (HA core, `dev`):
`favorites_payload` og `favorites_folder_payload` returnerer begge `media_content_id=""` og
`media_content_type="favorites"`. `SonosLibrary` bytte ut toppen av stabelen når svaret hadde
same id/type som toppen, så «Tracks»/«Playlists» erstatta «Favorites».

Retting: å opne ei mappe legg alltid eitt nivå på stabelen; berre ei omlasting av noden som alt
ligg øvst (same objekt) erstattar han. Ingen ID-heuristikk.

Testar først (`SonosLibraryNavigationTest`, HA-forma svar): Tilbake-tasten går Tracks → Favorites →
rot → ut; topp-knappen etter spel frå Playlists går til Favorites; ei mislukka mappe som blir prøvd
att, kjem øvst utan å erstatte foreldra. Alle tre feila før rettinga (Favorites var borte etter
Tilbake) og er grøne etter. Målretta `app.stova.sonos.*`, `app.stova.ha.cards.*`, `DesignRulesTest`,
`ThemePaletteTest` med skjermtestar: **187 testar, 0 feil**. `lintDebug` 0 feil, `assembleTv` grønt
(`app/build/outputs/apk/tv/app-tv.apk`). Ikkje installert av Claude.

## Fokus medan ei Sonos-handling ventar (funn frå TV-ane)

Funn: romlista gøymd, fokus på Pause, OK → `busy` slo av knappen, fokus hoppa til «Rom · …», og
neste OK opna romlista i staden for å pause. Heim-widgeten mista fokus til lys-widgeten på same vis.

Årsak: avspelingsknappane, widgetknappen og vala i «Fleire val» hadde `enabled = !busy && …`, så den
fokuserte kontrollen vart avslått medan HA svara.

Retting: tilgjenge og støtte avgjer `enabled`; ein ventande handling gjer det ikkje. Trykk medan
`busy` blir ignorerte i kontrollen sjølv (`SonosContent` sin `guarded`, widgeten sin `onClick`), i
tillegg til vakta i `SonosScreen`. Bibliotek, «Grupper rom» og Music Assistant opnar berre
sider/dialogar og er ikkje lenger avslegne av `busy`. Volum var alt stabilt (glidaren held fokus og
sender ingenting medan han er avslegen), så han er uendra. Utilgjengeleg eller ikkje-støtta
avspeling er framleis avslegen.

Testar først (`SonosBusyFocusTest`): Pause med lista gøymd held fokus og ignorerer andre OK, så
virkar att når `busy` er over; «Førre spor» med lista open held fokus; volum held fokus og sender
ingenting; ikkje-støtta avspeling er avslegen; widgetknappen held fokus og ignorerer gjentak; widget
utan støtte er avslegen. Tre feila før rettinga (`Focused = 'true'`), alle seks er grøne etter.
Målretta `app.stova.sonos.*`, `app.stova.ha.cards.*`, `DesignRulesTest`, `ThemePaletteTest` med
skjermtestar: **193 testar, 0 feil**. `lintDebug` 0 feil, `assembleTv` grønt. Ikkje installert av Claude.

## Høgre frå Heim-widgeten hoppar til mediebrikka (Shield), ikkje reprodusert

Funn frå root: Høgre frå tittelen i «Spelar no» (bounds 206,55–688,153) landa på HA-mediebrikka
(918–1100) og hoppa over spel/pause (679–775) og lysbrikka (787–904). Alle var fokuserbare.

Forsøk (`SonosTopBarFocusTest`, heile `TopBar` med `SonosNowPlaying` og `HomeStatusChips` som på
Heim, med nedtrekksbrikker, D-pad-tastar): vanleg rørsle (lyft og 1,02-zoom som på Shield),
Heim si `focusRestorer().focusGroup()`-innpakning, brikker til venstre og Halloween. Høgre går
tittel → spel/pause → lys → media, og Venstre same veg attende, i alle variantane. Feilen er
**ikkje reprodusert**, så ingen produksjonskode er endra. Testane står att som regresjonsdekning.

Merk: i testen sluttar tittelen der spel/pause byrjar (870 px), medan Shield-dumpen viser 9 px
overlapp (688 mot 679). Neste spor: resultatet av Venstre frå mediebrikka på eininga, og om ein
Send-TvRemote-Høgre kan gi tastrepetisjon (tre steg ville gi akkurat dette).

### Oppfølging frå root (Shield)

Høgre/Venstre/spel i toppfeltet virkar på Shield når ein ikkje tek uiautomator-dumpar mellom
tastane. Root sin slutning (ikkje bevist): dumpane forstyrra fokus i toppfeltet. Widgeten held
fokus ved spel og pause (`shield-widget-playing-final-small`, `paused-final-small`). HA på Loftet
kom att gjennom vanleg livssyklus (systeminnstillingar og så HEIM); ingen innloggingsendring.

## Namn i «Kople musikkspelarar» (Shield)

Funn: dialogen viste rå ID-ar (`media_player.kjokken1`, `media_player.sonos_lydplanke`,
`media_player.lydplanke_2`) fordi HA-registeret ikkje hadde namn, og `name()` berre brukte
registernamn/originalnamn (`shield-ma-links-small.png`, `shield-ma-choices-small.png`).

Retting: `musicAssistantNames` vel namnet personen gav i HA, så `friendly_name` frå tilstanden,
så originalnamnet, så ID-en. Like namn får Sonos-modellen frå einingsregisteret, elles ID-en, så to
spelarar aldri ser like ut. Dialogen abonnerer på tilstanden til berre Sonos- og MA-spelarane
(`observeEntities`), berre medan han er RESUMED; ingen tenestekall, ingen polling av alle einingar.
Koplingane er framleis eksplisitte val; lagra koplingar blir ikkje endra og blir viste med namn.
Layouten er uendra.

Testar først (`MusicAssistantTest`): venlege namn utan registernamn, registernamn vinn, originalnamn
og ID som siste utveg; like namn skilde med modell, så ID; ei lagra kopling blir vist med namn når
tilstanden kjem, og med ID før. Feila ved kompilering før rettinga. Målretta `app.stova.sonos.*`,
`app.stova.ha.cards.*`, `HaCatalogTest`, `DesignRulesTest`, `ThemePaletteTest` med skjermtestar:
**206 testar, 0 feil**. `lintDebug` 0 feil, `assembleTv` grønt. Ikkje installert av Claude.

## Ekte TV-ar: installasjon og kontroll (Codex)

Eigaren utvida løyvet til installasjon på Shield og Sonos-handlingar medan han var borte, med volum 0.
Siste produksjonskode `ffecfd0` er installert med `Install-Stova.ps1 -Tv` / `install -r` på Loftet
(192.168.10.125) og NVIDIA Shield Android TV (192.168.10.182). Profilar, kontoar og eksisterande
favorittar er bevarte. Ingen avinstallering, datatømming, force-stop, launcher-deaktivering eller push.

- Sonos aktivert på Shield; same modul på Loftet. Sonos-favoritt lagt til sist før «Alle appar» på
  begge. Ekte omslag fyller kortet, med låt/rom/status; OK opnar sida. På Loftet er langt OK,
  flytt eitt steg til venstre og tilbake prøvd, med opphavleg rekkjefølgje elles bevart.
- Romlista vist/gøymd, større omslag, fokus og daglege kontrollar utan rulling: blått tema på
  Shield og Halloween på Loftet. Valet overlever biblioteket. Lydplanke er valt på begge.
- Sonos-favoritten «2023» vart starta gjennom biblioteket: «Can't Tame Her», Zara Larsson.
  Ekte spel/pause, førre spor, kjeldeval og live metadata/omslag prøvde. Spel/pause held fokus
  på sida og i Heim-widgeten etter `aefca5e`. Maskinvare-Tilbake frå Playlists går til Favorites.
- Volum 0 stadfesta på Kjøkken, Storestova og Lydplanke. Lydplanka hugsa først 10 % på TV-kjelda;
  dette vart sett til 0 før lydtesten. Ingen test har auka volumet.
- Gruppering: Lydplanke lagt til den eksisterande Kjøkken/Storestova-gruppa, tre medlemmer
  observerte, så Lydplanke fjerna att. Kjøkken og Storestova er framleis grupperte som før.
- Heim-kunst slått på mellombels: musikk tok over bakgrunn og hero. Bytte til TV gav vanleg
  filmbakgrunn, TV-kort utan gammalt omslag og Sonos-side utan albumkunst, også medan valet stod på.
- Platespelar er faktisk fråkopla og viser reservefigur/status. Ingen handling sendt til han.
- Tom krasjbuffer på begge; Stova Heim-tenesta både aktivert og bound. Loftet fekk éin HA-feil
  etter installasjonen. HA svarte HTTP 200 frå eininga; vanleg ut/inn av appen kopla til att.
  Ingen innlogging endra. Dette var ein mellombels observasjon, ikkje ei bevist nettverksretting.
- Under fokustesting med uiautomator hamna eitt OK i lys-popupen og slo Stova-lyset av. Det vart
  straks sett på att, med lagra 74 % lysstyrke. Loftet-lyset vart ikkje endra. Vanleg D-pad med
  berre skjermbilete mellom tastane fungerer; uiautomator som årsak er ei slutning, ikkje bevist.

Lokale skjermbilete: `.superpowers/sonos-sidebar-card-2026-11-03/`:
`shield-home-card-final-small.png`, `shield-focus-fixed-final-small.png`, `loftet-hidden-final.png`,
`shield-widget-playing-final-small.png`, `shield-widget-paused-final-small.png`,
`shield-home-art-playing-small.png`, `shield-tv-blocks-home-art-small.png` og `shield-tv-main-final-small.png`.
Desse er faktiske skjermbilete; nokre Shield-bilete er skalerte ned frå 3840 × 2160 for lesing.

Etter siste installasjon fekk også Shield ei tilkoplingsfeil utan transportfeil i `StovaHa`-loggen.
Android-innstillingane på Shield er translucent og stoppar ikkje Stova, så det var ikkje nok å
opne dei og gå tilbake. Full ut/inn via Spole si startside og HEIM kopla til att på begge einingane.
Ingen avspeling i Spole. Årsaka til den mellombelse appøkta etter installasjon er ikkje fastslått;
installasjons-/livssyklussporet står att dersom det kjem att ved normal bruk. Skjermbilete:
`shield-ha-restarted-small.png`, `loftet-ha-final.png`. Øyvind er aktiv profil igjen på begge.

### Music Assistant og sluttilstand

På Shield er Music Assistant aktivert, og native Sonos Lydplanke er uttrykkeleg kopla til
MA-spelaren «Lydplanke» (`media_player.lydplanke_2`). Dei andre romma er ikkje automatisk kopla.
Dialogen viser no Sonos Kjøkken/Lydplanke/Platespelar/Storestova med rette namn. MA-biblioteket
opnar Artists/Albums/Tracks/Playlists m.m.; Playlists → «2023 UP» viser ekte låtar og omslag.
Spel → stadfest Spel starta «Can't Tame Her» med native romvolum 0; sida viste Spelar no,
Zara Larsson, framdrift og ekte omslag. Pause fungerte, med fokus halde på knappen. Romlista
gøymd etterpå: alle fire snarvegane, inkludert Music Assistant, innanfor same skjerm.
Sjå `shield-ma-names-working-small.png`, `shield-ma-linked-small.png`, `shield-ma-library-small.png`,
`shield-ma-playlists-small.png`, `shield-ma-playing-small.png`, `shield-sonos-complete-small.png`.

Slutt: Lydplanke er pausa ved 0, Kjøkken/Storestova er pausa og grupperte som før, Platespelar
fråkopla. Native kø og MA-kø på Lydplanke vart erstatta av testspelelister med eigaren sitt løyve.
Heim-kunst er slått av att etter testen; Sonos-sidekunst og «Spelar no» er på. På Loftet er MA
framleis av/ukopla; same installerte kode støttar oppsettet, men MA-avspeling vart prøvd frå Shield.
Ingen andre MA-rom, kølegging, tilfeldig/gjenta eller høyrbar lyd er testa. Ingen nye krasj,
og Heim-tenestene er bevarte. Testane verifiserer handling og status, ikkje lydkvalitet ved volum 0.

Siste kontroll etter MA-pause: HA gjekk seinare frå pause til «Klar» på Lydplanke og tømde
låttittel/omslag. Kortet viste då korrekt reserveflate; ingen musikk spelte. «På pause»-bileta er
frå sjølve testen. Loftet si Sonos-side er kontrollert att etter oppvakning: live rom, 0 %, rett
opphavleg gruppering, ingen tilkoplingsfeil (`loftet-last-sonos-check.png`).
