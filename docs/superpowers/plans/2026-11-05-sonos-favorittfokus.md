# Sonos-favorittar, sidehøgd og roleg fokus

- [x] Fokuser berre omslaget på biletfliser; tittelen står utanfor effekten. Rada heiter Favorittar.
- [x] Ned inn i favorittane går til første element, også etter at rada har vore rulla. Retur frå dialog/bibliotek held fram på opnaren.
- [x] Vanleg Sonos-vising viser favorittar og alle botnknappar på éi side, med og utan romlista. Bevar favorittane i spelarfeltet ved sida av romlista; stram berre inn høgd og mellomrom. Stor skrift kan rulle.
- [x] Standard fokus kombinerer svakt løft og mjukt lys. Berre det aktive vindauget får fokusmarkering; tydeleg ramme er framleis eit val.
- [x] Verifiser regresjonstestar, design/palett, lint og TV-bygg. Prøv på Shield utan å endre avspeling eller volum.

Apple HIG «Focus and selection» vektlegg djupn, skalering og plass rundt
fokuserte element. Me bruker matte flater og dei eksisterande rørsletokena,
utan glas eller blur. Ingen nye globale fokusmekanismar eller tenestekall.

Verifisering: full testpakke 1131 bestod / 86 skjermtestar hoppa over.
Etter siste høgdjustering: 121 målretta bestod / 22 hoppa over; lintDebug og
assembleTv grøne. install-r på Shield, samsvarande SHA-256
`1658400ca9c17758542eac17c86e6e9445c9a5b7eb33fb9e65875e7ba75b8cc0`.
Sluttbiletet `.superpowers/shield-sonos-favorites-2026-11-05/Sonos-Shield-retta.png`
viser opphavleg plassering og alle botnknappane. Favoritt 3 → Opp → Ned gir
første favoritt; fullskjermvolum er prøvd utan volumendring. Berre Shield brukt.
