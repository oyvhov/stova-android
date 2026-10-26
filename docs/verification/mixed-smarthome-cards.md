# Blanda Smarthus-kort

2026-10-26 · oppfølging av «Ikkje berre breidde kort?»

## Resultat

Shield har no eitt samla, kompakt rutenett med fem eksisterande kort:

| Venstre | Midten | Høgre |
|---|---|---|
| Ute L (2 × 2) | Straumpris M (2 × 1) | Stova S (1 × 1) |
| same vêrkort | Sonos M (2 × 1) | Varmepumpa S (1 × 1) |

S-korta har mindre innvendig luft og ikonflate, med same typografi og alle
rom/verdiar bevarte. L-vêr har større temperatur, eige vêrikon og prognosen
under over heile breidda. M-vêr er uendra. Intrinsisk høgd får framleis vekse
når større skrift eller meir innhald krev det.

## Verifisering

Tre native regresjonar feila før rettinga: S-lys var 177,5 dp mot 152 dp,
L-bandet enda ved 537 dp utanfor viewport 492 dp, og vêrprognosen låg ved sida av
temperaturen. Fire endelege prøver er grøne: S-detaljar innanfor kortet,
alle fem kort i første band, L-prognose/alle høg- og lågverdiar innanfor kortet,
og S-klima med desimalverdi/langt namn ved dobbel skrift. Fokusprøva set
Keyboard-inputmodus og verifiserer både stegvis og rask pilnavigasjon utan tenestekall.

`pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'`
bestod: 709 testar, 0 feil, 0 skip. Lint: 0 feil, 62 åtvaringar, 1 hint. Bygg OK.
XML-resultata er kontrollerte. Ingen nye navigasjons-/transportendringar.

APK SHA256:
`9bd09278c1abbb8b3f29cee4701760493696cb2e5bacd2f294531508cad66e4b`.
Installert med `-r` og hashverifisert på emulator-5580 og Shield.
Eksisterande emulatoroppsett er visuelt kontrollert. Shield er prøvd med
Straumpris → Ned → Sonos og Lys → Ned → Varmepumpa, og sida er opna på nytt.
Fokusert kort er heilt synleg; vanleg TV-fokus kan rulle bandet litt når ein går ned.

Før/etter-samanlikning består: berre storleik/rekkjefølgje for dei fem eksisterande
Shield-korta og tettleik Balansert → Kompakt er endra. Alle andre profilverdiar er
like. Emulatorprofilen og konto-/hushaldsfiler på begge er byteidentiske.
Ingen HA-styring eller avspelingskommandoar er brukte. Shield gjekk i skjermsparar
under arbeidet og vart vekt før vidare UI-verifisering.

Ekte 3840×2160-bilete med alle fem kort, heile tekstar, Tilkopla-status og utan meny/toast:
`.superpowers/smart-home-view/smarthus-mixed-shield.png`.
Evidens, før/etter-data, raud XML, UI-uttrekk og APK-/dataverifisering ligg i same
git-ignorerte mappe. Ingen bilete er genererte eller redigerte.
