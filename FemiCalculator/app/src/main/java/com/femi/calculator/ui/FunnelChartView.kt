package com.femi.calculator.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class FunnelChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Entry(val label: String, val value: Double)

    private var entries: List<Entry> = emptyList()
    private val palette = listOf(
        Color.parseColor("#FF9800"), Color.parseColor("#F57C00"), Color.parseColor("#EF6C00"),
        Color.parseColor("#E65100"), Color.parseColor("#BF360C"), Color.parseColor("#8D2E00")
    )

    private val segmentPaint = Paint().apply { isAntiAlias = true }
    private val labelPaint = Paint().apply { color = Color.WHITE; textSize = 26f; textAlign = Paint.Align.CENTER; isAntiAlias = true }

    fun setData(data: List<Entry>) {
        // Funnels read top (largest) to bottom (smallest); keep the order given.
        entries = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#121212"))
        if (entries.isEmpty()) return

        val maxVal = entries.maxOf { it.value }.coerceAtLeast(0.0001)
        val padding = 24f
        val usableWidth = width - 2 * padding
        val rowHeight = (height - 2 * padding) / entries.size
        val centerX = width / 2f

        var y = padding
        for ((i, entry) in entries.withIndex()) {
            val topFraction = entry.value / maxVal
            val nextValue = entries.getOrNull(i + 1)?.value ?: 0.0
            val bottomFraction = nextValue / maxVal

            val topHalfWidth = ((usableWidth / 2) * topFraction).toFloat()
            val bottomHalfWidth = ((usableWidth / 2) * bottomFraction).toFloat()

            val path = Path()
            path.moveTo(centerX - topHalfWidth, y)
            path.lineTo(centerX + topHalfWidth, y)
            path.lineTo(centerX + bottomHalfWidth, y + rowHeight)
            path.lineTo(centerX - bottomHalfWidth, y + rowHeight)
            path.close()

            segmentPaint.color = palette[i % palette.size]
            canvas.drawPath(path, segmentPaint)

            canvas.drawText("${entry.label}: ${formatVal(entry.value)}", centerX, y + rowHeight / 2 + 10f, labelPaint)
            y += rowHeight
        }
    }

    private fun formatVal(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
}
