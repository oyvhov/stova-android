# Tema v1 – verifisering 25.10.2026

Bestillinga `docs/briefs/2026-10-25-tema-v1.md` er gjennomført i
`C:/Launcher/.worktrees/appearance`, på `appearance-v2`. Spole er berre lesen.
Ingen filer under `app/src/main/java/app/stova/ha` er endra. Ingen emulator, installasjon eller push.

## Commitar

- `231b2e7` – stemningar, ti aksentar og kalenderoverlegg som ikkje blir lagra.
- `4f860bd` – sesongrader, snapshot og prioritert spotlight.
- `bd25331` – statiske sesongteikn, partiklar og tomgangsedderkopp.
- `2a0a747` – temagruppe, fargeprøver og førehandsvising.
- `decca82` – native skjermtestar, brukaraktivitet, fleirtalsstrengar og sesongbyte midt i rotasjon.
- Dokumentasjonscommitten med denne rapporten inneheld skjermbileta, spesifikasjonen og overleveringa.

## Raud/grøn

- Del 1: Først kompilering som feila på manglande temamodell. Deretter 13/13 på kalender,
  persistens, palett, eksisterande JSON-standardar og designreglar. Grensene 19/20.10, 1/2.11,
  30.11/1.12 og 13/14.1 er prøvde. Dato er eksplisitt i kalendertestane.
- Del 2: Først manglande sesongendepunkt/snapshot-integrasjon, deretter 39/39. MockWebServer
  kontrollerer faktiske førespurnader, tag OR sjanger, Movie/Series, Recursive, Jellyfin-404-fallback
  og Emby-sti. Kald snapshot, fråkopla sesongrad, sortering/skjuling og spotlight-utval er prøvde.
- Del 3: Først manglande pynt/klokke, deretter 14/14 med hero/toppfelt/designreglar. Ein kontrollert
  Compose-klokke stadfestar at rørsla stoppar ved redusert rørsle og lifecycle CREATED, og tek opp
  att utan å rekne med den skjulte tida. Statisk helsing/nedteljing står att i Roleg, og Av skjuler
  begge. Pyntnivå, fast frø og edderkoppgrensene 45/50 s har eigne kontrollar.
- Del 4: Først manglande Tema-valrad, deretter 12/12. Alle seks tema, ti aksentar, fire sesongval
  og tre pyntnivå kan veljast med D-pad; venstre og OK, eksisterande utsjånadsval og barnesperre
  er med. Profilrepository-testen lagrar eit anna val under Halloween og les Midnatt/Hav frå disk.
- Del 5: Dei nye sesongskjermtestane feila først fordi sesongrada mangla i skjermfiksturen.
  Fiksturen viser no Halloween etter kalenderen på fast dato 25.10.2026 og fast Jul, med Mykje og
  kontrollert animasjonsklokke. Dei eksisterande aksentkontrollane er eksplisitt sesongfrie.
  Ein ekstra raud/grøn-test avslørte og retta bevart spotlight-indeks ved sesongbyte: den første
  sesongtittelen kjem først sjølv når førre utval var midt i rotasjonen.

## Sluttkontroll

`pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'`

Resultat: **340/340 bestått, 0 feil og 0 hoppa over**, av dette AppScreensTest **25/25** og
DesignRulesTest **4/4**. Lint: **0 feil**, 23 åtvaringar og 1 hint (som før bestillinga).
Debug-APK bygd: `app/build/outputs/apk/debug/app-debug.apk`.
Siste samla køyring: `BUILD SUCCESSFUL in 54s`; logg lokalt i `.superpowers/tema-final.log`.

## Skjermbilete

Alle kjem frå den native Robolectric-harnessen (1920 × 1080), med lokale geometriske kunstfiksturar,
utan nettverk eller emulator. `AppScreensTest.renderMediaHome` viser sesongrada først i sesong-
fiksturane, slik at rada, fokusringen, toppmerket og partiklane kan vurderast i same bilete.
Bileta er opna og vurderte visuelt; metadata, radoverskrift og toppfelt er lesbare, og partiklane
ligg over kunsten til høgre. Sesongpynten blir teikna bak innhaldet, så han ikkje dekkjer korta.

- [Halloween, Mykje](../images/tema-halloween.png): kalenderdato 25.10, «6 dagar til Halloween»,
  gresskar, glør, sesongrad og Gresskar-fokus.
- [Jul, Mykje](../images/tema-jul.png): fast Jul, snøfnugg, snø, sesongrad og Kristtorn-fokus.
- [Midnatt + Hav](../images/tema-midnatt-hav.png).
- [Kino + Perle](../images/tema-kino-perle.png).
- [Skog utan sesong](../images/tema-skog.png).
- [Temagruppa i innstillingane](../images/tema-innstillingar.png): alle fargeprøvene og heile
  førehandsvisingskortet passar i den synlege flata.

Kjelder: same filnamn i `app/build/screens/`.

## Kostnad for ein launcher som står på heile dagen

Dette er eit kjeldebasert overslag, ikkje ei Shield-måling. Eitt fastfrøutval blir laga per sesong:
12 glør eller 18 snøfnugg. Éin Canvas teiknar berre korte linjer; maksimalt 54 linjer per oppdatering
for snø (3 armar per fnugg). Halloween har 12 glør og høgst 18 teikneoperasjonar ekstra for
edderkoppen. Ingen blur, biletdedekoding, nettverkskall eller nye lister i partikkel-løkka.
50 ms venting og neste vsync gir om lag 15–20 oppdateringar/s, avgrensa til høgst 20; ≤ 1080 enkle
linjeteikningar/s for snø. Klokkestaten blir berre lesen inne i Canvas, slik at hero, rader og
biletelastar ikkje blir rekomponerte for kvar oppdatering. Dette er eit lite avgrensa tillegg til
16,7 ms-budsjettet; faktisk CPU-/GPU-rammetid på Shield er ikkje målt. Partiklar og edderkopp blir
heilt fjerna når Heim ikkje er komponert, når lifecycle går under STARTED eller ved redusert
rørsle; Roleg/Av har ingen animasjonsklokke. Første brukaraktivitet skjuler edderkoppen og startar
45-sekundsteljaren på nytt. Ingen pyntenode gir skjermlesartekst eller eit ekstra D-pad-stopp.

## Avgrensingar

Ekte Jellyfin/Emby, fysisk fjernkontroll og Shield er ikkje køyrde; det er i tråd med forbodet mot
installasjon. Endepunkta er prøvde med MockWebServer og snapshot med diskbasert kaldstart.
Skog og dei åtte vanlege aksentane beheld Stova sine dempa verdiar; dei andre tema og
Kristtorn/Gresskar kjem frå Spole. Dette avklarer bestillinga sitt krav om dagens Skog som standard.
Dei ucommitta bestillingsfilene frå brukaren er urørte.
