# Diskret tilkoplingsstatus

Bestilling: ikkje gjenta «Tilkopla» overalt; bruk noko nøytralt og visuelt lite.
Felles HA-status finst på Smarthus, i kortveljaren og i HA-innstillingane.

- [x] 1. Oppdater eksisterande UI-prøver til ein liten, semantisk status utan
  synleg Tilkopla-tekst. Køyr og stadfest at den gamle visinga feilar kontrakten.
  Tre eksisterande prøver feila på den gamle tekstvisinga (vanleg/dobbel skrift og
  faktisk tilkopling i HA-innstillingane); 12 andre prøver bestod.
- [x] 2. Vis tilkopla som ein statisk nøytral prikk og tilkopling som ein liten
  ring. Bevar korte feil-/oppsetttekstar. Flytt status inline i HA-innstillingane.
  Køyr full sluttkontroll med DesignRulesTest og native skjermar.
  709/709 testar, 0 skip; lint 0 feil (62 åtvaringar, 1 hint), bygg OK.
- [x] 3. Installer med data bevarte på Stova-emulator og Shield. Kontroller
  Smarthus og innstillingar, ta eit nytt faktisk bilete og skriv overlevering.
  APK er hashverifisert på begge. Fem faktiske flater har semantisk status i
  14 dp markør utan synleg Tilkopla/Connected-tekst; alle profildata er byteidentiske.

Ingen transportendring, HA-styring, datatømming, push eller endring av lagra oppsett.
