# Smarthus v2 – finpuss

Bestilling: `docs/briefs/2026-10-25-smarthus-v2-finpuss.md`. Berre dette worktreet;
ingen push. Dei eldre release-/Shield-stega er utanfor denne bestillinga.

- [x] 1. Raud/grøn: temperaturord og lysglød etter lysstyrke/farge; eigne HaTokens.
- [ ] 2. Raud/grøn: prisnivå og timesnitt; felles graf med 15-minuttsval, akse og tider.
- [ ] 3. Raud/grøn: namn før verdi, kompakte S/M/L, klima/media, romtekst og fokus/rulling.
- [ ] 4. Ekte emulator-5580/HA: install -r, styring og stadfesta tilbakeføring, kritisk biletkontroll.
- [ ] 5. Grøn sluttkontroll, spesifikasjon §5.2, verifikasjonsrapport og HANDOFF.

Kvar del blir committa med planstatus; TDD-evidens i den lokale ledgeren.
Forbodne heim-/hero-/medie-/utsjånads-/temafiler blir ikkje endra, med unntak av den
uttrykkeleg tillatne nye `core/theme/HaTokens.kt`. Heim-brikker blir endra i `ha/ui`.
