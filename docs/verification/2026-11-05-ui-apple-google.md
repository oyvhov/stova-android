# UI-gjennomgang på Shield mot Apple og Google

2026-11-05, Codex. Lesande design- og navigasjonsgjennomgang av installert
Stova 0.2.0-tv, SHA-256
`1658400ca9c17758542eac17c86e6e9445c9a5b7eb33fb9e65875e7ba75b8cc0`.
Ingen appkode eller installering endra. Berre Shield, ingen emulator eller Loftet.

Rapporten er `.superpowers/ui-audit-2026-11-05/Stova-UI-rapport-Apple-Google.html`:
29 inspiserte skjermbilete, 12 flytar og tre innebygde opptak. Bilete og videoar
ligg òg som eigne filer i same mappe. `accepted-evidence.json` listar dei
aksepterte bileta med SHA-256. `Oversikt.png` gir ei kort visuell oversikt.

PDF-utgåve laga etter at eigaren ikkje fekk opna HTML-fila:
`output/pdf/Stova-UI-rapport.pdf` (28 sider, om lag 3,7 MB). Ho inneheld alle
29 skjermbileta og vurderingane, med stillbilete frå opptaka. Alle PDF-sidene
er renderte med Poppler og visuelt inspiserte; dei 12 flytane og
avgrensingane er òg kontrollerte med tekstuttrekk. Native filopning i Codex
vart kølagd, ikkje stadfesta som synleg. PDF-en blir levert som filvedlegg.

## Konklusjon og prioritet

Den rolege hovudforma, romlista, profilvalet og innstillingsstrukturen fungerer.
Bevar oppsettet, inkludert plasseringa av Sonos-favorittane. Rett samanheng,
lesbarheit og tilstandar før fleire visuelle effektar blir lagde til.

1. **P1 – svarte glimt ved Oppdag-sidebyte.** To ferske opptak av
   oversikt → The Mongoose → Tilbake viser heilt svarte mellomrammer.
   `ffmpeg blackdetect=d=0.01:pix_th=0.02` gav 0,515667–0,563789 s og
   3,101233–3,134833 s i første opptak; 0,717589–0,851256 s og
   3,386844–3,420789 s i repetisjonen. Svarte rammer og kontaktark er visuelt
   inspiserte. Årsaka er ikkje fastsett. Halde eksisterande kunst synleg
   gjennom bytet er ei tilråding, ikkje ei implementert retting.
2. **P1 – svak lesbarheit over lys kunst.** Omtalen i The Mongoose ligg
   over den lyse helikopterbakgrunnen. Medieradene på Heim har òg
   konkurrerande bakgrunnskunst. Sikre heile tekstområdet; ingen eksakt
   kontrastverdi eller WCAG-godkjenning er hevda.
3. **P1 – fråkopla uttrykk spriker.** Smarthus viser Fråkopla saman med
   sist kjende verdiar og levande kontrolluttrykk. Legg til kort blir tomt
   utan lokal forklaring eller Prøv igjen. Sonos går over til eiga feilmelding.
   Eitt nytt Prøv igjen-forsøk hjelpte ikkje. Det er ikkje undersøkt kva
   som ville skjedd om ein sende ein kontrollkommando utan samband.
4. **P2 – avkorta namn.** Heim, Sonos og søk treng ei roleg løysing for
   fullt namn på fokusert element, utan tittelramme eller flytting av rada.
5. **P2 – navigasjon og menyar.** Appmenyen er lang og raddelen blir
   avkorta i første vising. Samle sjeldnare oppsett. Vurder synleg Tilbake
   på vanleg Sonos-side opp mot Google sin TV-praksis; bevar Avbryt i
   stadfestingar.
6. **P2 – fokusrisiko som krev sitjeprøve.** Svakt løft og lys er ei god
   retning. Skiljet mellom valt og fokusert rom må prøvast frå sofaen.
   Det er ikkje grunnlag for å innføre tjukk global ramme.
7. **P3 – små inkonsistensar.** Sonos-namn inne i appkortet og under,
   og blanda omgrep som Heim og hero / Home-knappen.

## Dekning

| Steg | Flyt | Status |
|---|---|---|
| 1 | Heim, toppfelt, apprad og medierader | Bør strammast inn |
| 2 | Appar, snarvegar, Spole-profilar og installerte appar | God grunnstruktur |
| 3 | Langt OK på app → appmeny → retur | Bør kortast ned |
| 4 | Oppdag → titteldetaljar → retur, to opptak | P1-funn |
| 5 | Filmførespurnad → Avbryt | God; ingen førespurnad send |
| 6 | Søk etter Dune → lukka tastatur → Ned til første resultat | God med avkortingsrisiko |
| 7 | Sonos-normalvising og seinare feilvising | Delvis blokkert av HA |
| 8 | Smarthus Alle rom → Kjøkken | Tilstandshandtering bør rettast |
| 9 | Varmepumpepanel → Tilbake til Kjøkken | God i denne prøva |
| 10 | Smarthus-oppsett og Legg til kort | Blanda, svak tomtilstand |
| 11 | Innstillingsoversikt, fokus, profil, system, kontoar og namneskjema | Stort sett god |
| 12 | Profilval → same Øyvind-profil | God |

Opptaket `Smarthus-popup-lukking.mp4` viste ingen flygande svart stripe
i den kontrollerte lukkinga. Oppdag-opptaka er `Oppdag-sidebyte.mp4` og
`Oppdag-sidebyte-repetisjon.mp4`. Dei har variabel bilettakt og er ikkje
ei generell bildefrekvensmåling.

## Avgrensing og sluttilstand

HA var fråkopla under hovuddelen av gjennomgangen. Sonos-fullskjerm,
bibliotek, widget og aktivt låtbyte kunne ikkje prøvast på nytt. Barnevising,
PIN, større skrift, redusert rørsle, TalkBack, nulltreff og fysisk MENY er
ikkje verifiserte. Ingen kontrollkommandoar mot lyd, lys eller varme; ingen
førespurnader sende, profilendringar lagra eller kontoar endra. Shield står
att på Heim i same Øyvind-profil. Namneskjema og førespurnad vart avbrotne.

HTML-struktur, 60 interne lenkjer, 29 innebygde PNG-bilete og tre MP4-filer
er validerte. `Oversikt.png` og alle aksepterte kjeldebilete er inspiserte.
Automatisk HTML-førehandsvising er **ikkje** visuelt verifisert: køyring av
lokal server vart avvist av automatisk godkjenningskontroll, og nettlesaren
avviste `file://` fordi berre HTTP/HTTPS er tillate. Ingen omgåing prøvd.
Rapportfila kan opnast manuelt. Ingen appendringar som krev ny Gradle-køyring.

## Offisielle kjelder, lesne i denne økta

Google er plattformgrunnlaget for Shield; Apple er samanlikning for tvOS-kvalitet.
Begge opnar for fokus med skala og lys. Ingen av dei krev ei tjukk ramme.

- [Apple: Designing for tvOS](https://developer.apple.com/design/human-interface-guidelines/designing-for-tvos)
- [Apple: Focus and selection](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection)
- [Apple: Motion](https://developer.apple.com/design/human-interface-guidelines/motion)
- [Google: Design for TV](https://developer.android.com/design/ui/tv/guides/foundations/design-for-tv)
- [Google: Focus system](https://developer.android.com/design/ui/tv/guides/styles/focus-system)
- [Google: Layouts](https://developer.android.com/design/ui/tv/guides/styles/layouts)
- [Google: Typography](https://developer.android.com/design/ui/tv/guides/styles/typography)
- [Google: TV navigation](https://developer.android.com/training/tv/get-started/navigation)
