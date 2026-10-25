# Ei roleg framside og eit samanhengande visuelt system

Bestilling: éi app-/medierad synleg nedst som standard, med valfri radvising;
vakrare innstillingar med Spole sitt ikonhierarki; same detaljar i Smarthus og resten av appen.
Eigaren har seinare tilbydd Shield for prøving; ferdig testa APK kan installerast med -r der.
Ingen push eller styring av verkelege HA-einingar. Profil-/konto-/hushaldshashar er tekne på begge einingar.

## Task 1: Radfelt nedst på Heim
- [x] Skriv/køyr raude testar for standard éi rad, hero under radbyte og botnplassering.
- [x] Legg til profillagra éi/to/rullande radvising, med bakoverkompatible standardar.
- [x] Implementer stabilt radfelt med eksisterande fokus-, meny- og rulleeigar.
- [x] Verifiser format, store skrifter, tomme rader, retur, flytting/skjuling og redusert rørsle; commit og ledger.

## Task 2: Visuelle innstillingar
- [x] Skriv/køyr testar for ikon gjennom navigasjonen og ny radvisingsinnstilling.
- [x] Lag felles streksymbol, overskrifter og kompakte kategoriar; fjern doble titlar.
- [x] Lag små konkrete førehandsvisingar og samstem tekst/verdi/handling i innstillingsradene.
- [x] Kontroller fokus, profilisolasjon og native skjermprøver; commit og ledger.

## Task 3: Smarthus og dei andre flatene
- [ ] Skriv/køyr relevante testar før endra kontraktar.
- [ ] Samordne overskrifter, ikon, handlingsfelt, gruppeetikettar og tomtilstandar i Smarthus/Appar/profil-/menyflater.
- [ ] Finpuss kort, avstandar og tekstplassering utan å endre styringskontraktar.
- [ ] Køyr tastetestane og sjå native skjermprøver; commit og ledger.

## Task 4: Endeleg emulatorrunde
- [ ] TestDebugUnitTest, lintDebug, assembleDebug med skjermprøver; kontroller XML og bilete.
- [ ] Installer med -r på emulator-5580, prøv alle radval og navigasjonsflytar; ta bilete/klipp.
- [ ] Kontroller bevarte profilar/kontoar, oppdater spesifikasjon/arkitektur/rapport/HANDOFF, commit.

Berre hovudagenten skriv i arbeidsmappa og køyrer bygg. To underagentar har lese kjeldekode og gitt design-/kanttilfellevurderingar utan å endre filer.
