# Mindre kort, synlege titlar og rolegare toppfelt

Bestilling: litt mindre kort som standard, fleire storleiksval og synleg tekst på korta.
Android-innstillingar skal berre liggje i Innstillingar; ingen Halloween-nedteljing.

- [x] 1. Raude kontraktprøver: 224 dp standardbreidd, seks storleikar, synlege titlar,
  gamle serialiserte val, globale/radvise val, tekstgrenser og rydda toppfelt.
- [x] 2. Innfør seks storleikar med bevarte gamle mål, felles storleiksnamn og separate
  overskriftsnamn. Rett Watch Next-breidd og små appkort. Fjern systemikonet på Heim
  og Halloween-nedteljing; bevar Android-innstillingar i menyen og juleval.
- [x] 3. Køyr målretta prøver og full native skjerm-/unit-/lint-/byggkontroll.
- [ ] 4. Installer med -r på Stova-emulator og den autoriserte Shield-en. Slå på
  korttitlar og bruk kompakt standard for aktiv profil utan å endre radvise overstyringar.
  Kontroller faktisk tekst, storleiksval, fokus/retur og bevarte kontoar. Dokumenter og overlever.

Gamle SMALL/MEDIUM/LARGE bevarer namna og måla; ingen automatisk overskriving av
lagra preferansar. Seks val: Ekstra liten, Liten, Kompakt, Middels, Stor, Ekstra stor.
Den faste 248-referansen for plakat-/ikonproporsjonar blir bevart.

Verifisert før installasjon: 652/652 testar, ingen skip, lint 0 feil (48 åtvaringar,
1 hint), debug-bygg OK. Rutenettet i Appar brukte faste gamle mål; dette er òg retta
etter ei raud prøve. Native tekstprøve dekker dobbel skrift og kunstskalering ned til 0,6.
