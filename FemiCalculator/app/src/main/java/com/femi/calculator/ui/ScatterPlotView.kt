package com.femi.calculator.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ScatterPlotView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var points: List<Pair<Double, Double>> = emptyList()

    private val axisPaint = Paint().apply { color = Color.GRAY; strokeWidth = 2f }
    private val gridPaint = Paint().apply { color = Color.DKGRAY; strokeWidth = 1f }
    private val pointPaint = Paint().apply { color = Color.parseColor("#FF9800"); isAntiAlias = true; style = Paint.Style.FILL }
    private val labelPaint = Paint().apply { color = Color.LTGRAY; textSize = 22f; isAntiAlias = true }

    fun setData(data: List<Pair<Double, Double>>) {
        points = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#121212"))
        if (points.isEmpty()) return

        val padding = 60f
        val minX = points.minOf { it.first }
        val maxX = points.maxOf { it.first }
        val minY = points.minOf { it.second }
        val maxY = points.maxOf { it.second }
        val rangeX = (maxX - minX).let { if (it == 0.0) 1.0 else it }
        val rangeY = (maxY - minY).let { if (it == 0.0) 1.0 else it }

        val left = padding
        val right = width - padding
        val top = padding
        val bottom = height - padding

        // grid + axes
        canvas.drawLine(left, bottom, right, bottom, axisPaint)
        canvas.drawLine(left, top, left, bottom, axisPaint)
        for (i in 0..4) {
            val gx = left + (right - left) * i / 4
            val gy = top + (bottom - top) * i / 4
            canvas.drawLine(gx, top, gx, bottom, gridPaint)
            canvas.drawLine(left, gy, right, gy, gridPaint)
        }

        canvas.drawText(String.format("%.2f", minX), left, bottom + 34f, labelPaint)
        canvas.drawText(String.format("%.2f", maxX), right - 60f, bottom + 34f, labelPaint)
        canvas.drawText(String.format("%.2f", maxY), left - 50f, top + 10f, labelPaint)
        canvas.drawText(String.format("%.2f", minY), left - 50f, bottom, labelPaint)

        for ((x, y) in points) {
            val px = (left + (x - minX) / rangeX * (right - left)).toFloat()
            val py = (bottom - (y - minY) / rangeY * (bottom - top)).toFloat()
            canvas.drawCircle(px, py, 10f, pointPaint)
        }
    }
}
