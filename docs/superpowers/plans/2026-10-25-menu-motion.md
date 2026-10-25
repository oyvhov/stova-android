# Menysystem, fokus og mjuke overgangar

Bestilling: rydd dei mange vala i eit meir kompakt system, rett Heim → Utsjånad
som hoppar til Profil, og gjer rad-/side-/menyovergangar mjuke i heile appen.
Arbeid på emulator-5580 med ekte data; bevar eksisterande Smarthus-endringar.

## Task 1: Trygg fokusoverføring mellom innstillingssider
- [x] Skriv og køyr raude regresjonar for lenkjer begge vegar og raske piltrykk.
- [x] Rett fokusoverføringa utan utgåande fokusmål; køyr fokus- og designtestane grøne.
- [x] Commit kode, testar og denne planen; før ledger.

## Task 2: Eit oversiktleg, kompakt menysystem
- [x] Skriv og køyr raude testar for kategorioversikt, underkategori, retur og stor skrift.
- [x] Del utsjånad i åtte små sider, gi menyen tydeleg gruppering og meir plass til innhaldet. Samle radval under «Tilpass rada» i kontekstmenyane.
- [x] Verifiser tastestyring, profilval og visuell layout; commit og ledger.

## Task 3: Mjuk og avbrytbar rørsle
- [x] Skriv og køyr raude testar for radflytting over fleire rammer, reversering og redusert rørsle.
- [x] Samle roleg rulling og mjukare overgangar i temaet; bruk dei på Heim og Smarthus.
- [x] Køyr relevante testar grøne; commit og ledger.

## Task 4: Samla kontroll og overlevering
- [x] Køyr testDebugUnitTest lintDebug assembleDebug med skjermprøver og kontroller resultata.
- [x] Installer med -r på emulator-5580, prøv menyar og overgangar med ekte data og ta skjermbilete/klipp.
- [x] Oppdater arkitektur/spesifikasjon, verifikasjonsrapport og HANDOFF; commit. Ingen push.
