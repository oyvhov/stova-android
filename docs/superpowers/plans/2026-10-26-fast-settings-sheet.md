# Raskare menyar, fem innstillingsseksjonar og nytt langt-OK-panel

Bestilling (2026-10-26, kveld): «Navigering i menyen er treig når kjem inn. Lag alt raskare. Kan du slå
saman nokon av hovudkategoriane på settings? Lag det meir smooth og fint å sjå på. Redesign heile
longpress-menyane. Dei er heilt forferdeleg stygge og lite brukarvennlege.»
Brukaren valde: fem hovudseksjonar, og panel frå høgre for langt OK.

- [x] 1. Fart (93c7dec: tokens 150/200/300, rader 380, fokus 120/180; kontrasttest held): mål kva som gjer inngang til Innstillingar/menyar treg (sideovergang, `SoftEntrance`,
  fokusoverføring med `repeat(8) withFrameNanos`, dialoginngang). Kort ned til ≤ 150 ms inngang,
  fokus på første ramme, ingen ventande fokusløkker der ein FocusRequester kan be direkte.
  Mål før/etter på Shield (`scripts/Measure-Stova.ps1` eller `dumpsys gfxinfo`).
- [x] 2. Fem seksjonar (Rader på Heim er ein kategori i «Heim og utsjånad»; Android er ei rad under System): «Heim og utsjånad» (Heim + Utsjånad), «Smarthus og kontoar» (Smarthus +
  Kontoar), «Profilar», «Appar», «System» (Heim-knappen, Om, Android). Oversikt-sida går ut; første
  seksjon opnar direkte. Ingen gruppeoverskrifter i menyen. Tilbake eitt steg (som i dag). Same
  undersider/innhald, berre samla under nye overskrifter. Gamle lagra `SettingsSection` må lesast.
- [x] 3. Langt-OK-panel frå høgre (`StovaSheet`; media, appar, rader/Watch Next og Smarthus-kort): kunst/logo øvst, to store
  hovudhandlingar (Spel, Detaljar / Opne), resten som rolege rader med ikon; radval på eiga side i
  panelet. Glir inn frå høgre (≤ 200 ms, null ved redusert rørsle), Heim dempa bak. Erstattar
  dagens dialogar i `home/RowOptions.kt`, `apps/AppActionsMenu.kt`, media-meny og HA-kortmeny.
  Bevar: OK-trykket som opna menyen vel ingenting (StovaDialog-regelen), favoritt/flytt/skjul i
  app-scopet, alle eksisterande handlingar.
- [x] 4. Full kontroll (746/746, lint 0 feil), emulatorbilete, Shield (`6197504f…`, hash stemmer), overlevering.

Ingen push. Ingen glas/blur. Fargar berre i `core/theme`.
