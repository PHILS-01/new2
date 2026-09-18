package com.femi.calculator.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class BarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Entry(val label: String, val value: Double)

    private var entries: List<Entry> = emptyList()
    private var showLabels: Boolean = true
    private val palette = listOf(
        Color.parseColor("#FF9800"), Color.parseColor("#4CAF50"), Color.parseColor("#2196F3"),
        Color.parseColor("#E91E63"), Color.parseColor("#9C27B0"), Color.parseColor("#00BCD4"),
        Color.parseColor("#FFC107"), Color.parseColor("#8BC34A")
    )

    private val barPaint = Paint().apply { isAntiAlias = true }
    private val axisPaint = Paint().apply { color = Color.GRAY; strokeWidth = 2f }
    private val labelPaint = Paint().apply { color = Color.LTGRAY; textSize = 26f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val valuePaint = Paint().apply { color = Color.WHITE; textSize = 26f; textAlign = Paint.Align.CENTER; isAntiAlias = true }

    fun setData(data: List<Entry>, showAxisLabels: Boolean = true) {
        entries = data
        showLabels = showAxisLabels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#121212"))
        if (entries.isEmpty()) return

        val maxVal = entries.maxOf { it.value }.coerceAtLeast(0.0001)
        val padding = 60f
        val bottomAxis = height - padding
        val chartHeight = bottomAxis - padding
        val slotWidth = (width - 2 * padding) / entries.size
        val barWidth = slotWidth * 0.6f

        canvas.drawLine(padding, bottomAxis, width - padding, bottomAxis, axisPaint)

        for ((i, entry) in entries.withIndex()) {
            val barHeight = (entry.value / maxVal * chartHeight).toFloat()
            val left = padding + i * slotWidth + (slotWidth - barWidth) / 2
            val right = left + barWidth
            val top = bottomAxis - barHeight
            barPaint.color = palette[i % palette.size]
            canvas.drawRect(left, top, right, bottomAxis, barPaint)
            if (showLabels) {
                canvas.drawText(formatVal(entry.value), left + barWidth / 2, top - 10f, valuePaint)
                canvas.drawText(entry.label, left + barWidth / 2, bottomAxis + 36f, labelPaint)
            }
        }
    }

    private fun formatVal(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
}
