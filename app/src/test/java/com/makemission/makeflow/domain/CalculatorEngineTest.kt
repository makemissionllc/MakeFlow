package com.makemission.makeflow.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CalculatorEngineTest {

    private lateinit var engine: CalculatorEngine

    @Before
    fun setup() {
        engine = CalculatorEngine()
    }

    // --- Digit entry ---

    @Test
    fun initialDisplay_isZero() {
        assertEquals("0", engine.displayValue)
    }

    @Test
    fun singleDigit_tap() {
        engine.onDigit(5)
        assertEquals("5", engine.displayValue)
    }

    @Test
    fun multipleDigits_tap() {
        engine.onDigit(2)
        engine.onDigit(9)
        engine.onDigit(9)
        assertEquals("299", engine.displayValue)
    }

    @Test
    fun digitAfterZero_replacesZero() {
        engine.onDigit(0)
        assertEquals("0", engine.displayValue)
        engine.onDigit(7)
        assertEquals("7", engine.displayValue)
    }

    @Test
    fun confirm_emptyDisplay_returnsZero() {
        val result = engine.onConfirm()
        assertEquals(0.0, result!!, 0.001)
    }

    // --- Single operation ---

    @Test
    fun addition_5plus3() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        engine.onDigit(3)
        val result = engine.onConfirm()
        assertEquals(8.0, result!!, 0.001)
    }

    @Test
    fun subtraction_10minus4() {
        engine.onDigit(1)
        engine.onDigit(0)
        engine.onOperator(CalculatorEngine.Operator.SUBTRACT)
        engine.onDigit(4)
        val result = engine.onConfirm()
        assertEquals(6.0, result!!, 0.001)
    }

    @Test
    fun multiplication_6times7() {
        engine.onDigit(6)
        engine.onOperator(CalculatorEngine.Operator.MULTIPLY)
        engine.onDigit(7)
        val result = engine.onConfirm()
        assertEquals(42.0, result!!, 0.001)
    }

    @Test
    fun division_8div2() {
        engine.onDigit(8)
        engine.onOperator(CalculatorEngine.Operator.DIVIDE)
        engine.onDigit(2)
        val result = engine.onConfirm()
        assertEquals(4.0, result!!, 0.001)
    }

    // --- Chained operations: left-to-right, NO operator precedence ---

    @Test
    fun chained_leftToRight_10plus5times2_equals30() {
        // 10 + 5 × 2 should be (10+5)×2 = 30, NOT 10+(5×2) = 20
        engine.onDigit(1)
        engine.onDigit(0)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.MULTIPLY)
        // At this point: 10+5 = 15 evaluated, stored = 15, pending = ×
        assertEquals("15", engine.displayValue)
        engine.onDigit(2)
        val result = engine.onConfirm()
        assertEquals(30.0, result!!, 0.001)
    }

    @Test
    fun chained_threeOperations() {
        // 100 − 20 + 5 = 85
        engine.onDigit(1)
        engine.onDigit(0)
        engine.onDigit(0)
        engine.onOperator(CalculatorEngine.Operator.SUBTRACT)
        engine.onDigit(2)
        engine.onDigit(0)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        assertEquals("80", engine.displayValue)
        engine.onDigit(5)
        val result = engine.onConfirm()
        assertEquals(85.0, result!!, 0.001)
    }

    @Test
    fun chained_fourOperations() {
        // 8 ÷ 4 × 3 + 1 = 7
        engine.onDigit(8)
        engine.onOperator(CalculatorEngine.Operator.DIVIDE)
        engine.onDigit(4)
        engine.onOperator(CalculatorEngine.Operator.MULTIPLY)
        assertEquals("2", engine.displayValue)
        engine.onDigit(3)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        assertEquals("6", engine.displayValue)
        engine.onDigit(1)
        val result = engine.onConfirm()
        assertEquals(7.0, result!!, 0.001)
    }

    // --- Division by zero ---

    @Test
    fun divisionByZero_showsError() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.DIVIDE)
        engine.onDigit(0)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        assertEquals("Error", engine.displayValue)
    }

    @Test
    fun divisionByZero_confirm_returnsNull() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.DIVIDE)
        engine.onDigit(0)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        assertNull(engine.onConfirm())
    }

    @Test
    fun divisionByZero_digitAfterError_clearsAndStartsFresh() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.DIVIDE)
        engine.onDigit(0)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        assertEquals("Error", engine.displayValue)
        engine.onDigit(3)
        assertEquals("3", engine.displayValue)
    }

    @Test
    fun divisionByZero_directConfirm_returnsNull() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.DIVIDE)
        engine.onDigit(0)
        // Confirm without chaining — should still detect div-by-zero
        val result = engine.onConfirm()
        assertNull(result)
        assertEquals("Error", engine.displayValue)
    }

    // --- Decimal point ---

    @Test
    fun decimalPoint_single() {
        engine.onDigit(1)
        engine.onDecimalPoint()
        engine.onDigit(5)
        assertEquals("1.5", engine.displayValue)
    }

    @Test
    fun decimalPoint_duplicateIgnored() {
        engine.onDigit(1)
        engine.onDecimalPoint()
        engine.onDigit(2)
        engine.onDecimalPoint()
        engine.onDigit(3)
        assertEquals("1.23", engine.displayValue)
    }

    @Test
    fun decimalPoint_afterError_clearsAndStarts() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.DIVIDE)
        engine.onDigit(0)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        engine.onDecimalPoint()
        assertEquals("0.", engine.displayValue)
        engine.onDigit(5)
        assertEquals("0.5", engine.displayValue)
    }

    @Test
    fun decimalPoint_afterOperator_startsWith0() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        engine.onDecimalPoint()
        assertEquals("0.", engine.displayValue)
        engine.onDigit(5)
        assertEquals("0.5", engine.displayValue)
    }

    @Test
    fun confirm_withDecimal() {
        engine.onDigit(2)
        engine.onDecimalPoint()
        engine.onDigit(9)
        engine.onDigit(9)
        val result = engine.onConfirm()
        assertEquals(2.99, result!!, 0.001)
    }

    // --- Backspace ---

    @Test
    fun backspace_removesLastDigit() {
        engine.onDigit(1)
        engine.onDigit(2)
        engine.onDigit(3)
        engine.onBackspace()
        assertEquals("12", engine.displayValue)
    }

    @Test
    fun backspace_singleDigit_revertsToZero() {
        engine.onDigit(5)
        engine.onBackspace()
        assertEquals("0", engine.displayValue)
    }

    @Test
    fun backspace_afterDecimal_removesDecimal() {
        engine.onDigit(1)
        engine.onDecimalPoint()
        engine.onBackspace()
        assertEquals("1", engine.displayValue)
    }

    @Test
    fun backspace_afterOperator_removesPendingOperator() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        engine.onBackspace()
        // Should revert to showing the stored operand (5)
        assertEquals("5", engine.displayValue)
        // Entering more digits appends to the existing operand
        engine.onDigit(3)
        assertEquals("53", engine.displayValue)
        val result = engine.onConfirm()
        assertEquals(53.0, result!!, 0.001)
    }

    @Test
    fun backspace_onEmptyOperand_afterOperator_removesOperator() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        // No digits entered yet — display is still "5" with waitingForOperand
        engine.onBackspace()
        // Operator removed, back to "5" with no pending op
        assertEquals("5", engine.displayValue)
        val result = engine.onConfirm()
        assertEquals(5.0, result!!, 0.001)
    }

    @Test
    fun backspace_afterError_clears() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.DIVIDE)
        engine.onDigit(0)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        engine.onBackspace()
        assertEquals("0", engine.displayValue)
    }

    // --- Clear ---

    @Test
    fun clear_resetsEverything() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        engine.onDigit(3)
        engine.onClear()
        assertEquals("0", engine.displayValue)
        // After clear, entering a new expression should work from scratch
        engine.onDigit(9)
        engine.onOperator(CalculatorEngine.Operator.MULTIPLY)
        engine.onDigit(2)
        val result = engine.onConfirm()
        assertEquals(18.0, result!!, 0.001)
    }

    @Test
    fun clear_afterError_resets() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.DIVIDE)
        engine.onDigit(0)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        engine.onClear()
        assertEquals("0", engine.displayValue)
        engine.onDigit(7)
        assertEquals("7", engine.displayValue)
    }

    // --- Confirm ---

    @Test
    fun confirm_withPendingOperator_evaluatesFirst() {
        engine.onDigit(1)
        engine.onDigit(0)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        engine.onDigit(5)
        val result = engine.onConfirm()
        assertEquals(15.0, result!!, 0.001)
    }

    @Test
    fun confirm_withoutPendingOperator_returnsValueAsIs() {
        engine.onDigit(4)
        engine.onDigit(2)
        val result = engine.onConfirm()
        assertEquals(42.0, result!!, 0.001)
    }

    @Test
    fun confirm_resetsEngineForNewEntry() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        engine.onDigit(3)
        engine.onConfirm()
        // After confirm, next digit starts a fresh operand
        engine.onDigit(9)
        assertEquals("9", engine.displayValue)
        val result2 = engine.onConfirm()
        assertEquals(9.0, result2!!, 0.001)
    }

    // --- Result rounding / 2-decimal-place rule ---

    @Test
    fun confirm_roundsToTwoDecimals() {
        engine.onDigit(1)
        engine.onDigit(0)
        engine.onOperator(CalculatorEngine.Operator.DIVIDE)
        engine.onDigit(3)
        val result = engine.onConfirm()
        assertEquals(3.33, result!!, 0.01)
    }

    @Test
    fun confirm_rejectsNegativeResult() {
        // 5 − 10 = -5 → confirm should return null
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.SUBTRACT)
        engine.onDigit(1)
        engine.onDigit(0)
        val result = engine.onConfirm()
        assertNull(result)
    }

    // --- Operator replacement ---

    @Test
    fun operatorTap_replacesPendingOperator() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        engine.onOperator(CalculatorEngine.Operator.MULTIPLY)
        // Now it should be 5 × ... instead of 5 + ...
        engine.onDigit(3)
        val result = engine.onConfirm()
        assertEquals(15.0, result!!, 0.001)
    }

    // --- Decimal in chained operations ---

    @Test
    fun chainedWithDecimals() {
        // 2.5 + 1.5 × 2 = 8
        engine.onDigit(2)
        engine.onDecimalPoint()
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        engine.onDigit(1)
        engine.onDecimalPoint()
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.MULTIPLY)
        assertEquals("4", engine.displayValue)
        engine.onDigit(2)
        val result = engine.onConfirm()
        assertEquals(8.0, result!!, 0.001)
    }

    // --- Edge cases ---

    @Test
    fun operatorOnEmptyDisplay_doesNothing() {
        engine.onOperator(CalculatorEngine.Operator.ADD)
        // Display should still be "0", no crash
        assertEquals("0", engine.displayValue)
    }

    @Test
    fun confirmAfterClear_returnsZero() {
        engine.onClear()
        val result = engine.onConfirm()
        assertEquals(0.0, result!!, 0.001)
    }

    @Test
    fun backspaceOnZero_doesNothing() {
        engine.onBackspace()
        assertEquals("0", engine.displayValue)
    }

    @Test
    fun multipleBackspaces_fullClear() {
        engine.onDigit(1)
        engine.onDigit(2)
        engine.onBackspace()
        engine.onBackspace()
        assertEquals("0", engine.displayValue)
    }

    @Test
    fun longNumber_displayCorrectly() {
        engine.onDigit(1)
        engine.onDigit(2)
        engine.onDigit(3)
        engine.onDigit(4)
        engine.onDigit(5)
        engine.onDigit(6)
        assertEquals("123456", engine.displayValue)
    }

    @Test
    fun zeroTimesAnyNumber_returnsZero() {
        engine.onDigit(0)
        engine.onOperator(CalculatorEngine.Operator.MULTIPLY)
        engine.onDigit(9)
        engine.onDigit(9)
        engine.onDigit(9)
        val result = engine.onConfirm()
        assertEquals(0.0, result!!, 0.001)
    }

    @Test
    fun confirmWithDecimal_noIntegerPart() {
        engine.onDecimalPoint()
        engine.onDigit(5)
        val result = engine.onConfirm()
        assertEquals(0.5, result!!, 0.001)
    }

    // --- Format result ---

    @Test
    fun formatResult_wholeNumber_noDecimalPoint() {
        assertEquals("42", engine.formatResult(42.0))
    }

    @Test
    fun formatResult_decimalNumber_stripsTrailingZeros() {
        assertEquals("2.5", engine.formatResult(2.5))
    }

    @Test
    fun formatResult_floatingPointNoise_cleanedUp() {
        // 0.1 + 0.2 = 0.30000000000000004 → should display cleanly
        val noisy = 0.1 + 0.2
        val formatted = engine.formatResult(noisy)
        assertEquals("0.3", formatted)
    }

    @Test
    fun formatResult_infinity_showsError() {
        assertEquals("Error", engine.formatResult(Double.POSITIVE_INFINITY))
    }

    // --- Toggle sign ---

    @Test
    fun toggleSign_onFreshZero_noOp() {
        engine.toggleSign()
        assertEquals("0", engine.displayValue)
    }

    @Test
    fun toggleSign_positiveToNegative() {
        engine.onDigit(5)
        engine.onDigit(0)
        engine.toggleSign()
        assertEquals("-50", engine.displayValue)
    }

    @Test
    fun toggleSign_toggleTwice_returnsOriginal() {
        engine.onDigit(5)
        engine.onDigit(0)
        engine.toggleSign()
        engine.toggleSign()
        assertEquals("50", engine.displayValue)
    }

    @Test
    fun toggleSign_midDecimalEntry() {
        engine.onDigit(1)
        engine.onDigit(2)
        engine.onDecimalPoint()
        engine.onDigit(5)
        engine.toggleSign()
        assertEquals("-12.5", engine.displayValue)
    }

    @Test
    fun toggleSign_afterOperator_startsNegativeOperand() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        engine.toggleSign()
        assertEquals("-", engine.displayValue)
        engine.onDigit(3)
        assertEquals("-3", engine.displayValue)
        val result = engine.onConfirm()
        assertEquals(2.0, result!!, 0.001)
    }

    @Test
    fun toggleSign_afterError_resetsLikeDigitEntry() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.DIVIDE)
        engine.onDigit(0)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        assertEquals("Error", engine.displayValue)
        engine.toggleSign()
        assertEquals("-", engine.displayValue)
        engine.onDigit(3)
        assertEquals("-3", engine.displayValue)
        val result = engine.onConfirm()
        assertNull(result)
    }

    @Test
    fun toggleSign_fullExpression_negativeOperand() {
        engine.onDigit(1)
        engine.onDigit(0)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        engine.toggleSign()
        engine.onDigit(3)
        val result = engine.onConfirm()
        assertEquals(7.0, result!!, 0.001)
    }

    @Test
    fun toggleSign_negativeFinalResult_rejected() {
        engine.onDigit(5)
        engine.onOperator(CalculatorEngine.Operator.ADD)
        engine.toggleSign()
        engine.onDigit(1)
        engine.onDigit(0)
        val result = engine.onConfirm()
        assertNull(result)
    }
}
