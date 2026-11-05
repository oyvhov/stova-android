# Samla UI-utbetringar etter Apple-/Google-gjennomgangen

Bestilt av eigaren 2026-11-05: rett funna i UI-rapporten og gjer det mogleg
å flytte fleire plassar på ei rad utan å opne langtrykkmenyen på nytt.
Bevar hovudoppsett, roleg løft/lys og plasseringa av Sonos-favorittane.
Ingen glas/blur eller endring av verkelege lys-, varme- eller lydverdiar.

- [x] 1. `DiscoverStage` held både biletkomposisjonen og den dekoda kunsten utanfor stega sin livssyklus og kontrollanimasjon. Den første rettinga av berre alfa/geometri var utilstrekkeleg på Shield; ein ny test byter faktisk steg og krev same biletnode. Endeleg opptak av to opningar/returar har ingen svarte rammer ved same blackdetect-terskel som før.
- [x] 2. Lesbar omtale over lyse bilete og rolegare kunst bak aktive medierader. `ArtworkCaption` rullar lange namn roleg ved fokus i Heim, søk og Sonos. Inga tittelramme eller endra radhøgd; rørsla stoppar ved redusert rørsle og full tekst er bevart i semantikken. Endeleg skjermkontroll i steg 6.
- [x] 3. Felles fråkopla status med sist kjende verdiar og relevant Prøv igjen. Ingen kontrollsending utan samband, heller ikkje frå eit ope panel. Legg til viser forklaring/retry i staden for tom flate. Sonos har klar veg til ny tilkopling og direkte til HA-oppsett. HA-/Sonos-/navigasjonspakka: 464 bestått, 29 hoppa over; korrigert paneltest separat: 3 bestått.
- [x] 4. Vedvarande flyttemodus for favorittar på Heim og i Appar: piler flyttar fleire plassar, OK lagrar og Tilbake avbryt. Same nøkkel held fokus; profilar deler ikkje utkast. Kortare app-/snarvegmeny med sjeldnare val samla; radinnstillingar tek ikkje over heile menyen. Testa gjentekne piler, lagring, avbrot, grenser, nøkkeltap og bevaring av usynlege favorittar. 202 bestått, 1 hoppa over.
- [x] 5. Samla finpuss: eit nøytralt hakemerke skil valt rom frå fokus, éin Sonos-tittel på appkortet, namn på appvariantar når dei kolliderer, konsekvent nynorsk, tittel i profilskjema og mindre fast panelhjelp. Vanleg Sonos-side bruker fjernkontrollen sin Tilbake; Avbryt/Lukk står i dialogar. Dei tre nye regresjonane feila før retting og består no. HA/Sonos/profilar/innstillingar/appar testa; ein eldre tekstassert vart oppdatert og AppearanceSettingsTest køyrd på nytt med grønt resultat.
- [x] 6. Full testpakke med skjermtestar: 1 245 bestått, ingen hoppa over. Design/palett, lint, debug- og TV-bygg grøne. Installert på Shield og SHA-256 samanlikna med APK-en på eininga. Native bilete/opptak viser heil applogo, lesbare romnamn, flat Sonos-romliste, heile spelarfeltet, popup-lukking og gjentekne flytt med avbrot. Større skrift/redusert rørsle testa med kontrollerte data. Sjå `docs/verification/2026-11-05-ui-utbetringar.md` for dekning og avgrensingar.

- [x] 7. Romikon frå Home Assistant sitt områderegister i Smarthus, Sonos-romlista og romveljaren ved Legg til. Einingsoverstyring før apparatet sitt rom, ikon i eksisterande snapshot og synleg ikon også på valt rom. Sonos held høgtalarbiletet når eit rom manglar ikon og bevarer lysindikasjonen for musikk. Fire nye testar feila før implementeringa og består no. Ekte kjøkkenikon observert på Shield i begge romlistene; fleire HA-ikon i romveljaren. Ingen endring av HA-oppsettet.

Kvar oppgåve får ein eigen commit med testresultat. Framdrift og avvik:
`.superpowers/sdd/2026-11-05-ui-utbetringar/progress.md`.
