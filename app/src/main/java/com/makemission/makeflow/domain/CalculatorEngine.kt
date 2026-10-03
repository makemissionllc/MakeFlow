package com.makemission.makeflow.domain

import kotlin.math.abs
import kotlin.math.floor

/**
 * Pure-Kotlin sequential calculator engine for in-app amount entry.
 *
 * Implements standard left-to-right evaluation (NO operator precedence):
 * "10 + 5 × 2" evaluates as (10+5)×2 = 30, NOT 10+(5×2) = 20.
 * This matches user expectations from mobile calculator apps.
 *
 * All arithmetic is platform-independent; this class has zero Android
 * dependencies and is fully unit-testable.
 */
class CalculatorEngine {

    enum class Operator(val symbol: Char) {
        ADD('+'),
        SUBTRACT('−'),
        MULTIPLY('×'),
        DIVIDE('÷');

        fun apply(a: Double, b: Double): Double = when (this) {
            ADD -> a + b
            SUBTRACT -> a - b
            MULTIPLY -> a * b
            DIVIDE -> a / b
        }
    }

    private var _displayValue: String = "0"
    private var pendingOperator: Operator? = null
    private var storedOperand: Double = 0.0
    private var waitingForOperand: Boolean = false
    private var hasError: Boolean = false
    private var _expression: String? = null

    /** Current value shown on screen (e.g. "29.99", "0", "Error"). */
    val displayValue: String get() = _displayValue
    val expression: String? get() = _expression
    val hasUnconfirmedOperation: Boolean get() = pendingOperator != null

    fun onDigit(digit: Int) {
        require(digit in 0..9) { "Digit must be 0-9" }
        if (hasError) {
            onClear()
        }
        if (waitingForOperand) {
            if (pendingOperator == null) _expression = null
            _displayValue = digit.toString()
            waitingForOperand = false
        } else {
            _displayValue = if (_displayValue == "0") digit.toString() else _displayValue + digit
        }
    }

    fun onDecimalPoint() {
        if (hasError) {
            onClear()
        }
        if (waitingForOperand) {
            if (pendingOperator == null) _expression = null
            _displayValue = "0."
            waitingForOperand = false
            return
        }
        if (!_displayValue.contains('.')) {
            _displayValue += "."
        }
    }

    fun onOperator(op: Operator) {
        if (hasError) return

        val currentValue = _displayValue.toDoubleOrNull() ?: return

        if (pendingOperator != null && !waitingForOperand) {
            // Evaluate the pending operation first (left-to-right chaining).
            val result = pendingOperator!!.apply(storedOperand, currentValue)
            if (pendingOperator == Operator.DIVIDE && currentValue == 0.0) {
                _displayValue = "Error"
                hasError = true
                pendingOperator = null
                return
            }
            storedOperand = result
            _displayValue = formatResult(result)
        } else {
            storedOperand = currentValue
        }

        pendingOperator = op
        _expression = "${formatResult(storedOperand)} ${op.symbol}"
        waitingForOperand = true
    }

    /**
     * Confirm / checkmark: finalises the current amount.
     * If a pending operator exists, evaluates it first.
     * Returns a validated Double (≥ 0, max 2 decimal places) or null
     * if the engine is in an error state or the display is empty.
     */
    fun onConfirm(): Double? {
        if (hasError) return null
        if (pendingOperator != null && waitingForOperand) return null

        val currentValue = _displayValue.toDoubleOrNull() ?: return null

        val result = if (pendingOperator != null && !waitingForOperand) {
            if (pendingOperator == Operator.DIVIDE && currentValue == 0.0) {
                _displayValue = "Error"
                hasError = true
                pendingOperator = null
                return null
            }
            pendingOperator!!.apply(storedOperand, currentValue)
        } else {
            currentValue
        }

        // Validate: non-negative, max 2 decimal places.
        if (result < 0) return null
        val rounded = roundToTwoDecimals(result)
        if (pendingOperator != null && !waitingForOperand) {
            _expression = "${_expression.orEmpty()} ${formatResult(currentValue)}"
        }
        _displayValue = formatResult(rounded)

        // Reset engine state after confirm — the value is finalised.
        pendingOperator = null
        waitingForOperand = true

        return rounded
    }

    fun onBackspace() {
        if (hasError) {
            onClear()
            return
        }
        if (waitingForOperand) {
            // Remove the pending operator, revert to the stored operand.
            pendingOperator = null
            _expression = null
            waitingForOperand = false
            _displayValue = formatResult(storedOperand)
            return
        }
        _displayValue = if (_displayValue.length <= 1) {
            "0"
        } else {
            _displayValue.dropLast(1)
        }
    }

    fun onClear() {
        _displayValue = "0"
        _expression = null
        pendingOperator = null
        storedOperand = 0.0
        waitingForOperand = false
        hasError = false
    }

    fun toggleSign() {
        if (hasError) {
            onClear()
            _displayValue = "-"
            return
        }
        if (waitingForOperand && pendingOperator != null) {
            _displayValue = "-"
            waitingForOperand = false
            return
        }
        if (_displayValue == "0") return
        if (_displayValue.startsWith("-")) {
            _displayValue = _displayValue.removePrefix("-")
            if (_displayValue.isEmpty() || _displayValue == ".") {
                _displayValue = "0$_displayValue"
            }
        } else {
            _displayValue = "-$_displayValue"
        }
    }

    /**
     * Formats a Double for display: strips unnecessary trailing zeros and
     * the decimal point for whole numbers. Handles floating-point imprecision
     * by rounding to 10 significant digits before formatting.
     */
    internal fun formatResult(value: Double): String {
        if (value.isInfinite() || value.isNaN()) return "Error"
        // Round to 10 significant digits to clean up floating-point noise.
        val rounded = if (abs(value) >= 1.0) {
            val magnitude = floor(kotlin.math.log10(abs(value))).toInt()
            val factor = Math.pow(10.0, (10 - magnitude - 1).toDouble())
            kotlin.math.round(value * factor) / factor
        } else {
            kotlin.math.round(value * 1e10) / 1e10
        }
        return if (rounded == floor(rounded) && abs(rounded) < 1e15) {
            rounded.toLong().toString()
        } else {
            // Strip trailing zeros after decimal, but keep at least one digit.
            val s = "%.10f".format(rounded)
                .trimEnd('0')
                .trimEnd('.')
            s
        }
    }

    private fun roundToTwoDecimals(value: Double): Double =
        kotlin.math.round(value * 100) / 100
}
