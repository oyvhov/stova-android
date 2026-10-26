# Smarthus, innstillingsoversikt og Heim på Shield

Bestilling: rett tilkoplingsstatus, ryddigare innstillingar som ikkje opnar Profil,
samla Smarthus-rutenett som standard, synlege storleiksval og dynamisk pakking.
Android-innstillingar skal heite Android. Projectivy skal kunne prøvast med/utan
kortbakgrunn. Heim-låsen skal verke på Shield med eksisterande oppsett.

- [x] 1. Reproduser feil med målretta testar og les faktisk Shield-status.
- [x] 2. Lag innstillingsoversikt, rett HA-livssyklus/status og appikon-førehandsvising.
- [x] 3. Innfør profillagra gruppering/tettleik, dynamisk tett rutenett og visuell
  storleiksmeny. Del geometri mellom teikning, piler og flytting; bevar fokus.
- [x] 4. Rett Heim-reservevegen for installerte launcherar og kontroller status utan
  å slå av systemlauncheren eller andre tilgjengetenester.
- [x] 5. Full native/unit/lint/byggkontroll, visuell vurdering og verifisering på
  emulator-5580 og Shield med bevarte data. Prøv Projectivy på/av og HOME frå andre sider.
- [x] 6. Oppdater arkitektur/spesifikasjon, rapport, ledger og HANDOFF; lokale commitar.

Design: eksisterande S/M/L bevarer format. Eitt samla felt i lagra kortrekkjefølgje
er standard; rom kan veljast. Tettleik og tilgjengeleg breidd avgjer kolonnar, og
stor skrift får meir plass. Seinare småkort kan fylle ledige plassar i eit band.
Innstillingar opnar ei oversikt med klare ikonlenkjer; Profil er ei vanleg underside.
Berre hovudagenten skriv/byggjer/prøver einingar. Underagentar gjer lesande review.
Ingen HA-handlingar, avspeling, nullstilling, push eller endring av andre launcherar.

Avgjerd for steg 3: vanleg navigering følgjer dei faktiske kortrektangla. Flytting endrar
lagra rekkjefølgje med Tidlegare/Seinare og halde OK + Venstre/Høgre. Ei geometrisk
«flytt til høgre»-handling er tvitydig når småkort fyller hol ved sida av store kort.
Det er derfor rekkjefølgja brukaren redigerer, og rutenettet blir pakka på nytt.

Steg 4–5: første reserveveg vart prøvd fysisk og feila ved andre trykk. Sporing viste
stock-launcher → Projectivy innan 334 ms, der den globale sperra avviste Projectivy.
Sperra er erstatta med samanslåing per komponent/vindauge. Endeleg APK bestod 6/6
ADB HOME-trykk på Shield og retur frå Android. Ny fysisk fjernkontrollstadfesting er etterspurd;
ADB-prøva blir ikkje rekna som fysisk stadfesting. Sjå verifikasjonsrapporten.
