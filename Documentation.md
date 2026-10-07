# Documentation.md — MakeFlow change log

Living record of code changes, errors encountered, and fixes applied.
**Newest entries go first.** Each entry: date (UTC), scope, what changed and why,
plus verification status.

Conventions: `path:line` for code refs, commit hashes where available.

---

## 2026-10-07 — Strengthen type hierarchy and unify motion

- **Scope:** shared typography and motion tokens, navigation, dashboard, onboarding,
  transaction feedback, history, and expandable content.
- **What:** separated display, heading, title, body, and label roles with clearer size
  and weight steps, including a declared SemiBold font face. Standardized navigation,
  content-size, progress, value, and reduced-motion specs in the shared transition
  system. Applied them to onboarding paging, tab and FAB transitions, balance changes,
  staggered reveals, selection colors, anomaly banners, swipe feedback, and expanding
  rows; centralized stagger and movement ratios. Screen and section titles now carry
  clearer emphasis.
- **Verification:** `:app:compileDebugKotlin test --rerun-tasks` passed with the bundled
  Android Studio JDK and Gradle 9.3.1. `git diff --check` passed. Device screenshot
  verification was unavailable because `adb` is not installed in this environment.

---

## 2026-10-07 — Standardize sheets and rebuild onboarding

- **Scope:** shared modal surfaces, date selection, onboarding, and account setup.
- **What:** routed app prompts and date pickers through the shared Material 3 bottom
  sheet pattern on compact and larger layouts. Added a reusable alert-sheet layout and
  consistent themed surface, spacing, and scrolling. Reworked onboarding into a guided
  three-step flow for a quick app preview, profile/currency, and starter accounts, with
  add-account and currency selection sheets, inline validation, save feedback, haptics,
  and reduced-motion-aware paging. New accounts use the app's signature accent.
- **Fixes:** the first compile caught an inaccessible explicit `weight` import in the
  shared sheet; removed it and relied on the `RowScope` extension.
- **Verification:** `:app:compileDebugKotlin test --rerun-tasks` passed with the bundled
  Android Studio JDK and Gradle 9.3.1. `git diff --check` passed. Device screenshot
  verification was unavailable because `adb` is not installed in this environment.

---

## 2026-10-07 — Unify the app around one signature color

- **Scope:** shared Material theme, Settings, saved preferences, Glance widgets, and
  theme-resolution tests.
- **What:** replaced selectable/dynamic palettes with a fixed chartreuse brand accent,
  warm ivory light surfaces, sage containers, and olive-ink text. Dark and AMOLED modes
  remain available, with a consistent accent and semantic status colors. Removed palette
  selection from Settings, ViewModel, DataStore, and app startup; existing saved palette
  values are now ignored. Added night-qualified widget and splash colors.
- **Verification:** `:app:compileDebugKotlin test` passed with the Android Studio JDK
  and Gradle 9.3.1. The first token refactor surfaced duplicate legacy alias names;
  the role tokens were renamed before the passing run.

---

## 2026-10-07 — Polish Home around the mockup and complete M-PESA review import

- **Scope:** compact Home dashboard, shared banner/button styling, primary navigation,
  onboarding copy, and transaction import handoff.
- **What:** aligned the phone Home layout to the reference with a compact greeting and
  avatar, compact currency balance, naturally sized actions, horizontal weekly summary,
  emphasized yellow import banner, and unboxed recent transaction rows. Recent rows now
  prioritize merchant notes and show category/account context without dates; navigation
  icons match the Home, Insights, Budget, and Goals references. Added a paste-and-review
  M-PESA path that uses the existing parser, duplicate check, and transaction editor,
  without implying SMS inbox access. Updated onboarding to describe share/paste import.
  Balance changes use shared reduced-motion-aware transition specs; action haptics remain
  connected through the existing haptic manager. Compact currency formatting is covered
  by unit tests.
- **Fixes:** the initial compile found a stale composable argument and a removed shared
  divider import still used by dashboard modules; both were corrected. Kotlin also fell
  back from a damaged incremental cache entry to a full compile.
- **Verification:** `:app:compileDebugKotlin test` passed with the Android Studio JDK
  and Gradle 9.3.1. Device screenshot verification was unavailable because `adb` is not
  installed in this environment.

---

## 2026-10-03 — Unify app screens around the mockup system

- **Scope:** Shared amounts, navigation dock, primary tab actions, Accounts, and Settings.
- **What:** matched title/amount emphasis and transaction type selection to the mockups;
  unified bottom-dock insets across Home, Insights, Budget, Goals, and History; animated
  dock selection with reduced-motion support; routed its add action to transaction,
  budget, or goal entry by active tab; removed duplicate primary-screen add buttons;
  prevented Insights metric truncation; aligned Settings palette selection, switch rows,
  and notices with the unboxed reference; and rebuilt compact Accounts around initials,
  per-account balances, editable exchange rates, and a fixed add action while preserving
  expanded layouts. Compile checks found missing `fillMaxHeight` and
  `ExperimentalFoundationApi` opt-ins plus an invalid mixed `PaddingValues` overload;
  all were corrected.
- **Verification:** `:app:compileDebugKotlin test` passed with the cached JDK 21 and
  Gradle 9.3.1.

---

## 2026-10-03 — Match Budget and Goals tabs to their references

- **Scope:** Primary Budget and Goals destinations.
- **What:** restyled monthly budget usage around the total planned/spent values, an
  adaptive progress indicator, threshold warning, and unboxed editable rows. The primary
  Goals tab now uses the mockup title and hides its secondary-screen back affordance.
  Compile validation caught optional composable-lambda typing errors in the two header
  conditions; both were corrected.
- **Verification:** `:app:compileDebugKotlin test` passed with the cached JDK 21 and Gradle 9.6.

---

## 2026-10-03 — Align core screens with the mockups

- **Scope:** System bars, primary navigation, Home balance actions, History/Insights
  layouts, Settings appearance, and shared mockup buttons.
- **What:** restored readable status and navigation bar icons in light and dark themes;
  made Home, Insights, Budget, and Goals the four primary destinations, with History
  remaining reachable from Recent; removed the Home balance card treatment; added
  one-line action labels; and simplified Settings rows while exposing theme palettes as
  direct swatches. History keeps the primary dock available and reserves its height;
  Insights uses the same adaptive dock inset. Insights now opens with spending trend, a cash-flow
  projection, frugal/savings summaries, budget-pace warnings, and selected-month PDF
  export while retaining its detailed breakdowns below. A compile check caught a missing
  Compose `clip` import for the chart bars; the import was added. Projection arithmetic
  now uses the existing cash-flow domain helper, with direct tests for positive and
  negative monthly outcomes.
- **Verification:** `:app:compileDebugKotlin test` passed with the cached JDK 21 and
  Gradle 9.6.

---

## 2026-10-03 — Match Add Transaction to the mockup

- **Scope:** Add Transaction screen, calculator expression state, recent amount shortcuts,
  transaction commit flow, and UI strings.
- **What:** reordered the form around the mockup's type selector, grouped amount and
  expression, smart category/account chips, usual amounts, open numeric keypad, and
  pill-shaped Save action. Usual amounts now come from recent matching transactions,
  falling back to familiar amounts when there is no history. The default type is Expense.
  Routed save, duplicate confirmation, anomaly feedback, and edits through
  `TransactionCommitHandler`; blocked invalid, negative, incomplete, and account-less
  saves; and replaced the keypad's overlapping tap/long-press handling with a single
  combined gesture. Added calculator expression coverage and localized the new labels.
- **Verification:** `:app:compileDebugKotlin test` passed with the cached JDK 21 and
  Gradle 9.6. The first compile identified the need to opt the long-press gesture into
  Compose's experimental foundation API; that was fixed before the passing run.

---

## 2026-10-03 — Redesign the Home dashboard around daily money actions

- **Scope:** Home dashboard, dashboard view model, weekly budget calculation, and tests.
- **What:** reshaped Home around a personalized greeting, adaptive total balance, quick
  expense and transfer actions, weekly budget feedback, and recent transactions. Weekly
  spend is calculated from dated expenses and budget categories instead of the app's
  cumulative budget counter; added tests for date, category, and focused-account filtering.
  Existing dashboard modules remain below the primary content.
- **Verification:** `:app:compileDebugKotlin test` passed with cached JDK 21 and
  Gradle 9.6. An intermediate compile caught a misplaced modifier declaration in the
  weekly card; it was moved into the correct composable before the successful run.

## 2026-10-03 — Add shared mockup components and compact navigation dock

- **Scope:** shared Compose controls, app dimensions, compact navigation, and UI strings.
- **What:** introduced reusable amount, transaction row, action button, banner, progress,
  switch row, and pill navigation components using design tokens. The compact phone dock
  now combines the app's three real tabs with a one-tap add action; adaptive navigation
  rails remain unchanged for larger layouts.
- **Verification:** `:app:compileDebugKotlin test` passed with the cached JDK 21 and
  Gradle 9.6. An initial compile caught stale/unavailable Compose extension imports in
  the new controls; those were removed or replaced with supported click semantics.

## 2026-10-03 — Add mockup theme foundation

- **Scope:** `ui/theme/`, DM Sans font resource and license, theme resolution tests.
- **What:** matched the light and dark mockups with white/black backgrounds, neutral
  surfaces, green actions with black text, and a yellow alert surface with black text.
  Added shared dimensions for pill buttons, cards, chips, progress, and large amounts;
  updated Material typography to use DM Sans and aligned the shared shapes. Kept the
  other palettes, dynamic color, and AMOLED handling available.
- **Verification:** `:app:compileDebugKotlin test` passed using the cached JDK 21 and
  Gradle 9.6. The first offline attempt could not find a cached AAPT2 artifact;
  rerunning with dependency access resolved it and the full compile and unit suite passed.

## 2026-10-03 — Project docs created (`README.md`, `AGENTS.md`, `Documentation.md`)

- **Scope:** repo root (new files, no code touched).
- **What:** added `README.md` (overview, tech stack table, features, structure, build/test
  commands, ML workflow), `AGENTS.md` (mandatory agent procedures: read-first, exact
  edits, verify-by-execution, log changes, commit+push hygiene, design-token-only UI,
  Room-migration + test rules), and this file.
- **Basis:** full exploration of `data/` (Room v20, 15 entities, 15 DAOs, 19 migrations,
  backup/restore, DataStore keys), `ui/` (24 screens, state-driven navigation, 6 theme
  files, 8 ViewModels), `domain/` (19 pure-Kotlin objects), `core/` (ml, haptics,
  notifications, lock, logging, layout, device), `widget/` (6 Glance widgets),
  `train_category_classifier.py`, `PLAN.md`, and the `app/src/test` (38 classes) +
  `app/src/androidTest` (5 classes) suites.
- **Verification:** n/a (markdown only). Committed and pushed with the fix below.

## 2026-10-03 — Fix: missing `LaunchedEffect` import in `AccountsScreen.kt`

- **Error:** `e: .../ui/screens/AccountsScreen.kt:98:5 Unresolved reference 'LaunchedEffect'`
  (used at `AccountsScreen.kt:99` to refresh widgets when `accounts` changes).
- **Fix:** added `import androidx.compose.runtime.LaunchedEffect`
  (`app/src/main/java/com/makemission/makeflow/ui/screens/AccountsScreen.kt:41`).
- **Verification:** static cross-check — all 8 `androidx.compose.runtime` symbols used in
  the file now have matching imports, zero missing. Full Gradle compile not possible in
  this environment: no JDK (`JAVA_HOME is not set`). Rebuild in Android Studio to confirm.

## 2026-10-03 — Local work in progress (uncommitted at time of writing)

- `app/src/main/AndroidManifest.xml` and `.idea/misc.xml` carry uncommitted local
  modifications; commit `38347b4 Improve dashboard and add adaptive widgets` (15 files,
  HomeDashboard/widgets/onboarding/animations) sits on `main` ahead of `origin/main`.
  Agent docs commits do not touch these paths.

## 2026-10-03 — History: `Backup1` merged onto `main`, pushed

- `main` was a stub (`Initial commit`: `.gitignore` + `LICENSE` only). Latest branch by
  date was `origin/Backup1` (`8ab9368`, 83 commits). Merged with
  `--allow-unrelated-histories` into `3411a96 Merge Backup1 into main...`,
  `.gitignore` add/add conflict resolved as a union (kept `Backup1`'s selective `.idea/`
  ignores so tracked IDE configs stay tracked, plus `main`'s keystore/`google-services`/
  profiling entries), `LICENSE` retained. Pushed `0715fb7..3411a96 main -> main`.

## Notable history (from `git log`, for context)

- `8ab9368` Design-system migration: hardcoded values → Material 3 tokens, advanced theming.
- `0fc8c56` Production polish; account-focus filtering across dashboard/history/insights.
- Splash Screen API + initial-load optimization; centralized motion system with reduced-motion.
- Release changelog system + onboarding; suspending backup/restore refactor.
- Room migration test suite; TensorFlow Lite → LiteRT migration.
- Self-learning categorization (on-device retraining + correction memory).
- Savings goals with feasibility analysis; quick-add transaction system + validation.

## Known environment / repo issues

- Agent sandbox has **no JDK**: `./gradlew` fails with `JAVA_HOME is not set`. Builds and
  instrumented tests must run in Android Studio or a JDK-equipped machine.
- `err.txt` (1 MB binary) and `.kotlin/errors/` dumps are committed accidentally in
  history; per `AGENTS.md` these paths must not be committed going forward.
- Display name is "Mindful / Mindful Wallet" while the package stays
  `com.makemission.makeflow` — keep both in mind when editing launcher/widget strings.
