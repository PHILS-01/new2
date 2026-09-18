package com.femi.calculator.engine

class EquationException(message: String) : Exception(message)

object EquationEngine {

    /** Solve ax + b = 0 for x. */
    fun solveLinear(a: Double, b: Double): Double {
        if (a == 0.0) throw EquationException("Not a valid linear equation ('a' cannot be 0)")
        return -b / a
    }

    data class SimultaneousResult(val x: Double, val y: Double)

    /** Solve ax + by = c, dx + ey = f for x and y. */
    fun solveSimultaneous(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double): SimultaneousResult {
        val determinant = a * e - b * d
        if (determinant == 0.0) throw EquationException("No unique solution (equations are parallel or identical)")
        val x = (c * e - b * f) / determinant
        val y = (a * f - c * d) / determinant
        return SimultaneousResult(x, y)
    }

    enum class InequalityOp { GT, LT, GTE, LTE }

    data class InequalityResult(val threshold: Double, val op: InequalityOp)

    /** Solve ax + b [op] 0 for x. Flips the inequality direction if a is negative. */
    fun solveLinearInequality(a: Double, b: Double, op: InequalityOp): InequalityResult {
        if (a == 0.0) throw EquationException("Not a valid inequality ('a' cannot be 0)")
        val threshold = -b / a
        val finalOp = if (a < 0) flip(op) else op
        return InequalityResult(threshold, finalOp)
    }

    private fun flip(op: InequalityOp): InequalityOp = when (op) {
        InequalityOp.GT -> InequalityOp.LT
        InequalityOp.LT -> InequalityOp.GT
        InequalityOp.GTE -> InequalityOp.LTE
        InequalityOp.LTE -> InequalityOp.GTE
    }

    fun opSymbol(op: InequalityOp): String = when (op) {
        InequalityOp.GT -> ">"
        InequalityOp.LT -> "<"
        InequalityOp.GTE -> "≥"
        InequalityOp.LTE -> "≤"
    }

    /** Central-difference numerical derivative of f at x0. */
    fun numericalDerivative(f: (Double) -> Double, x0: Double, h: Double = 1e-5): Double =
        (f(x0 + h) - f(x0 - h)) / (2 * h)

    /** Definite integral of f from a to b via Simpson's rule. */
    fun numericalIntegral(f: (Double) -> Double, a: Double, b: Double, steps: Int = 1000): Double {
        val n = if (steps % 2 == 0) steps else steps + 1
        val h = (b - a) / n
        var sum = f(a) + f(b)
        for (i in 1 until n) {
            val x = a + i * h
            sum += if (i % 2 == 0) 2 * f(x) else 4 * f(x)
        }
        return sum * h / 3
    }

    data class PolynomialCoefficients(val a: Double, val b: Double, val c: Double)

    /**
     * Recovers ax²+bx+c from f by sampling at x=0,1,2, then verifies the fit at x=3.
     * Returns null if f isn't actually a degree ≤ 2 polynomial in x (e.g. contains sin(x), 1/x, etc.),
     * so callers never silently present a wrong "solution" for equations we can't really solve.
     */
    fun tryExtractPolynomial(f: (Double) -> Double): PolynomialCoefficients? {
        val f0 = f(0.0); val f1 = f(1.0); val f2 = f(2.0); val f3 = f(3.0)
        val a = (f2 - 2 * f1 + f0) / 2.0
        val b = f1 - f0 - a
        val c = f0
        val predicted3 = a * 9 + b * 3 + c
        val tolerance = 1e-6 * maxOf(1.0, kotlin.math.abs(f3))
        return if (kotlin.math.abs(predicted3 - f3) < tolerance) PolynomialCoefficients(a, b, c) else null
    }

    enum class QuadIneqType { ALL_REALS, NO_SOLUTION, INTERVAL, OUTER }

    data class QuadIneqResult(
        val type: QuadIneqType,
        val lo: Double = 0.0,
        val hi: Double = 0.0,
        val inclusive: Boolean = false
    )

    /** Solve ax²+bx+c [op] 0 for x. */
    fun solveQuadraticInequality(a: Double, b: Double, c: Double, op: InequalityOp): QuadIneqResult {
        val discriminant = b * b - 4 * a * c
        val inclusive = op == InequalityOp.GTE || op == InequalityOp.LTE
        val wantPositive = op == InequalityOp.GT || op == InequalityOp.GTE

        if (discriminant < 0) {
            val alwaysPositive = a > 0
            return if (alwaysPositive == wantPositive) QuadIneqResult(QuadIneqType.ALL_REALS)
                   else QuadIneqResult(QuadIneqType.NO_SOLUTION)
        }
        val sqrtD = kotlin.math.sqrt(discriminant)
        val r1 = (-b - sqrtD) / (2 * a)
        val r2 = (-b + sqrtD) / (2 * a)
        val lo = minOf(r1, r2)
        val hi = maxOf(r1, r2)
        val positiveIsOutside = a > 0
        return if (wantPositive == positiveIsOutside) QuadIneqResult(QuadIneqType.OUTER, lo, hi, inclusive)
               else QuadIneqResult(QuadIneqType.INTERVAL, lo, hi, inclusive)
    }
}
