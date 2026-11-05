# Ny Smarthus-oversikt

Eigaren ser ikkje eit designløft på sjølve skjermen. Førre runde endra i
hovudsak panela. Denne bestillinga gjeld komposisjonen til hovudskjermen.

- [x] Lag romnavigasjon i venstre kant og eit eige hovudfelt med stor romtittel og samla verktøy.
- [x] La romval vise dei rette korta, med D-pad inn/ut av romlista og fokusretur frå panel. Endring av vising sender ingen tenestekall eller lagringskall.
- [x] Gje korta tydelegare verdiar og ikon, og balanser kortfeltet innanfor dei lagra storleikane.
- [x] Verifiser testar, stor skrift, redusert rørsle, designreglar, lint og TV-bygg.
- [x] Installer på Shield og vurder ekte før-/etterbilete av hovudskjermen og rombyte.

Retning: matt, romleg og roleg. Ingen glas eller blur. Romlista har eit
tydeleg valt rom; kortfeltet skiftar mjukt. Eksisterande rom, kortrekkjefølgje,
storleikar, favorittar og kontrollhandlingar blir brukte vidare.

## Verifisering

64 målretta oversikts-, rom-, fokus-, layout-, design- og palett-testar grøne.
Den breiare Smarthus-køyringa avdekte statusmeldinga som forsvann brått;
den siste køyringa verifiserer at ho tonar ut i botnen utan å flytte korta.
Stor skrift og både vanleg og redusert rørsle er prøvde i kontrollerte UI-testar.
`lintDebug` og `assembleTv` grøne. Native kontroll på Shield brukte berre
piltastar etter opning av oversikta: Alle rom → Kjøkken → Småstova → Alle rom.
Ingen tenestehandlingar stadfesta; lys 80 %, varmepumpemål 22,5° og TV-volum
20 % heldt seg. Smarthus står open med Alle rom valt.

APK installert med `install -r`, SHA-256 på eininga og lokalt samsvarar:
`7e0453b04e50480e804b5846240ecd0e6318217c1fc2e23bc67f024f4b9bccbf`.
Bilete: `.superpowers/shield-smart-home-overview-2026-11-05/for-og-no.png`.
Opptak: `Smarthus-rombyte-Shield.mp4` i same mappe, 20,1 s, 1080p H.264,
30 fps utan lyd. Enkeltbilete av Alle rom, Kjøkken og Småstova ligg ved.
