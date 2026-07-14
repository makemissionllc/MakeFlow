# Plan: Sign Toggle + Spacing Polish

## Part 1 — Sign Toggle (functional)

### A. CalculatorEngine.kt — add `toggleSign()`

Add `fun toggleSign()` with this logic (top-to-bottom, first match wins):

1. **`hasError` true** → call `onClear()`, set `_displayValue = "-"`, return.
   (Mirrors `onDigit` pattern: clear error, start fresh.)
2. **`waitingForOperand && pendingOperator != null`** → set `_displayValue = "-"`, `waitingForOperand = false`, return.
   (After "10 +", tapping ± starts a fresh negative operand.)
3. **`_displayValue == "0"`** → return (no-op, prevents "-0").
4. **`_displayValue.startsWith("-")`** → remove prefix. If result is empty or ".", prepend "0" (→ "0" or "0.").
5. **else** → prepend "-".

`onClear()` remains unchanged — it's still called by the engine internally and tested separately.

### B. CalculatorEngineTest.kt — 8 new tests

| # | Test name | Sequence | Assertion |
|---|-----------|----------|-----------|
| 1 | `toggleSign_onFreshZero_noOp` | toggle | display stays "0" |
| 2 | `toggleSign_positiveToNegative` | enter "50", toggle | display "-50" |
| 3 | `toggleSign_toggleTwice_returnsOriginal` | enter "50", toggle, toggle | display "50" |
| 4 | `toggleSign_midDecimalEntry` | enter "12.5", toggle | display "-12.5" |
| 5 | `toggleSign_afterOperator_startsNegativeOperand` | "5 +", toggle, enter "3", confirm | result = 2.0 |
| 6 | `toggleSign_afterError_resetsLikeDigitEntry` | trigger div/0 error, toggle | display "-", then digit "3" → display "3" |
| 7 | `toggleSign_fullExpression_negativeOperand` | "10 + -3 =", toggle on 3 | result = 7.0 |
| 8 | `toggleSign_negativeFinalResult_rejected` | "5 + ±10 =" (via toggle after +) | confirm returns null |

Existing Clear tests (`clear_resetsEverything`, `clear_afterError_resets`, `confirmAfterClear_returnsZero`) stay — they test `engine.onClear()` which is unchanged.

### C. CalculatorKeypad.kt UI changes

1. **Replace "C" with "±"** — row 4, col 2:
   ```
   CalcBtn("±", OperatorBg, AccentGreen, ...) { engine.toggleSign(); refresh() }
   ```
   Uses operator styling (OperatorBg + AccentGreen) consistent with ÷×−+.

2. **Long-press ⌫ for clear** — add `onLongClick` parameter to `CalcBtn`:
   ```kotlin
   private fun CalcBtn(
       label: String, bgColor: Color, textColor: Color,
       modifier: Modifier,
       onClick: () -> Unit,
       onLongClick: (() -> Unit)? = null   // ← NEW
   )
   ```
   When `onLongClick != null`, use `combinedClickable(onClick = ..., onLongClick = ...)` instead of plain `.clickable`. Pattern matches existing BudgetScreen/AccountsScreen usage.

   The ⌫ button becomes:
   ```kotlin
   CalcBtn("⌫", ActionBg, Color.White, Modifier.weight(1f),
       onClick = { engine.onBackspace(); refresh() },
       onLongClick = { engine.onClear(); refresh() })
   ```

3. **Haptic feedback** — add `onHaptic: (() -> Unit)? = null` to `CalculatorKeypadOverlay` (NOT to CalcBtn — keeps it simple).
   - Fire in ± tap: `onHaptic?.invoke()` after `engine.toggleSign()`
   - Fire in ⌫ long-press clear: `onHaptic?.invoke()` after `engine.onClear()`
   - In `AddTransactionBottomSheet`, pass: `onHaptic = { hapticManager.fire(HapticEvent.TOGGLE) }`
   - Add import for `HapticEvent` in AddTransactionBottomSheet (already imports `HapticManager`).

### D. AddTransactionBottomSheet.kt — pass haptic callback

At the `CalculatorKeypadOverlay(...)` call site (~line 866), add:
```kotlin
onHaptic = { hapticManager.fire(HapticEvent.TOGGLE) },
```

---

## Part 2 — Spacing Polish (visual only)

All values below are the FINAL target after both Part 1 and Part 2. Current → Target.

### CalculatorKeypad.kt

| Constant / Value | Current | Target |
|------------------|---------|--------|
| `BUTTON_SPACING` | 10dp | 14dp |
| `ROW_HEIGHT` | 64dp | 76dp |
| `btnShape` | `RoundedCornerShape(14.dp)` | `RoundedCornerShape(18.dp)` |
| Outer padding (h/v) | 16dp / 12dp | 20dp / 18dp |
| KeypadBg top corners | 20dp | keep |
| Display padding (h/v) | 20dp / 20dp | 24dp / 24dp |
| Display corner radius | 14dp | 18dp |
| CalcBtn internal padding (v) | 4dp | 8dp |

### AddTransactionBottomSheet.kt

| Element | Current | Target |
|---------|---------|--------|
| Form Column `spacedBy` | 20dp | 28dp |
| Type toggle Row `spacedBy` | 12dp | 16dp |
| Type toggle button padding (v) | 14dp | 16dp |
| Amount row padding (v) | 20dp | 32dp |
| Chip label→row spacing (Column `spacedBy`) | 10dp | 14dp |
| Chip Row `spacedBy` | 10dp | 14dp |
| Chip internal padding (h/v) | 16dp / 10dp | 20dp / 12dp |
| Subcategory row Row `spacedBy` | 12dp | 16dp |
| Template chip Row `spacedBy` | 10dp | 14dp |
| Template section Column `spacedBy` | 10dp | 14dp |
| Note section Column `spacedBy` | 8dp | 12dp |
| Date row Row `spacedBy` | 12dp | 14dp |
| Save button height | 54dp | 56dp |

AdaptiveSheetDialog horizontal padding stays at 24dp (unchanged — it's a shared component).

---

## Files modified

1. `app/src/main/java/com/makemission/makeflow/domain/CalculatorEngine.kt` — add `toggleSign()`
2. `app/src/test/java/com/makemission/makeflow/domain/CalculatorEngineTest.kt` — 8 new tests
3. `app/src/main/java/com/makemission/makeflow/ui/components/CalculatorKeypad.kt` — ± button, long-press ⌫, CalcBtn onLongClick, spacing, onHaptic param
4. `app/src/main/java/com/makemission/makeflow/ui/screens/AddTransactionBottomSheet.kt` — pass onHaptic, spacing restyle

## Verification

- `./gradlew test` — all existing + 8 new CalculatorEngine tests pass
- `./gradlew assembleDebug` — builds cleanly
- On-device: open Add Transaction → verify ± toggles sign, long-press ⌫ clears, spacing is generous
