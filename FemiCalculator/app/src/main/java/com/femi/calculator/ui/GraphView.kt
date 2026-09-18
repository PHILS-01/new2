package com.femi.calculator.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.femi.calculator.engine.ExpressionEvaluator
import kotlin.math.abs

class GraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class PlotFunction(val expression: String, val color: Int)

    private val functions = mutableListOf<PlotFunction>()
    private val evaluator = ExpressionEvaluator(ExpressionEvaluator.AngleMode.RAD)

    // View window in math-space
    private var centerX = 0.0
    private var centerY = 0.0
    private var scale = 60.0 // pixels per unit

    private val axisPaint = Paint().apply { color = Color.GRAY; strokeWidth = 2f }
    private val gridPaint = Paint().apply { color = Color.DKGRAY; strokeWidth = 1f }
    private val textPaint = Paint().apply { color = Color.LTGRAY; textSize = 24f }
    private val curvePaint = Paint().apply { strokeWidth = 5f; style = Paint.Style.STROKE; isAntiAlias = true }

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    // Pinch-zoom
    private val scaleGestureDetector = android.view.ScaleGestureDetector(context,
        object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                scale = (scale * detector.scaleFactor).coerceIn(5.0, 2000.0)
                invalidate()
                return true
            }
        })

    fun setFunctions(list: List<PlotFunction>) {
        functions.clear()
        functions.addAll(list)
        invalidate()
    }

    fun resetView() {
        centerX = 0.0; centerY = 0.0; scale = 60.0
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x; lastTouchY = event.y; isDragging = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && event.pointerCount == 1) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    centerX -= dx / scale
                    centerY += dy / scale
                    lastTouchX = event.x; lastTouchY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isDragging = false
        }
        return true
    }

    private fun mathToScreenX(x: Double) = (width / 2.0 + (x - centerX) * scale).toFloat()
    private fun mathToScreenY(y: Double) = (height / 2.0 - (y - centerY) * scale).toFloat()
    private fun screenToMathX(px: Float) = (px - width / 2.0) / scale + centerX

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#121212"))
        drawGrid(canvas)
        drawAxes(canvas)
        for (fn in functions) drawFunction(canvas, fn)
    }

    private fun drawGrid(canvas: Canvas) {
        val step = niceStep()
        var x = centerX - width / (2 * scale) - step
        val maxX = centerX + width / (2 * scale) + step
        while (x < maxX) {
            val sx = mathToScreenX(x)
            canvas.drawLine(sx, 0f, sx, height.toFloat(), gridPaint)
            x += step
        }
        var y = centerY - height / (2 * scale) - step
        val maxY = centerY + height / (2 * scale) + step
        while (y < maxY) {
            val sy = mathToScreenY(y)
            canvas.drawLine(0f, sy, width.toFloat(), sy, gridPaint)
            y += step
        }
    }

    private fun niceStep(): Double {
        // choose a grid spacing so lines are roughly 60-100px apart
        val target = 80.0 / scale
        val magnitude = Math.pow(10.0, Math.floor(Math.log10(target)))
        val residual = target / magnitude
        return when {
            residual > 5 -> 10 * magnitude
            residual > 2 -> 5 * magnitude
            else -> magnitude
        }
    }

    private fun drawAxes(canvas: Canvas) {
        val originX = mathToScreenX(0.0)
        val originY = mathToScreenY(0.0)
        canvas.drawLine(0f, originY, width.toFloat(), originY, axisPaint)
        canvas.drawLine(originX, 0f, originX, height.toFloat(), axisPaint)
    }

    private fun drawFunction(canvas: Canvas, fn: PlotFunction) {
        curvePaint.color = fn.color
        var prevX: Float? = null
        var prevY: Float? = null
        var px = 0
        while (px < width) {
            val mx = screenToMathX(px.toFloat())
            val expr = fn.expression.replace("x", "($mx)", ignoreCase = false)
            val y = try { evaluator.evaluate(expr) } catch (e: Exception) { null }
            if (y != null && !y.isNaN() && !y.isInfinite() && abs(y) < 1e6) {
                val sy = mathToScreenY(y)
                if (prevX != null && prevY != null && abs(sy - prevY) < height * 2) {
                    canvas.drawLine(prevX, prevY, px.toFloat(), sy, curvePaint)
                }
                prevX = px.toFloat(); prevY = sy
            } else {
                prevX = null; prevY = null
            }
            px += 2
        }
    }
}
