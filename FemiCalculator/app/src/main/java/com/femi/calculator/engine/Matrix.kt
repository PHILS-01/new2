package com.femi.calculator.engine

class MatrixException(message: String) : Exception(message)

class Matrix(val rows: Int, val cols: Int) {
    val data: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }

    companion object {
        fun fromRows(values: List<List<Double>>): Matrix {
            val r = values.size
            val c = if (r > 0) values[0].size else 0
            val m = Matrix(r, c)
            for (i in 0 until r) {
                if (values[i].size != c) throw MatrixException("Rows must have equal length")
                for (j in 0 until c) m.data[i][j] = values[i][j]
            }
            return m
        }

        fun identity(n: Int): Matrix {
            val m = Matrix(n, n)
            for (i in 0 until n) m.data[i][i] = 1.0
            return m
        }
    }

    operator fun plus(o: Matrix): Matrix {
        if (rows != o.rows || cols != o.cols) throw MatrixException("Dimension mismatch for addition")
        val res = Matrix(rows, cols)
        for (i in 0 until rows) for (j in 0 until cols) res.data[i][j] = data[i][j] + o.data[i][j]
        return res
    }

    operator fun minus(o: Matrix): Matrix {
        if (rows != o.rows || cols != o.cols) throw MatrixException("Dimension mismatch for subtraction")
        val res = Matrix(rows, cols)
        for (i in 0 until rows) for (j in 0 until cols) res.data[i][j] = data[i][j] - o.data[i][j]
        return res
    }

    operator fun times(o: Matrix): Matrix {
        if (cols != o.rows) throw MatrixException("Dimension mismatch for multiplication")
        val res = Matrix(rows, o.cols)
        for (i in 0 until rows) {
            for (j in 0 until o.cols) {
                var sum = 0.0
                for (k in 0 until cols) sum += data[i][k] * o.data[k][j]
                res.data[i][j] = sum
            }
        }
        return res
    }

    fun transpose(): Matrix {
        val res = Matrix(cols, rows)
        for (i in 0 until rows) for (j in 0 until cols) res.data[j][i] = data[i][j]
        return res
    }

    fun determinant(): Double {
        if (rows != cols) throw MatrixException("Determinant requires a square matrix")
        val n = rows
        val a = Array(n) { i -> data[i].copyOf() }
        var det = 1.0
        for (col in 0 until n) {
            var pivot = col
            for (r in col until n) if (kotlin.math.abs(a[r][col]) > kotlin.math.abs(a[pivot][col])) pivot = r
            if (kotlin.math.abs(a[pivot][col]) < 1e-12) return 0.0
            if (pivot != col) { val tmp = a[pivot]; a[pivot] = a[col]; a[col] = tmp; det = -det }
            det *= a[col][col]
            for (r in col + 1 until n) {
                val factor = a[r][col] / a[col][col]
                for (c in col until n) a[r][c] -= factor * a[col][c]
            }
        }
        return det
    }

    fun inverse(): Matrix {
        if (rows != cols) throw MatrixException("Inverse requires a square matrix")
        val n = rows
        val aug = Array(n) { i -> DoubleArray(2 * n).also { row ->
            for (j in 0 until n) row[j] = data[i][j]
            row[n + i] = 1.0
        } }
        for (col in 0 until n) {
            var pivot = col
            for (r in col until n) if (kotlin.math.abs(aug[r][col]) > kotlin.math.abs(aug[pivot][col])) pivot = r
            if (kotlin.math.abs(aug[pivot][col]) < 1e-12) throw MatrixException("Matrix is singular; no inverse exists")
            val tmp = aug[pivot]; aug[pivot] = aug[col]; aug[col] = tmp
            val pivotVal = aug[col][col]
            for (c in 0 until 2 * n) aug[col][c] /= pivotVal
            for (r in 0 until n) {
                if (r == col) continue
                val factor = aug[r][col]
                for (c in 0 until 2 * n) aug[r][c] -= factor * aug[col][c]
            }
        }
        val res = Matrix(n, n)
        for (i in 0 until n) for (j in 0 until n) res.data[i][j] = aug[i][n + j]
        return res
    }

    /** Solve Ax = b using Gaussian elimination with partial pivoting. */
    fun solveLinearSystem(b: DoubleArray): DoubleArray {
        if (rows != cols) throw MatrixException("System must have a square coefficient matrix")
        if (b.size != rows) throw MatrixException("Constant vector size mismatch")
        val n = rows
        val aug = Array(n) { i -> DoubleArray(n + 1).also { row ->
            for (j in 0 until n) row[j] = data[i][j]
            row[n] = b[i]
        } }
        for (col in 0 until n) {
            var pivot = col
            for (r in col until n) if (kotlin.math.abs(aug[r][col]) > kotlin.math.abs(aug[pivot][col])) pivot = r
            if (kotlin.math.abs(aug[pivot][col]) < 1e-12) throw MatrixException("System has no unique solution")
            val tmp = aug[pivot]; aug[pivot] = aug[col]; aug[col] = tmp
            for (r in col + 1 until n) {
                val factor = aug[r][col] / aug[col][col]
                for (c in col..n) aug[r][c] -= factor * aug[col][c]
            }
        }
        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var sum = aug[i][n]
            for (j in i + 1 until n) sum -= aug[i][j] * x[j]
            x[i] = sum / aug[i][i]
        }
        return x
    }

    fun toDisplayString(): String =
        data.joinToString("\n") { row -> row.joinToString("  ") { formatNum(it) } }

    private fun formatNum(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.4f", v)
}
