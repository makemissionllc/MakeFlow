# MakeFlow

Android personal finance manager — track accounts, transactions, budgets, savings goals,
recurring payments, and spending insights. On-device ML smart-categorization, M-PESA SMS
import, multi-currency support, JSON backup/restore, and Glance home-screen widgets.

- Display name: **Mindful** / **Mindful Wallet** (see `app/src/main/res/values/strings.xml`)
- Package / applicationId: `com.makemission.makeflow`
- License: MIT (`LICENSE`)

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.2.10 (`kotlin.code.style=official`), Java 11 bytecode |
| UI | Jetpack Compose (BOM `2024.12.01`) + Material 3 Expressive, adaptive layouts (phone/tablet) |
| Charts | Vico (`2.0.0-beta.2`) |
| Persistence | Room 2.7.1 via KSP (`makeflow_database`, version 22), DataStore Preferences (`user_preferences`) |
| Async | Coroutines 1.9.0 + Flow, Lifecycle Runtime Compose, WorkManager 2.12.0 |
| ML | LiteRT (`com.google.ai.edge.litert` 1.4.2), on-device TFLite classifier + retraining |
| Widgets | Glance AppWidget (`1.1.0`), 6 widgets |
| System | Splash Screen API, Biometric, NotificationCompat, Material3 WindowSizeClass |
| Build | AGP 9.1.1, Gradle version catalog (`gradle/libs.versions.toml`), single `:app` module |
| Tests | JUnit4 + `kotlinx-coroutines-test` (unit), Compose UI Test + Espresso + Room testing (instrumented) |

SDK levels: `minSdk 28`, `targetSdk 36`, `compileSdk 36.1`. Release builds use R8
minify + resource shrinking (`app/proguard-rules.pro`).

## App features

- **Accounts** — multiple accounts with per-account currency, balances, guarded delete.
- **Transactions** — add/edit with calculator keypad, transfers (`from/to` account),
  custom numeric engine (`domain/CalculatorEngine.kt`), duplicate detection, anomaly alerts,
  and a "Log again" action that reuses transaction details with today's date.
- **M-PESA import** — rule-based parsing for share/paste. The optional new-message listener
  is enabled only on the `community` branch; the `play` branch does not request SMS permission.
  The listener is opt-in, sender-filtered, and review-before-save. On supported devices, Gemini Nano may
  offer an on-device fallback for shared messages and receipt totals. Suggestions must be
  reviewed before use. See `GOOGLE_PLAY_SMS_PERMISSION_REVIEW.md` for the release decision.
- **Smart categorization** — 3-tier suggester (history → correction memory → on-device
  TFLite classifier), self-learning retraining from user corrections.
- **Budgets** — per-category budgets with suggestion engine, forecaster, spending velocity alerts.
- **Savings goals** — targets, linked accounts, feasibility analysis with trade-off suggestions.
- **Recurring** — rules with due/overdue processing plus auto-detected patterns from history.
- **Insights** — trends, forecasts, seasonality, anomalies, frugal streaks, savings capacity,
  cash-flow projection, PDF statement export (multi-currency).
- **Multi-currency** — ISO currency picker, per-account balances and transaction entry,
  daily automatic reference rates from Frankfurter, and net worth converted to the
  selected default currency. Missing conversions are excluded rather than mixed as raw
  amounts.
- **Monthly finance review** — an animated recap of income, spending, top category,
  month-over-month change, and recorded activity, with conversion-date disclosures. An
  optional fifth page can generate an on-device reflection from aggregate totals only, and
  the recap can be shared through Android's system share sheet.
- **Backup/restore** — versioned JSON export/import (`BackupManager`), atomic Room restore.
- **Widgets & shortcuts** — 6 Glance widgets (snapshot, quick actions, balance, monthly
  summary, quick expense/income) + launcher shortcuts + quick-add activity.
- **Reminders** — optional weekly finance-review reminders, scheduled locally and disabled
  until the user opts in.
- **System integration** — DND-aware notification gate with queue, bill reminders, biometric
  app lock, adjustable haptic strength, a unified chartreuse brand theme with light/dark + AMOLED modes, changelog screen,
  skippable onboarding tour, one-time widget pin offer, adaptive phone/tablet navigation.
- **Privacy and release** — local finance storage, automatic Android backup disabled,
  user-initiated export/restore, in-app privacy policy, and a Play manifest without SMS
  listener access. See `PRIVACY_POLICY.md` and `PLAY_STORE_RELEASE_CHECKLIST.md`.

## Release branches

- The `play` branch builds the Play-safe AAB with `:app:bundleRelease`; it has no
  `RECEIVE_SMS` permission or SMS receiver.
- The `community` branch keeps all app features enabled, including the disclosed, opt-in
  M-PESA listener. Build its GitHub APK with `:app:assembleRelease`; its application ID is
  `com.makemission.makeflow.community`.
- Keep Play policy restrictions on the `play` branch. Do not submit the Community artifact
  to Google Play.
- SMS Retriever is not used for M-PESA: it only returns messages containing this app's
  verification hash, which a third-party sender such as M-PESA cannot add.

## Project structure

```
MakeFlow/
  app/                                   # single Android module
    src/main/
      AndroidManifest.xml
      java/com/makemission/makeflow/
        MainActivity.kt / MakeFlowApp.kt / QuickAddTransactionActivity.kt
        core/        # device, format, haptics, layout, lock, logging, ml, notifications
        data/        # Room (local/ + dao/ + entity/), repository/, backup/, changelog/
        domain/      # pure-Kotlin finance logic (19 files, 1:1 unit-tested)
        ui/          # screens/, components/, navigation/, theme/, viewmodel/, MainScreen.kt
        widget/      # 6 Glance widgets + receivers + update helper
      assets/ml/     # category_classifier.tflite, vocab.json, label_set.json
      res/           # strings, themes, widget/shortcut XML
    src/test/        # 38 unit test classes
    src/androidTest/ # 5 instrumented test classes
  gradle/libs.versions.toml              # version catalog (single source of truth)
  train_category_classifier.py           # offline ML training script
  PLAN.md / Documentation.md / AGENTS.md
```

Key entry points: `MakeFlowApp` (DB + logger init), `MainActivity` (share-intent handling),
`ui/navigation/AppNavigationEngine.kt` (root state-driven navigation, no NavController back
stack), `ui/MainScreen.kt` (tab shell + overlay panels).

## Getting started

Requirements: Android Studio (a recent Narwhal+ release for AGP 9.x), JDK 11+,
Android SDK with API 36, and network access for the first Gradle sync.

```bash
git clone https://github.com/makemissionllc/MakeFlow.git
cd MakeFlow
# open in Android Studio and let Gradle sync, then Run > app
```

Command line:

```bash
./gradlew assembleDebug          # debug APK
./gradlew test                    # unit tests
./gradlew connectedAndroidTest    # instrumented tests (device/emulator)
./gradlew :app:compileDebugKotlin # quick compile check
```

## ML model workflow

The transaction classifier is trained **offline**, never on a phone from scratch. M-PESA
messages use deterministic local parsing first; the optional Gemini Nano fallback uses
Google's beta ML Kit Prompt API only for foreground shared text when the model is already
available. The SMS itself is not used to train a model, uploaded, or passed to that fallback
from the background SMS receiver. Nano suggestions are checked against the original message
and remain subject to the usual review-before-save flow. Users can also request receipt
amount/merchant suggestions from a selected image and monthly reflections from aggregate
figures; neither feature stores the image or generated text, and both remain optional.

1. Expand the seed data in `train_category_classifier.py` (aim for hundreds of real
   Kenya-market descriptions).
2. Run it: `python3 train_category_classifier.py` — outputs quantized `.tflite`,
   `vocab.json`, `label_set.json`.
3. Copy the outputs into `app/src/main/assets/ml/`.
4. On-device, user corrections accumulate via `CorrectionRepository` and retrain the
   model in batches (min batch 5) — see `core/ml/RetrainableCategoryClassifier.kt`.

## Further reading

- `Documentation.md` — living log of code changes, errors, and fixes.
- `AGENTS.md` — mandatory working procedures for coding agents on this repo.
- `PLAN.md` — focused implementation plans (e.g. calculator sign-toggle + spacing polish).
