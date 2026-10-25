# Brukstest og klargjering for release

Bestilling: «Gjer det du kan» etter forslaget om ekte ikonpakke, heile Stova → Spole → tilbake,
Shield-kontroll og første signerte utgåve. Bevar ekte kontoar og innstillingar.

- [x] Installer og prøv offisiell Arcticons på emulator-5580: oppdaging, ikon, overstyring og reserveikon.
- [x] Prøv navigasjon, parallax, retur frå Spole og framdrift med ekte data; rett påviste feil med TDD.
- [x] Kontroller profiler/PIN og fråkopling med automatiske testar og reversible emulatorprøver.
- [x] Kontroller tilgjengeleg Shield-status; gjer praktiske målingar når eininga kan brukast.
- [x] Klargjer release-signering og sperre mot manglande signeringsoppsett; produksjonsnøkkel og passord skal eigaren styre.
- [x] Køyr sluttkontroll, dokumenter resultat og konkrete restpunkt, commit og push til det private repoet.

Shield var i kvile ved start; etter «Shield er på» vart òg fysisk eining oppdatert og prøvd.
Sjå [rapporten](../../verification/brukstest-og-release.md) for målingar og avgrensingar.
Ingen nullstilling eller deaktivering av systemlauncheren. Signert produksjonsrelease krev ein eksisterande nøkkel eller
førstegongsoppsett utført av eigaren; ikkje lag ein tilfeldig nøkkel for å få eit grønt bygg.
