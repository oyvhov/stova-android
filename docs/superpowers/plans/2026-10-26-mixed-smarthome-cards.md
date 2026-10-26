# Blanda kortstorleikar i Smarthus

Brukaren ønskjer ei fin vising med fleire kortstorleikar, ikkje berre M-kort.
På Shield gir L-vêr, M-pris/media og S-lys/klima eit pakka femkolonnerutenett,
men S-korta løftar radhøgda slik at botnen blir klipt. L-vêr brukar dessutan
same horisontale innhald som M og utnyttar den ekstra høgda dårleg.

- [x] 1. Legg til og køyr målretta native layoutprøver for heile det blanda bandet,
  detaljar i S-kort og vertikalt L-vêr. Dokumenter raudt resultat.
  Alle tre feila før retting: S-kort 177,5 dp mot 152; L-band slutt på 537 dp
  utanfor viewport 492 dp; prognosen låg ved sida av temperaturen. Evidens:
  `.superpowers/smart-home-view/mixed-red.xml`.
- [x] 2. Tilpass luft/ikon i S utan å redusere tekst eller fjerne rom/verdiar.
  Gi L-vêr temperatur/tilstand over fullbrei prognose. Bevar M og stor skrift.
  Køyr målretta prøver, DesignRulesTest og full sluttkontroll; commit kode/plan.
  709/709 native/unit-prøver, 0 skip; lint 0 feil (62 åtvaringar, 1 hint), bygg OK.
  Fire nye prøver dekkjer heile bandet, S-detaljar, L-prognose, dobbel skrift og pilnavigasjon.
- [x] 3. Installer med data bevarte på Stova-emulatoren og Shield, kontroller
  blanda rutenett/fjernkontroll og ta nytt ekte Shield-bilete. Oppdater overlevering.
  APK er hashverifisert på begge. Shield har L-vêr, M-pris/Sonos og S-lys/klima,
  alle fem synlege ved opning. Bilete: `.superpowers/smart-home-view/smarthus-mixed-shield.png`.

Berre root skriv eller køyrer bygg/einingar. Andre agentar kan gi lesande råd.
Ingen HA-styring, avspeling, sletting av profilar eller push.
