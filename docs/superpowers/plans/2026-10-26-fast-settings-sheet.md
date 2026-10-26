# Raskare menyar, fem innstillingsseksjonar og nytt langt-OK-panel

Bestilling (2026-10-26, kveld): «Navigering i menyen er treig når kjem inn. Lag alt raskare. Kan du slå
saman nokon av hovudkategoriane på settings? Lag det meir smooth og fint å sjå på. Redesign heile
longpress-menyane. Dei er heilt forferdeleg stygge og lite brukarvennlege.»
Brukaren valde: fem hovudseksjonar, og panel frå høgre for langt OK.

- [ ] 1. Fart: mål kva som gjer inngang til Innstillingar/menyar treg (sideovergang, `SoftEntrance`,
  fokusoverføring med `repeat(8) withFrameNanos`, dialoginngang). Kort ned til ≤ 150 ms inngang,
  fokus på første ramme, ingen ventande fokusløkker der ein FocusRequester kan be direkte.
  Mål før/etter på Shield (`scripts/Measure-Stova.ps1` eller `dumpsys gfxinfo`).
- [ ] 2. Fem seksjonar: «Heim og utsjånad» (Heim + Utsjånad), «Smarthus og kontoar» (Smarthus +
  Kontoar), «Profilar», «Appar», «System» (Heim-knappen, Om, Android). Oversikt-sida går ut; første
  seksjon opnar direkte. Ingen gruppeoverskrifter i menyen. Tilbake eitt steg (som i dag). Same
  undersider/innhald, berre samla under nye overskrifter. Gamle lagra `SettingsSection` må lesast.
- [ ] 3. Langt-OK-panel frå høgre for kort (media, appar, Watch Next): kunst/logo øvst, to store
  hovudhandlingar (Spel, Detaljar / Opne), resten som rolege rader med ikon; radval på eiga side i
  panelet. Glir inn frå høgre (≤ 200 ms, null ved redusert rørsle), Heim dempa bak. Erstattar
  dagens dialogar i `home/RowOptions.kt`, `apps/AppActionsMenu.kt`, media-meny og HA-kortmeny.
  Bevar: OK-trykket som opna menyen vel ingenting (StovaDialog-regelen), favoritt/flytt/skjul i
  app-scopet, alle eksisterande handlingar.
- [ ] 4. Full kontroll, emulatorbilete, Shield (brukaren har sagt ja til installering i kveld), overlevering.

Ingen push. Ingen glas/blur. Fargar berre i `core/theme`.
