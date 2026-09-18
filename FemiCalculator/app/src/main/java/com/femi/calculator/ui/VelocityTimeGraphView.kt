package com.femi.calculator.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/** Plots a velocity-time graph from (t, v) points, connected in order of increasing time. */
class VelocityTimeGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var points: List<Pair<Double, Double>> = emptyList()

    private val axisPaint = Paint().apply { color = Color.GRAY; strokeWidth = 2f }
    private val gridPaint = Paint().apply { color = Color.DKGRAY; strokeWidth = 1f }
    private val linePaint = Paint().apply { color = Color.parseColor("#FF9800"); strokeWidth = 5f; style = Paint.Style.STROKE; isAntiAlias = true }
    private val fillPaint = Paint().apply { color = Color.parseColor("#33FF9800"); style = Paint.Style.FILL }
    private val pointPaint = Paint().apply { color = Color.parseColor("#FF9800"); isAntiAlias = true }
    private val labelPaint = Paint().apply { color = Color.LTGRAY; textSize = 24f; isAntiAlias = true }

    fun setData(data: List<Pair<Double, Double>>) {
        points = data.sortedBy { it.first }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#121212"))
        if (points.size < 2) return

        val padding = 60f
        val minT = points.minOf { it.first }
        val maxT = points.maxOf { it.first }
        val minV = minOf(0.0, points.minOf { it.second })
        val maxV = points.maxOf { it.second }
        val rangeT = (maxT - minT).let { if (it == 0.0) 1.0 else it }
        val rangeV = (maxV - minV).let { if (it == 0.0) 1.0 else it }

        val left = padding
        val right = width - padding
        val top = padding
        val bottom = height - padding

        for (i in 0..4) {
            val gx = left + (right - left) * i / 4
            val gy = top + (bottom - top) * i / 4
            canvas.drawLine(gx, top, gx, bottom, gridPaint)
            canvas.drawLine(left, gy, right, gy, gridPaint)
        }

        fun toScreenX(t: Double) = (left + (t - minT) / rangeT * (right - left)).toFloat()
        fun toScreenY(v: Double) = (bottom - (v - minV) / rangeV * (bottom - top)).toFloat()

        val zeroY = toScreenY(0.0)
        canvas.drawLine(left, zeroY, right, zeroY, axisPaint)
        canvas.drawLine(left, top, left, bottom, axisPaint)

        // shaded area under the curve (represents displacement)
        val fillPath = Path()
        fillPath.moveTo(toScreenX(points.first().first), zeroY)
        for (p in points) fillPath.lineTo(toScreenX(p.first), toScreenY(p.second))
        fillPath.lineTo(toScreenX(points.last().first), zeroY)
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)

        val linePath = Path()
        linePath.moveTo(toScreenX(points.first().first), toScreenY(points.first().second))
        for (p in points.drop(1)) linePath.lineTo(toScreenX(p.first), toScreenY(p.second))
        canvas.drawPath(linePath, linePaint)

        for (p in points) {
            canvas.drawCircle(toScreenX(p.first), toScreenY(p.second), 8f, pointPaint)
        }

        canvas.drawText("t (s)", right - 60f, bottom + 40f, labelPaint)
        canvas.drawText("v (m/s)", left - 45f, top - 10f, labelPaint)
        canvas.drawText(String.format("%.2f", minT), left, bottom + 34f, labelPaint)
        canvas.drawText(String.format("%.2f", maxT), right - 50f, bottom + 34f, labelPaint)
        canvas.drawText(String.format("%.2f", maxV), left - 50f, top + 10f, labelPaint)
    }
}
