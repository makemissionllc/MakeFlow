# AGENTS.md — Coding-agent operating procedures for MakeFlow

Every coding agent working in this repo **must** read and follow this file, plus
`README.md` (project overview) and `Documentation.md` (change log), before editing code.

## 1. Session workflow

1. **Read first.** Inspect the files you will touch (and their callers/tests) before
   changing anything. `read` shows hidden entries — do not re-list with shell.
   Prefer the `subagent` explore agent for broad questions; it saves context.
2. **Small, exact edits.** Use `edit` with the smallest unique `oldString`; never
   rewrite whole files to make a small change. Preserve formatting and imports order.
3. **No placeholders.** Never leave `TODO`, stub functions, or guessed APIs. If an API
   is uncertain, read the source or version-catalog entry first.
4. **Verify by execution.** After a change, run the relevant check:
   `./gradlew test` (unit), `./gradlew :app:compileDebugKotlin` (compile),
   `./gradlew connectedAndroidTest` (instrumented, needs device). If the environment
   lacks a JDK (`JAVA_HOME` unset, as in some agent sandboxes), say so and do a static
   import/usage cross-check instead of claiming a build passed.
   Do not start emulators or other device simulation environments. Run instrumented
   tests only on a connected physical Android device; otherwise compile their sources
   and report device UI verification as not run.
5. **Log it.** Append an entry to `Documentation.md` (newest first) for every code
   change, error found, and fix applied.
6. **Commit + push.** After every edit, commit and push to the current branch unless
   the user says not to. Never commit unrelated dirty files — stage only your own
   paths (`git add <paths>`, never `git add -A`).

## 2. Git conventions (observed)

- Main branch: `main` (`origin/main`, GitHub `makemissionllc/MakeFlow`).
- Commit messages: imperative summary sentence, e.g. `Implement ...`, `Fix ...`,
  `Refactor ...`. Merge commits name the source branch and reason.
- History is real project history — do not rewrite it (`--force`, rebase of pushed
  commits, or squashing others' work is forbidden).
- If histories diverge with no merge-base (legacy stub branches), merge with
  `--allow-unrelated-histories` and resolve carefully.

## 3. Architecture rules

- **UI:** Jetpack Compose + Material 3 only. Navigation is **state-driven overlay flags**
  in `ui/MainScreen.kt`, wired by `ui/navigation/AppNavigationEngine.kt` — there is no
  `NavController` back stack; do not introduce one without explicit approval.
- **Design tokens only.** Never hardcode colors, spacing, text sizes, shapes, or motion:
  use `ui/theme/` tokens via `LocalAppDimens`, `LocalMakeFlowColors`,
  `LocalExpressiveMotion`, `AppDimens`/typography/`ExpressiveShapes`. This is enforced
  since the design-system migration — hardcoded values in new code are a defect.
- **Adaptive + reduced motion.** New UI must handle `DeviceLayoutMode`
  (`COMPACT/MEDIUM/EXPANDED`, `core/layout/`) and honor `LocalReducedMotion`
  (see `Motion.kt` `OverlayTransition` specs and `AdaptiveLayout.kt`).
- **Feedback.** User actions fire haptics via `HapticManager` (`core/haptics/`);
  diagnostics go through `AppLogger` (`core/logging/`) — never `println`/ad-hoc logging.
- **Data.** Room is the source of truth (`data/local/`). Every schema change requires a
  `MIGRATION_x_y` in `AppDatabase.kt` **and** coverage in `MigrationsTest`
  (androidTest). User preferences live in `UserPreferencesDataStore` (DataStore), not
  SharedPreferences. Business logic goes in `domain/` as pure Kotlin; screens talk to
  `FinanceRepository`/repositories via per-screen ViewModels (+ `*Factory`).
- **Transaction writes** must go through `TransactionCommitHandler` (duplicate-check
  gate, anomaly toast, haptics) — do not bypass it with direct DAO inserts from UI.
- **Secrets/keys:** never commit `google-services.json`, `*.jks`/`*.keystore`, or
  `local.properties` (all gitignored). Never commit `*.log`, `err.txt` binaries, or
  `.kotlin/errors/` dumps.

## 4. Dependencies

- `gradle/libs.versions.toml` is the single source of truth. Add/upgrade libraries
  there, reference via `libs.*` in `app/build.gradle.kts`. Do not hardcode versions in
  module build files.
- Do not add a new library when a repo-local utility already covers it
  (`CurrencyFormatter`, `CalculatorEngine`, Vico charts, LiteRT classifier).

## 5. Tests

- Every `domain/` object has a 1:1 unit test under `app/src/test/` — keep that mapping:
  new domain logic ships with a same-named `*Test`.
- Keep `BackupRestoreContractTest` green: backups include all user tables and exclude
  transient ones (`card_interactions`, `queued_notifications`).
- M-PESA parser changes require fixture tests in `MpesaMessageParserTest`.
- Run at least `./gradlew test` before pushing logic changes.

## 6. Communication rules

- Be concise and factual; reference code as `path:line`.
- Never invent URLs, APIs, or file contents — verify from the repo.
- If investigation contradicts an earlier claim, state the discrepancy plainly.
- Ask (via question tool) when a task is ambiguous or a push would surprise the user;
  otherwise proceed and report.
