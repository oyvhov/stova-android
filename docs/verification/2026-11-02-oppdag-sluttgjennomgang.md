# Final whole-branch review — Oppdag (Seerr through Spole)

Stova `oppdag` 9455c45..7ac3f63 (17 commits, 58 files) and Spole `launcher-discover` 37a695f..88ab573 (6 commits, 27 files). Read-only; no Gradle run, no device touched.

How I split the reading (the Stova package is ~395 KB):
1. Spec, plan (Global Constraints, Review Focus, Tasks S1–11), the full ledger, AGENTS.md.
2. Stova data layer as whole files from the worktree (all new): `DiscoverModels`, `DiscoverParsers`, `SpoleDiscover`, `DiscoverRepository`, `DiscoverRowSource`.
3. Stova UI layer as whole files (all new): `DiscoverScreen`, `DiscoverContent`, `DiscoverTitleContent`, `DiscoverRequestCard`, `RequestSheet`, `DiscoverSearchContent`, `DiscoverStrings`.
4. Stova modified files from the diff: `StovaApp`, `HomeScreen`, `HomeRows`, `HomeContent`, `AppsScreen`, `HomeRowSettings`, `SpoleHandoff`, `SpoleProfiles`, `ItemActionsMenu`, `MediaRowHeader`, `RowStyle`, `RowOptions`, `HomeDeckGeometry`, `KidsHomeContent`, `MediaRepository`, `MediaSnapshot`, `StovaIcons`, `AppContainer`, both `strings.xml`; the navigation test (discover half), the shared fake, test inventories of every discover test class.
5. Spole: `LauncherDiscover`, `LauncherDiscoverQueries`, `LauncherDiscoverActions` whole; diffs of `LauncherProfiles`, `LauncherLink`, `LauncherTarget`, `PendingLauncherOpen`, `LauncherOpenActivity`, `DetailsCoordinator`, `ReelstackViewModel`, `ReelstackUiState`, `MainActivity`, manifest, `docs/LAUNCHER_LINK.md`; test inventories; plus `EndpointValidator`, `RequestTrackingRepository` and `ServiceMessage` to settle what can escape the provider.
6. Docs (`ARCHITECTURE`, `HANDOFF`, plans README) and the eight screens.

Two things I verified outside the diff because conclusions hung on them: Compose animation 1.11.3's `AnimatedContent` class files contain no `SaveableStateHolder`/`removeState` reference, so a page's `rememberSaveable` state is dropped when `MainShell` switches destination (the `steps` stack cannot leak from one Discover visit into the next — a worry I had after reading `DiscoverScreen.kt:98`); and `InvalidEndpoint` extends `IllegalArgumentException`, so the one Spole call outside `runCatching` is forwarded over the binder rather than crashing Spole.

## Strengths

- The architecture is exactly what the spec asked for and stays modular: Stova holds no Seerr secret, asks nothing without a Spole package (`SpoleDiscover.first` iterates `installedPackages()`), nothing for a child (`identity`, `DiscoverRowSource.rows`, `AppsScreen`, `HomeRowSettings` all gate on `profile.kid`), and Spole answers only Stova (`launcherMayList` on every query and call, outsiders get the bare columns and no extras / `null`).
- The cross-repo contract is consistent end to end. Columns, row words, `status` ints, `stage` lowercase names, `seerr` extra on every discover answer (Ruling S2-a) and relied on by `SpoleDiscover.ask`, `page`/`has_more` as Int/Boolean, `tmdb`/`request_id` as `putInt`, `seasons` as `putIntArray`, result words, `kind` lowercase, `spole://discover` parameters — both sides agree and `LAUNCHER_LINK.md` matches the code (including `not_personal` on withdraw). Beta30 answers unknown paths with the extras-less profiles cursor, which Stova reads as "no answer"; an older Stova never touches the new paths.
- Failure handling is careful on both sides: every Spole `call` outcome is a word (`discoverAction` wraps in `runCatching`), rows fail one at a time without taking the others, Stova distinguishes "nobody answered" from "an installed Spole tripped" (Ruling 2-c/2-d, including the null-cursor-from-a-declared-provider case), and the repository never caches a failure.
- Privacy was thought through: title/related caches are keyed per `(link, type, tmdb)` (Ruling 2-a); the state is per profile; withdraw invalidates only pages carrying that request id.
- Focus work is unusually thorough for a TV page: first focus on the first card or «Prøv att», Back from a title lands on the card on the rows page and on the search grid, the request/withdraw round trip keeps focus on the pressed button or moves it to the new first button, the system's own first focus is not mistaken for the viewer moving (7-a), a focused request card brings its caption into view, «Hent fleire» focuses the first new title and never drops focus when the tile goes.
- Tests verify behaviour, not shapes: full-app navigation tests drive a fake Spole provider with D-pad keys and focus assertions, check wire extras (`getInt("tmdb")`, `getIntArray("seasons")`), check the `refresh=1` ordering before `discover/title`, the debounce with a frozen clock, and the no-second-search-after-Back case. Spole has offline tests against a fake Seerr transport for queries and actions. All five Review Focus items have the named tests.
- The screens read as Stova: header, hero band with facts in `onSurfaceVariant`, titles under cards, lime only on the primary button, progress bar and chosen tick, status never in accent. Emulator Home and Apps show nothing of Oppdag against an old Spole.
- Spole's cold-start handling of `spole://discover` (stand-in looked up last, filled from Seerr's answer, a dedicated refusal for a child's account) is a sound fix of a real gap found in review.

## Issues

### Critical (Must Fix)

None.

### Important (Should Fix)

1. **Stova — no timeout on any provider question.** `app/src/main/java/app/stova/discover/SpoleDiscover.kt:114-125` (`SpoleDiscoverFetch`) and `DiscoverRepository.kt:47-70`. `refresh` holds the per-profile mutex for the whole binder round trip; Home's 5-minute loop (`HomeScreen.kt:52` → `StovaApp.kt` `onRefresh`), the page's own `LaunchedEffect(link)` (`DiscoverScreen.kt:109`) and «Prøv att» (`:138`) all launch new waiters in `appScope`. A Spole stuck on a Seerr that accepts the TCP connection but never answers (Spole's own HTTP timeouts are the only bound) leaves the page on «Hentar frå Seerr …» with no way out and queues waiters. Fix: `withTimeout(15_000)` (or similar) around each `withContext(Dispatchers.IO)` in `SpoleDiscoverFetch`, letting `TimeoutCancellationException` surface as a failure (the repository already treats a throw as `failed`); note `CancellationException` is rethrown in the repository, so wrap the timeout as an `IOException`/`IllegalStateException` or catch `TimeoutCancellationException` explicitly. Ledger already marked this for the final wave; I agree it must land before merge.
2. **Stova — the search step saves every fetched title with overview and genres.** `DiscoverScreen.kt:65-71` (`DiscoverPageSaver`), `:74-79` (`savedTitle`), `:161`. A launcher is stopped on every app launch, so the activity's saved state is written often; ~20 KB per «Hent fleire» approaches the 1 MB binder limit after a long search session and `TransactionTooLargeException` kills the launcher on exit. Fix: save search items without `overview`/`genres` (the title page refetches details anyway) and/or cap the saved list (say 60 items; a title opened from further down still has its own fallback on the Title step). Ledger: final wave; agree.
3. **Stova — the withdraw sheet opens with focus on the destructive action.** `RequestSheet.kt:71-72`: `SheetAction(«Trekk tilbake», danger = true)` is the first focusable, and `StovaSheet` focuses the first child (`StovaSheet.kt:91-92`), so the remote's second OK withdraws. On a TV, a confirm sheet for a destructive step should open on the safe choice. Fix: put «Behald» first, or give «Trekk tilbake» a `FocusRequester` only after «Behald» has focus (ordering is simplest and matches the screenshot layout once swapped). The navigation test `discoverWithdrawalGoesThroughSpole` clicks by semantics, so it keeps passing. Ledger: final wave; agree.
4. **Spec drift and documentation accuracy.** (a) `docs/superpowers/specs/2026-11-01-oppdag-design.md:75` says `discover_withdraw` answers `ok, failed, unknown`; code (`LauncherDiscoverActions.personally`) and `LAUNCHER_LINK.md` also answer `not_personal` (Ruling S3-b). The spec is binding; add the word. (b) `docs/ARCHITECTURE.md` bullet «Søk blir ikkje halde» is false since Task 8 (the search step saves query and hits for Back), and the table row says «rader → tittel → søk» though the stack is rows, then title or search, then title again. (c) `docs/HANDOFF.md` top entry should name the ledger path (`.superpowers/sdd/2026-11-01-oppdag/progress.md`, git-ignored) and the final-wave items so the next agent sees them. (d) The plan's S1–S5 boxes are unticked although done (`2026-11-01-oppdag.md:55-133`); AGENTS.md says open boxes in older plans are no order, but this is the current plan.

### Minor (Nice to Have)

5. **Stova — an unknown non-null result word tells the person to update Spole.** `DiscoverModels.kt:93-101`: `discoverActionResult("some-new-word")` → `NOT_SUPPORTED` → «Oppdater Spole for å be om frå Stova.» If a *newer* Spole ever adds a word, the advice is backwards. Map a non-null unknown word to `FAILED` and keep `NOT_SUPPORTED` for `null` only (`SpoleDiscover.act:96-98` already returns `NOT_SUPPORTED` when nobody answers).
6. **Stova — search shows nothing when page 1 fails.** `DiscoverScreen.kt:178` (null answer leaves `page == null`), `DiscoverSearchContent.kt:96-101` (`note` is null while `page == null` and not loading). A tripped or absent answer after typing «Dune» is a blank grid with no word; the rows page says «Seerr svara ikkje» in the same situation. A short note (reuse `discover_failed`) when the last ask for `asked` answered nothing would do.
7. **Stova — Back from a related title lands on the first button, not the related card.** `DiscoverScreen.kt:142` pushes a new Title step; on Back the lower `TitleStep` is composed anew and `DiscoverTitleContent.kt:110-114` places focus on the first button. The rows page and search both remember their card; the title page's related rows do not. Remember the related card key per step (as `focusedAt` does on the rows page) and pass `focus`/`focusOn` to that `DiscoverTitleRow`.
8. **Stova — Apps focus and scroll are not restored after Back from Discover.** `StovaApp.kt` `AnimatedContent` keeps no saveable state and `AppsScreen.kt:104` has a bare grid, so Apps → «Oppdag» → Back rebuilds Apps at the top. Home restores its tile; Apps does not. Consistent with how Apps behaved before this branch, so not a regression, but a reasonable person expects to land on the tile they pressed.
9. **Stova — `DiscoverTitleContent.kt:126` compares the pressed button with the last button that gained focus, not the one that holds it.** Add `onButtons.value &&` so a lost focus is re-placed. Unreachable today (the sheet's dialog returns focus to the pressed button), cheap to pin.
10. **Stova — first-focus effect can pull focus from «Søk».** `DiscoverContent.kt:102-105` keys on `(start != null, showRetry)`: rows arriving while the viewer stands on «Søk», or a retry failing again, moves focus to the first card / «Prøv att». Brief and rare.
11. **Stova — freshness keyed by profile id only, caches never evicted.** `DiscoverRepository.kt:36-37,50`: a changed Spole link is forced by the row source (3-c) but `DiscoverScreen`'s own refresh is unforced; `titles`/`related` keep expired entries for the process lifetime (small, bounded by titles opened).
12. **Stova — `act` treats a package without the provider as FAILED while `ask` treats it as no answer.** `SpoleDiscover.kt:91-98` (`IllegalArgumentException: Unknown authority`). Unreachable: no request flow exists without a provider that answered.
13. **Stova — duplication.** `DiscoverRequestRow` copies `DiscoverTitleRow`'s scaffolding (`DiscoverContent.kt:217-232`); the icon-plus-label tile body is written four times (`AppsScreen.kt:131`, `HomeRows.kt:69-73`, `DiscoverSearchContent.kt:142-147`, plus «Alle appar»); the facts line is built twice (`DiscoverContent.kt:179-183`, `DiscoverTitleContent.kt:186-192`); the kid check is in two screens; `SeerrSpole` and the nav test's `DiscoverSpole` overlap. An `IconTile` and a `discoverFacts` helper would do.
14. **Stova — `HomeRowSettings.kt:47` collects the whole `DiscoverState`** to read one enum; `derivedStateOf` on access as `StovaApp` does for Apps avoids recomposing settings on every refresh.
15. **Spole — `followed()` outside `runCatching`.** `LauncherDiscoverQueries.kt:84,90`: `scope()` can throw `InvalidEndpoint` through `normalizeBaseUrl`. It extends `IllegalArgumentException`, so the binder forwards it and Stova reads it as "tripped" — no crash — but a plain `runCatching { … }.getOrDefault(emptyList())` would make the contract «a call that fails gives no rows, never an error» true as written.
16. **Spole — `critic` is not cancelled when `details` fails.** `LauncherDiscoverQueries.kt:87-92`: `runBlocking` waits for the Rotten Tomatoes call before returning null. Wasted call and latency on a failing title; `coroutineScope` + `cancel()` fixes it.
17. **Spole — `tmdb` accepts at most nine digits** (`LauncherLink.kt:52`) while Stova sends any positive `Int` (ten digits possible). TMDB ids are ~1.5 M today; note only.
18. **Spole — no-Seerr cold start of `spole://discover`** opens a sheet with the placeholder, an empty title and only «Film»/«Serie» (S4 note). Stova only offers «Opne i Spole» when Spole answered with Seerr access, so it needs a hand-built link; accepted as deferred.
19. **Tests, gaps a reasonable person could meet:** no full-app series request (seasons `IntArray` on the wire is covered only by `SpoleDiscoverTest`); no test that a provider tripping mid-session keeps the rows on the page; no test of page-2 search failure or a query shortened below two characters; `HomeContentDiscoverTest` does not reach `onOpenDiscoverTitle` through `HomeScreen`; the stale-EMPTY leave guard (`DiscoverScreen.kt:113-116`) relies on `appScope` being `Main.immediate` and effect order and is untested; deprecated `createComposeRule` suite-wide (pre-existing).

## Deferred-minor triage

Ledger lines in order; verdict per item (fix before merge | can wait | disagree).

**S1**
- year takes card metadata before `details.facts` — can wait (bare card has empty metadata, so details win where it matters).
- tv case for `can_request` at status 5/6 in `LauncherDiscoverTest` — can wait.
- pin all eight stage words over `RequestStage.entries` — can wait (`theWordsStovaReadsAreFixed` exists; extend when touched).
- status 7 not normalised on the wire — can wait (Stova maps it to UNKNOWN).

**S2**
- `followed()` outside `runCatching` (:84, :90) — can wait, but cheap; see Minor 15 (forwarded, not a crash).
- `critic` deferred not cancelled (:87-92) — can wait; Minor 16.
- follows read for BROWSE (:80, :90) — can wait (reads an empty scope).
- cookie session with blank `userId` reads a scope nothing writes — can wait.
- `!!` warning in `LauncherProfilesTest.kt:66` — can wait.
- `titleIn(uri)` computed on every path (:49) — can wait (trivial cost).
- «send»/«sendt» wording in `LAUNCHER_LINK.md` — can wait.
- (re-review) `EMPTY.cursor(path, NONE)` asserts `seerr=none`; bad-title/short-`q` assert the extra; bad `page` test — can wait.

**S3**
- withdraw filters on `stage.isRequest` instead of `!availabilityOnly` — can wait (`isRequest` is `!= WATCHING`, which is the rule the fixture models).
- tv request with no seasons costs an `auth/me` call before `failed` — can wait.
- `err_sesongane_er_endra` → `already` also covers blocklisted/nonexistent season — can wait (the person sees «Alt bede om eller i biblioteket»; acceptable).
- cookie session with blank `userId` → `failed`/`unknown` rather than `not_personal` — can wait.
- no test for a film sent with a seasons array — can wait.

**S4**
- silent no-op in `LauncherLink.kt:43` when `discoverTitle()` is null — can wait (parser guarantees the id).
- id format written in two places — can wait.
- no test for the no-Seerr cold start — can wait.
- `spole://discover` doc is a bullet, «Konto og barnemodus» table lacks the Discover exception — can wait.
- no test for a grown-up own profile from main mode / `openLauncherDiscover` — can wait.
- (re-review) `RemoteDetailsTest.kt:190` second half assertion — can wait.
- Back from the discover details stays in Spole — can wait (Spole's own navigation; declined below).

**S5**
- `RemoteDetailsTest` teardown race — can wait (pre-existing design; flake, not a wrong result).

**Task 1**
- blank `request_status` → UNKNOWN instead of null — can wait (Spole never sends blank).
- blank `title` still yields a card/request — can wait.
- `page`/`has_more` read only as Int/Boolean — can wait (that is the contract).
- no test that `parseDiscoverPage` ignores non-search rows / negative season — can wait.
- `discoverActionResult` asserted inside `requestsAndPagesAreParsed` — can wait.
- plan amendment rode along in the Task 1 commit — can wait (history).
- `media` imports `discover` and `discover` imports `media.model` — can wait (both are feature packages; no cycle at the module level).
- kind wire mapping in three places in `SpoleHandoff.kt` — can wait.

**Task 2**
- `act` FAILED vs `ask` no-answer for a missing provider — can wait; Minor 12.
- a throw from the first package lets the second be asked for request/withdraw — can wait (the first did not send; pre-existing pattern).
- details/search return null for both "nobody" and "tripped" — can wait.
- `fetchedAt` keyed by profile id only — can wait; Minor 11.
- withdraw test cannot catch over-invalidation — can wait.
- fall-through to the second Spole untested — can wait.
- title/related caches never drop expired entries — can wait; Minor 11.
- a new refresh keeps `failed = true` while `loading` — can wait (the UI reads `loading` first).

**Task 3**
- previous person's trending replays for one round trip on a person change — can wait (forced refresh follows at once).
- no HomeScreen-level test for a card click → `onOpenDiscoverTitle` — can wait (the nav test covers it end to end).
- no test that the first-collection refresh is non-forced — can wait.
- `viaSeerr`/`seerrAccount` in `media/SpoleProfiles.kt` rather than `MediaModels.kt` — disagree that it needs moving; they sit beside `spoleRowAccount`, which is the right neighbour.

**Task 4**
- failed retry pulls focus from «Søk» — can wait; Minor 10.
- «Back lands on the pressed card» proven only for the first card — can wait.
- loading state untested; `failedStateOffersRetry` does not assert first focus — can wait.
- «Søk» an empty Box until Task 8 — resolved (Task 8 shipped).
- `onLeave` unused parameter note (4 accepted) — resolved: it is used for the self-leave (Ruling 4-b/9-a); the stale KDoc in Task 9's list is what remains.

**Task 5**
- `details`/`related` not cleared when the effect key changes (`DiscoverScreen.kt`) — can wait (a step is one title; the key never changes within a step).
- restored step without its card draws an empty headline until details arrive — can wait (process-death path only).
- no screen-level test for details/related loading, `openInSpole` mapping, related push — can wait.
- `enabled = !busy` untested — resolved by 7-b (buttons stay enabled).
- facts/THEME duplication between hero and title page — can wait; Minor 13.
- Back from a related title focuses the first button — can wait, but worth doing; Minor 7.
- without a Spole link «Opne i Spole» does nothing — can wait (the page cannot have details without a link).

**Task 6**
- stale-EMPTY leave guard relies on `Main.immediate` and effect order, untested — can wait; Minor 19.
- access with empty rows sets `failed` and still asks for requests — can wait (one extra question on a bad day).
- fire-and-forget refresh without timeout — **fix before merge**; Important 1.
- `DiscoverRequestRow` copies scaffolding — can wait; Minor 13.
- `backdropFor` checks the request's own backdrop first — can wait (harmless, future-proof).
- a request focused before the first picture loads drops it — can wait.
- Back-to-request-card memory and `distinctBy { key }` untested — can wait.
- `StovaAppNavigationTest` not re-run after 9db7c10 — resolved (Task 10's full run: 1034 tests, 0 failures).

**Task 7**
- withdraw sheet opens on the destructive action — **fix before merge**; Important 3.
- keep-branch compare `onButtons.value &&` — can wait, cheap; Minor 9.
- withdraw failures reuse request wording for ALREADY/NOT_PERSONAL/NOT_SUPPORTED — can wait (NOT_PERSONAL cannot reach a visible «Trekk tilbake»: BROWSE access yields no `request_id`).
- busy-end placement can miss a one-frame answer — can wait.
- `busy` belongs to the title step (reopen allows a second send; Spole answers ALREADY) — can wait.
- stale withdraw sheet if a `requestId` came back — can wait.
- «Avbryt»/«Behald» skip the sheet fade — can wait (same as other in-sheet close buttons).
- poster decode size assumes 2 px/dp (`RequestSheet.kt:85-86`) — can wait (a 1.5× TV gets a slightly larger decode, nothing more; `LocalDensity` would be tidier as `TitlePoster` does).
- no full-app series request / double-OK / non-DONE test — can wait; Minor 19.
- `ThemePaletteTest`/`DesignRulesTest` not re-run after the fix — resolved (full run).

**Task 8**
- search saved-state size — **fix before merge**; Important 2.
- debounce upper bound not really asserted — can wait.
- "no second search after Back" check can race the IO thread — can wait (watch for flakes).
- no feedback or retry when page 1 fails; no-hits text names `asked` rather than `answered` — can wait, but recommended; Minor 6.
- no test for shortening below 2 characters or a page-2 failure — can wait.
- deprecated `createComposeRule` — can wait (suite-wide, pre-existing).

**Task 9**
- Apps focus/scroll not restored after Back from Discover — can wait; Minor 8.
- self-leave from Apps and its guard untested; only Settings → Apps asserted; access flipping to NONE with the tile focused untested — can wait.
- stale `DiscoverScreen` KDoc about `onLeave` — resolved at 7ac3f63 (`DiscoverScreen.kt:89-91` now describes the self-leave).
- icon-plus-label tile copied four times — can wait; Minor 13.
- kid check in two screens — can wait.
- `HomeRowSettings` collects the whole state — can wait; Minor 14.
- `SeerrSpole` duplicates the nav test's fake — can wait.

**Task 10**
- HANDOFF names the ledger path and the three final-wave items — **fix before merge** (part of Important 4; one commit with the docs).
- ARCHITECTURE table wording and «Søk blir ikkje halde» — **fix before merge**; Important 4.
- plan S1–S5 boxes unticked — **fix before merge**; Important 4 (trivial).
- spec drift on `discover_withdraw` — **fix before merge**; Important 4.

**Rulings I checked and find sound:** R1, S1-a/b/c, S2-a/b/c, S3-a/b/c/d, S4-a/b/c, 1-a, 2-a/b/c/d, 3-a/b/c, 4-a/b, 5-a, 6-a, 7-a/b, 9-a, 10-a. The only one I would amend is 1-a's "unknown word → NOT_SUPPORTED" (Minor 5): keep NOT_SUPPORTED for *no* word, use FAILED for an unrecognised one. Ruling S2-a (the `seerr` extra on every path) has become load-bearing on the Stova side (`SpoleDiscover.ask` requires it on every answer); it is documented in `LAUNCHER_LINK.md`, so fine, but the spec table still names it only on `discover` — worth one clause there with Important 4(a).

## Declined to judge

- The profile's age limit is not applied anywhere in Oppdag (row, page, search, related) — the spec names it as a known limitation for this version.
- Trailer, person pages, genre pages, 4K, quality/folder choice, the bell — spec «Ikkje med no»; «Opne i Spole» is the way there.
- Back from Spole's Discover details staying in Spole rather than returning to Stova (S4 note) — Spole's own navigation, accepted in the ledger; not part of this plan.
- `launcherMayList` identifies the caller by package name only (`LauncherApproval.LAUNCHERS`) — the pre-existing provider contract; the OS binds `callingPackage` to the calling uid, and this branch did not change it.
- Both installed Spole packages (release and debug) asked in turn and the first with an answer winning — the pre-existing `SpoleHandoff` pattern that Spole rows already follow.
- The refresh cadence (Home loop every 5 min, repository min age 10 min, titles 5 min, `refresh=1` only when the requests' look is older than 10 min) — set by the plan.
- Toast as the channel for the request's answer — spec: «ein kort tekst nedst».
- «Dine førespurnader» includes follows at stage `watching` (captioned «Følgjer») — spec lists `watching` among the stages; not a request but part of Spole's followed list.
- Emulator check covered "old Spole" rather than "no Spole" (Ruling 10-a) — the unit test covers the no-package path; the real-device case is Task 11.
- Commit trailers crediting another model (Ruling S2-c) — history, not code.
- `RemoteDetailsTest` teardown race (S5) — pre-existing test design in Spole.
- Season sheet has no «Avbryt» row while the film sheet has one — Back closes both; a style choice within the sheet pattern, not a defect.

## Recommendations

1. One fix wave, in this order: fetch timeout (Important 1), search saved-state payload (2), withdraw sheet focus (3), spec/docs/plan ticks (4). All four are small and self-contained; a focused run of `StovaAppNavigationTest`, `RequestSheetTest`, `DiscoverRepositoryTest` and `DesignRulesTest` covers them.
2. Take Minor 5, 6, 7 and 9 in the same wave if time allows: each is a few lines and each removes a thing a person on the sofa could meet (wrong advice, a blank search, a focus jump).
3. Task 11 on the TVs — verify specifically: (a) Home with the real Spole beta30 still installed shows no Oppdag and `adb logcat` shows no provider query beyond `rows`/`profiles`; (b) after installing Spole `launcher-discover`: the row appears for Øyvind, the Apps tile, the page's first focus, Back from a title to the pressed card, «Opne i Spole» opening the correct title cold; Eilev (kid) sees nothing anywhere including Settings → rows; (c) a title «I biblioteket» shows no «Be om»; a missing film shows it (no real request without the owner's yes); (d) the `browse` state if an API-key Seerr is available: hint shown, no «Be om», tile present; (e) Loft TV (PowerVR): scroll the page and the search grid for jank with real TMDB w500/w1280 images — the hero's `softEntrance` and the full-screen backdrop are the only extra layers over Home, but this GPU is the weak one.
4. What a real Seerr can do that the fakes did not: `status` 7 (deleted) arrives as UNKNOWN (fine); a series at status 4 (partly) with every season already requested yields no «Vel sesongar» (intended); an expired personal session makes Seerr answer 401 → rows come empty → «Seerr svara ikkje» on the page and `forbidden` on a request, which is the right word but gives no hint to sign in again in Spole — consider mapping `forbidden` to a sentence that names Spole; genre names come in the server's language, not Stova's; `year` is parsed from Spole's metadata text and may be missing for upcoming titles; follows from before Spole kept `request_id` cannot be withdrawn from Stova (documented); image hosts other than TMDB are not a concern because Spole sends full URLs.
5. Spole beta31 release only on the owner's order, with the `versionCode` bump the plan names; the Stova `-Tv` build should be installed after Spole so the first Home refresh sees the new provider.

## Assessment

**Ready to merge?** With fixes

**Reasoning:** The feature is complete across S1–S5 and 1–10, the contract agrees on both sides including the backward-compatible paths, and the code is calm, modular and well tested; what remains are four bounded items (a missing timeout on the blocking provider calls, an unbounded saved-state payload in search, a destructive default focus, and spec/docs drift) that belong in the planned single fix wave before the branch lands.
