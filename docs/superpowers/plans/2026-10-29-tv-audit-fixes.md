# Rett funna frå TV-gjennomgangen

Bestilt 29.10.2026: M2, M1, A1, A2, S1, S2, K1, K2, K3, U1, P1 og Y2,
med særleg vekt på vakre smarthuspopup-ar som er enkle med TV-fjernkontroll.
Utgangspunkt: `docs/verification/2026-10-29-tv-audit.md` og skjermbileta frå begge TV-ar.
Ingen installasjon på ekte TV er bestilt. Loftet skal halde fram med å sove.

- [x] 1. M2/A1/A2: framdrift i mediemenyen, Alle appar frå favorittrada, kompakt appmeny med Opne først. Test etikett, fokus og callback før implementering.
- [x] 2. U1/P1/Y2: heil ikonprøve ved inngang, eiga mediekontoflyt og teknisk Heim-oppsett under Avansert. Test fokus/vising før implementering.
- [x] 3. S1/S2: rombasert start med gjennomgått utval før lagring, roleg sjølvstendig Smarthus-bakgrunn og namn på fokuserte brikker. Test romfilter, eksisterande kort og tomtilstand.
- [ ] 4. K1/K2/K3 og popupdesign: fast tittel/botn, stor korrekt hovudverdi, presise klimaetikettar, volum først og eigne kjeldeval, kompakt vêr/straum. Bevar lesemodus, stadfesting av lås/port, sending og fokusretur. Test med D-pad og kontrollerte data.
- [ ] 5. M1: retur frå eksternt opna filmdetaljar i Spole til launcheren, med vern av vanleg Spole-navigasjon og nested detaljar. Test i Spole-repoet etter reglane der.
- [ ] 6. Sluttkontroll: relevante regresjonstestar, DesignRulesTest/ThemePaletteTest, lint og bygg. Installer på Stova_TV emulator-5580 med bevarte data; sjå originale skjermbilete og prøv lange trykk. Oppdater arkitektur og overlevering.

Design: matte temaflater utan blur, stor lesbar verdi, namn og rom som fast orientering.
Opp/Ned flyttar mellom kontrollar, Venstre/Høgre justerer berre den fokuserte verdien,
OK aktiverer merkte handlingar og Tilbake går eitt nivå. Underpanel bevarer fokus på opnaren.
Ingen nye tenestekall når integrasjonen er av. Testdata skal ikkje styre verkelege einingar.
