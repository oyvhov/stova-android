# Smartheim, MENY og popup-lukking på Shield

Bestilling: rett MENY på den ekte Shield-en, fjern den svarte flata under
popup-lukking og fullfør ein Apple-inspirert gjennomgang av Smartheim.
Behald Stova sitt tema, dei lagra korta og oppsettet. Ingen glas eller blur.

- [x] Reproduser native MENY og krymping av dialogvindauget i regresjonstestar.
- [x] Rett tastemottaket og hald vindaugsramma stabil gjennom utgangen.
- [x] Forenkle Smartheim-hierarkiet og kontrollpanelet etter ekte skjermbilete.
- [x] Køyr testar, designreglar, lint og TV-bygg; installer på Shield.
- [ ] Prøv panel, Oppsett og topp-popup; kontroller opptaksrammer og fokusretur utan å endre lys, varme eller lyd.
- [ ] Verifiser og eventuelt rett den fysiske MENY-knappen når eigaren kan prøve han.

Utgangspunkt: `.superpowers/shield-menu-popup-fix-2026-11-05/` har ekte
førbilete og opptak. På Shield har dialogen alt ingen native vindaugsanimasjon
eller dim. Han har `wrap`-høgd og native skugge; det animerte innhaldet blir
fjerna før vindauget blir demontert. Dette skal prøvast i test før retting.

Eigaren bad òg om eit gjennomført, penare Smarthus medan arbeidet gjekk.
Toppfeltet gjentek ikkje temperatur og straumpris. Kontrollpanel har same
breidd som Oppsett, midtstilt hovudverdi, eit lite ikon og ein tynn slider med
tydeleg gripepunkt. Tilstandsverdiar, lysfyll og nivå glir mellom endringane.
Utilgjengelege kort held lesbar fokusring. Topp-popupen held òg sine native
vindaugsgrenser gjennom heile utgangen; eigen regresjonstest er raud/grøn.
Drift, førehandsval og mediekjelder bruker same stille valrader med hake og
tilgjengeleg valt/ikkje valt-semantikk. Lesemodus lèt ingen val sende handlingar.

Full testkøyring: 1115 køyrde testar bestod, 86 skjermtestar vart hoppa over
utan skjermtest-flagget. Etter siste valrad-endring: 327 målretta testar
bestod, 8 skjermtestar vart hoppa over. `DesignRulesTest`, `ThemePaletteTest`,
`lintDebug` og `assembleTv` grøne. Slutt-APK-en er installert med `install -r`
på Shield. Siste kosmetiske retting fjernar lik tittel og undertittel i
kortmenyen; 31 meny-/design-/palett-testar bestod etterpå. Slutt-APK SHA-256:
`172c41e3e75750c7e2e486e6bc4fdea2e32f79fd36511ddc77359232be773bba`.

MENY: Native logging på Shield viser at Android ikkje leverer KEYCODE_MENU
til aktiviteten. Prøva utan kvar av dei to tastemapparane, og utan begge,
gav same resultat. Alle tilgjengetenestene er sette tilbake ordrett.
Eigaren kan ikkje prøve den fysiske knappen no. Stova si slepphandsaming
er testa, men den fysiske MENY-feilen må framleis reknast som uverifisert.
