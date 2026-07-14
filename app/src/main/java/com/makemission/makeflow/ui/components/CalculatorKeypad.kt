package com.makemission.makeflow.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.domain.CalculatorEngine

private val ButtonBg = Color(0xFF2C2C2E)
private val OperatorBg = Color(0xFF3A3A3C)
private val ActionBg = Color(0xFF3A3A3C)
private val AccentGreen = Color(0xFF6DD881)
private val KeypadBg = Color(0xFF1B1C1F)
private val DisplayBg = Color(0xFF111411)

private val btnShape = RoundedCornerShape(18.dp)
private val ROW_HEIGHT = 76.dp
private val BUTTON_SPACING = 14.dp

/**
 * In-app calculator keypad overlay for amount entry.
 *
 * Renders a 5×4 grid matching the reference design:
 * ```
 * ÷   7   8   9   ⌫   (long-press ⌫ = clear)
 * ×   4   5   6   📋
 * −   1   2   3   ✓  ← checkmark spans rows 3-4
 * +   ±   0   .   ✓
 * ```
 */
@Composable
fun CalculatorKeypadOverlay(
    initialAmount: String,
    currencySymbol: String,
    onConfirm: (Double?) -> Unit,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
    onHaptic: (() -> Unit)? = null
) {
    val engine = remember { CalculatorEngine() }

    // Seed the engine when initialAmount changes (e.g. keypad reopens for editing).
    var lastInitial by remember { mutableStateOf(initialAmount) }
    LaunchedEffect(initialAmount) {
        if (initialAmount != lastInitial) {
            engine.onClear()
            if (initialAmount.isNotEmpty()) {
                initialAmount.forEach { ch ->
                    when {
                        ch.isDigit() -> engine.onDigit(ch.digitToInt())
                        ch == '.' -> engine.onDecimalPoint()
                    }
                }
            }
            lastInitial = initialAmount
        }
    }

    // Tick counter to trigger recomposition after engine mutations.
    var displayTick by remember { mutableIntStateOf(0) }
    fun refresh() { displayTick++ }

    @Suppress("UNUSED_VARIABLE")
    val tickUsed = displayTick

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KeypadBg, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(BUTTON_SPACING)
    ) {
        // ── Display row ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DisplayBg, RoundedCornerShape(18.dp))
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            val dv = engine.displayValue
            val formatted = when (dv) {
                "Error" -> "Error"
                "0" -> "${currencySymbol}0"
                else -> "$currencySymbol$dv"
            }
            Text(
                text = formatted,
                color = if (dv == "Error") Color(0xFFFFB4A5) else Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── Row 1: ÷ 7 8 9 ⌫ ──
        Row(
            modifier = Modifier.height(ROW_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(BUTTON_SPACING)
        ) {
            CalcBtn("÷", OperatorBg, AccentGreen, Modifier.weight(1f), onClick = { engine.onOperator(CalculatorEngine.Operator.DIVIDE); refresh() })
            CalcBtn("7", ButtonBg, Color.White, Modifier.weight(1f), onClick = { engine.onDigit(7); refresh() })
            CalcBtn("8", ButtonBg, Color.White, Modifier.weight(1f), onClick = { engine.onDigit(8); refresh() })
            CalcBtn("9", ButtonBg, Color.White, Modifier.weight(1f), onClick = { engine.onDigit(9); refresh() })
            CalcBtn("⌫", ActionBg, Color.White, Modifier.weight(1f),
                onClick = { engine.onBackspace(); refresh() },
                onLongClick = { engine.onClear(); refresh(); onHaptic?.invoke() })
        }

        // ── Row 2: × 4 5 6 📋 ──
        Row(
            modifier = Modifier.height(ROW_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(BUTTON_SPACING)
        ) {
            CalcBtn("×", OperatorBg, AccentGreen, Modifier.weight(1f), onClick = { engine.onOperator(CalculatorEngine.Operator.MULTIPLY); refresh() })
            CalcBtn("4", ButtonBg, Color.White, Modifier.weight(1f), onClick = { engine.onDigit(4); refresh() })
            CalcBtn("5", ButtonBg, Color.White, Modifier.weight(1f), onClick = { engine.onDigit(5); refresh() })
            CalcBtn("6", ButtonBg, Color.White, Modifier.weight(1f), onClick = { engine.onDigit(6); refresh() })
            CalcBtn("📋", ActionBg, Color.White, Modifier.weight(1f), onClick = { onCopy(engine.displayValue) })
        }

        // ── Rows 3–4: checkmark spans full height ──
        Row(
            modifier = Modifier.height(ROW_HEIGHT * 2 + BUTTON_SPACING),
            horizontalArrangement = Arrangement.spacedBy(BUTTON_SPACING)
        ) {
            // Col 1: − then +
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(BUTTON_SPACING)
            ) {
                CalcBtn("−", OperatorBg, AccentGreen, Modifier.fillMaxWidth().weight(1f), onClick = { engine.onOperator(CalculatorEngine.Operator.SUBTRACT); refresh() })
                CalcBtn("+", OperatorBg, AccentGreen, Modifier.fillMaxWidth().weight(1f), onClick = { engine.onOperator(CalculatorEngine.Operator.ADD); refresh() })
            }
            // Col 2: 1 then ±
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(BUTTON_SPACING)
            ) {
                CalcBtn("1", ButtonBg, Color.White, Modifier.fillMaxWidth().weight(1f), onClick = { engine.onDigit(1); refresh() })
                CalcBtn("±", OperatorBg, AccentGreen, Modifier.fillMaxWidth().weight(1f), onClick = { engine.toggleSign(); refresh(); onHaptic?.invoke() })
            }
            // Col 3: 2 then 0
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(BUTTON_SPACING)
            ) {
                CalcBtn("2", ButtonBg, Color.White, Modifier.fillMaxWidth().weight(1f), onClick = { engine.onDigit(2); refresh() })
                CalcBtn("0", ButtonBg, Color.White, Modifier.fillMaxWidth().weight(1f), onClick = { engine.onDigit(0); refresh() })
            }
            // Col 4: 3 then .
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(BUTTON_SPACING)
            ) {
                CalcBtn("3", ButtonBg, Color.White, Modifier.fillMaxWidth().weight(1f), onClick = { engine.onDigit(3); refresh() })
                CalcBtn(".", ButtonBg, Color.White, Modifier.fillMaxWidth().weight(1f), onClick = { engine.onDecimalPoint(); refresh() })
            }
            // Col 5: ✓ (tall, spans both rows)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(AccentGreen, btnShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        val result = engine.onConfirm()
                        refresh()
                        onConfirm(result)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = Color(0xFF111411),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalcBtn(
    label: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .background(bgColor, btnShape)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onClick() }
                }
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
