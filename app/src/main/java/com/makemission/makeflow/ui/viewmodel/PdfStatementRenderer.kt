package com.makemission.makeflow.ui.viewmodel

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.graphics.toArgb
import com.makemission.makeflow.ui.theme.MindfulAccent
import com.makemission.makeflow.ui.theme.MindfulLightBackground
import com.makemission.makeflow.ui.theme.MindfulLightMutedText
import com.makemission.makeflow.ui.theme.MindfulLightSurfaceStrong
import com.makemission.makeflow.ui.theme.MindfulLightText
import java.text.NumberFormat
import java.util.Locale

internal object PdfStatementRenderer {
    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792
    private const val LEFT = 42f
    private const val RIGHT = 570f
    private const val BODY_BOTTOM = 728f
    private const val LINE_HEIGHT = 13f
    private const val ROW_GAP = 10f

    private val columnWidths = floatArrayOf(78f, 162f, 86f, 106f, 96f)
    private val columnTitles = listOf("Date", "Description", "Category", "Account", "Amount")

    fun render(
        rows: List<PdfRow>,
        summary: PdfSummary,
        currencyCode: String,
        dateRangeLabel: String
    ): PdfDocument {
        val document = PdfDocument()
        val titlePaint = paint(MindfulLightText.toArgb(), 22f, bold = true)
        val subtitlePaint = paint(MindfulLightMutedText.toArgb(), 10f)
        val headerPaint = paint(MindfulLightText.toArgb(), 9f, bold = true)
        val rowPaint = paint(MindfulLightText.toArgb(), 9f)
        val mutedPaint = paint(MindfulLightMutedText.toArgb(), 8f)
        val summaryTitlePaint = paint(MindfulLightText.toArgb(), 11f, bold = true)
        val summaryLabelPaint = paint(MindfulLightMutedText.toArgb(), 10f)
        val summaryValuePaint = paint(MindfulLightText.toArgb(), 10f, bold = true)
        val footerPaint = paint(MindfulLightMutedText.toArgb(), 8f)
        val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MindfulLightSurfaceStrong.toArgb()
            strokeWidth = 0.7f
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MindfulAccent.toArgb()
            style = Paint.Style.FILL
        }
        val headerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MindfulLightBackground.toArgb()
            style = Paint.Style.FILL
        }
        val summaryFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MindfulLightSurfaceStrong.toArgb()
            style = Paint.Style.FILL
        }

        var pageNumber = 0
        var activePage: PdfDocument.Page? = null
        var activeCanvas: Canvas? = null
        var y = 0f

        fun drawTableHeader(canvas: Canvas) {
            val top = y - 14f
            canvas.drawRect(LEFT, top, RIGHT, y + 8f, headerFillPaint)
            var x = LEFT
            columnTitles.forEachIndexed { index, title ->
                canvas.drawText(title, x + 6f, y, headerPaint)
                x += columnWidths[index]
            }
            y += 10f
            canvas.drawLine(LEFT, y, RIGHT, y, separatorPaint)
            y += 14f
        }

        fun startPage(firstPage: Boolean) {
            pageNumber += 1
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(pageInfo)
            activePage = page
            activeCanvas = page.canvas
            val canvas = page.canvas
            canvas.drawRect(LEFT, 30f, RIGHT, 35f, accentPaint)
            if (firstPage) {
                canvas.drawText("MakeFlow Statement", LEFT, 61f, titlePaint)
                canvas.drawText("Date range  $dateRangeLabel", LEFT, 82f, subtitlePaint)
                canvas.drawText("Display currency  $currencyCode", LEFT, 98f, subtitlePaint)
                y = 128f
            } else {
                canvas.drawText("MakeFlow Statement - continued", LEFT, 59f, titlePaint)
                canvas.drawText(dateRangeLabel, LEFT, 79f, subtitlePaint)
                y = 105f
            }
            drawTableHeader(canvas)
        }

        fun finishPage() {
            val canvas = checkNotNull(activeCanvas)
            canvas.drawLine(LEFT, 750f, RIGHT, 750f, separatorPaint)
            val label = "Page $pageNumber"
            canvas.drawText(label, RIGHT - footerPaint.measureText(label), 766f, footerPaint)
            document.finishPage(checkNotNull(activePage))
            activePage = null
            activeCanvas = null
        }

        startPage(firstPage = true)
        if (rows.isEmpty()) {
            checkNotNull(activeCanvas).drawText(
                "No transactions in this date range.",
                LEFT + 6f,
                y + 8f,
                mutedPaint
            )
            y += 34f
        } else {
            rows.forEachIndexed { rowIndex, row ->
                val description = if (row.isTransfer && row.transferDetail.isNotBlank()) {
                    "${row.description} - ${row.transferDetail}"
                } else {
                    row.description
                }
                val cells = listOf(row.date, description, row.category, row.account, row.amount)
                val wrapped = cells.mapIndexed { index, value ->
                    wrapText(value, columnWidths[index] - 12f, rowPaint)
                }
                val rowLineCount = wrapped.maxOf { it.size }.coerceAtLeast(1)
                var consumedLines = 0

                while (consumedLines < rowLineCount) {
                    val availableLines = ((BODY_BOTTOM - y - ROW_GAP) / LINE_HEIGHT).toInt()
                    if (availableLines <= 0) {
                        finishPage()
                        startPage(firstPage = false)
                        continue
                    }

                    val linesThisSegment = minOf(rowLineCount - consumedLines, availableLines)
                    val canvas = checkNotNull(activeCanvas)
                    if (rowIndex % 2 == 0) {
                        canvas.drawRect(
                            LEFT,
                            y - 10f,
                            RIGHT,
                            y + (linesThisSegment * LINE_HEIGHT) - 2f,
                            headerFillPaint
                        )
                    }

                    var x = LEFT
                    wrapped.forEachIndexed { columnIndex, lines ->
                        val rightAligned = columnIndex == 4
                        for (lineOffset in 0 until linesThisSegment) {
                            val line = lines.getOrNull(consumedLines + lineOffset).orEmpty()
                            val drawX = if (rightAligned) {
                                x + columnWidths[columnIndex] - 6f - rowPaint.measureText(line)
                            } else {
                                x + 6f
                            }
                            canvas.drawText(
                                line,
                                drawX,
                                y + lineOffset * LINE_HEIGHT,
                                rowPaint
                            )
                        }
                        x += columnWidths[columnIndex]
                    }

                    consumedLines += linesThisSegment
                    y += linesThisSegment * LINE_HEIGHT + ROW_GAP
                    checkNotNull(activeCanvas).drawLine(LEFT, y - 5f, RIGHT, y - 5f, separatorPaint)

                    if (consumedLines < rowLineCount) {
                        finishPage()
                        startPage(firstPage = false)
                    }
                }
            }
        }

        val summaryHeight = 90f
        if (y + summaryHeight > BODY_BOTTOM) {
            finishPage()
            startPage(firstPage = false)
        }
        val canvas = checkNotNull(activeCanvas)
        canvas.drawRoundRect(
            RectF(LEFT, y, RIGHT, y + summaryHeight),
            8f,
            8f,
            summaryFillPaint
        )
        canvas.drawText("Summary", LEFT + 12f, y + 20f, summaryTitlePaint)
        val summaryRows = listOf(
            "Income" to formatTotal(summary.totalIncome, currencyCode),
            "Expenses" to formatTotal(summary.totalExpense, currencyCode),
            "Net" to formatTotal(summary.net, currencyCode)
        )
        summaryRows.forEachIndexed { index, (label, value) ->
            val baseline = y + 40f + index * 15f
            canvas.drawText(label, LEFT + 12f, baseline, summaryLabelPaint)
            canvas.drawText(
                value,
                RIGHT - 12f - summaryValuePaint.measureText(value),
                baseline,
                summaryValuePaint
            )
        }
        y += summaryHeight + 12f
        finishPage()
        return document
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        if (text.isBlank()) return listOf("")
        val lines = mutableListOf<String>()
        var currentLine = ""

        text.trim().split(Regex("\\s+")).forEach { word ->
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = candidate
            } else {
                if (currentLine.isNotEmpty()) {
                    lines += currentLine
                    currentLine = ""
                }
                var fragment = ""
                word.forEach { character ->
                    val candidateFragment = fragment + character
                    if (fragment.isNotEmpty() && paint.measureText(candidateFragment) > maxWidth) {
                        lines += fragment
                        fragment = character.toString()
                    } else {
                        fragment = candidateFragment
                    }
                }
                currentLine = fragment
            }
        }
        if (currentLine.isNotEmpty()) lines += currentLine
        return lines.ifEmpty { listOf("") }
    }

    private fun formatTotal(amount: Double, currencyCode: String): String {
        val digits = try {
            java.util.Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
        } catch (_: Exception) {
            2
        }
        val formatted = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = digits.coerceAtMost(2)
            minimumFractionDigits = 0
        }.format(amount)
        return "$currencyCode $formatted"
    }

    private fun paint(color: Int, size: Float, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
    }
}
