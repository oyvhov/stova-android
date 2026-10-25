# Bestilling: Smarthus – trykk og handlingar for fjernkontroll

Worktree: `C:\Launcher\.worktrees\ha-v2`, greina `ha-v2` (Smarthus v2 og finpuss er ferdige).
Les og følg `AGENTS.md`.

## Brukaren sine ord

> «Tenk godt igjennom design og trykking/aksjonar på smart home-delen.»

## Problem i dagens modell (funne ved gjennomgang av koden)

1. **Piltastane er kapra.** `CardFrame.onDirection` og `directionAction` gjer at venstre og høgre
   endrar verdien på eit fokusert lys-, klima-, media- eller gardinkort i staden for å flytte fokus.
   - Ein kjem ikkje vidare bortetter ei rad med lampar.
   - Ein endrar lysstyrke, volum eller temperatur ved eit uhell.
   - Ein garasjeport kan flytte seg berre ved navigering.
2. **Hald OK og Meny gjer det same.** `menu = onMenu ?: onEdit` gjer at begge opnar «tilpass».
   Lysstyrkedialogen blir nesten umogleg å nå.
3. **`longAction`** («neste spor» på media) finst, men er ikkje kopla til noko.
4. **Ingen tydeleg regel for lås, port eller garasje,** og hinten nedst er den same for alle korttypar.

## Den nye modellen: éin regel per tast

| Tast | Rolle | Gjeld |
|---|---|---|
| **Piler** | **Berre navigasjon** i rutenettet. Endrar aldri tilstanden til ei eining. | alle kort |
| **OK** | Den openberre, trygge og reversible snøgghandlinga | sjå tabellen under |
| **Hald OK** | Opnar **kontrollpanelet** for eininga («Fleire val») | alle kort |
| **Meny** (≡) | **Kortval**: storleik, flytt, gi nytt namn, ikon, vis på Heim, fjern | vaksne; òg i Rediger-modus |
| **Spel/pause** på fjernkontrollen | Spel eller pause den fokuserte mediaspelaren | media |
| **Spol fram/tilbake** | Neste eller førre spor på den fokuserte mediaspelaren | media |
| **Tilbake** | Lukkar panel, val eller Rediger-modus; elles tilbake til Heim | alle |

### Kva OK gjer per korttype

| Korttype | OK | Kontrollpanel (hald OK) |
|---|---|---|
| Lys | slå på eller av | lysstyrke-glidebrytar, fargetemperatur (Varm, Nøytral, Kald), fargeførehandsval viss det er støtta, av/på |
| Brytar, vifte, input_boolean | slå på eller av | av/på, viftefart og retning viss det er støtta |
| Scene, skript | køyr; kortet viser «Køyrt ✓» i 2 s | køyr og sist køyrt |
| Mediaspelar | spel eller pause | førre, spel/pause, neste, volum-glidebrytar, kjelde viss det er støtta |
| Klima | opnar panelet | måltemperatur ±0,5, modus (Varme, Kjøling, Auto, Av) og forhandsinnstillingar viss det er støtta |
| Gardin/port (`cover`) | opnar panelet | Opne, Stopp, Lukk og posisjon; **garasje og port** (`device_class` garage/gate) krev stadfesting |
| Lås | opnar panelet | Lås med eitt trykk; **Lås opp krev stadfesting** (eigen knapp «Ja, lås opp») |
| Kamera | fullskjerm (oppdatert kvart 2. s) | same |
| Sensor, binærsensor | opnar panelet | 24 t-graf, min, maks og sist endra |
| Vêr | opnar panelet | timar og dagar |
| Straumpris | opnar panelet | grafen (i dag/i morgon, time/15 min) |

### Kontrollpanelet

- **Utsjånad:** eit roleg ark i Stova-stil, same som dei andre dialogane. Namn og rom øvst, og stor
  hovudverdi.
- **Navigasjon:** opp og ned flyttar mellom kontrollrader. **Venstre og høgre justerer berre den
  fokuserte glidebrytaren.** OK aktiverer knappar.
- **Justering:** held ein inne, aukar steget gradvis (1 → 5 → 10 % for lys og volum; 0,5° for klima).
- **Sending:**
  - Verdien blir vist med ein gong (optimistisk).
  - Kallet går 300 ms etter siste tastetrykk (debounce).
  - Ved feil går verdien tilbake, med ei roleg melding.
- **Lukking:** Tilbake lukkar, og fokus går tilbake til same kort.

### Tilbakemelding og tryggleik

- **Optimistisk tilstand** på alle snøgghandlingar, med eit lite ventemerke. Ved feil går
  tilstanden tilbake, og ei melding seier kva som skjedde.
- **Utilgjengeleg eining:** dempa kort. OK viser «Utilgjengeleg» og gjer ingenting.
- **Barneprofil** utan `kidSmartHomeControl`: OK og hald OK opnar panelet i lesemodus. Ingen
  kontroll, ingen kortval.
- **Alarm** (`alarm_control_panel`) blir ikkje støtta for styring. Viss han finst, berre les.

### Kontekstuell hint

- **Hinten nedst** viser handlingane for det fokuserte kortet:
  - Lys: «OK: slå av · Hald OK: lysstyrke og farge · Meny: tilpass»
  - Klima: «OK: juster temperatur · Meny: tilpass»
- **Visning:** han tonar inn når fokus flyttar seg og tonar ut etter 4 s, og tek aldri plass frå korta.

### Overskrift og Heim

- **Klikkbar oppsummering:** «3 lys på» blir ein knapp. OK gir «Slå av alle 3 lys?» med Ja eller
  Avbryt. Dei andre delane er berre informasjon.
- **Rediger** i overskrifta slår på Rediger-modus: kortet viser ramme for flytting, OK opnar kortval,
  piler med OK held flyttar kortet, og Tilbake avsluttar. Dette er vegen for fjernkontrollar utan
  Meny-tast.
- **Statusbrikkene på Heim:** OK opnar Smarthus med det kortet fokusert. Ingen direkte handling frå
  Heim, så ingen uhell der.
- **Fokusminne:** Smarthus hugsar sist fokuserte kort når ein kjem tilbake.

## Rammer

- **TDD med raud/grøn.** Viktige testar:
  - piler på eit fokusert lyskort flyttar fokus til nabokortet og sender **ingen** tenestekall;
  - OK slår lyset av og på;
  - hald OK opnar lyspanelet, og Meny opnar kortval;
  - venstre og høgre i panelet endrar lysstyrken, med debounce og akselerasjon;
  - Spel/pause-tasten på eit fokusert mediakort;
  - stadfesting for garasje og opplåsing;
  - barneprofil i lesemodus;
  - «Slå av alle lys» med stadfesting;
  - hinten per korttype.
- **Ekte test** på `emulator-5580` mot brukaren sitt HA, med same reglar som før: set alt tilbake,
  aldri alarm, opplåsing eller garasje i røynda, aldri emulator-5560/-5562/-5564.
  - Ta ekte skjermbilete av panela (lys, media, klima), Rediger-modus og hinten. Lagre dei i
    `docs/images/ha-v2-handling-*.png`.
- **Spesifikasjon:** erstatt tabellen i §5.2 med tabellane over, så modellen er bindande.
- **Forbodne filer:** ikkje rør heim, hero, mediekort, utsjånad, tema eller ikonpakke. Ein annan
  agent arbeider der.
- **Strengar:** i blokka `<!-- Smarthus v2 -->`.
- **Sluttkontroll:** `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug`
  skal vere grøn.
- **Commits:** små. Ingen push.
- **Til slutt:** oppdater `docs/HANDOFF.md`, og svar med commit-liste, testtal og bilete.
