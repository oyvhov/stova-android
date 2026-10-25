# Smarthus: ned frå kategoriar til einingar

Eigaren kjem ikkje ned på einingane. Shield viser «Legg til kort», med fokus i ei
horisontalt rulla kategoriliste og eit rutenett av einingar under. Førre navigasjonsplan
dekte Smarthus-dashbordet, men ikkje denne veljaren.

- [x] Reproduser Ned frå rulla kategoriar i tastetest og på Shield, utan å leggje til eller styre ei eining. Tre nye testar feilar før retting; på Shield går Ned frå Kamera til Straumpris sidelengs.
- [x] Rett fokus mellom søk, kategoriar og rutenett; hopp over einingar som alt er lagde til. Bevar søk/filtrering og trygg retur Opp. Alle 9 veljartestar er grøne, inkludert rulla kategoriar, lange lister, inaktive lister og Prøv igjen.
- [x] Køyr relevante regresjonar og full test/lint/bygg. Prøv den faktiske veljaren på emulator-5580 og Shield; installer med `-r` og bevar data.
- [x] Dokumenter konkrete resultat og avgrensingar, oppdater overlevering og ledger, commit lokalt.
