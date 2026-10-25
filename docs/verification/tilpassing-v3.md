# Tilpassing v3 – verifikasjon

2026-10-25, `appearance-v2`, berre `C:/Launcher/.worktrees/appearance`.
Bestilling: `docs/briefs/2026-10-25-tilpassing-v3.md`.

## Resultat og kontroll

Heile bestillinga er implementert: direkte radmeny på Heim for media, appar og Watch Next;
flyttemodus, skjul/angre, fem radformat, tre storleikar og radtittel; fire hjørneradiusar;
kortavstand, fire fokuseffektar, ringbreidd og framdriftsstrek; hero Av, bakgrunns- og
spotlight-kjelde, tittel/logo og valfri teiknelagszoom; toppfeltval; stadfesta tilbakestilling,
kopiering og vaksenredigering av barneprofilar; åtte grupper og fast live førehandsvising.

Sluttkommando:

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug '-Pstova.screens=true'
```

Resultat på kodecommit `a60fded`: **487/487 bestått**, 0 feil og 0 hoppa over.
`AppScreensTest` **40/40**, `DesignRulesTest` **4/4**; lint **0 feil, 35 åtvaringar og 1 hint**;
`assembleDebug` OK. Ingen kodeendringar etter denne køyringa. Rålogg ligg lokalt i
`.superpowers/tilpassing-final.log`; testresultat i `app/build/test-results/testDebugUnitTest`,
lint i `app/build/reports/lint-results-debug.html` og APK i `app/build/outputs/apk/debug/app-debug.apk`.

Ingen emulatorinstallasjon, Shield-endring eller push. `app/src/main/java/app/stova/ha/**`
er urørt. Strengane er på nynorsk og engelsk i blokka `<!-- Tilpassing v3 -->`.

## Raud/grøn-evidens

| Del | Test og opphavleg feil |
|---|---|
| Migrering | `CustomizationPrefsTest`: manglande radius/ringfelt; SHARP/ROUND og strongFocus får rette nye verdiar, eksplisitte nye felt vinn |
| Radformat | Same test: manglande `rowFormats`; JSON-rundtur og isolasjon mellom to kontoar |
| Fjernkontroll | `RowEditingTest`: manglande redigeringsinngang; Meny → Flytt → opp/ned → OK/Tilbake med varig callback |
| Skjul/angre | Same test: fråvær/synlegheit og fokus tilbake, også på kort nummer to etter fokus i ei anna rad |
| Fokus og form | `CustomizationRenderingTest`: feil radius, manglande Ring + zoom og framdriftsval; native teikning viser demping av berre nabokort |
| Hero av | `HeroBackgroundChoicesTest`: heroen var framleis i treet og tok høgd; ingen hero og høg første rad etter rettinga |
| Bakgrunn | Same test: manglande kjelde-/rørslefunksjonar og bibliotekspørjingar. `MediaRepositoryTest` prøver fast kunst etter omstart/refresh, kontofjerning og favorittfilter |
| Toppfelt | `TopBarChoicesTest`: dato og synlegheitsval mangla; native test måler større klokke og dato utan klokke |
| Profil | `AppearanceTransferTest`: manglande reset/copy; bevarer målprofilen sin identitet, barnemodus og appar. `AppearanceProfileActionsTest` prøver stadfesting og profilval |
| Innstillingar | `AppearanceSettingsTest`: manglande grupper og val; alle nye verdiar prøvde med høgre/venstre/OK, og hero i førehandsvisinga blir fjerna live |
| Barneprofil | `SettingsScreenTest.adultCanEditKidAppearanceWithoutChangingAdult`: globale val og radformat blir lagra berre på barnet |
| TV-kant | `AppearanceRenderingTest.largePosterRowOverrideKeepsTheBottomTvSafeEdge`: plakat låg for lågt; heile kunsten er no innanfor 48 dp nedre trygg kant |

Dei fullførte oppgåvene og kommandoane står òg i den lokale, ignorerte ledgeren
`.superpowers/sdd/2026-10-25-tilpassing-v3.md/progress.md`.

## Skjermbilete og visuell vurdering

Ni 1920 × 1080-PNG-ar frå Robolectric med native renderer og lokale demobilete.
Dei nye Heim-bileta ventar på ferdig dekoda bakgrunn; opptaket skjer på UI-tråden.
Ingen personlege kontoar, token eller mediedata er med. Kjeldene har same filnamn under
`app/build/screens/`.

| Bilete | Vurdering |
|---|---|
| [Hero av](../images/tilpassing-hero-av.png) | To breie rader får god plass; toppfeltet held seg roleg og artworken fyller korta |
| [Stor plakatrad](../images/tilpassing-plakatrad-stor.png) | Heile store plakaten og fokusringen er innanfor nedre TV-kant; heroen krympar stabilt, og høgre del av bakgrunnen får mykje rom |
| [Ekstra rund + Lys opp](../images/tilpassing-ekstra-rund-lys-opp.png) | Tydeleg, roleg prioritering av valt kort. Visuell kontroll fann og fjerna gjennomskinlege nabokort; artworken er no ugjennomsiktig |
| [Toppfelt med dato](../images/tilpassing-toppfelt-dato.png) | Stor klokke og liten dato held navigasjonen samla til høgre. Datoen fekk lys tekst for betre lesbarheit over lys kunst |
| [Flyttemodus](../images/tilpassing-flyttemodus.png) | Ramma viser at heile rada blir flytta; eitt kort held fokus, og hintet er kort og synleg |
| [Utsjånad](../images/tilpassing-innstillingar.png) | Faktisk SettingsScreen med alle sju seksjonsval synlege, matt flate og fast førehandsvising med bibliotekkunst og fokus |
| [Fast bakgrunn](../images/tilpassing-fast-bakgrunn.png) | Annan bakgrunn enn hero-/kortkunsten, med teksttittel i staden for logo |
| [Einsfarga og minimalt](../images/tilpassing-einsfarga-minimal.png) | Ingen hero, bilete, klokke eller valfri navigasjon; Innstillingar er framleis synleg |
| [Ring + zoom, liten brei](../images/tilpassing-ring-zoom-liten-brei.png) | Reell 1,05-forstørring på kunsten utan layoutflytting; tjukk ring, luftige mellomrom og avslått radtittel |

Vurdert mot ønsket om «rom til artworken»: standarden held omtale og korttitlar av, let kunsten
dominere og brukar berre aksent til handling/fokus. Stor plakat prioriterer éi heil rad framfor
stor logo. Fast førehandsvising gjer at færre innstillingsrader er synlege samstundes; D-pad-rulling
held alle vala tilgjengelege. Dette er lokal funksjons- og layoutkontroll; kvaliteten på ekte
kunst, fysisk fjernkontroll, overscan og Shield-ytelse er ikkje målt her.

## Avklarte detaljar

- Gamle `corners`/`strongFocus` finst berre som lesekompatible felt. UI og rendering bruker dei nye vala.
- Manglande radformat og storleik arvar eksisterande format/global storleik. «Bruk standard for rada» fjernar overstyringa.
- Kopiering omfattar utsjånad og heimerader. Kontoavhengige radnøklar blir omsette på same server;
  fast bakgrunn blir vald på nytt i mottakaren sitt bibliotek, som kopidialogen forklarar.
- Rein Forstørring får ring ved redusert rørsle, slik at fokus aldri forsvinn.
- Bakgrunnszoom bruker `Animatable` lesen i `graphicsLayer`, utan animasjonslesing i komposisjonen,
  og er livssyklus- og synlegheitsstyrt. Ingen Shield-måling er gjord.
