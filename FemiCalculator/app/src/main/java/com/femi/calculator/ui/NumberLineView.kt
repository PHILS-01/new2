package com.femi.calculator.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.femi.calculator.engine.EquationEngine

/** Draws a number line with a shaded ray and an open/closed circle at the threshold. */
class NumberLineView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var threshold: Double = 0.0
    private var op: EquationEngine.InequalityOp = EquationEngine.InequalityOp.GT

    private val linePaint = Paint().apply { color = Color.GRAY; strokeWidth = 4f }
    private val rayPaint = Paint().apply { color = Color.parseColor("#FF9800"); strokeWidth = 8f }
    private val tickPaint = Paint().apply { color = Color.GRAY; strokeWidth = 3f }
    private val labelPaint = Paint().apply { color = Color.LTGRAY; textSize = 24f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val circleFillPaint = Paint().apply { color = Color.parseColor("#121212"); style = Paint.Style.FILL; isAntiAlias = true }
    private val circleStrokePaint = Paint().apply { color = Color.parseColor("#FF9800"); style = Paint.Style.STROKE; strokeWidth = 5f; isAntiAlias = true }
    private val circleFilledPaint = Paint().apply { color = Color.parseColor("#FF9800"); style = Paint.Style.FILL; isAntiAlias = true }

    fun setThreshold(value: Double, operator: EquationEngine.InequalityOp) {
        threshold = value
        op = operator
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#121212"))

        val padding = 60f
        val left = padding
        val right = width - padding
        val centerY = height / 2f

        // Range spans a fixed window around the threshold for readability
        val span = 10.0
        val rangeMin = threshold - span
        val rangeMax = threshold + span

        fun toX(v: Double) = (left + (v - rangeMin) / (rangeMax - rangeMin) * (right - left)).toFloat()

        canvas.drawLine(left, centerY, right, centerY, linePaint)

        val includesPoint = op == EquationEngine.InequalityOp.GTE || op == EquationEngine.InequalityOp.LTE
        val goesRight = op == EquationEngine.InequalityOp.GT || op == EquationEngine.InequalityOp.GTE
        val thresholdX = toX(threshold)

        if (goesRight) {
            canvas.drawLine(thresholdX, centerY, right, centerY, rayPaint)
        } else {
            canvas.drawLine(left, centerY, thresholdX, centerY, rayPaint)
        }

        // tick marks at integer steps
        var t = Math.ceil(rangeMin).toInt()
        while (t <= rangeMax) {
            val x = toX(t.toDouble())
            canvas.drawLine(x, centerY - 12f, x, centerY + 12f, tickPaint)
            if (t % 2 == 0) canvas.drawText(t.toString(), x, centerY + 44f, labelPaint)
            t++
        }

        // threshold circle: filled if the point is included (>=, <=), open otherwise
        if (includesPoint) {
            canvas.drawCircle(thresholdX, centerY, 16f, circleFilledPaint)
        } else {
            canvas.drawCircle(thresholdX, centerY, 16f, circleFillPaint)
            canvas.drawCircle(thresholdX, centerY, 16f, circleStrokePaint)
        }
    }
}
