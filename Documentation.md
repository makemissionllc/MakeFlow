# Documentation.md — MakeFlow change log

Living record of code changes, errors encountered, and fixes applied.
**Newest entries go first.** Each entry: date (UTC), scope, what changed and why,
plus verification status.

Conventions: `path:line` for code refs, commit hashes where available.

## 2026-10-09 — Add calendar-aware History filters

- **Scope:** quicker transaction lookup without scrolling through all history.
- **What:** added an accessible date-range bottom sheet with All time, Today, Last 7 days,
  This month, and Last month. Date filters compose with search, type, and the focused account,
  including transfer destinations. The shared sheet and themed filter chips use existing design
  tokens and haptic feedback. Empty results offer a reset that preserves the focused account.
- **Calendar correctness:** ranges use local midnight boundaries and an exclusive end,
  handling month/year changes, leap years, and daylight-saving transitions. Returning to
  History or reopening the date sheet refreshes relative dates and day headers.
- **Release:** Community `2.0.5-community` / code 8; updated the in-app changelog and README.
- **Verification:** `:app:test`, `:app:compileDebugAndroidTestKotlin`, and the minified
  `:app:assembleRelease` build passed, as did `git diff --check`. Added eight calendar
  tests and four ViewModel regressions for combined filters, reset, day/time-zone refresh,
  and transfer destinations. Four sheet interaction/accessibility tests compiled; they
  were not run because no Android device was connected.

---

## 2026-10-08 — Add Log again for existing transactions

- **Scope:** repeat common purchases, payments, income, and transfers from the transaction editor.
- **What:** added an icon-and-text "Log again" action that retains the current form details,
  switches to new-entry mode, and sets today's date/time. Saving creates a new transaction through
  `TransactionCommitHandler`, including duplicate checks and existing save feedback, without copying
  the original identity or M-PESA reference. Transfer values use current conversion rates.
- **Related fixes:** Save requires accounts that still exist, and transfer writes now use the
  selected source account as `accountId`. Repeat mode and date survive saved-state restoration.
- **Release:** Community `2.0.4-community` / code 7; updated the in-app changelog and README.
- **Verification:** unit tests and all four `RepeatTransactionTest` regressions passed on the
  connected Android 16 phone: fresh identity/date/reference, duplicate confirmation, transfer
  conversion, and a missing account. The minified `:app:assembleRelease` build and
  `git diff --check` also passed.

---

## 2026-10-08 — Improve History lookup and PDF statements

- **Scope:** faster transaction lookup and legible, shareable PDF statements.
- **What:** History search matches notes, amounts, transaction type, category, account, and
  external reference while respecting the existing type/account filters. Empty results distinguish
  a search miss from a new account and offer a one-tap reset. PDF exports now resolve account and
  category labels, wrap long text across rows/pages, align amounts, format totals, and show the
  inclusive date range. Files use a date-based `MakeFlow_Statement` name, save to
  `Downloads/MakeFlow` on Android 10+, and open the system share chooser; Android 9 uses app
  Documents storage and a granted FileProvider URI. Added search, PDF builder, and filename tests.
- **Issue reviewed:** `Images/Issues/Mindful_Statement_20261008_083358.pdf` showed transaction
  columns colliding because IDs and descriptions were drawn as unbounded single-line text.
- **Release:** advanced Community to `2.0.3-community` / code 6 and added both improvements to
  the in-app changelog.
- **Verification:** `:app:test`, `:app:compileDebugAndroidTestKotlin`, and
  `:app:assembleRelease` passed. Instrumented tests compiled but were not run on a device.

---

## 2026-10-08 — Fix Settings haptic crash and add Community permission setup

- **Scope:** Community first-run access setup and the Settings scroll crash reported from
  an Oppo device.
- **Crash diagnosis:** the attached trace shows `NoSuchMethodError` for the Compose
  Foundation `FlowRow` overload in `SettingsScreen.kt:909`, reached while composing the
  haptic choices during LazyColumn prefetch. Replaced that call with stable two-row layout;
  the failure was a runtime Compose ABI mismatch, not a Settings scroll-state exception.
- **Permission setup:** added a one-time, optional access sheet for Community users, also
  reachable from Settings. It clearly explains the separate SMS and notification prompts,
  requests only `RECEIVE_SMS` and Android 13+ `POST_NOTIFICATIONS`, enables M-PESA review
  only when both are allowed, and leaves weekly reminders opt-in. Other system-mediated
  features are called out as not needing blanket permissions. Dismissal is remembered in
  DataStore, and the existing widget offer waits until this sheet is completed or skipped.
- **Release:** incremented the Community package to version `2.0.1-community` / code 4 so
  existing 2.0.0 Community installs can accept the update. Added the changes to the in-app
  changelog.
- **Verification:** `:app:test`, `:app:compileDebugAndroidTestKotlin`, and
  `:app:assembleRelease` passed; `git diff --check` passed. Android tests were compiled but
  not executed on a device in this environment.

---

## 2026-10-08 — Stabilize Settings scrolling and replace the profile glyph

- **Scope:** compact home header settings entry and Settings list scrolling on `community`.
- **What:** replaced the tappable profile initial with the settings gear. Removed the scroll-
  state-driven `LocalReducedMotion` override, which restarted staggered row animations when
  scrolling began or ended; Settings now respects the user's reduced-motion preference
  without changing animation mode during a gesture. Updated the scroll regression test to
  perform repeated upward swipes before checking the last settings row.
- **Verification:** `gradle test --offline` passed and `:app:compileDebugAndroidTestKotlin`
  passed with the repeated-swipe regression. `git diff --check` passed. `adb` is unavailable
  in this environment, so the instrumented test could not run on a device and the user's
  original crash was not reproduced against a live build.

---

## 2026-10-08 — Separate Play and Community release branches

- **Scope:** distribution-specific feature availability after the release branches were
  created from the same 2.0.0 commit.
- **What:** make `community` a full-feature build by default, with its own app ID and version,
  the explicit M-PESA permission/receiver, and the opt-in setting enabled. This branch no
  longer needs a flavor selector. `play` will build as its own restricted single-variant
  branch, without SMS access. Updated release instructions to build the correct artifact on
  each branch.
- **Verification:** unit tests passed; Android test sources compiled; `:app:assembleRelease`
  completed and produced an unsigned GitHub APK. Its merged manifest reports version
  `2.0.0-community` and includes `RECEIVE_SMS` plus `MpesaSmsReceiver`. An initial offline
  release-lint attempt could not resolve uncached Compose desktop metadata; rerunning with
  dependency access resolved the artifacts and release lint/build passed. Play branch
  verification is still pending.

---

## 2026-10-07 — Prepare the 2.0.0 release and complete first-run follow-up

- **Scope:** Play release metadata, Settings scrolling, onboarding, widget discovery,
  optional finance reminders, and privacy-policy access.
- **What:** moved the Play app to version 2.0.0 / code 3. Added Play and Community product
  flavors from one source branch: Play does not declare `RECEIVE_SMS`; the separately named Community
  release uses the existing opt-in receiver and `.community` application ID. The Community
  package is intended for GitHub side-loading and not Play submission. Onboarding now includes a skippable
  practical tour; after setup, a one-time sheet offers Android's supported widget-pinning
  flow and uses the optional profile name. Added opt-in weekly finance-review notifications
  through WorkManager, with a seven-day first delay, a dedicated notification channel, and
  a name-aware message. The in-app privacy page now opens
  `https://makemission.gt.tc/privacy`.
- **Settings scroll:** staggered entrance effects now settle immediately when the user is
  actively scrolling, and an instrumented regression test scrolls to the final privacy row.
  No device crash trace was available, so the original device-specific trigger still needs
  confirmation on a representative phone.
- **SMS policy:** the Play release manifest has neither `RECEIVE_SMS` nor its receiver. Current Play
  guidance does not allow alternate APIs to derive SMS content, so a notification listener
  or accessibility service is not an acceptable workaround. User-initiated share/paste
  import remains the Play-compatible path. SMS Retriever is not used for M-PESA because
  its app-hash-matched message must be sent by a system MakeFlow controls; SMS User Consent
  is a one-message OTP flow, not persistent transaction monitoring. Only the Community
  flavor uses the disclosed direct permission flow.
- **Fixes:** moved the Community app label from a disabled custom `resValue` path into its
  flavor resource file. Fixed onboarding widget-sheet lambda typing, a missing layout import,
  and a misplaced privacy-screen list item found during the first compile. Android-test
  compilation then exposed a stale savings-goal/account test API; updated it to the current
  default-currency and four-argument save callback.
- **Verification:** `gradle test --offline` passed for Play and Community. Both Android-test
  source sets compiled; both release AAB tasks completed. The merged Play manifest contains
  no SMS permission or receiver; Community contains both. `git diff --check` passed. There is
  no attached device/ADB here, so instrumented tests and device QA were not run. The public
  policy URL, Play Console declarations, production signing, and store submission remain
  publisher-side checks.

---

## 2026-10-07 — Prepare the 1.1.0 Play release candidate

- **Scope:** first-run onboarding, app-wide haptics, privacy disclosures and backup
  behavior, release manifest, versioning, store materials, and release readiness checks.
- **What:** made onboarding name entry optional and clarified that exchange-rate requests
  use currency codes while transaction records stay local. Added saved Off, Soft, Standard,
  and Strong haptic levels; all app entry points now apply the selected level, and waveform
  pauses remain silent. Added an in-app privacy page, public policy copy, Play listing draft,
  and a publisher checklist. Updated the build and in-app changelog to version 1.1.0 / code 2.
- **Privacy and Play policy:** Android-managed backups are disabled and backup/extraction
  rules exclude finance data. The Play release removes `RECEIVE_SMS` and the SMS receiver;
  local debug builds retain the opt-in development path, while share/paste import remains
  available in Play. The SMS policy note now reflects this release decision.
- **Verification:** unit tests passed. The release bundle and lint-vital checks passed; the
  merged release manifest was inspected and contains no `RECEIVE_SMS` or
  `MpesaSmsReceiver`. XML parsing and `git diff --check` passed. The environment has only
  Java 25 while the repository pins Java 21, so Gradle was run with a temporary Java 25
  daemon selection (Kotlin reported its JVM target fallback); the repository's Java 21 pin
  was restored afterward. The produced AAB is unsigned. Play signing, real store contact,
  public policy URL submission, listing assets, Console declarations, and device testing
  remain publisher-side steps.

---

## 2026-10-07 — Add receipt scanning and monthly reflections with Gemini Nano

- **Scope:** transaction entry, monthly finance review, on-device GenAI helpers, and unit
  coverage.
- **What:** added a photo-picker receipt flow that downsamples the selected image in memory
  and asks Gemini Nano for the final total and merchant. A consistent bottom sheet previews
  the suggestion; only after the user accepts are the amount and note placed in the regular
  editable transaction form. No receipt image is persisted or uploaded. Added an optional
  monthly-review page that generates a short reflection from aggregate numeric data
  only, plus a system share action for the recap. Generated text is ephemeral and never
  replaces the deterministic figures.
- **Availability and privacy:** both experiences require an explicit user action, check
  Prompt API feature status, and do not trigger model downloads. Unsupported or temporarily
  unavailable devices retain the manual transaction and standard monthly review flows.
- **Fixes:** the first integration compile caught a mismatched receipt-result mapper and a
  missing shared-sheet import; both were corrected before verification.
- **Verification:** `./gradlew test` passed, including debug Kotlin compilation and the new
  receipt/reflection unit tests; `git diff --check` passed. Runtime inference and visual QA
  still require a compatible Android device.

---

## 2026-10-07 — Improve M-PESA parsing with optional on-device Nano assistance

- **Scope:** M-PESA confirmation parsing, shared-message review, and unit coverage.
- **What:** expanded local parsing for case-insensitive/spaced `Ksh` amounts, timestamp
  variants without the word “at”, and agent cash withdrawals. Reversal and service/balance
  notices stay out of transaction classification. Audited the user-provided local sample
  export: all 5,025 confirmation messages now parse; 5,011 receive a known transaction type
  and 14 remain conservatively unknown. The sample files and temporary audit harness are
  not part of the app or commit.
- **Nano assistance:** added the beta ML Kit Prompt API as a foreground-only fallback for
  shared messages on supported devices where Gemini Nano is already available. No model
  download is triggered; background SMS processing never invokes AI. Candidate type and
  reference are validated against source text, numeric fields still come from local parsing,
  and the review screen marks AI-assisted suggestions for user verification. This does not
  train on or upload personal SMS.
- **Verification:** focused parser tests, the local corpus audit, and `./gradlew test`
  passed; `git diff --check` passed. Gemini Nano runtime behavior still requires a
  compatible physical device and was not verified in this environment.

---

## 2026-10-07 — Add opt-in M-PESA SMS detection with review-before-save

- **Scope:** SMS permission handling, M-PESA sender validation, review notifications,
  Settings consent, and transaction-editor handoff.
- **What:** added an off-by-default `RECEIVE_SMS` flow for new broadcasts only. It accepts
  the exact M-PESA sender ID, parses matching confirmation messages locally, suppresses
  references already recorded, then opens the existing transaction review/save flow from
  a private notification. No inbox-history permission or automatic transaction write is
  used. Added a prominent pre-permission disclosure, notification channel, opt-in setting,
  and sender-filter tests. Share/paste import remains available.
- **Privacy and release gate:** raw SMS text is not logged, persisted, or sent off-device;
  only parsed fields are temporarily carried by the app's notification action for review.
  Google Play Console declaration and permission approval are publisher-side steps and
  have not been performed. Eligibility is uncertain because manual/share import remains an
  alternative; see `GOOGLE_PLAY_SMS_PERMISSION_REVIEW.md` and do not release with this
  permission until eligibility is confirmed and Play approves the declaration.
- **Verification:** `./gradlew test` passed, including debug Kotlin compilation;
  `git diff --check` passed. Play Console declaration and device SMS-flow testing remain
  publisher/device-side checks.

---

## 2026-10-07 — Unify widgets and bottom-sheet interactions

- **Scope:** Glance widget family, shared adaptive sheet layout, transaction/account/
  budget/goal/category/currency/review sheets, onboarding account entry, and date picker.
- **What:** redesigned balance, monthly summary, quick-add, expense, and income widgets
  around the same rounded surface, typography, spacing, and chartreuse action hierarchy;
  included richer monthly and projected-balance details at expanded sizes. Standardized
  sheet insets, headers, close affordances, primary actions, and reduced-motion-aware
  content-size transitions across the app's editing and selection sheets.
- **Fixes:** corrected dark-mode widget action-label contrast so secondary actions remain
  readable on dark widget surfaces.
- **Verification:** `./gradlew test` passed, including debug Kotlin compilation;
  `git diff --check` passed. Device screenshot verification was unavailable because
  `adb` is not installed in this environment.

---

## 2026-10-07 — Add automatic multi-currency accounts and monthly review

- **Scope:** exchange-rate data, account/transaction currency entry, net worth and
  dashboard totals, insights, transfer handling, widgets, PDF summaries, and Room schema.
- **What:** added a searchable ISO currency picker and per-account currency display;
  integrated daily reference-rate refresh from Frankfurter with cached rates and manual
  refresh; convert account balances, dashboard totals, insights, widgets, and exports to
  the selected default currency, excluding amounts without a valid rate. Transfers now
  preserve their destination-currency amount. Added an animated four-part monthly finance
  review and migration to Room v22.
- **Fixes:** the first unit run found an incorrect transaction-count expectation in the
  review fixture and an existing dashboard test that added USD/EUR raw balances without
  rates. Corrected the fixture and aligned Home’s monthly/category totals and account
  chips with the currency conversion rules.
- **Verification:** `./gradlew test` passed (398 unit tests), including debug Kotlin
  compilation; `git diff --check` passed. Instrumented migration tests were not run because
  `adb` is not installed in this environment.

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
