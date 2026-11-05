# Overgangar og rørsle i heile Stova

Stova skal kjennast som éin TV-app. Når du trykkjer OK, skal det vere
lett å sjå kva som opna seg. Når du trykkjer Tilbake, skal blikket og
fokuset finne att staden du kom frå.

## Fem mønster

| Mønster | Bruk det her | Slik skal det kjennast |
|---|---|---|
| Opne ei side frå eit punkt | Sonos frå widget, favoritt eller ikon. Appar frå Heim. Oppdag frå flis. Smarthus frå toppfeltet. | Sidan kjem inn med eit kort, lett løft frå kontrollen. Tilbake tonar sida ut mot same stad når kontrollen framleis finst. |
| Gå eitt steg djupare | Oppdag → tittel, Sonos → mappe, Innstillingar → underside, Smarthus → detaljar. | Hovudflata står roleg. Berre innhaldet i det nye steget flyttar seg litt. Tilbake snur retninga og gir fokus tilbake. |
| Opne noko oppå sida | Snøggmeny, langt OK, smarthuspanel, stadfesting. | Eit lite panel opnar seg ved kontrollen når det er plass. Eit større panel tonar inn med demping bak. Same lukking og fokusretur på tvers av appen. |
| Byte innhald på same plass | Albumomslag, filmhero, spel/pause, profilbilete og rommarkering. | Berre det som faktisk endrar seg, får ei overtoning eller ei kort markering. Ny kunst kjem inn når ho er dekoda. Tekst, kontrollar og radstorleik held plassen. |
| Flytte fokus eller rulle | D-pad på Heim, Appar, bibliotek, rom og Smarthus. | Fokus reagerer med ein gong. Rulling følgjer fokus i éi rørsle. Eit nytt trykk kan endre retning undervegs. |

## Felles reglar

- Bruk `StovaMotion`: om lag 150 ms for små svar, 200 ms for sideopning,
  og opptil 900 ms berre for roleg bakgrunnskunst. Bygg på dei eksisterande
  tidene og kurvene før me vurderer fleire.
- Rørsla skal aldri halde igjen OK eller Tilbake. Den nye sida eig
  tastane med ein gong, og sida som går ut, slepper fokus.
- Redusert rørsle byter side direkte. Fokus, valt tilstand og avspelingsstatus
  skal vere lesbare også når alt står stille.
- Bruk alfa, skala og forskyving i grafikklaget. Hald tekstmåling, layout og
  biletlading utanfor animasjonsklokka. Unngå blur og nye heilskjermslag.
- Stopp avspelingsstrekar, høgtalarpuls og pynt når dei er utanfor skjermen
  eller appen går i bakgrunnen. Ved sakte biletlading blir den førre gode
  visinga verande så lenge ho høyrer til same innhaldskjelde.

## Rekkefølgje

1. **Pilot frå Heim og Appar**
   - [x] Bruk eit felles opningspunkt for Sonos-widgeten, Sonos-favoritten,
     toppfeltet, Alle appar og Oppdag-flisene. Redusert rørsle hoppar over
     sideovergangen.
   - [x] Køyr målretta navigasjons-, Sonos- og designtestar, lint og TV-bygg.
   - [ ] Verifiser punkt, fokusretur og raskt OK → Tilbake på TV. Ta eit kort
     opptak av widget → Sonos → Heim, med og utan redusert rørsle.
2. **Sidehistorikk og fordjuping**
   - [x] Knyt opningspunkt og returfokus til kvart steg i navigasjonen.
     Når opnaren manglar, bruker sida vanleg startfokus.
   - [ ] Prøv Heim → Appar → Sonos → Appar → Heim og tilsvarande for Oppdag på TV.
   - [x] Bruk same stegvise overgang i Oppdag, Sonos-bibliotek,
     Innstillingar og Smarthus. Sjå over skjermane som alt brukar lokal fade,
     slik at to overgangar ikkje spelar samtidig.
3. **Panel og endringar i innhald**
   - [x] Bruk felles panel og dialog, og slå av overgangen ved redusert rørsle.
   - [ ] Prøv langt OK, Avbryt og returfokus på TV.
   - [x] La Sonos-widgeten halde på førre omslag til det neste er dekoda.
   - [ ] Gå over filmhero, profilbyte, romval og oppdateringar
     frå Home Assistant. Prøv forseinka bilete, raske byte og fråkopling.
4. **TV-kvalitet før utrulling**
   - [ ] Prøv normal og stor skrift, rask D-pad, gjenteken Tilbake og redusert
     rørsle. Kontroller at ingen kontrollar mistar fokus eller flyttar seg
     medan ein held inne ei pil.
   - [ ] Samanlikn frametid og tapte rammer før og etter på same eining, med
     same sekvens. Prøv på Stova-emulatoren med eigna prøvedata og på
     Shield/Loftet etter bestilling. Behald berre rørsle som fungerer på
     den tregaste av dei prøvde TV-ane.

Pip, sesongpynt og Sonos-indikatorar har framleis eigne motiv. Dei skal
følgje dei same reglane for redusert rørsle og synlegheit.

## Oppfølging etter Shield-opptaket 4. november

- [x] Start Heim i rett rullestilling ved retur til toppfeltet eller Sonos-widgeten.
- [x] Del Sonos-tilstand mellom Heim og spelaren og la det dekoda omslaget gli begge vegar.
- [x] Vis låt og kontrollrom éin stad når widgeten er synleg.
- [x] Gjer tid, framdrift og volum tydelegare i fullskjerm.
- [x] Merk hovudrommet i romgrupperinga.
- [x] Hald omslaget fast og ton dempinga mellom vanleg spelar og fullskjerm.
- [x] Samle transporten i midten, opne volum under høgtalarknappen og ton inn bileta i favoritt- og spelelisterada.
- [ ] Prøv den nye omslagsovergangen og returen frå Smarthus med fjernkontroll på Shield.

## Oppfølging på Loftet 5. november

- [x] Ta opp eit naturleg låtbyte og kontroller mellomrammene for bortfall av kunst.
- [x] Førehandshent neste omslag også frå den vanlege Sonos-køen når neste spor er kjent.
- [x] Bruk same inn- og utgang for Smartheim-panel, handlingsknappar og popupane frå toppfeltet, med fokusretur.
- [x] Verifiser målretta testar, designreglar, TV-bygg og dei endra flatene på Loftet.
- [x] Stopp gjenteken OK frå eit tidlegare panel ved kortet, og prøv langt trykk igjen etter at Loftet-lyset er sett attende til Av.
