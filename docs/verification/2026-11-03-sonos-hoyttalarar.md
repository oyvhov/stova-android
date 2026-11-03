# Sonos: teikna høgtalarar — 3. november 2026

Plan: [`2026-11-03-sonos-hoyttalarar.md`](../superpowers/plans/2026-11-03-sonos-hoyttalarar.md).

## Automatisk kontroll

Testane vart skrivne først og feila (manglande `model`/`manufacturer`, `SonosSpeakerActive`,
`sonosRoomVisual`). Nye testar: `SonosSpeakerTest` (modellar, generasjonar og alias, ukjend/
anna produsent, modell frå registeret og ikkje namnet, aktivitet ved spel/pause/TV/fråkopla/
ikkje RESUMED/redusert rørsle, MA held native modell, gruppemedlem viser eigen modell),
`HaCatalogTest.sonosEntitiesCarryTheirDeviceModelOthersStayLean` og tre UI-testar i
`SonosContentTest` (modell/høgtalar i romlista, rørsle berre når musikk spelar, gruppedialog).

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'
```

**1097 testar, 0 feil, 0 hoppa over**, lint 0 feil, debug-APK bygd (ikkje installert).
`DesignRulesTest` og `ThemePaletteTest` grøne.

## Skjermkontroll (Robolectric, native grafikk)

`docs/images/sonos-speakers.png` (alle former), `sonos-playing.png`, `sonos-grouped.png`,
`sonos-group.png` og `sonos-large.png`. Retta etter første bilete: toppflata såg ut som eit
laust lokk, det ukjende motivet likna ein One, lydplanka fekk for høg scene, og ei gruppe
viste same høgtalar to gonger og skauv volumet under kanten.

## Ikkje verifisert

Animasjonen er berre kontrollert via semantikk/testar, ikkje sett i rørsle på ei eining.
Ingen eining, HA eller Sonos er brukt. Kva modellstrengar eigaren sitt HA faktisk rapporterer,
og korleis teikningane ser ut på Loftet, står att for Codex.
