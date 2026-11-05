# Smartheim, MENY og popup-lukking på Shield

Bestilling: rett MENY på den ekte Shield-en, fjern den svarte flata under
popup-lukking og fullfør ein Apple-inspirert gjennomgang av Smartheim.
Behald Stova sitt tema, dei lagra korta og oppsettet. Ingen glas eller blur.

- [x] Reproduser native MENY og krymping av dialogvindauget i regresjonstestar.
- [x] Rett tastemottaket og hald vindaugsramma stabil gjennom utgangen.
- [ ] Forenkle Smartheim-hierarkiet og kontrollpanelet etter ekte skjermbilete.
- [ ] Køyr testar, designreglar, lint og TV-bygg; installer på Shield.
- [ ] Prøv MENY, panel, Oppsett og topp-popup; kontroller opptaksrammer og fokusretur utan å endre lys, varme eller lyd.

Utgangspunkt: `.superpowers/shield-menu-popup-fix-2026-11-05/` har ekte
førbilete og opptak. På Shield har dialogen alt ingen native vindaugsanimasjon
eller dim. Han har `wrap`-høgd og native skugge; det animerte innhaldet blir
fjerna før vindauget blir demontert. Dette skal prøvast i test før retting.
