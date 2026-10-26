# Eineståande launcher: finpuss, Heim etter tid, fart og huset

Bestilling (2026-10-26): «Gjer 2, 3, 4 og 6» frå lista «Kva som skal til for at Stova blir eineståande»:
2 Heim som kjenner deg, 3 raskt overalt, 4 huset og TV-en heng saman, 6 konsekvent finpuss
(funna frå designgjennomgangen, https://claude.ai/artifact/8J7YwXCGrQgZWr8nYR1HCC).

Brukaren sine val:
- Kino-modus: «Eg vel lys i oppsettet». Av som standard. Under Smarthus vel ein lys og nivå;
  lysa går tilbake til det dei var når Stova kjem fram att.
- Varsel: «Ja, eg vel einingar». Ein ringeklokke-/rørslesensor og eit kamera; eit lite kamerabilete
  i hjørnet i nokre sekund. Over Spole krev det Androids «Vis over andre appar» – det må brukaren slå
  på sjølv (systeminnstilling); utan det blir varselet berre vist når Stova er framme.
- Heim: «Rader etter tid på døgnet». På som standard, kan slåast av. Morgon/ettermiddag (05–17)
  løfter «Hald fram» og Watch Next; kveld (17–05) løfter nye filmar og seriar rett etter «Hald fram».
  Lagra rekkjefølgje blir ikkje endra; berre visinga.

Tillegg (seinare same kveld): «1, 2 og 4» frå high-end-lista (rørsle, fokus, skrift og farge frå
kunsten), og «Hero skal vere frå barnefilmer og barne-TV mellom 06:00 og 20:00 alle dagar».
Rekkjefølgje no: A Barnetid, så steg 1 (fokus), B rørsle, C skrift og farge, deretter resten.
- [ ] A. Barnetid: `AppearancePrefs.kidsHours` (av som standard; 06–20 når på). I vindauget brukar
  heroen same bibliotekval med maks 7 år (kjend aldersgrense påkravd). Heim byter ved timegrensa berre
  medan Heim er synleg. Val under Utsjånad → Bakgrunn.
- [ ] B. Rørsle: fjør utan sprett for fokus og rulling, korte, forskuvde inngangar av rader, sidebyte som
  veks frå ikonet, krysstoning via kunsten sin hovudfarge i staden for blekk.
- [ ] C. Open skrift med karakter til titlar/klokke (OFL, inkludert i appen) og ein svak tone frå
  kunsten i scrim, fokus og appfliser. Fargar framleis berre i `core/theme`.

Reglar som før: modulært (ingen arbeid når noko er av eller ikkje sett opp), test først,
`DesignRulesTest` grøn, nynorsk/engelsk tekst, Shield berre når brukaren seier ja.

- [ ] 1. (6) Synleg fokus (funn 1): kunst 6 % større ved fokus, resten av den aktive rada dempa til
  ~70 %, tittel under fokuskortet lys og dei andre dempa; kontrollflater 16 % lys. Ingen glød.
- [ ] 2. (6) Harde klipp nedst (funn 3): Innstillingar og Smarthus brukar `contentPadding` i
  staden for ytre botnmarg, med mjuk overgang.
- [ ] 3. (6) Småfunn: 7 hjelpetekst berre første gongene, 9 `Accent.container` frå tema og tydelegare
  brytar, 10 eitt namn per stad, 11 eige Smarthus-ikon, 12 dialogjustering, 13 NO-A → «Alle», skjul TBA,
  ingen duplikat i Watch Next, 14 statusprikken inn i ikonet.
- [ ] 4. (6) Større funn: 4 profilval med kunst og logo, 5 appfliser med farge frå ikonet og jamt
  rutenett, 8 ekte førehandsvisingar i Tema/Kort.
- [ ] 5. (2) Rader etter tid på døgnet, med val under Utsjånad → Heim og hero.
- [ ] 6. (3) Fart: mål kaldstart, første ramme på Heim og Smarthus med `scripts/Measure-Stova.ps1`;
  rett dei største; ny Baseline Profile.
- [ ] 7. (4) Kino-modus: oppsett under Innstillingar → Smarthus; dimming ved «Spel» til Spole,
  tilbakestilling når Stova kjem fram att. Ingenting skjer utan oppsett.
- [ ] 8. (4) Varsel: vel sensor og kamera; lite kamerabilete i hjørnet. Over andre appar berre med
  løyvet brukaren gir sjølv.
- [ ] 9. Full kontroll, emulatorbilete, overlevering. Shield berre med løyve.

Ingen push. Ingen endring i Home Assistant-oppsett utan at brukaren ber om det.
