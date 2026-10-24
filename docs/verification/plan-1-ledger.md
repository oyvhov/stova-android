# SDD ledger — plan: docs/superpowers/plans/2026-10-24-plan-1-fundament.md
Setup: Ruling: work on branch plan-1-fundament (main holds only the docs commit cf2bf57) — skill forbids implementing on main; repo was new so no worktree needed — cost if wrong: none, branch merges at finish.
Pre-flight (shared interfaces):
- T2→T5: AvatarColor (Tokens.kt, @Serializable) consumed by Profile — consistent.
- T3→T4/T5/T12: JsonFileStore(file, serializer, default, io) + writeBlocking — consistent.
- T4→T5/T6: PinHash/PinHasher/PinPolicy; PinCheck (T5) consumed by PinDialog (T6) — consistent; T6 depends on T5 order (fine, T5 first).
- T5→T7/T8: decideStart/leavingNeedsAdultPin/ProfileRepository — consistent.
- T7→T8: StovaApp renders ProfileGate "filled by Task 8". Ruling: Task 7 renders a placeholder (testTag "screen-gate") when active==null; Task 8 replaces it — cost if wrong: none.
- T9→T10/T11: AppRepository/ArrangedApps/AppCatalog — consistent.
- T10→T11: HomeViewModel.availableRows added in T11 — consistent (T11 Modify).
- T11→T12: SettingsSection.HOME_BUTTON row filled by T12; HouseholdSettings created in T12 — consistent.
- Env: Robolectric sdk=30 jar not cached (only 34/35) — will download once; fallback ruling if it fails.
Setup: Ruling: added scripts/Start-CodexHandoff.ps1 + AGENTS.md handoff rule (user request: start Codex just before the 5h window runs out) — check get_usage at each task start; handoff at 5h>=90% (reset >15 min away) or weekly>=95% — cost if wrong: Codex starts early or late; work is committed per step either way.
Usage check before Task 1: 5h 72% (resets 00:40 local), weekly 74% — continue.
Task 1: complete (commits 84c35c9..b08e485, tests: pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest → Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.4.1/userguide/configuration_cache_enabling.html)
Task 2: complete (commits b08e485..6e97772, tests: pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug → Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.4.1/userguide/configuration_cache_enabling.html)
Task 3: Ruling: java.nio Files.move(ATOMIC_MOVE, REPLACE_EXISTING) via .tmp instead of androidx AtomicFile — AtomicFile renames with File.renameTo (fails over an existing file on Windows JVM tests) and logs via android.util.Log (not mocked in plain JUnit); nio move is atomic on Android API 26+ — cost if wrong: none on device.
Task 3: Ruling: up to 5 retries (10 ms) on AccessDeniedException in moveOver — Windows Defender/indexer briefly locks fresh files (seen in concurrentUpdatesAreNotLost); no effect on Android — cost if wrong: <=40 ms worst case per write on Windows.
Task 3: Ruling: dataEmitsEachUpdate needed testScheduler.runCurrent() before update — the plan test raced (collector started after the update and saw only one value); contract of data unchanged — cost if wrong: none.
Task 3: complete (commits 6e97772..fdcfada, tests: pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest → Configuration cache entry stored.)
Task 4: complete (commits fdcfada..7702cb0, tests: pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest assembleDebugAndroidTest → Configuration cache entry stored.)
Task 5: complete (commits 7702cb0..03b3e47, tests: pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest → Configuration cache entry reused.)
Task 6: Ruling: TV_QUALIFIERS gets the nn language qualifier ("nn-w960dp-...") — Robolectric defaults to English and resolved values-en, so nynorsk assertions failed — cost if wrong: none; English UI can be tested with an explicit qualifier.
Task 6: Ruling: StovaButton gains enabled: Boolean = true (needed for the PIN OK lockout the plan tests); PinDialog wraps a reusable PinPanel — cost if wrong: none, additive.
Task 6: Ruling: added screenshot harness (Screens.kt/ScreensTest, -Pstova.screens=true, decorView.draw instead of captureToImage which times out under Robolectric) — user asked to see images along the way — cost if wrong: test-only code.
Task 6: complete (commits 03b3e47..1f9296c, tests: pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug → Configuration cache entry reused.)
Task 7: Ruling: SideRail drops the expanded parameter and expands from its own focus state (spec: opens when focus enters, closes when it leaves) — cost if wrong: one parameter to re-add.
Task 7: Ruling: AvatarBadge (planned for Task 8) and a minimal StovaIcons set (planned for Plan 4) created now because the rail needs them; Destination carries an icon — cost if wrong: none, files move earlier.
Task 7: Ruling: navigation test waits for idle between the click and Back — the Back handler is enabled by the recomposition the click causes — cost if wrong: none (test ordering only).
Task 7: complete (commits 1f9296c..f2b6604, tests: pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug → Configuration cache entry reused.)
Task 8: Ruling: gate tests seed through ProfileRepository + session.leave()/enter() instead of writing profiles.json before launch — the app reads the file asynchronously at startup, so a pre-written file races that read — cost if wrong: none (same states reached).
Task 8: Ruling: first-run test waits with waitUntil for screen-HOME — saving writes on Dispatchers.IO, which waitForIdle does not track — cost if wrong: none.
Task 8: Ruling: a kid leaving asks the PIN of the first adult that has one (spec: leaving needs an adult PIN) — cost if wrong: households with several PIN adults must use that adult; easy to widen later.
Task 8: complete (commits f2b6604..989ac78, tests: pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug → Configuration cache entry reused.)
Task 9: Ruling: AppRepository never prunes on an empty scan (test emptyScanNeverPrunes added) — an early-boot empty package list would otherwise wipe every favourite — cost if wrong: stale keys linger until the next non-empty scan.
Task 9: complete (commits 989ac78..8efa054, tests: pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug → Configuration cache entry reused.)
Task 10: Ruling: HomeScreen takes onOpenApps and shows an empty state ("Ingen favorittappar enno" + Opne Appar) — an empty home with only a clock looked broken — cost if wrong: one extra composable.
Task 10: Ruling: HomeViewModel restarts sources only when the active profile id changes (distinctUntilChangedBy id) — otherwise every favourite toggle would refetch media rows in Plan 2 — cost if wrong: sources needing other profile fields must observe the session themselves (AppsRowSource does).
Task 10: Ruling: availableRows added to HomeViewModel now (planned for Task 11) — cost if wrong: none.
Task 10: fixed Apps page opening scrolled (lazy-grid key anchoring when favourites arrive after an empty first frame) — opensAtTopWithFavorites RED→GREEN, suite 67/67.
Task 10: complete (commits 8efa054..cf67fe7, tests: pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug → Configuration cache entry reused.)
Task 11: Ruling: HOME_BUTTON section deferred to Task 12 (no empty section in between); backdrop-rotation switch deferred to Plan 2 where it has an effect — cost if wrong: none.
Task 11: Ruling: kid profile settings reach adult-only pages through Profil -> <kid> (detail page with Tillatne appar and Slett profil) with a local back stack — cost if wrong: none.
Task 11: complete (commits cf67fe7..958d1ef, tests: pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug → Configuration cache entry reused.)
Nav: Ruling: side rail removed on user request ("Skal en ikkje ha sidebar!!" + "Skal jo vere ein minimalistisk deilig launcher") — Home is full screen with a TopBar (clock, Apps, Settings, profile; labels only on focus); Apps/Settings are full-screen pages Back closes; Destination loses label/icon; spec §5 and plans 2/4/5 updated — cost if wrong: TopBar.kt is small; a rail could return as an option.
Nav: tests homeHasNoSidebarAndFillsTheScreen + topBarOpensSettings RED→GREEN, suite 72/72.
Task 12: Ruling: crash guard state lives in its own crash_guard.json store held by AppContainer (the uncaught handler writes it with writeBlocking; the service combines it with household.homeButtonOverride) — cost if wrong: none.
Task 12: Ruling: HomeButtonStatusTest runs under Robolectric because ComponentName is an Android class — cost if wrong: none.
Task 12: complete (commits 6b02a67..5c1fc66, tests: pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug → Configuration cache entry reused.)
Palette: Ruling: muted palette on user request ("meir dempa farger enn i spole") — sage-lime accent #BAC79C, text #E9E9E2, muted #9AA299, desaturated status and avatar colours; surfaces unchanged; spec §2 + Plan 1 constraint updated — cost if wrong: one token file.
Palette: schemeUsesMutedForestTokens + avatarColoursAreMutedToo RED→GREEN, suite 87/87.
Agents: Ruling: Plans 3 and 4 run in parallel as detached Codex agents (user asked "Kan du køyre fleire agenter? Og i codex?"). Plan 4 in worktree .worktrees/plan-4 (branch plan-4-home-assistant from a87fb8f); Plan 3 in Spole at C:\JellyBin\spole-launcher-link (branch feat/spole-launcher-link; Codex moved there itself after its --worktree path under the Ø profile broke Gradle). Device and manual steps excluded for both — cost if wrong: merge work later; branches are isolated.
Task 13: fixed Home service killed by UI crashes (Android marks a crashed a11y service and stops binding it) — service moved to process :homebutton reading HomeOverrideState from disk; HomeOverrideStateTest (6) RED→GREEN, verified on Stova_TV after am crash <ui pid>.
Task 13: fixed empty home had no initial focus — emptyHomeFocusesOpenApps RED→GREEN, suite 94/94.
Task 13: Ruling: Shield verification and key-filter path (no flash) not run — no Shield attached; adb input bypasses the input filter and adb emu events do not reach input on this image — cost if wrong: a brief stock-launcher flash on Shield until verified.
Task 13: Ruling: emulator locale stays English (production image, no root for setprop) — nynorsk verified in Robolectric — cost if wrong: none.
Task 13: complete (commits a87fb8f..98aa73e, tests: pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug → Configuration cache entry reused.)
Final review: Opus reviewer, cf2bf57..98aa73e. Critical 0, Important 7, Minor 17. Re-graded: all 7 Important stand.
Final: fixed I1 crash loop (guard installed before AppContainer; service-side LaunchGuard stands down after 3 launches without an alive mark written on MainActivity.onResume) — launchGuardStandsDownAfterThreeDeadLaunches RED→GREEN
Final: fixed I2 Home inside Stova did nothing (onNewIntent + homeRequests → HOME) — homeIntentReturnsToHome RED→GREEN
Final: fixed I3 empty state flashed on cold start (rows null until sources answer) — rowsAreNullUntilSourcesAnswer RED→GREEN
Final: fixed I4 PBKDF2 on main thread (injectable cpu dispatcher, Default) — pinHashingRunsOnTheCpuDispatcher RED→GREEN
Final: fixed I5 kid exit only first adult's PIN (verifyAnyAdultPin with its own kid-exit counter) — kidExitAcceptsAnyAdultPin RED→GREEN; Task 8 ruling reversed
Final: fixed I6 new PIN unconfirmed (NEW → CONFIRM step) — newPinNeedsConfirmation RED→GREEN
Final: fixed I7 prefs pruned on unavailable/renamed apps (reconcileKeys: follow rename, keep installed-but-unavailable, drop uninstalled) — reconcileRemapsRenamedKeepsUnavailableDropsUninstalled RED→GREEN; suite 94 passed + 7 skipped screenshot tests, 0 failed, lint clean
Final: minor (fixed): kid-exit check fails open while profiles file is null (one frame) — kidCannotLeaveBeforeProfilesLoad RED→GREEN; DesignRulesTest 3/3
Final: minor (fixed): PIN lockout uses wall clock; backwards jumps can show huge waits — backwardsClockJumpNeverExtendsLockoutBeyondPolicy RED→GREEN; DesignRulesTest green
Final: minor (skipped): disabled OK loses focus after a PIN attempt — already handled by Compose focus relocation; rejectedPinReturnsFocusToDigits passes before changes; see HANDOFF Småfunn
Final: minor (skipped): holding OK may click the first item of the app menu (verify on Shield) — user excludes this; requires real remote; see HANDOFF Småfunn
Final: minor (fixed): top-bar focus labels shift layout (spec: focus never changes layout) — focusingLabelsKeepsAllButtonBoundsStable RED→GREEN; DesignRulesTest green
Final: minor (fixed): clock stale up to 60 s after sleep; refresh on resume — resumeRefreshesClockBeforeNextMinuteTick RED→GREEN; DesignRulesTest green
Final: minor (fixed): AppsScreen rescans all apps on every profile edit (remember keyed on profile) — profileEditsUpdateGridWithoutRescanningPackages RED→GREEN; DesignRulesTest green
Final: minor (deferred): HomeRowSettings rows lack key(); focus does not follow moved row
Final: minor (deferred): AppArtCache never retries missing banners / refreshes updated ones
Final: minor (deferred): ensureEnabled overrides a user's own "off" for the Home service
Final: minor (deferred): JsonFileStore.read does not catch IOException (would crash at start)
Final: minor (deferred): adult types PIN twice to leave a kid profile (picker asks again)
Final: minor (deferred): PIN pad "Slett" wraps at font scale 2.0
Final: minor (deferred): container lateinit fails opaquely in :homebutton process
Final: minor (deferred): HomeOverrideState treats unreadable guard file as "on"
Final: minor (deferred): warm the JSON reader in onServiceConnected
Final: minor (deferred): "siffer skrive" → "siffer skrivne"
Final: Ruling: reviewer's declined-to-judge items (Home long-press shortcuts consumed, no shortcut to system Settings, any adult may delete another adult, PIN optional, corrupt profiles.json → first-run, favourites listed twice, launcher manifest attrs, Muted on Raised 4.9:1) stand as spec'd or unspecified — cost if wrong: small follow-ups after the user decides.
