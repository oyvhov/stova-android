# Sonos og Music Assistant

Stova bruker Home Assistant til å finne og styre Sonos. Modulen er av som standard.
Han endrar ingen høgtalarar berre ved å bli opna.

## Oppsett

1. Legg til Sonos i Home Assistant og kontroller at romma er tilgjengelege der.
2. Kople Stova til Home Assistant under **Innstillingar → Smarthus og kontoar**.
3. Slå på **Aktiver Sonos** i same seksjon. Sonos får eiga side, ein snarveg på Appar
   og eit valfritt ikon i toppfeltet. Modulen gjeld vaksne profilar.
4. Vel om **Spelar no** skal visast saman med widgetane, og om albumkunst skal overta
   bakgrunnen på Heim medan musikken spelar. Desse er separate val. Heim-kunst er av
   som standard og følgjer profilen sitt val om å bruke bilete.

Widgeten viser musikk, romnamn og spel/pause. Trykk på omslaget eller tittelen for å
opne Sonos-sida. Han rullar vekk saman med resten av toppfeltet. TV-lyd blir ikkje vist
i widgeten. TV-lyd på ein aktiv Sonos-lydplanke sperrar også Heim-kunst, sjølv om
lydplanken framleis rapporterer det førre omslaget, eller eit anna rom spelar musikk.

## Musikk, favorittar og rom

Vel rom i venstre kolonne. Avspelingsknappane styrer musikken til gruppa, medan volum
og demping gjeld det valde rommet. Kjelder, tilfeldig avspeling og gjentaking blir viste
når spelaren støttar dei. Innhaldet i **Musikk og favorittar** kjem frå Sonos gjennom HA.
Ei mappe opnar ei ny liste. Spelbart innhald kan startast eller leggjast til i køen
når spelaren støttar dette. Tilbake går opp eitt nivå, også etter ein lastefeil.

**Grupper rom** lagar eit utkast. Ingenting blir flytta før du vel **Bruk**.
Koordinatoren blir verande. Om gruppa er endra frå ein annan app, må romvalet opnast
att. Ein delvis mislukka operasjon stoppar vidare gruppeendringar og viser den faktiske
tilstanden frå HA.

## Høgtalarane

Kvart rom viser ei teikning av høgtalaren slik han faktisk ser ut, med modellnamnet ved volumet
og i gruppevalet. Modellen kjem frå einingsregisteret i Home Assistant, ikkje frå namnet på
rommet. Ukjende modellar får eit nøytralt motiv. Lyset på høgtalaren pustar sakte berre medan
musikk faktisk spelar; det viser ikkje lydnivået. Det står stille ved pause, TV-lyd, fråkopling
og med redusert rørsle.

![Høgtalarformene](images/sonos-speakers.png)

## Music Assistant

Legg til Music Assistant i Home Assistant, slå på **Bruk Music Assistant** i Stova og
opne **Kople musikkspelarar**. Vel Sonos-rommet og den tilsvarande Music Assistant-spelaren.
Koplinga er eksplisitt; to spelarar med same namn blir ikkje kopla automatisk. Eitt MA-val
kan berre vere knytt til eitt Sonos-rom i oppsettet.

**Music Assistant** opnar biblioteket som HA gjer tilgjengeleg: til dømes album,
spelelister og favorittar. Spelehandlingar frå dette biblioteket går til MA-spelaren.
Når ein MA-kø er aktiv, kjem metadata og avspelingskontrollane derfrå. Sonos-favorittar,
fysiske kjelder, romvolum og Sonos-gruppering bruker framleis dei native Sonos-einingane.
TV-kjelda overstyrer ei gammal MA-kømarkering.

Innlogging i musikktenester, oppsett av Sonos og administrasjon av Music Assistant
skjer i dei respektive tenestene. Stova har ikkje eit eige musikkabonnement eller ein
direkte Music Assistant-servertilkopling. Innhald og støtta handlingar avheng av HA
og dei tilkopla tenestene.

## Kontroll før bruk på ekte høgtalarar

Kode og kontrollerte testar er skilde frå kontroll av eigaren sitt anlegg. Sjå
[verifiseringsloggen](verification/2026-11-03-sonos.md) for kva som faktisk er prøvd.
Kontroller avspeling, gruppering, omslagsadresser, TV-lyd og MA-koplinga på det aktuelle
anlegget før dette blir rekna som ferdig verifisert der.
