# Status
2026-10-25: Plan 2, oppgåve 1–4 ferdige med raud/grøn TDD. Oppgåve 5 står for tur.
Full testsamling og lint bestod etter oppgåve 4. Ingen serverar eller emulatorar brukte.
Oppgåve 10 skal hoppast over etter brukarønske. Ingen push.

## Plan 2
- Berre denne worktreen er lesen og endra, slik brukaren bad om. Spole-kjelder utanfor mappa er derfor ikkje kopierte; tenestelogoane er lokale, enkle vektorvariantar med originalfargane. Eksakt samsvar med Spole er ikkje verifisert.
- UI-testane bruker den smale AddAccountContent-grensa med ekte REST mot MockWebServer og eit lokalt kontolager; produksjonsflyten brukar same kode.
- Quick Connect-nettverkstesten bruker reell coroutine-klokke: virtuell timeout kan gå ut før MockWebServer-tråden får svart.
