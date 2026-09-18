package com.femi.calculator.engine

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

class AnovaException(message: String) : Exception(message)

object AnovaEngine {

    data class Result(
        val groupCount: Int,
        val totalCount: Int,
        val ssBetween: Double,
        val ssWithin: Double,
        val dfBetween: Int,
        val dfWithin: Int,
        val msBetween: Double,
        val msWithin: Double,
        val fStatistic: Double,
        val pValue: Double
    )

    /** One-way ANOVA across 2 or more groups. */
    fun oneWayAnova(groups: List<List<Double>>): Result {
        val validGroups = groups.filter { it.isNotEmpty() }
        if (validGroups.size < 2) throw AnovaException("Need at least 2 groups with data")

        val allValues = validGroups.flatten()
        val n = allValues.size
        val k = validGroups.size
        val grandMean = allValues.sum() / n

        val ssBetween = validGroups.sumOf { g ->
            val groupMean = g.sum() / g.size
            g.size * (groupMean - grandMean) * (groupMean - grandMean)
        }
        val ssWithin = validGroups.sumOf { g ->
            val groupMean = g.sum() / g.size
            g.sumOf { (it - groupMean) * (it - groupMean) }
        }

        val dfBetween = k - 1
        val dfWithin = n - k
        if (dfWithin <= 0) throw AnovaException("Not enough data points for the number of groups")

        val msBetween = ssBetween / dfBetween
        val msWithin = ssWithin / dfWithin
        val fStatistic = if (msWithin == 0.0) Double.POSITIVE_INFINITY else msBetween / msWithin
        val pValue = fDistributionPValue(fStatistic, dfBetween, dfWithin)

        return Result(k, n, ssBetween, ssWithin, dfBetween, dfWithin, msBetween, msWithin, fStatistic, pValue)
    }

    data class TwoWayResult(
        val aLevels: Int,
        val bLevels: Int,
        val replications: Int,
        val ssA: Double,
        val ssB: Double,
        val ssAB: Double,
        val ssWithin: Double,
        val dfA: Int,
        val dfB: Int,
        val dfAB: Int,
        val dfWithin: Int,
        val msA: Double,
        val msB: Double,
        val msAB: Double,
        val msWithin: Double,
        val fA: Double,
        val fB: Double,
        val fAB: Double,
        val pA: Double,
        val pB: Double,
        val pAB: Double
    )

    /**
     * Balanced two-way ANOVA with interaction. Requires every (A level, B level)
     * combination to be present with the same number of replications.
     */
    fun twoWayAnova(
        aOrder: List<String>,
        bOrder: List<String>,
        cells: Map<Pair<String, String>, List<Double>>
    ): TwoWayResult {
        val a = aOrder.size
        val b = bOrder.size
        if (a < 2) throw AnovaException("Factor A needs at least 2 levels")
        if (b < 2) throw AnovaException("Factor B needs at least 2 levels")

        val missing = mutableListOf<String>()
        var n = -1
        for (av in aOrder) {
            for (bv in bOrder) {
                val values = cells[av to bv]
                if (values == null || values.isEmpty()) {
                    missing.add("$av / $bv")
                    continue
                }
                if (n == -1) n = values.size
                else if (values.size != n) throw AnovaException(
                    "Every cell needs the same number of replications (found ${values.size} and $n)"
                )
            }
        }
        if (missing.isNotEmpty()) throw AnovaException("Missing cell(s): ${missing.joinToString(", ")}")
        if (n < 2) throw AnovaException("Each cell needs at least 2 replications for a within-cell error term")

        val allValues = cells.values.flatten()
        val grandMean = allValues.sum() / allValues.size

        val aMeans = aOrder.associateWith { av ->
            val vals = bOrder.flatMap { bv -> cells[av to bv]!! }
            vals.sum() / vals.size
        }
        val bMeans = bOrder.associateWith { bv ->
            val vals = aOrder.flatMap { av -> cells[av to bv]!! }
            vals.sum() / vals.size
        }
        val cellMeans = aOrder.flatMap { av -> bOrder.map { bv -> (av to bv) to (cells[av to bv]!!.sum() / n) } }.toMap()

        val ssA = b * n * aOrder.sumOf { av -> (aMeans[av]!! - grandMean).let { it * it } }
        val ssB = a * n * bOrder.sumOf { bv -> (bMeans[bv]!! - grandMean).let { it * it } }
        val ssCells = n * aOrder.sumOf { av -> bOrder.sumOf { bv ->
            val d = cellMeans[av to bv]!! - grandMean
            d * d
        } }
        val ssAB = ssCells - ssA - ssB
        val ssWithin = aOrder.sumOf { av -> bOrder.sumOf { bv ->
            val cellMean = cellMeans[av to bv]!!
            cells[av to bv]!!.sumOf { (it - cellMean) * (it - cellMean) }
        } }

        val dfA = a - 1
        val dfB = b - 1
        val dfAB = (a - 1) * (b - 1)
        val dfWithin = a * b * (n - 1)

        val msA = ssA / dfA
        val msB = ssB / dfB
        val msAB = ssAB / dfAB
        val msWithin = ssWithin / dfWithin

        val fA = if (msWithin == 0.0) Double.POSITIVE_INFINITY else msA / msWithin
        val fB = if (msWithin == 0.0) Double.POSITIVE_INFINITY else msB / msWithin
        val fAB = if (msWithin == 0.0) Double.POSITIVE_INFINITY else msAB / msWithin

        val pA = fDistributionPValue(fA, dfA, dfWithin)
        val pB = fDistributionPValue(fB, dfB, dfWithin)
        val pAB = fDistributionPValue(fAB, dfAB, dfWithin)

        return TwoWayResult(
            a, b, n, ssA, ssB, ssAB, ssWithin, dfA, dfB, dfAB, dfWithin,
            msA, msB, msAB, msWithin, fA, fB, fAB, pA, pB, pAB
        )
    }

    /** Upper-tail p-value for the F-distribution: P(F >= f) with d1, d2 degrees of freedom. */
    private fun fDistributionPValue(f: Double, d1: Int, d2: Int): Double {
        if (f.isInfinite()) return 0.0
        if (f <= 0.0) return 1.0
        val x = d1.toDouble() * f / (d1.toDouble() * f + d2.toDouble())
        return 1.0 - regularizedIncompleteBeta(x, d1 / 2.0, d2 / 2.0)
    }

    // ---- Numerical helpers (Lanczos log-gamma + continued-fraction incomplete beta) ----

    private fun logGamma(x: Double): Double {
        val cof = doubleArrayOf(
            76.18009172947146, -86.50532032941677, 24.01409824083091,
            -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5
        )
        var y = x
        var tmp = x + 5.5
        tmp -= (x + 0.5) * ln(tmp)
        var ser = 1.000000000190015
        for (j in 0..5) {
            y += 1.0
            ser += cof[j] / y
        }
        return -tmp + ln(2.5066282746310005 * ser / x)
    }

    private fun betaContinuedFraction(x: Double, a: Double, b: Double): Double {
        val maxIter = 200
        val eps = 3.0e-9
        val fpMin = 1.0e-30
        val qab = a + b
        val qap = a + 1.0
        val qam = a - 1.0
        var c = 1.0
        var d = 1.0 - qab * x / qap
        if (abs(d) < fpMin) d = fpMin
        d = 1.0 / d
        var h = d
        for (m in 1..maxIter) {
            val m2 = 2 * m
            var aa = m * (b - m) * x / ((qam + m2) * (a + m2))
            d = 1.0 + aa * d
            if (abs(d) < fpMin) d = fpMin
            c = 1.0 + aa / c
            if (abs(c) < fpMin) c = fpMin
            d = 1.0 / d
            h *= d * c
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
            d = 1.0 + aa * d
            if (abs(d) < fpMin) d = fpMin
            c = 1.0 + aa / c
            if (abs(c) < fpMin) c = fpMin
            d = 1.0 / d
            val del = d * c
            h *= del
            if (abs(del - 1.0) < eps) break
        }
        return h
    }

    /** Regularized incomplete beta function I_x(a, b). */
    private fun regularizedIncompleteBeta(x: Double, a: Double, b: Double): Double {
        if (x <= 0.0) return 0.0
        if (x >= 1.0) return 1.0
        val bt = exp(
            logGamma(a + b) - logGamma(a) - logGamma(b) +
                a * ln(x) + b * ln(1.0 - x)
        )
        return if (x < (a + 1.0) / (a + b + 2.0)) {
            bt * betaContinuedFraction(x, a, b) / a
        } else {
            1.0 - bt * betaContinuedFraction(1.0 - x, b, a) / b
        }
    }
}
