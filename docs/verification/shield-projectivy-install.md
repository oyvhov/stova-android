# Shield: Stova og Projectivy installert 25.10.2026

Uttrykkeleg bestilling: «Installer alt på Shield for meg».
Eining: NVIDIA SHIELD Android TV, `192.168.10.182:5555`.

Begge pakkane er installerte med `adb install -r`. SHA-256 av dei installerte APK-filene er
kontrollert på Shield og er lik dei lokale, tidlegare testa filene:

| Pakke | Versjon | SHA-256 |
|---|---|---|
| `app.stova.debug` | 0.1.0-debug, frå `0ed3e34` | `1569035b96db6e5d7d4d8840233d874515418759c93c4c01013fde52205cf361` |
| `dev.projectivy.blueprint.app` | 1.1.9 / 119 | `c3343564f6ff7a5ab51542a086f52820997fadbf26d947c1d30c3ef7bdad2d83` |

Stova-byggkontroll: `scripts/gradle.ps1 assembleDebug` → BUILD SUCCESSFUL, alle oppgåver uendra.
Denne APK-en har 588/588 beståtte testar frå fulltesten før installasjon; dei er ikkje køyrde om att her.

Profil- og kontofiler var byteidentiske rett etter installasjon. Projectivy og appkortbakgrunn Av
er deretter valde for Shield-profilen. Alle andre tidlegare profilfelt, favorittar og overstyringar er uendra.
Kontofila er framleis byteidentisk: `2be55c39a5d4fc7ddf3ba971defaeaec85336d32d5c3e737249e2a6706ff7517`.
Stova opnar med ekte Midttunet-rader og viser det nye Spole-merket. Standardlauncheren var
og er Google TV; ingen launcher er slått av eller omvald. Ingen HA-styring eller publisering.

Under eigaren si prøve kom ny bestilling om å fjerne fokusskuggen og rette navigasjonen.
Dette er no retta, bygd og installert på begge einingar. Sjå [fokus- og navigasjonsprøva](directional-focus.md)
for endeleg Stova-hash, 609/609 testar, native D-pad og menyprøve. Planen for Shield-installasjon er fullført.
Spole er òg oppdatert lokalt med same signeringsnøkkel; sluttverifisering av tittellenkje frå faktisk
Stova-kort blir ført i `C:/JellyBin/spole-launcher-link/docs/VERIFICATION_v1.0.0-beta22-local.md`.
