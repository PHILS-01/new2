package com.femi.calculator.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.TextView
import com.femi.calculator.engine.Matrix

/**
 * Displays a matrix as a grid of cells (editable EditTexts, or read-only TextViews)
 * with a large bracket drawn on either side, and lets the grid dimensions be
 * changed at any time via [configure].
 */
class BracketedMatrixView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val grid = GridLayout(context)
    private val bracketPaint = Paint().apply {
        color = Color.parseColor("#FF9800")
        strokeWidth = dpToPx(2.5f)
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val bracketMargin = dpToPx(14f)
    private var rows = 2
    private var cols = 2
    private var editable = true
    val matrixRows: Int get() = rows
    val matrixCols: Int get() = cols

    init {
        setWillNotDraw(false)
        val gridParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        gridParams.gravity = Gravity.CENTER
        grid.layoutParams = gridParams
        addView(grid)
        setPadding(bracketMargin.toInt(), dpToPx(8f).toInt(), bracketMargin.toInt(), dpToPx(8f).toInt())
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density

    /** (Re)builds the grid at the given size. Existing values are preserved where possible. */
    fun configure(newRows: Int, newCols: Int, editableCells: Boolean = true) {
        val previous = if (editable) currentValuesOrNull() else null
        rows = newRows.coerceIn(1, 6)
        cols = newCols.coerceIn(1, 6)
        editable = editableCells
        grid.removeAllViews()
        grid.rowCount = rows
        grid.columnCount = cols
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cellSize = dpToPx(46f).toInt()
                val lp = GridLayout.LayoutParams(
                    GridLayout.spec(r), GridLayout.spec(c)
                )
                lp.width = cellSize
                lp.height = cellSize
                lp.setMargins(dpToPx(2f).toInt(), dpToPx(2f).toInt(), dpToPx(2f).toInt(), dpToPx(2f).toInt())

                if (editable) {
                    val et = EditText(context)
                    et.layoutParams = lp
                    et.gravity = Gravity.CENTER
                    et.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                    et.setBackgroundColor(Color.parseColor("#262626"))
                    et.setTextColor(Color.parseColor("#F5F5F5"))
                    et.textSize = 14f
                    val prefill = previous?.getOrNull(r)?.getOrNull(c)
                    et.setText(prefill ?: "0")
                    grid.addView(et)
                } else {
                    val tv = TextView(context)
                    tv.layoutParams = lp
                    tv.gravity = Gravity.CENTER
                    tv.setTextColor(Color.parseColor("#F5F5F5"))
                    tv.textSize = 13f
                    tv.text = ""
                    grid.addView(tv)
                }
            }
        }
        requestLayout()
        invalidate()
    }

    private fun currentValuesOrNull(): Array<Array<String>>? {
        if (grid.childCount == 0) return null
        return Array(rows) { r -> Array(cols) { c ->
            val idx = r * cols + c
            (grid.getChildAt(idx) as? EditText)?.text?.toString() ?: ""
        } }
    }

    /** Reads the current editable grid into a Matrix, throwing if any cell isn't a valid number. */
    fun toMatrix(): Matrix {
        val m = Matrix(rows, cols)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                val et = grid.getChildAt(idx) as EditText
                m.data[r][c] = et.text.toString().trim().toDouble()
            }
        }
        return m
    }

    /** Displays a computed Matrix in read-only cells, resizing the grid to match. */
    fun showResult(m: Matrix) {
        configure(m.rows, m.cols, editableCells = false)
        for (r in 0 until m.rows) {
            for (c in 0 until m.cols) {
                val idx = r * m.cols + c
                val tv = grid.getChildAt(idx) as TextView
                tv.text = formatCell(m.data[r][c])
            }
        }
    }

    private fun formatCell(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.3f", v)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val top = dpToPx(2f)
        val bottom = h - dpToPx(2f)
        val tick = dpToPx(9f)

        val leftX = bracketMargin * 0.6f
        canvas.drawLine(leftX, top, leftX, bottom, bracketPaint)
        canvas.drawLine(leftX, top, leftX + tick, top, bracketPaint)
        canvas.drawLine(leftX, bottom, leftX + tick, bottom, bracketPaint)

        val rightX = width - bracketMargin * 0.6f
        canvas.drawLine(rightX, top, rightX, bottom, bracketPaint)
        canvas.drawLine(rightX, top, rightX - tick, top, bracketPaint)
        canvas.drawLine(rightX, bottom, rightX - tick, bottom, bracketPaint)
    }
}
