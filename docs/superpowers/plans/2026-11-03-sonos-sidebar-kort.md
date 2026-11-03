# Sonos: romliste som kan gøymast, og Sonos-kort i favorittrada

Bestilling 3. november 2026, eigaren: «eg skal kunne gjemme høgtalarane som ein slags sidemeny,
slik at det som spelar no får fokus», «Korleis er kortet på framsida, som ein kan setje inn i rad?
Det skal vise status og coverart over heile» og «Tenk heilskapen gjennom heile Stova-appen».
Bindande frå før: profesjonelt, det meste på éin skjerm, dempa knappar, ekte høgtalarfigurar,
tema/sesong integrert, TV-lyd aldri som musikkunst. Berre visuell testing på Loftet.

## A. Romlista som sidemeny

- [x] A1. Test først: knappen «Gøym rom» gøymer lista og flyttar fokus til avspelinga; knappen
  «Rom · <valt rom>» opnar ho att med fokus på det valde rommet. Ingen fokus mot ein fjerna
  komponent (Venstre frå avspelinga når lista er gøymd gjer ingenting galt). Tom/fråkopla side
  har ingen knapp. Callback for val av rom virkar framleis.
- [x] A2. Tilstanden er hoista til `SonosScreen` med `rememberSaveable` per profil, så han
  overlever biblioteket og retur. Lista kjem inn med alfa/forskyving i lag-fasen
  (`stovaMotion.enter`), ingen breiddeanimasjon. Redusert rørsle: direkte byte.
- [x] A3. Med lista gøymd får omslaget meir plass; avspeling, volum og snarvegar står framleis
  innanfor skjermen ved vanleg skrift (skjermtest). Stor skrift rullar som før.

## B. Sonos-kort som favoritt

- [x] B1. Test først: `ShortcutKey.Sonos` (`stova:sonos`) og `FavoriteTile.Sonos` i
  `favoriteTiles`: berre når modulen er på med HA (`sonosReady`), aldri for barn, og berre når
  favoritten er lagra i profilen. Rekkjefølgje/flytt/fjern går gjennom same `AppPrefs`.
- [x] B2. Test først: `sonosHomeWanted` startar Heim-økta òg når kortet er ein favoritt, sjølv
  med widget og Heim-kunst av; aldri for barn, utan HA eller med modulen av. Den same
  `rememberSonosHome`-økta forsyner widget, bakgrunn og kort (ingen nye abonnement).
- [x] B3. Test først: `sonosCardStatus`/`sonosCardArt`: spelar, pause, lastar, fråkopla, TV-lyd og
  ingen musikk. TV-lyd og fråkopla gir aldri albumkunst, sjølv med gammalt omslag. MA-spelar
  gir status/kunst, men namnet er det native rommet.
- [x] B4. Test først (UI): kortet viser tittel og «rom · status», heile kortet opnar Sonos utan
  spelehandling, feila bilete gir reserveflate med høgtalarfiguren. Langt trykk på Sonos-flisa på
  Appar opnar same snarvegsmeny som andre snarvegar.
- [x] B5. Skjermtestar for kortet i favorittrada (spelar, TV-lyd), og Sonos-sida med lista gøymd.

## Kontroll

- [x] Målretta Sonos/apps/home-testar, `DesignRulesTest`, `ThemePaletteTest`, skjermtestar,
  `lintDebug assembleDebug`. Les dei rendra bileta.
- [ ] Loftet: root verifiserer visuelt (ingen lyd-, gruppe- eller lyshandlingar).
- [x] TV-funn: `HaImage` må vente på autentisering før Coil får ein modell. Testen
  `HaImageLoadingTest` feila før rettinga og er grøn etter; kameraplassen blir halden av medan
  innlogginga ventar. Kamera-/Sonos-/design-testar, lint og TV-bygg grøne.
- [x] TV-funn: Tilbake frå ei favorittmappe hoppa til Sonos-rota. HA gir Favorites og kvar
  favorittmappe same id/type; mappeopning legg no alltid eitt nivå på stabelen
  (`SonosLibraryNavigationTest`, raud før, grøn etter). Lint og TV-bygg grøne.
- [ ] Ny bestilling: installer og prøv på Shield òg. Eigaren har no tillate Sonos-handlingar
  medan han er borte, med volum på 0. Kontroller volum før test og rydd tilbake etterpå.

Verifisering: [`../../verification/2026-11-03-sonos-sidebar-kort.md`](../../verification/2026-11-03-sonos-sidebar-kort.md).
