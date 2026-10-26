# Diskret tilkoplingsstatus

2026-10-26 · «Treng ikkje stå tilkopla alle plasser»

`HaStatusLabel` er felles for Smarthus, kortveljaren og HA-innstillingane.
Tilkopla er no ein statisk 6 dp prikk i den nøytrale temafargen, medan tilkopling
er ein 8 dp ring. Markøren har ei fast 14 dp flate, lokalisert innhaldsomtale
for skjermlesarar og ingen eigne fjernkontrollstopp. Feil/oppsett har korte
`labelSmall`-tekstar; utlogging bevarer «Logg inn på nytt».
I innstillingane står markøren ved Home Assistant-tittelen, utan eiga statuslinje.

Tre eksisterande prøver feila før implementering: storleik/tekst ved vanleg og
dobbel skrift og faktisk tilkopling i HA-innstillingane. Kortveljaren si prøve
for tilkopling, fråkopling og ny tilkopling er oppdatert til semantisk normalstatus;
synleg Fråkopla-tekst og transportkontrakten er bevarte.

Full kontroll med `scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug -Pstova.screens=true`
bestod: 709 testar, 0 feil, 0 skip. Lint: 0 feil, 62 åtvaringar, 1 hint.
Native bilete `app/build/screens/14-smarthome-heading-aligned.png` er visuelt kontrollert.

APK `08c252f1fc8471b706d5b8036ad2e3c7116772370a8c139e0b3fffdd38794ff9`
er installert med `-r` og hashverifisert på emulator-5580 og Shield.
Smarthus og HA-innstillingar er kontrollerte på begge, kortveljaren på Shield.
Ferske UI-uttrekk stadfestar ingen synleg Tilkopla/Connected-tekst og éin liten,
ikkje-fokuserbar markør på kvar av dei fem flatene.

Profil-, mediekonto- og hushaldsfiler er byteidentiske før/etter på begge einingar.
Ingen HA-styring, avspelingskommandoar, transportendring eller endring av oppsett.
Begge står på Smarthus. Reine bilete, UI-uttrekk, raude XML-resultat og verifisering
ligg privat i `.superpowers/quiet-connection-status/`; sluttbiletet er
`smarthus-shield.png` (3840×2160, faktisk skjermopptak, utan etterbehandling).
