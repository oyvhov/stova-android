# Sonos på éin skjerm – 3. november 2026

Plan: [navigasjon og tema](../superpowers/plans/2026-11-03-sonos-ein-skjerm.md).

## Endringa

Daglege kontrollar ved vanleg TV-skrift har fast musikkfelt, høgtalar/volumrad og snarvegar.
Lang tittel blir høgst to linjer, artist/album éi. Fleire val held kjelder, demping,
tilfeldig/gjenta og ±15 sekund i eit panel med fast tittel/Lukk; innhaldet kan rulle.
Kallfeil blir viste også inne i panelet. Med stor skrift kan hovudsida rulle utan å krympe
teksten. Romliste og store bibliotek kan framleis rulle.

Høgre frå rom går til første tilgjengelege avspelingsknapp, elles Fleire val. Venstre frå
første avspelingsknapp går til valt rom. Fleire val gir fokus attende til opningsknappen.
Figuren ved volum er alltid det valde rommet, også i ei gruppe. Gruppemedlemmer har eigne
figurar i romlista og gruppevalet; ingen ekstra medlemsrad skyv hovudkontrollane ned.

Alle flater, fokus og kontrollar bruker dei eksisterande temarollene. Sonos-sida og
biblioteket bruker dei same SeasonalScene/SeasonalClock-komponentane som Heim, og eit
sesongmerke frå same profilval. Av/Roleg/Max, kalender/manuell sesong og redusert rørsle
følgjer eksisterande oppførsel. Høgtalarane bevarer den matte produktfargen.

## Testar og bygg

Test først: dei nye skjermplass-/Fleire val-testane feila på førre layout; sesongmerke-testen
feila før integreringa. Deretter grøne. Ein D-pad-test sjekkar siste rom → Høgre → Førre spor
→ Venstre → valt rom. Skjermplasstesten bruker lang metadata, gruppe, MA-knapp og feiltekst
og sjekkar heile kontrollen innanfor trygg skjermkant utan rulling.

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests 'app.stova.sonos.*' --tests 'app.stova.home.SeasonalDecorTest' --tests 'app.stova.DesignRulesTest' --tests 'app.stova.core.theme.ThemePaletteTest' '-Pstova.screens=true' lintDebug assembleTv
```

**76 testar, 0 feil, 0 hoppa over**. Lint **0 feil, 84 åtvaringar, 1 hint**. Tv-bygg grønt.
Ingen full suite i denne avgrensa runden. Skjermtestar rendra til `app/build/screens/`;
normal, gruppe, stor skrift, jul og Halloween kontrollerte og kopierte til `docs/images/`.

## Loftet

Eigaren hadde autorisert installasjon og presisert berre visuell testing. Installert med
`Install-Stova.ps1 -Serial 192.168.10.125:5555 -Tv` (install -r). Kontoar og preferansar er
bevarte. Ingen demodata, clear, avinstallering, force-stop eller lyd-/lys-/gruppehandlingar.

På ekte skjerm: fire rom, lang Halloween-tittel og heile volumkontrollen + snarvegane innanfor
skjermen. Høgre → Førre spor, Ned → Volum, Ned → Fleire val; inga rulling og ingen volumendring.
OK opnar Fleire val, Tilbake returnerer fokus til knappen. Biblioteket har same sesongpynt og
Tilbake returnerer til Sonos. Heim-tenesta enabled og bound etter installasjonen; krasjbuffer tom.

Eigaren endra sjølv Sonos-innhaldet medan testen gjekk: først Great Expectation (SIENNA SPIRO),
så Inspector Norse (Todd Terje), og sette volum til 0 %. Albumomslag, bakgrunn og metadata
oppdaterte seg utan ny opning. Eigaren starta sjølv avspeling; ein kort skjermvideo og uttrekte
rammer viser at statuslyset og konturane til Playbar-figuren endrar seg. Codex sende ingen
avspelingskommando. Lyd, MA og aktiv TV-kjelde er ikkje testa. Loftet er late stå på Sonos-sida
fordi eigaren bad om fortløpande skjermbilete; ikkje sett i søvn i denne runden.

Lokale bilete/opptak (ikkje i Git): `.superpowers/sonos-one-screen-2026-11-03/`:
`sonos-main.png`, `controls-focus.png`, `more-options.png`, `library-themed.png`,
`sonos-updated-cover.png`, `sonos-updated-cover-2.png`, `final-sonos.png`, `sonos-motion.mp4`
og `speaker-motion-frames.png`. Ingen generert eller redigert albumkunst: bileta er dei
som eigaren sjølv valde i Sonos.
