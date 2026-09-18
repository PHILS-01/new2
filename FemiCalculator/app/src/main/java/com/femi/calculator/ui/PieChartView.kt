package com.femi.calculator.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class PieChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Entry(val label: String, val value: Double)

    private var entries: List<Entry> = emptyList()
    private val palette = listOf(
        Color.parseColor("#FF9800"), Color.parseColor("#4CAF50"), Color.parseColor("#2196F3"),
        Color.parseColor("#E91E63"), Color.parseColor("#9C27B0"), Color.parseColor("#00BCD4"),
        Color.parseColor("#FFC107"), Color.parseColor("#8BC34A")
    )

    private val slicePaint = Paint().apply { isAntiAlias = true }
    private val legendPaint = Paint().apply { color = Color.LTGRAY; textSize = 26f; isAntiAlias = true }
    private val swatchPaint = Paint().apply { isAntiAlias = true }

    fun setData(data: List<Entry>) {
        entries = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#121212"))
        if (entries.isEmpty()) return

        val total = entries.sumOf { it.value }.coerceAtLeast(0.0001)
        val legendHeight = entries.size * 44f
        val diameter = minOf(width.toFloat(), height - legendHeight - 40f) * 0.9f
        val left = (width - diameter) / 2
        val top = 20f
        val rect = RectF(left, top, left + diameter, top + diameter)

        var startAngle = -90f
        for ((i, entry) in entries.withIndex()) {
            val sweep = (entry.value / total * 360.0).toFloat()
            slicePaint.color = palette[i % palette.size]
            canvas.drawArc(rect, startAngle, sweep, true, slicePaint)
            startAngle += sweep
        }

        var legendY = top + diameter + 40f
        for ((i, entry) in entries.withIndex()) {
            swatchPaint.color = palette[i % palette.size]
            canvas.drawRect(30f, legendY - 24f, 60f, legendY + 4f, swatchPaint)
            val pct = entry.value / total * 100.0
            canvas.drawText("${entry.label}: ${formatVal(entry.value)} (${String.format("%.1f", pct)}%)", 76f, legendY, legendPaint)
            legendY += 44f
        }
    }

    private fun formatVal(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
}
