# Samla UI-utbetringar etter Apple-/Google-gjennomgangen

Bestilt av eigaren 2026-11-05: rett funna i UI-rapporten og gjer det mogleg
å flytte fleire plassar på ei rad utan å opne langtrykkmenyen på nytt.
Bevar hovudoppsett, roleg løft/lys og plasseringa av Sonos-favorittane.
Ingen glas/blur eller endring av verkelege lys-, varme- eller lydverdiar.

- [x] 1. Oppdag held bakgrunnskunst utanfor inngangsanimasjonen for kontrollane. Regresjonstest les lys og geometri i mellomrammer ved opning/retur; eksisterande dekodingstestar vernar kunsten medan neste bilete blir henta. Endeleg skjermopptak ligg i steg 6.
- [x] 2. Lesbar omtale over lyse bilete og rolegare kunst bak aktive medierader. `ArtworkCaption` rullar lange namn roleg ved fokus i Heim, søk og Sonos. Inga tittelramme eller endra radhøgd; rørsla stoppar ved redusert rørsle og full tekst er bevart i semantikken. Endeleg skjermkontroll i steg 6.
- [x] 3. Felles fråkopla status med sist kjende verdiar og relevant Prøv igjen. Ingen kontrollsending utan samband, heller ikkje frå eit ope panel. Legg til viser forklaring/retry i staden for tom flate. Sonos har klar veg til ny tilkopling og direkte til HA-oppsett. HA-/Sonos-/navigasjonspakka: 464 bestått, 29 hoppa over; korrigert paneltest separat: 3 bestått.
- [ ] 4. Vedvarande flyttemodus for favorittar på Heim og i Appar: piler flyttar fleire plassar, OK lagrar og Tilbake avbryt. Same nøkkel held fokus. Kortare app-/snarvegmeny med sjeldnare val samla; radinnstillingar tek ikkje over heile menyen.
- [ ] 5. Samla finpuss: tydeleg skilje mellom valt rom og fokus, éin Sonos-tittel på appkortet, forståelege namn på appvariantar, konsekvent nynorsk, tittel i profilskjema og mindre fast panelhjelp. Vanleg Sonos-side bruker fjernkontrollen sin Tilbake; Avbryt/Lukk står i dialogar.
- [ ] 6. Relevante testar etter kvart steg, deretter full testpakke, design/palett, lint og TV-bygg. Installer og prøv på Shield med skjermbilete/opptak, utan emulator. Større skrift og redusert rørsle blir prøvde med kontrollerte UI-testdata. Dokumenter eventuelle eksterne hinder presist.

Kvar oppgåve får ein eigen commit med testresultat. Framdrift og avvik:
`.superpowers/sdd/2026-11-05-ui-utbetringar/progress.md`.
