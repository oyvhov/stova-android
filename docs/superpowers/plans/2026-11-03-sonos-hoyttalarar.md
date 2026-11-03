# Sonos: teikna høgtalarar med modell frå HA

Bestilling 3. november 2026, eigaren: «Eg vil ha med meir visuelle element i Sonos. Kan du lage
visuelle element av korleis faktisk spelarane ser ut og animere dei?» Codex koordinerer og tek
installasjon/test på Loftet etterpå. Vala frå førre finpuss står: dempa knappar, «Spelar no ·
romnamn», utan «Musikk i heile huset», og TV-lyd verken i widgeten eller som bakgrunn.

Designvurdering før endringa (`docs/images/sonos-playing.png`): roleg og lesbar, men romlista
var rein tekst der alle rom såg like ut. Volumet høyrer til eitt rom utan noko fysisk anker, og
gruppevalet var ei flat brytarliste. Medlemmane i ei gruppe var berre ei namnelinje.

- [x] 1. Registeret: `HaEntityInfo.model`/`manufacturer` (valfrie, standard null) frå
  device-registeret, berre for `platform == sonos` og berre streng-verdiar. Ingen nye kall.
  `SonosRoom` får felta; namnet på rommet blir aldri brukt som modell. Test først.
- [x] 2. Modelltolking (`SonosSpeaker.kt`): merke, generasjon («(Gen 2)», «2nd gen») og
  teiknsetting blir tekne bort før eksakt oppslag, så «Play:5 (Gen 2)» → Five og «Arcade» ≠ Arc.
  Ein annan produsent eller ukjend modell gir eit nøytralt, svakt teikna motiv; etiketten er
  modellen HA rapporterer, utan «Sonos».
- [x] 3. Aktivitet: `sonosRoomVisual` brukar den native rommet sin modell, medan musikken til
  gruppa (også gjennom Music Assistant) avgjer om han lyser/pustar. Stopp ved pause, TV-lyd,
  fråkopling, ikkje RESUMED og redusert rørsle.
- [x] 4. Teikning (`SonosSpeakerArt.kt`): Canvas med `drawWithCache`; geometri per storleik,
  berre teiknefasen les klokka (høgst kvar 50 ms). Statuslyset pustar sakte
  (`StovaMotion.SpeakerBreathMillis`); det store motivet sender i tillegg svake konturar ut frå
  si eiga form i fast takt. Ingen lydnivåmålar. Fargar i `core/theme/SonosTokens.kt`.
- [x] 5. Plassering: høgtalar i kvart romkort (utan eige spelemerke, lyset seier det), eit
  større motiv med modellnamn ved volumet til valt rom, ei medlemsrekkje med kvar sin modell for
  grupper (då utan det store motivet, så sida ikkje veks), og høgtalar + modell i gruppedialogen.
- [x] 6. Skjermtestar: galleri over alle former (`sonos-speakers`), gruppert side
  (`sonos-grouped`), avspeling, stor skrift og gruppedialog. Full suite, lint og bygg.
- [ ] 7. Loftet: installasjon og test med eigaren sine verkelege høgtalarar (Codex).

Former: One/One SL/Play:1, Era 100, Era 300 (midje), Five/Play:5/Play:3, Beam, Arc/Arc Ultra,
Ray (lameller), Playbar, Playbase, Move/Move 2 (smalnar), Roam (kapsel med rygg), Sub
(gjennomgåande opning), Sub Mini (sylinder med spalte), Amp/Port/Connect og ukjend.
Proporsjonane følgjer omtrentlege frontmål for produkta (kjende mål, ikkje slått opp på nytt hos
Sonos i denne økta); svært lange lydplanker er korta så dei kan lesast på TV. Ingen
tredjepartsbilete er brukte; alt er teikna i kode.
Verifisering: [`../../verification/2026-11-03-sonos-hoyttalarar.md`](../../verification/2026-11-03-sonos-hoyttalarar.md).
